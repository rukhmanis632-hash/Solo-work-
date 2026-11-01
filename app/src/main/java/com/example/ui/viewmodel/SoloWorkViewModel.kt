package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ClientItem
import com.example.data.model.InvoiceItem
import com.example.data.model.ProjectItem
import com.example.data.model.TaskItem
import com.example.data.model.TimeLogItem
import com.example.data.model.UserSettingsItem
import com.example.data.repository.SoloWorkRepository
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class SoloWorkTab(val title: String) {
    Dashboard("Dashboard"),
    Projects("Projects"),
    Invoices("Invoices"),
    Clients("Clients"),
    TaskBoard("Task Board"),
    TimeTracker("Time Tracker"),
    Analytics("Analytics"),
    AiCopilot("AI Copilot"),
    Settings("Settings")
}

sealed interface AiStreamState {
    data object Idle : AiStreamState
    data object Loading : AiStreamState
    data class Streaming(val text: String) : AiStreamState
    data class Success(val text: String) : AiStreamState
    data class Error(val message: String) : AiStreamState
}

data class AiGeneratedProjectBlueprint(
    val projectName: String,
    val clientName: String,
    val category: String,
    val estimatedBudgetUsd: Long,
    val executiveSummary: String,
    val milestones: List<String>
)

sealed interface AiBlueprintState {
    data object Idle : AiBlueprintState
    data object Loading : AiBlueprintState
    data class Success(val blueprint: AiGeneratedProjectBlueprint) : AiBlueprintState
    data class Error(val message: String) : AiBlueprintState
}

data class AiChatMessage(
    val id: String,
    val role: String, // "user" or "assistant"
    val text: String,
    val timestamp: String
)

data class AiGeneratedClientDoc(
    val documentType: String,
    val subjectLine: String,
    val recipientName: String,
    val bodyText: String,
    val keyHighlights: List<String>,
    val suggestedFollowUpDate: String
)

sealed interface AiClientDocState {
    data object Idle : AiClientDocState
    data object Loading : AiClientDocState
    data class Success(val doc: AiGeneratedClientDoc) : AiClientDocState
    data class Error(val message: String) : AiClientDocState
}

data class NotificationEntry(
    val id: String,
    val title: String,
    val subtitle: String,
    val timeAgo: String,
    val unread: Boolean = true
)

class SoloWorkViewModel(
    private val repository: SoloWorkRepository,
    private val currentUserId: String
) : ViewModel() {

    // Navigation & Global Search State
    private val _activeTab = MutableStateFlow(SoloWorkTab.Dashboard)
    val activeTab: StateFlow<SoloWorkTab> = _activeTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _projectPeriodFilter = MutableStateFlow("All Periods")
    val projectPeriodFilter: StateFlow<String> = _projectPeriodFilter.asStateFlow()

    private val _isAppearancePanelOpen = MutableStateFlow(false)
    val isAppearancePanelOpen: StateFlow<Boolean> = _isAppearancePanelOpen.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _notificationsReadAll = MutableStateFlow(false)

    // Live Theme & Layout Customization State (instant local update + Firestore sync)
    private val _localSettings = MutableStateFlow(
        UserSettingsItem(
            id = currentUserId,
            userId = currentUserId,
            themeMode = "Dark",
            themePreset = "Classic Dark",
            accentColor = "Indigo",
            fontFamily = "Inter",
            customizeLayoutStyle = true,
            sidebarCompact = false,
            displayName = "Alex Miller",
            roleTitle = "Principal Freelancer"
        )
    )
    val localSettings: StateFlow<UserSettingsItem> = _localSettings.asStateFlow()

    // Live Stopwatch State for Time Tracker
    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _timerProjectName = MutableStateFlow("")
    val timerProjectName: StateFlow<String> = _timerProjectName.asStateFlow()

    private val _timerTaskNote = MutableStateFlow("")
    val timerTaskNote: StateFlow<String> = _timerTaskNote.asStateFlow()

    private val _timerHourlyRate = MutableStateFlow(110L)
    val timerHourlyRate: StateFlow<Long> = _timerHourlyRate.asStateFlow()

    private var timerJob: Job? = null

    // --- FIREBASE AI LOGIC (GEMINI 3.8 FLASH) LAZY MODELS & STATE ---

    private val streamingAiModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    private val projectBlueprintSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "projectName" to Schema.string(description = "Concise commercial project title"),
                "clientName" to Schema.string(description = "Client or studio name"),
                "category" to Schema.enumeration(
                    values = listOf("Web Design", "Branding", "Development", "Strategy"),
                    description = "Primary project discipline"
                ),
                "estimatedBudgetUsd" to Schema.integer(description = "Recommended project budget in USD"),
                "executiveSummary" to Schema.string(description = "2-3 sentence executive scope summary"),
                "milestones" to Schema.array(
                    items = Schema.string(description = "Actionable deliverable milestone task title"),
                    description = "4 to 6 sequential project milestones for the Kanban task board"
                )
            )
        )
    }

    private val structuredBlueprintModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = projectBlueprintSchema
            }
        )
    }

    private val clientDocSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "documentType" to Schema.string(description = "Type of client document or email"),
                "subjectLine" to Schema.string(description = "Compelling professional email subject line"),
                "recipientName" to Schema.string(description = "Client contact or company name"),
                "bodyText" to Schema.string(description = "Full polished email or proposal body text"),
                "keyHighlights" to Schema.array(
                    items = Schema.string(description = "Bullet point takeaway or deliverable"),
                    description = "3 to 4 key bullet highlights included in the communication"
                ),
                "suggestedFollowUpDate" to Schema.string(description = "Recommended follow-up date")
            )
        )
    }

    private val structuredClientDocModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = clientDocSchema
            }
        )
    }

    private val _aiStreamState = MutableStateFlow<AiStreamState>(AiStreamState.Idle)
    val aiStreamState: StateFlow<AiStreamState> = _aiStreamState.asStateFlow()

    private val _aiBlueprintState = MutableStateFlow<AiBlueprintState>(AiBlueprintState.Idle)
    val aiBlueprintState: StateFlow<AiBlueprintState> = _aiBlueprintState.asStateFlow()

    private val _aiClientDocState = MutableStateFlow<AiClientDocState>(AiClientDocState.Idle)
    val aiClientDocState: StateFlow<AiClientDocState> = _aiClientDocState.asStateFlow()

    private val _aiChatMessages = MutableStateFlow(
        listOf(
            AiChatMessage(
                id = "welcome_1",
                role = "assistant",
                text = "Hi! I'm your SoloWork AI Copilot powered by Gemini 3.8 Flash. You can ask me to audit your freelance revenue, draft client proposals, follow up on pending invoices, or scope a brand-new project with Kanban tasks in one tap!",
                timestamp = "Ready"
            )
        )
    )
    val aiChatMessages: StateFlow<List<AiChatMessage>> = _aiChatMessages.asStateFlow()

    // Two-Tier Kotlin Flow Error Handling Architecture for all Firestore streams
    val projectsState: StateFlow<UiState<List<ProjectItem>>> = repository.observeProjects(currentUserId)
        .map<List<ProjectItem>, UiState<List<ProjectItem>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing projects", e)
            emit(UiState.Error(e.message ?: "Failed to load projects"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val clientsState: StateFlow<UiState<List<ClientItem>>> = repository.observeClients(currentUserId)
        .map<List<ClientItem>, UiState<List<ClientItem>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing clients", e)
            emit(UiState.Error(e.message ?: "Failed to load clients"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val invoicesState: StateFlow<UiState<List<InvoiceItem>>> = repository.observeInvoices(currentUserId)
        .map<List<InvoiceItem>, UiState<List<InvoiceItem>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing invoices", e)
            emit(UiState.Error(e.message ?: "Failed to load invoices"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val tasksState: StateFlow<UiState<List<TaskItem>>> = repository.observeTasks(currentUserId)
        .map<List<TaskItem>, UiState<List<TaskItem>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing tasks", e)
            emit(UiState.Error(e.message ?: "Failed to load tasks"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val timeLogsState: StateFlow<UiState<List<TimeLogItem>>> = repository.observeTimeLogs(currentUserId)
        .map<List<TimeLogItem>, UiState<List<TimeLogItem>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing timeLogs", e)
            emit(UiState.Error(e.message ?: "Failed to load time logs"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    // Dynamically detects whether the user's workspace is currently showing Sample / Example Data
    val isViewingExampleData: StateFlow<Boolean> = combine(
        projectsState,
        clientsState,
        invoicesState,
        tasksState,
        timeLogsState
    ) { projState, clientState, invState, taskState, logState ->
        val projs = (projState as? UiState.Success)?.data.orEmpty()
        val clients = (clientState as? UiState.Success)?.data.orEmpty()
        val invs = (invState as? UiState.Success)?.data.orEmpty()
        val tasks = (taskState as? UiState.Success)?.data.orEmpty()
        val logs = (logState as? UiState.Success)?.data.orEmpty()

        projs.any { it.id.startsWith("example_") } ||
            clients.any { it.id.startsWith("example_") } ||
            invs.any { it.id.startsWith("example_") } ||
            tasks.any { it.id.startsWith("example_") } ||
            logs.any { it.id.startsWith("example_") }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    // Dynamic notifications derived strictly from real Firestore records
    val notifications: StateFlow<List<NotificationEntry>> = combine(
        projectsState,
        invoicesState,
        tasksState,
        _notificationsReadAll
    ) { projState, invState, taskState, readAll ->
        val projs = (projState as? UiState.Success)?.data.orEmpty()
        val invs = (invState as? UiState.Success)?.data.orEmpty()
        val tasks = (taskState as? UiState.Success)?.data.orEmpty()

        val items = mutableListOf<NotificationEntry>()
        invs.filter { it.status == "Pending" || it.status == "Overdue" }.take(2).forEach { inv ->
            items.add(
                NotificationEntry(
                    id = "inv_${inv.id}",
                    title = "${inv.status} Invoice ${inv.invoiceNumber}",
                    subtitle = "${inv.clientName} • $${String.format(Locale.US, "%,.2f", inv.amountCents / 100.0)} due ${inv.dueDate}",
                    timeAgo = "Active",
                    unread = !readAll
                )
            )
        }
        projs.filter { it.progress >= 70L }.take(2).forEach { proj ->
            items.add(
                NotificationEntry(
                    id = "proj_${proj.id}",
                    title = "Project Milestone: ${proj.name}",
                    subtitle = "${proj.clientName} deliverable is at ${proj.progress}% completion",
                    timeAgo = "Updated",
                    unread = !readAll
                )
            )
        }
        val openHighTasks = tasks.count { !it.completed && it.priority == "High" }
        if (openHighTasks > 0) {
            items.add(
                NotificationEntry(
                    id = "tasks_high",
                    title = "$openHighTasks High-Priority Tasks Open",
                    subtitle = "Review your Kanban Task Board to stay on schedule",
                    timeAgo = "Today",
                    unread = !readAll
                )
            )
        }
        items
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val hasUnreadNotifications: StateFlow<Boolean> = notifications
        .map { list -> list.any { it.unread } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    init {
        viewModelScope.launch {
            repository.ensureSeededOrReset(forceReset = false)
        }
        viewModelScope.launch {
            repository.observeUserSettings(currentUserId)
                .catch { e -> Log.w(TAG, "Error observing settings", e) }
                .collect { remote ->
                    if (remote != null) {
                        _localSettings.value = remote
                    }
                }
        }
        viewModelScope.launch {
            projectsState.collect { state ->
                val list = (state as? UiState.Success)?.data.orEmpty()
                if (_timerProjectName.value.isBlank() && list.isNotEmpty()) {
                    _timerProjectName.value = list.first().name
                }
            }
        }
    }

    fun selectTab(tab: SoloWorkTab) {
        _activeTab.value = tab
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateProjectPeriodFilter(filter: String) {
        _projectPeriodFilter.value = filter
    }

    fun toggleAppearancePanel() {
        _isAppearancePanelOpen.value = !_isAppearancePanelOpen.value
    }

    fun setAppearancePanelOpen(open: Boolean) {
        _isAppearancePanelOpen.value = open
    }

    fun markNotificationsRead() {
        _notificationsReadAll.value = true
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    private fun showToast(message: String) {
        _statusBanner.value = message
    }

    // --- LIVE THEME & APPEARANCE ENGINE ACTIONS ---

    fun updateThemeMode(mode: String) {
        val preset = if (mode == "Light") "Daylight" else if (_localSettings.value.themePreset == "Daylight") "Classic Dark" else _localSettings.value.themePreset
        val updated = _localSettings.value.copy(themeMode = mode, themePreset = preset)
        _localSettings.value = updated
        persistSettings(updated)
    }

    fun updateThemePreset(preset: String) {
        val mode = if (preset == "Daylight") "Light" else "Dark"
        val updated = _localSettings.value.copy(themePreset = preset, themeMode = mode)
        _localSettings.value = updated
        persistSettings(updated)
    }

    fun updateAccentColor(accent: String) {
        val updated = _localSettings.value.copy(accentColor = accent)
        _localSettings.value = updated
        persistSettings(updated)
    }

    fun updateFontFamily(font: String) {
        val updated = _localSettings.value.copy(fontFamily = font)
        _localSettings.value = updated
        persistSettings(updated)
    }

    fun updateCustomizeLayoutStyle(enabled: Boolean) {
        val updated = _localSettings.value.copy(customizeLayoutStyle = enabled)
        _localSettings.value = updated
        persistSettings(updated)
    }

    fun updateSidebarCompact(compact: Boolean) {
        val updated = _localSettings.value.copy(sidebarCompact = compact)
        _localSettings.value = updated
        persistSettings(updated)
    }

    fun updateProfileInfo(displayName: String, roleTitle: String) {
        val safeName = displayName.trim().take(80).ifEmpty { "Alex Miller" }
        val safeRole = roleTitle.trim().take(80).ifEmpty { "Principal Freelancer" }
        val updated = _localSettings.value.copy(displayName = safeName, roleTitle = safeRole)
        _localSettings.value = updated
        persistSettings(updated)
        showToast("Profile saved to cloud")
    }

    private fun persistSettings(settings: UserSettingsItem) {
        viewModelScope.launch {
            repository.saveUserSettings(settings)
        }
    }

    // --- PROJECTS CRUD ---

    fun addProject(
        name: String,
        clientName: String,
        budget: Long,
        progress: Long,
        dueDate: String,
        status: String,
        category: String,
        avatarColor: String
    ) {
        viewModelScope.launch {
            val item = ProjectItem(
                userId = currentUserId,
                name = name.trim().take(120).ifEmpty { "Untitled Project" },
                clientName = clientName.trim().take(100).ifEmpty { "Orbit Studio" },
                clientAvatarColor = avatarColor,
                progress = progress.coerceIn(0L, 100L),
                dueDate = dueDate.trim().take(40).ifEmpty { "May 1, 2026" },
                budget = budget.coerceIn(0L, 100000000L),
                status = if (status in listOf("Active", "Completed", "On Hold")) status else "Active",
                category = category.trim().take(50).ifEmpty { "Web Design" }
            )
            repository.createProject(item).onSuccess {
                showToast("Project '${item.name}' created")
            }
        }
    }

    fun updateProjectProgress(project: ProjectItem, newProgress: Long) {
        viewModelScope.launch {
            val coerced = newProgress.coerceIn(0L, 100L)
            val newStatus = if (coerced == 100L) "Completed" else if (project.status == "Completed") "Active" else project.status
            repository.updateProject(project.copy(progress = coerced, status = newStatus))
        }
    }

    fun updateProjectDetails(
        project: ProjectItem,
        name: String,
        clientName: String,
        budget: Long,
        progress: Long,
        dueDate: String,
        status: String,
        category: String
    ) {
        viewModelScope.launch {
            val safeStatus = if (status in listOf("Active", "Completed", "On Hold")) status else project.status
            val updated = project.copy(
                name = name.trim().take(120).ifEmpty { project.name },
                clientName = clientName.trim().take(100).ifEmpty { project.clientName },
                budget = budget.coerceIn(0L, 100000000L),
                progress = progress.coerceIn(0L, 100L),
                dueDate = dueDate.trim().take(40).ifEmpty { project.dueDate },
                status = safeStatus,
                category = category.trim().take(50).ifEmpty { project.category }
            )
            repository.updateProject(updated).onSuccess {
                showToast("Project '${updated.name}' updated")
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(projectId).onSuccess {
                showToast("Project deleted")
            }
        }
    }

    // --- CLIENTS CRUD ---

    fun addClient(
        name: String,
        company: String,
        email: String,
        status: String,
        totalBilled: Long,
        avatarColor: String
    ) {
        viewModelScope.launch {
            val item = ClientItem(
                userId = currentUserId,
                name = name.trim().take(100).ifEmpty { "New Client" },
                company = company.trim().take(100).ifEmpty { "Studio Partner" },
                email = email.trim().take(120).ifEmpty { "contact@client.io" },
                status = if (status in listOf("Active", "Lead", "Archived")) status else "Active",
                totalBilled = totalBilled.coerceIn(0L, 100000000L),
                projectsCount = 1L,
                avatarColor = avatarColor
            )
            repository.createClient(item).onSuccess {
                showToast("Client '${item.company}' added")
            }
        }
    }

    fun updateClientDetails(
        client: ClientItem,
        name: String,
        company: String,
        email: String,
        status: String,
        totalBilled: Long,
        projectsCount: Long
    ) {
        viewModelScope.launch {
            val safeStatus = if (status in listOf("Active", "Lead", "Archived")) status else client.status
            val updated = client.copy(
                name = name.trim().take(100).ifEmpty { client.name },
                company = company.trim().take(100).ifEmpty { client.company },
                email = email.trim().take(120).ifEmpty { client.email },
                status = safeStatus,
                totalBilled = totalBilled.coerceIn(0L, 100000000L),
                projectsCount = projectsCount.coerceIn(0L, 1000L)
            )
            repository.updateClient(updated).onSuccess {
                showToast("Client '${updated.company}' updated")
            }
        }
    }

    fun deleteClient(clientId: String) {
        viewModelScope.launch {
            repository.deleteClient(clientId).onSuccess {
                showToast("Client removed")
            }
        }
    }

    // --- INVOICES CRUD ---

    fun addInvoice(
        invoiceNumber: String,
        clientName: String,
        projectTitle: String,
        amountDollars: Double,
        status: String,
        dueDate: String
    ) {
        viewModelScope.launch {
            val cents = (amountDollars * 100).toLong().coerceIn(0L, 1000000000L)
            val today = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date())
            val item = InvoiceItem(
                userId = currentUserId,
                invoiceNumber = invoiceNumber.trim().take(40).ifEmpty { "INV-2026-200" },
                clientName = clientName.trim().take(100).ifEmpty { "Orbit Studio" },
                projectTitle = projectTitle.trim().take(120).ifEmpty { "Web Redesign" },
                amountCents = cents,
                status = if (status in listOf("Paid", "Pending", "Overdue")) status else "Pending",
                issuedDate = today,
                dueDate = dueDate.trim().take(40).ifEmpty { "May 15, 2026" }
            )
            repository.createInvoice(item).onSuccess {
                showToast("Invoice ${item.invoiceNumber} created")
            }
        }
    }

    fun updateInvoiceDetails(
        invoice: InvoiceItem,
        invoiceNumber: String,
        clientName: String,
        projectTitle: String,
        amountDollars: Double,
        status: String,
        dueDate: String
    ) {
        viewModelScope.launch {
            val cents = (amountDollars * 100).toLong().coerceIn(0L, 1000000000L)
            val safeStatus = if (status in listOf("Paid", "Pending", "Overdue")) status else invoice.status
            val updated = invoice.copy(
                invoiceNumber = invoiceNumber.trim().take(40).ifEmpty { invoice.invoiceNumber },
                clientName = clientName.trim().take(100).ifEmpty { invoice.clientName },
                projectTitle = projectTitle.trim().take(120).ifEmpty { invoice.projectTitle },
                amountCents = cents,
                status = safeStatus,
                dueDate = dueDate.trim().take(40).ifEmpty { invoice.dueDate }
            )
            repository.updateInvoice(updated).onSuccess {
                showToast("Invoice ${updated.invoiceNumber} updated")
            }
        }
    }

    fun toggleInvoiceStatus(invoice: InvoiceItem) {
        viewModelScope.launch {
            val nextStatus = when (invoice.status) {
                "Pending" -> "Paid"
                "Overdue" -> "Paid"
                else -> "Pending"
            }
            repository.updateInvoice(invoice.copy(status = nextStatus)).onSuccess {
                showToast("Invoice ${invoice.invoiceNumber} marked as $nextStatus")
            }
        }
    }

    fun deleteInvoice(invoiceId: String) {
        viewModelScope.launch {
            repository.deleteInvoice(invoiceId).onSuccess {
                showToast("Invoice deleted")
            }
        }
    }

    // --- TASKS CRUD ---

    fun addTask(
        title: String,
        projectName: String,
        priority: String,
        status: String,
        dueDate: String
    ) {
        viewModelScope.launch {
            val safeStatus = if (status in listOf("Todo", "In Progress", "Review", "Done")) status else "Todo"
            val item = TaskItem(
                userId = currentUserId,
                title = title.trim().take(200).ifEmpty { "New Task" },
                projectName = projectName.trim().take(120).ifEmpty { "Web Redesign" },
                status = safeStatus,
                priority = if (priority in listOf("High", "Medium", "Low")) priority else "Medium",
                completed = safeStatus == "Done",
                dueDate = dueDate.trim().take(40).ifEmpty { "Apr 15, 2026" }
            )
            repository.createTask(item).onSuccess {
                showToast("Task added to $safeStatus")
            }
        }
    }

    fun updateTaskDetails(
        task: TaskItem,
        title: String,
        projectName: String,
        priority: String,
        status: String,
        dueDate: String
    ) {
        viewModelScope.launch {
            val safeStatus = if (status in listOf("Todo", "In Progress", "Review", "Done")) status else task.status
            val safePriority = if (priority in listOf("High", "Medium", "Low")) priority else task.priority
            val updated = task.copy(
                title = title.trim().take(200).ifEmpty { task.title },
                projectName = projectName.trim().take(120).ifEmpty { task.projectName },
                priority = safePriority,
                status = safeStatus,
                completed = safeStatus == "Done",
                dueDate = dueDate.trim().take(40).ifEmpty { task.dueDate }
            )
            repository.updateTask(updated).onSuccess {
                showToast("Task updated")
            }
        }
    }

    fun toggleTaskCompletion(task: TaskItem) {
        viewModelScope.launch {
            val newCompleted = !task.completed
            val newStatus = if (newCompleted) "Done" else "Todo"
            repository.updateTask(task.copy(completed = newCompleted, status = newStatus))

            // Recalculate matching project's progress automatically based on task completion
            val currentProjects = (projectsState.value as? UiState.Success)?.data ?: emptyList()
            val matchingProject = currentProjects.firstOrNull {
                it.name.equals(task.projectName, ignoreCase = true)
            }
            if (matchingProject != null) {
                val delta = if (newCompleted) 10L else -10L
                val updatedProgress = (matchingProject.progress + delta).coerceIn(5L, 100L)
                repository.updateProject(matchingProject.copy(progress = updatedProgress))
            }
        }
    }

    fun moveTaskStatus(task: TaskItem, newStatus: String) {
        viewModelScope.launch {
            if (newStatus !in listOf("Todo", "In Progress", "Review", "Done")) return@launch
            val completed = newStatus == "Done"
            repository.updateTask(task.copy(status = newStatus, completed = completed))
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(taskId).onSuccess {
                showToast("Task deleted")
            }
        }
    }

    // --- LIVE STOPWATCH & TIME TRACKER ---

    fun updateTimerMetadata(projectName: String, note: String, hourlyRate: Long) {
        _timerProjectName.value = projectName
        _timerTaskNote.value = note
        _timerHourlyRate.value = hourlyRate.coerceIn(0L, 10000L)
    }

    fun toggleStopwatch() {
        if (_isTimerRunning.value) {
            _isTimerRunning.value = false
            timerJob?.cancel()
        } else {
            _isTimerRunning.value = true
            timerJob?.cancel()
            timerJob = viewModelScope.launch {
                while (isActive && _isTimerRunning.value) {
                    delay(1000L)
                    _elapsedSeconds.value += 1L
                }
            }
        }
    }

    fun resetStopwatch() {
        _isTimerRunning.value = false
        timerJob?.cancel()
        _elapsedSeconds.value = 0L
    }

    fun logCurrentStopwatchSession() {
        val seconds = _elapsedSeconds.value
        val minutesToLog = ((seconds + 59L) / 60L).coerceAtLeast(15L) // minimum 15m billable block
        resetStopwatch()
        addTimeLog(
            projectName = _timerProjectName.value,
            taskDescription = _timerTaskNote.value,
            durationMinutes = minutesToLog,
            hourlyRate = _timerHourlyRate.value,
            billable = true
        )
    }

    fun addTimeLog(
        projectName: String,
        taskDescription: String,
        durationMinutes: Long,
        hourlyRate: Long,
        billable: Boolean
    ) {
        viewModelScope.launch {
            val today = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date())
            val log = TimeLogItem(
                userId = currentUserId,
                projectName = projectName.trim().take(120).ifEmpty { "Web Redesign" },
                taskDescription = taskDescription.trim().take(200).ifEmpty { "Focused sprint session" },
                durationMinutes = durationMinutes.coerceIn(1L, 100000L),
                hourlyRate = hourlyRate.coerceIn(0L, 10000L),
                loggedDate = today,
                billable = billable
            )
            repository.createTimeLog(log).onSuccess {
                showToast("Logged ${durationMinutes}m to ${log.projectName}")
            }
        }
    }

    fun deleteTimeLog(logId: String) {
        viewModelScope.launch {
            repository.deleteTimeLog(logId).onSuccess {
                showToast("Time log removed")
            }
        }
    }

    fun clearExampleDataAndStartFresh() {
        viewModelScope.launch {
            showToast("Clearing example data & preparing your clean workspace...")
            repository.clearAllUserData().onSuccess {
                _timerProjectName.value = ""
                _timerTaskNote.value = ""
                showToast("Workspace cleared! All metrics & boards are now starting fresh at zero.")
            }.onFailure { e ->
                showToast(e.localizedMessage ?: "Failed to clear workspace data")
            }
        }
    }

    fun resetDemoData() {
        viewModelScope.launch {
            showToast("Loading example starter data into workspace...")
            repository.ensureSeededOrReset(forceReset = true).onSuccess {
                showToast("Example starter data loaded!")
            }
        }
    }

    // --- GEMINI AI COPILOT ACTIONS (STREAMING, CHAT, & STRUCTURED OUTPUT) ---

    fun clearAiChatHistory() {
        _aiStreamState.value = AiStreamState.Idle
        _aiChatMessages.value = listOf(
            AiChatMessage(
                id = "welcome_reset",
                role = "assistant",
                text = "Conversation cleared! How can I help you optimize your SoloWork projects, clients, or invoices today?",
                timestamp = "Just now"
            )
        )
    }

    fun streamAiFreelanceAssistant(userPrompt: String) {
        if (userPrompt.isBlank()) return
        val nowTime = SimpleDateFormat("h:mm a", Locale.US).format(Date())
        _aiChatMessages.value = _aiChatMessages.value + AiChatMessage(
            id = "user_${System.currentTimeMillis()}",
            role = "user",
            text = userPrompt.trim(),
            timestamp = nowTime
        )

        viewModelScope.launch {
            _aiStreamState.value = AiStreamState.Loading
            val accumulatedText = StringBuilder()

            val currentProjects = (projectsState.value as? UiState.Success)?.data.orEmpty()
            val currentInvoices = (invoicesState.value as? UiState.Success)?.data.orEmpty()
            val currentClients = (clientsState.value as? UiState.Success)?.data.orEmpty()
            val currentLogs = (timeLogsState.value as? UiState.Success)?.data.orEmpty()

            val paidDollars = currentInvoices.filter { it.status == "Paid" }.sumOf { it.amountCents } / 100.0
            val pendingInvoicesSummary = currentInvoices
                .filter { it.status == "Pending" || it.status == "Overdue" }
                .joinToString("; ") { "${it.invoiceNumber} (${it.clientName}: $${String.format(Locale.US, "%.2f", it.amountCents / 100.0)}, due ${it.dueDate})" }
                .ifBlank { "None" }
            val activeProjectsSummary = currentProjects
                .take(6)
                .joinToString("; ") { "${it.name} for ${it.clientName} (${it.progress}% complete, $${it.budget} budget)" }
                .ifBlank { "None (Empty workspace)" }
            val trackedHours = String.format(Locale.US, "%.1fh", currentLogs.sumOf { it.durationMinutes } / 60.0)
            val recentHistory = _aiChatMessages.value.takeLast(4).joinToString("\n") { "${it.role.uppercase()}: ${it.text}" }

            val fullPrompt = """
                You are the SoloWork AI Copilot for ${_localSettings.value.displayName} (${_localSettings.value.roleTitle}).
                Live Workspace Context:
                - Active Projects: $activeProjectsSummary
                - Active Clients Count: ${currentClients.size}
                - Total Paid Revenue: $${String.format(Locale.US, "%,.2f", paidDollars)}
                - Pending Invoices: $pendingInvoicesSummary
                - Billable Hours Logged: $trackedHours

                Recent Conversation:
                $recentHistory

                Freelancer Request:
                $userPrompt

                Provide a clear, friendly, actionable, commercial-grade response tailored to their SoloWork freelance workspace.
            """.trimIndent()

            try {
                withTimeout(60_000L) {
                    streamingAiModel.generateContentStream(fullPrompt).collect { chunk ->
                        val chunkText = chunk.text.orEmpty()
                        accumulatedText.append(chunkText)
                        _aiStreamState.value = AiStreamState.Streaming(accumulatedText.toString())
                    }
                }
                val finalReply = accumulatedText.toString().trim()
                if (finalReply.isNotBlank()) {
                    _aiStreamState.value = AiStreamState.Success(finalReply)
                    _aiChatMessages.value = _aiChatMessages.value + AiChatMessage(
                        id = "ai_${System.currentTimeMillis()}",
                        role = "assistant",
                        text = finalReply,
                        timestamp = SimpleDateFormat("h:mm a", Locale.US).format(Date())
                    )
                } else {
                    _aiStreamState.value = AiStreamState.Error("Model returned an empty response.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini streaming timed out", e)
                _aiStreamState.value = AiStreamState.Error("Generation timed out after 60 seconds. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI streaming generation failed", e)
                _aiStreamState.value = AiStreamState.Error(e.localizedMessage ?: "AI generation failed.")
            }
        }
    }

    fun generateStructuredClientDoc(
        documentType: String,
        targetRecipient: String,
        customNotes: String
    ) {
        viewModelScope.launch {
            _aiClientDocState.value = AiClientDocState.Loading
            try {
                val prompt = """
                    Generate a professional freelance client communication document.
                    Sender: ${_localSettings.value.displayName} (${_localSettings.value.roleTitle} at SoloWork Studio)
                    Document Type: $documentType
                    Recipient / Client Context: $targetRecipient
                    Additional Notes: $customNotes
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    structuredClientDocModel.generateContent(prompt)
                }
                val jsonString = response.text ?: throw IllegalStateException("Model returned empty JSON response.")
                val json = JSONObject(jsonString)

                val docType = json.optString("documentType", documentType)
                val subjectLine = json.getString("subjectLine")
                val recipientName = json.optString("recipientName", targetRecipient)
                val bodyText = json.getString("bodyText")
                val highlightsArr = json.getJSONArray("keyHighlights")
                val highlights = (0 until highlightsArr.length()).map { highlightsArr.getString(it) }
                val followUpDate = json.optString("suggestedFollowUpDate", "In 3 business days")

                _aiClientDocState.value = AiClientDocState.Success(
                    AiGeneratedClientDoc(
                        documentType = docType,
                        subjectLine = subjectLine,
                        recipientName = recipientName,
                        bodyText = bodyText,
                        keyHighlights = highlights,
                        suggestedFollowUpDate = followUpDate
                    )
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Structured client doc timed out", e)
                _aiClientDocState.value = AiClientDocState.Error("Document generation timed out after 60 seconds.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate structured client doc", e)
                _aiClientDocState.value = AiClientDocState.Error(e.localizedMessage ?: "Failed to generate client document.")
            }
        }
    }

    fun launchContextualAiPrompt(prompt: String, navigateToAiTab: Boolean = true) {
        if (navigateToAiTab) {
            _activeTab.value = SoloWorkTab.AiCopilot
        }
        streamAiFreelanceAssistant(prompt)
    }

    fun generateStructuredProjectBlueprint(projectBrief: String) {
        if (projectBrief.isBlank()) return
        viewModelScope.launch {
            _aiBlueprintState.value = AiBlueprintState.Loading
            try {
                val prompt = """
                    Generate a complete freelance project scope blueprint and actionable Kanban task milestones for the following client brief:
                    "$projectBrief"
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    structuredBlueprintModel.generateContent(prompt)
                }
                val jsonString = response.text ?: throw IllegalStateException("Model returned empty JSON response.")

                // Parse raw JSON directly without regex or fence-stripping
                val json = JSONObject(jsonString)
                val projectName = json.getString("projectName")
                val clientName = json.getString("clientName")
                val category = json.optString("category", "Web Design")
                val estimatedBudgetUsd = json.optLong("estimatedBudgetUsd", 4500L).coerceIn(100L, 10000000L)
                val executiveSummary = json.getString("executiveSummary")
                val milestonesArray = json.getJSONArray("milestones")
                val milestones = (0 until milestonesArray.length()).map { milestonesArray.getString(it) }

                _aiBlueprintState.value = AiBlueprintState.Success(
                    AiGeneratedProjectBlueprint(
                        projectName = projectName,
                        clientName = clientName,
                        category = category,
                        estimatedBudgetUsd = estimatedBudgetUsd,
                        executiveSummary = executiveSummary,
                        milestones = milestones
                    )
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Structured project blueprint timed out", e)
                _aiBlueprintState.value = AiBlueprintState.Error("Blueprint generation timed out after 60 seconds.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate or parse structured project blueprint", e)
                _aiBlueprintState.value = AiBlueprintState.Error(e.localizedMessage ?: "Failed to generate project blueprint.")
            }
        }
    }

    fun importAiBlueprintToWorkspace(blueprint: AiGeneratedProjectBlueprint) {
        viewModelScope.launch {
            val dueDate = "May 28, 2026"
            val newProject = ProjectItem(
                userId = currentUserId,
                name = blueprint.projectName.trim().take(120).ifEmpty { "AI Scoped Project" },
                clientName = blueprint.clientName.trim().take(100).ifEmpty { "Orbit Studio" },
                clientAvatarColor = "#6366F1",
                progress = 10L,
                dueDate = dueDate,
                budget = blueprint.estimatedBudgetUsd.coerceIn(0L, 100000000L),
                status = "Active",
                category = if (blueprint.category in listOf("Web Design", "Branding", "Development", "Strategy")) blueprint.category else "Web Design"
            )
            repository.createProject(newProject)

            blueprint.milestones.take(6).forEachIndexed { index, milestoneTitle ->
                val task = TaskItem(
                    userId = currentUserId,
                    title = milestoneTitle.trim().take(200),
                    projectName = newProject.name,
                    status = if (index == 0) "In Progress" else "Todo",
                    priority = if (index < 2) "High" else "Medium",
                    completed = false,
                    dueDate = dueDate
                )
                repository.createTask(task)
            }

            showToast("Imported '${newProject.name}' + ${blueprint.milestones.size} tasks to Firestore!")
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    private companion object {
        const val TAG = "SoloWorkVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
