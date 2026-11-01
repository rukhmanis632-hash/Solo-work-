package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PeopleOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.configureAppCheck
import com.example.data.repository.SoloWorkRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOut
import com.example.ui.components.AppearanceSettingsPanel
import com.example.ui.components.LeftNavigationSidebar
import com.example.ui.components.TopHeaderBar
import com.example.ui.screens.AiCopilotView
import com.example.ui.screens.AnalyticsView
import com.example.ui.screens.ClientsView
import com.example.ui.screens.DashboardView
import com.example.ui.screens.InvoicesView
import com.example.ui.screens.NewProjectDialog
import com.example.ui.screens.NewTaskDialog
import com.example.ui.screens.ProjectsView
import com.example.ui.screens.QuickAiAssistantModal
import com.example.ui.screens.SettingsView
import com.example.ui.screens.TaskBoardView
import com.example.ui.screens.TimeTrackerView
import com.example.ui.theme.LocalSoloWorkPalette
import com.example.ui.theme.SoloWorkTheme
import com.example.ui.viewmodel.SoloWorkTab
import com.example.ui.viewmodel.SoloWorkViewModel
import com.example.ui.viewmodel.UiState
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            AppNavigation()
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        SoloWorkTheme {
            AuthScreen(onAuthSuccess = {})
        }
    } else {
        SoloWorkWorkspaceScreen(currentUserId = user.uid)
    }
}

@Composable
fun SoloWorkWorkspaceScreen(currentUserId: String) {
    val viewModel: SoloWorkViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                SoloWorkViewModel(SoloWorkRepository(db), currentUserId)
            }
        }
    )

    val settings by viewModel.localSettings.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val periodFilter by viewModel.projectPeriodFilter.collectAsStateWithLifecycle()
    val isAppearanceOpen by viewModel.isAppearancePanelOpen.collectAsStateWithLifecycle()
    val hasUnreadNotifications by viewModel.hasUnreadNotifications.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    val projectsState by viewModel.projectsState.collectAsStateWithLifecycle()
    val clientsState by viewModel.clientsState.collectAsStateWithLifecycle()
    val invoicesState by viewModel.invoicesState.collectAsStateWithLifecycle()
    val tasksState by viewModel.tasksState.collectAsStateWithLifecycle()
    val timeLogsState by viewModel.timeLogsState.collectAsStateWithLifecycle()
    val isViewingExampleData by viewModel.isViewingExampleData.collectAsStateWithLifecycle()

    val isTimerRunning by viewModel.isTimerRunning.collectAsStateWithLifecycle()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsStateWithLifecycle()
    val timerProjectName by viewModel.timerProjectName.collectAsStateWithLifecycle()
    val timerTaskNote by viewModel.timerTaskNote.collectAsStateWithLifecycle()
    val timerHourlyRate by viewModel.timerHourlyRate.collectAsStateWithLifecycle()

    val aiStreamState by viewModel.aiStreamState.collectAsStateWithLifecycle()
    val aiBlueprintState by viewModel.aiBlueprintState.collectAsStateWithLifecycle()
    val aiClientDocState by viewModel.aiClientDocState.collectAsStateWithLifecycle()
    val aiChatMessages by viewModel.aiChatMessages.collectAsStateWithLifecycle()

    var showNewProjectModal by remember { mutableStateOf(false) }
    var showNewTaskModal by remember { mutableStateOf(false) }
    var showNotificationsModal by remember { mutableStateOf(false) }
    var showQuickAiModal by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // BackHandler for secondary screens or appearance drawer
    BackHandler(enabled = isAppearanceOpen || activeTab != SoloWorkTab.Dashboard) {
        if (isAppearanceOpen) {
            viewModel.setAppearancePanelOpen(false)
        } else {
            viewModel.selectTab(SoloWorkTab.Dashboard)
        }
    }

    val allProjects = (projectsState as? UiState.Success)?.data ?: emptyList()
    val allClients = (clientsState as? UiState.Success)?.data ?: emptyList()
    val allInvoices = (invoicesState as? UiState.Success)?.data ?: emptyList()
    val allTasks = (tasksState as? UiState.Success)?.data ?: emptyList()
    val allTimeLogs = (timeLogsState as? UiState.Success)?.data ?: emptyList()

    val q = searchQuery.trim().lowercase()
    val filteredProjects = if (q.isEmpty()) allProjects else allProjects.filter {
        it.name.lowercase().contains(q) || it.clientName.lowercase().contains(q) || it.category.lowercase().contains(q)
    }
    val filteredClients = if (q.isEmpty()) allClients else allClients.filter {
        it.name.lowercase().contains(q) || it.company.lowercase().contains(q) || it.email.lowercase().contains(q)
    }
    val filteredInvoices = if (q.isEmpty()) allInvoices else allInvoices.filter {
        it.invoiceNumber.lowercase().contains(q) || it.clientName.lowercase().contains(q) || it.projectTitle.lowercase().contains(q)
    }
    val filteredTasks = if (q.isEmpty()) allTasks else allTasks.filter {
        it.title.lowercase().contains(q) || it.projectName.lowercase().contains(q)
    }
    val filteredTimeLogs = if (q.isEmpty()) allTimeLogs else allTimeLogs.filter {
        it.projectName.lowercase().contains(q) || it.taskDescription.lowercase().contains(q)
    }

    SoloWorkTheme(
        themeMode = settings.themeMode,
        themePreset = settings.themePreset,
        accentColor = settings.accentColor,
        fontFamily = settings.fontFamily,
        customizeLayoutStyle = settings.customizeLayoutStyle,
        sidebarCompact = settings.sidebarCompact
    ) {
        val palette = LocalSoloWorkPalette.current

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            val isDesktopOrTablet = maxWidth >= 840.dp
            val isUltraWide = maxWidth >= 1120.dp

            val mainWorkspaceContent: @Composable (Boolean) -> Unit = { showHamburger ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.background)
                ) {
                    TopHeaderBar(
                        userName = settings.displayName,
                        searchQuery = searchQuery,
                        onSearchQueryChange = viewModel::updateSearchQuery,
                        isDark = palette.isDark,
                        onToggleThemeMode = viewModel::updateThemeMode,
                        hasUnreadNotifications = hasUnreadNotifications,
                        onNotificationClick = {
                            viewModel.markNotificationsRead()
                            showNotificationsModal = true
                        },
                        onToggleAppearancePanel = viewModel::toggleAppearancePanel,
                        showMenuButton = showHamburger,
                        onMenuClick = {
                            scope.launch { drawerState.open() }
                        }
                    )

                    // On handheld/compact screens, render an 8-Module Quick Navigation Strip so all 8 pages are 1-tap reachable
                    if (!isDesktopOrTablet) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SoloWorkTab.entries.forEach { tab ->
                                val selected = activeTab == tab
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(tab) },
                                    label = { Text(tab.title) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = palette.accent,
                                        selectedLabelColor = Color.White,
                                        containerColor = palette.cardBg,
                                        labelColor = palette.textSecondary
                                    )
                                )
                            }
                        }
                    }

                    if (statusBanner != null) {
                        Surface(
                            color = palette.accent.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .border(1.dp, palette.accent, RoundedCornerShape(10.dp))
                                .clickable { viewModel.clearStatusBanner() }
                        ) {
                            Text(
                                text = statusBanner ?: "",
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.textPrimary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }

                    if (isViewingExampleData && activeTab != SoloWorkTab.Dashboard) {
                        Surface(
                            color = com.example.ui.theme.AccentAmber.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .border(1.dp, com.example.ui.theme.AccentAmber, RoundedCornerShape(10.dp))
                                .testTag("global_example_data_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Viewing Example Data",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = com.example.ui.theme.AccentAmber,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(
                                    onClick = viewModel::clearExampleDataAndStartFresh,
                                    modifier = Modifier.testTag("global_clear_example_data_button")
                                ) {
                                    Text(
                                        text = "Clear Example Data & Start Fresh",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        when (activeTab) {
                            SoloWorkTab.Dashboard -> DashboardView(
                                projects = filteredProjects,
                                clients = filteredClients,
                                invoices = filteredInvoices,
                                tasks = filteredTasks,
                                timeLogs = filteredTimeLogs,
                                periodFilter = periodFilter,
                                isViewingExampleData = isViewingExampleData,
                                onPeriodFilterChange = viewModel::updateProjectPeriodFilter,
                                onToggleTask = viewModel::toggleTaskCompletion,
                                onQuickAddTask = { title ->
                                    val defaultProj = allProjects.firstOrNull()?.name ?: "General"
                                    viewModel.addTask(title, defaultProj, "Medium", "Todo", "Upcoming")
                                },
                                onEditProject = viewModel::updateProjectDetails,
                                onOpenNewProjectModal = { showNewProjectModal = true },
                                onOpenNewTaskModal = { showNewTaskModal = true },
                                onNavigateToTab = viewModel::selectTab,
                                onClearExampleData = viewModel::clearExampleDataAndStartFresh,
                                onLoadExampleData = viewModel::resetDemoData
                            )
                            SoloWorkTab.Projects -> ProjectsView(
                                projects = filteredProjects,
                                onAddProject = viewModel::addProject,
                                onUpdateProgress = viewModel::updateProjectProgress,
                                onEditProject = viewModel::updateProjectDetails,
                                onDeleteProject = viewModel::deleteProject,
                                onAiAction = viewModel::launchContextualAiPrompt
                            )
                            SoloWorkTab.Invoices -> InvoicesView(
                                invoices = filteredInvoices,
                                onAddInvoice = viewModel::addInvoice,
                                onEditInvoice = viewModel::updateInvoiceDetails,
                                onToggleInvoiceStatus = viewModel::toggleInvoiceStatus,
                                onDeleteInvoice = viewModel::deleteInvoice,
                                onAiAction = viewModel::launchContextualAiPrompt
                            )
                            SoloWorkTab.Clients -> ClientsView(
                                clients = filteredClients,
                                onAddClient = viewModel::addClient,
                                onEditClient = viewModel::updateClientDetails,
                                onDeleteClient = viewModel::deleteClient,
                                onAiAction = viewModel::launchContextualAiPrompt
                            )
                            SoloWorkTab.TaskBoard -> TaskBoardView(
                                tasks = filteredTasks,
                                onAddTask = viewModel::addTask,
                                onToggleTask = viewModel::toggleTaskCompletion,
                                onMoveTaskStatus = viewModel::moveTaskStatus,
                                onDeleteTask = viewModel::deleteTask
                            )
                            SoloWorkTab.TimeTracker -> TimeTrackerView(
                                projects = allProjects,
                                timeLogs = filteredTimeLogs,
                                isTimerRunning = isTimerRunning,
                                elapsedSeconds = elapsedSeconds,
                                timerProjectName = timerProjectName,
                                timerTaskNote = timerTaskNote,
                                timerHourlyRate = timerHourlyRate,
                                onUpdateTimerMetadata = viewModel::updateTimerMetadata,
                                onToggleStopwatch = viewModel::toggleStopwatch,
                                onResetStopwatch = viewModel::resetStopwatch,
                                onLogStopwatchSession = viewModel::logCurrentStopwatchSession,
                                onAddManualTimeLog = viewModel::addTimeLog,
                                onDeleteTimeLog = viewModel::deleteTimeLog
                            )
                            SoloWorkTab.Analytics -> AnalyticsView(
                                projects = filteredProjects,
                                invoices = filteredInvoices,
                                clients = filteredClients,
                                timeLogs = filteredTimeLogs
                            )
                            SoloWorkTab.AiCopilot -> AiCopilotView(
                                aiStreamState = aiStreamState,
                                aiBlueprintState = aiBlueprintState,
                                aiClientDocState = aiClientDocState,
                                aiChatMessages = aiChatMessages,
                                onStreamPrompt = viewModel::streamAiFreelanceAssistant,
                                onClearChat = viewModel::clearAiChatHistory,
                                onGenerateBlueprint = viewModel::generateStructuredProjectBlueprint,
                                onImportBlueprint = viewModel::importAiBlueprintToWorkspace,
                                onGenerateClientDoc = viewModel::generateStructuredClientDoc
                            )
                            SoloWorkTab.Settings -> SettingsView(
                                settings = settings,
                                isViewingExampleData = isViewingExampleData,
                                onUpdateProfile = viewModel::updateProfileInfo,
                                onUpdateThemeMode = viewModel::updateThemeMode,
                                onUpdateThemePreset = viewModel::updateThemePreset,
                                onUpdateAccentColor = viewModel::updateAccentColor,
                                onUpdateFontFamily = viewModel::updateFontFamily,
                                onUpdateCustomizeLayoutStyle = viewModel::updateCustomizeLayoutStyle,
                                onUpdateSidebarCompact = viewModel::updateSidebarCompact,
                                onResetDemoData = viewModel::resetDemoData,
                                onClearAllData = viewModel::clearExampleDataAndStartFresh,
                                onSignOut = {
                                    signOut(context, credentialManager, {}, scope)
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Compact Bottom Navigation Bar on handheld screens
                    if (!isDesktopOrTablet) {
                        NavigationBar(
                            containerColor = palette.sidebarBg,
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, palette.border)
                                .navigationBarsPadding()
                        ) {
                            val quickTabs = listOf(
                                SoloWorkTab.Dashboard to Icons.Default.Home,
                                SoloWorkTab.Projects to Icons.Default.FolderOpen,
                                SoloWorkTab.Invoices to Icons.Default.Description,
                                SoloWorkTab.TaskBoard to Icons.Default.Checklist,
                                SoloWorkTab.AiCopilot to Icons.Default.AutoAwesome
                            )
                            quickTabs.forEach { (tab, icon) ->
                                val selected = activeTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = { Icon(icon, contentDescription = tab.title) },
                                    label = { Text(tab.title, maxLines = 1) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.White,
                                        selectedTextColor = palette.accent,
                                        indicatorColor = palette.accent,
                                        unselectedIconColor = palette.textSecondary,
                                        unselectedTextColor = palette.textSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            if (isDesktopOrTablet) {
                // Three-Column Desktop/Tablet Layout: Left Sidebar + Center Workspace + Right Appearance Settings Panel
                Row(modifier = Modifier.fillMaxSize()) {
                    LeftNavigationSidebar(
                        activeTab = activeTab,
                        onSelectTab = viewModel::selectTab,
                        userName = settings.displayName,
                        userRole = settings.roleTitle,
                        compact = settings.sidebarCompact,
                        onSignOut = { signOut(context, credentialManager, {}, scope) }
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        mainWorkspaceContent(false)
                    }

                    if (isUltraWide || isAppearanceOpen) {
                        AppearanceSettingsPanel(
                            settings = settings,
                            onUpdateThemeMode = viewModel::updateThemeMode,
                            onUpdateThemePreset = viewModel::updateThemePreset,
                            onUpdateAccentColor = viewModel::updateAccentColor,
                            onUpdateFontFamily = viewModel::updateFontFamily,
                            onUpdateCustomizeLayoutStyle = viewModel::updateCustomizeLayoutStyle,
                            onUpdateSidebarCompact = viewModel::updateSidebarCompact,
                            onClose = if (isUltraWide) null else ({ viewModel.setAppearancePanelOpen(false) })
                        )
                    }
                }
            } else {
                // Mobile / Compact Layout with Left Navigation Drawer & Slide-over Right Appearance Panel
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        LeftNavigationSidebar(
                            activeTab = activeTab,
                            onSelectTab = { tab ->
                                viewModel.selectTab(tab)
                                scope.launch { drawerState.close() }
                            },
                            userName = settings.displayName,
                            userRole = settings.roleTitle,
                            compact = settings.sidebarCompact,
                            onSignOut = {
                                scope.launch { drawerState.close() }
                                signOut(context, credentialManager, {}, scope)
                            }
                        )
                    }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        mainWorkspaceContent(true)

                        // Slide-over Right Appearance Settings Panel
                        AnimatedVisibility(
                            visible = isAppearanceOpen,
                            enter = slideInHorizontally(initialOffsetX = { it }),
                            exit = slideOutHorizontally(targetOffsetX = { it }),
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            AppearanceSettingsPanel(
                                settings = settings,
                                onUpdateThemeMode = viewModel::updateThemeMode,
                                onUpdateThemePreset = viewModel::updateThemePreset,
                                onUpdateAccentColor = viewModel::updateAccentColor,
                                onUpdateFontFamily = viewModel::updateFontFamily,
                                onUpdateCustomizeLayoutStyle = viewModel::updateCustomizeLayoutStyle,
                                onUpdateSidebarCompact = viewModel::updateSidebarCompact,
                                onClose = { viewModel.setAppearancePanelOpen(false) }
                            )
                        }
                    }
                }
            }

            // Global Floating "✨ Ask AI" Quick-Assistant Pill (accessible from any screen outside AI Copilot)
            if (activeTab != SoloWorkTab.AiCopilot) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = palette.accent,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = 18.dp,
                            bottom = if (isDesktopOrTablet) 20.dp else 90.dp
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { showQuickAiModal = true }
                        .testTag("global_ai_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Ask SoloWork AI",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ask AI",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (showQuickAiModal) {
            QuickAiAssistantModal(
                currentTab = activeTab,
                onDismiss = { showQuickAiModal = false },
                onLaunchPromptInCopilot = { prompt ->
                    viewModel.launchContextualAiPrompt(prompt, navigateToAiTab = true)
                },
                onOpenFullCopilot = {
                    viewModel.selectTab(SoloWorkTab.AiCopilot)
                }
            )
        }

        if (showNewProjectModal) {
            NewProjectDialog(
                onDismiss = { showNewProjectModal = false },
                onConfirm = { name, client, budget, progress, due, status, category ->
                    viewModel.addProject(name, client, budget, progress, due, status, category, "#6366F1")
                    showNewProjectModal = false
                }
            )
        }

        if (showNewTaskModal) {
            NewTaskDialog(
                onDismiss = { showNewTaskModal = false },
                onConfirm = { title, proj, prio, status, due ->
                    viewModel.addTask(title, proj, prio, status, due)
                    showNewTaskModal = false
                }
            )
        }

        if (showNotificationsModal) {
            AlertDialog(
                onDismissRequest = { showNotificationsModal = false },
                title = { Text("Workspace Notifications") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (notifications.isEmpty()) {
                            Text(
                                text = "No notifications yet. Activity from your projects, tasks, and invoices will appear here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                        } else {
                            notifications.forEach { n ->
                                Column {
                                    Text(n.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(n.subtitle, style = MaterialTheme.typography.bodyMedium)
                                    Text(n.timeAgo, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showNotificationsModal = false }) {
                        Text("Done")
                    }
                }
            )
        }
    }
}
