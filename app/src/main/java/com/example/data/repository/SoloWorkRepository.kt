package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.ClientItem
import com.example.data.model.InvoiceItem
import com.example.data.model.ProjectItem
import com.example.data.model.TaskItem
import com.example.data.model.TimeLogItem
import com.example.data.model.UserSettingsItem
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SoloWorkRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- REALTIME OBSERVATION FLOWS ---

    fun observeProjects(userId: String = requireUserId()): Flow<List<ProjectItem>> = flow {
        val path = "projects"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ProjectItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeClients(userId: String = requireUserId()): Flow<List<ClientItem>> = flow {
        val path = "clients"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ClientItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeInvoices(userId: String = requireUserId()): Flow<List<InvoiceItem>> = flow {
        val path = "invoices"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(InvoiceItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeTasks(userId: String = requireUserId()): Flow<List<TaskItem>> = flow {
        val path = "tasks"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(TaskItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeTimeLogs(userId: String = requireUserId()): Flow<List<TimeLogItem>> = flow {
        val path = "timeLogs"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(TimeLogItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeUserSettings(userId: String = requireUserId()): Flow<UserSettingsItem?> = flow {
        val path = "userSettings/$userId"
        emitAll(
            db.collection("userSettings").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserSettingsItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    // --- PROJECTS CRUD ---

    suspend fun createProject(project: ProjectItem): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("projects")
        val docRef = if (project.id.isNotBlank()) colRef.document(project.id) else colRef.document()
        try {
            val payload = project.copy(userId = uid, createdAt = null, updatedAt = null)
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun getProjectById(projectId: String): Result<ProjectItem?> = runCatching {
        val docRef = db.collection("projects").document(projectId)
        try {
            docRef.get().await().toObject(ProjectItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun updateProject(project: ProjectItem): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(project.id.isNotBlank()) { "Project ID must not be blank for update" }
        requireNotNull(project.createdAt) { "Project createdAt must not be null for update" }
        val docRef = db.collection("projects").document(project.id)
        try {
            docRef.set(project.copy(userId = uid, updatedAt = null)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteProject(projectId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("projects").document(projectId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- CLIENTS CRUD ---

    suspend fun createClient(client: ClientItem): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("clients")
        val docRef = if (client.id.isNotBlank()) colRef.document(client.id) else colRef.document()
        try {
            docRef.set(client.copy(userId = uid, createdAt = null, updatedAt = null)).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateClient(client: ClientItem): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(client.id.isNotBlank()) { "Client ID must not be blank for update" }
        requireNotNull(client.createdAt) { "Client createdAt must not be null for update" }
        val docRef = db.collection("clients").document(client.id)
        try {
            docRef.set(client.copy(userId = uid, updatedAt = null)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteClient(clientId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("clients").document(clientId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- INVOICES CRUD ---

    suspend fun createInvoice(invoice: InvoiceItem): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("invoices")
        val docRef = if (invoice.id.isNotBlank()) colRef.document(invoice.id) else colRef.document()
        try {
            docRef.set(invoice.copy(userId = uid, createdAt = null, updatedAt = null)).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateInvoice(invoice: InvoiceItem): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(invoice.id.isNotBlank()) { "Invoice ID must not be blank for update" }
        requireNotNull(invoice.createdAt) { "Invoice createdAt must not be null for update" }
        val docRef = db.collection("invoices").document(invoice.id)
        try {
            docRef.set(invoice.copy(userId = uid, updatedAt = null)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteInvoice(invoiceId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("invoices").document(invoiceId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- TASKS CRUD ---

    suspend fun createTask(task: TaskItem): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("tasks")
        val docRef = if (task.id.isNotBlank()) colRef.document(task.id) else colRef.document()
        try {
            docRef.set(task.copy(userId = uid, createdAt = null, updatedAt = null)).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateTask(task: TaskItem): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(task.id.isNotBlank()) { "Task ID must not be blank for update" }
        requireNotNull(task.createdAt) { "Task createdAt must not be null for update" }
        val docRef = db.collection("tasks").document(task.id)
        try {
            docRef.set(task.copy(userId = uid, updatedAt = null)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteTask(taskId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("tasks").document(taskId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- TIME LOGS CRUD ---

    suspend fun createTimeLog(timeLog: TimeLogItem): Result<String> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("timeLogs")
        val docRef = if (timeLog.id.isNotBlank()) colRef.document(timeLog.id) else colRef.document()
        try {
            docRef.set(timeLog.copy(userId = uid, createdAt = null, updatedAt = null)).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteTimeLog(logId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("timeLogs").document(logId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- USER SETTINGS CRUD ---

    suspend fun saveUserSettings(settings: UserSettingsItem): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("userSettings").document(uid)
        try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                val existingCreatedAt = snap.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                docRef.set(settings.copy(id = uid, userId = uid, createdAt = existingCreatedAt, updatedAt = null)).await()
            } else {
                docRef.set(settings.copy(id = uid, userId = uid, createdAt = null, updatedAt = null)).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // --- OPTIONAL EXAMPLE DATA & CLEAN-SLATE WORKSPACE MANAGEMENT ---

    suspend fun clearAllUserData(): Result<Unit> = runCatching {
        val uid = requireUserId()
        for (collectionName in listOf("projects", "clients", "invoices", "tasks", "timeLogs")) {
            val docs = db.collection(collectionName).whereEqualTo("userId", uid).get().await()
            for (doc in docs.documents) {
                db.collection(collectionName).document(doc.id).delete().await()
            }
        }
        // Persist userSettings document so the app knows this user initialized their workspace
        // and chose a clean slate (preventing automatic re-seeding on subsequent launches).
        val settingsSnap = db.collection("userSettings").document(uid).get().await()
        if (!settingsSnap.exists()) {
            saveUserSettings(UserSettingsItem(id = uid, userId = uid)).getOrThrow()
        }
    }

    suspend fun ensureSeededOrReset(forceReset: Boolean = false): Result<Unit> = runCatching {
        val uid = requireUserId()
        val safeUid = uid.replace(Regex("[^a-zA-Z0-9_\\-]"), "").take(40).ifEmpty { "user" }

        if (!forceReset) {
            // If userSettings already exists, the user has already onboarded (or cleared example data).
            val settingsSnap = db.collection("userSettings").document(uid).get().await()
            if (settingsSnap.exists()) {
                return@runCatching
            }
            val existingProjects = db.collection("projects").whereEqualTo("userId", uid).get().await()
            if (!existingProjects.isEmpty) {
                return@runCatching
            }
        } else {
            for (collectionName in listOf("projects", "clients", "invoices", "tasks", "timeLogs")) {
                val docs = db.collection(collectionName).whereEqualTo("userId", uid).get().await()
                for (doc in docs.documents) {
                    db.collection(collectionName).document(doc.id).delete().await()
                }
            }
        }

        // Seed 6 Example Projects (tagged with "example_" ID prefix so UI can identify Example Mode)
        val demoProjects = listOf(
            ProjectItem(id = "example_${safeUid}_proj_1", userId = uid, name = "Web Redesign", clientName = "Orbit Studio", clientAvatarColor = "#6366F1", progress = 50L, dueDate = "Apr 8, 2026", budget = 4800L, status = "Active", category = "Web Design"),
            ProjectItem(id = "example_${safeUid}_proj_2", userId = uid, name = "Brand Identity", clientName = "Pulse Media", clientAvatarColor = "#10B981", progress = 30L, dueDate = "Apr 8, 2026", budget = 3200L, status = "Active", category = "Branding"),
            ProjectItem(id = "example_${safeUid}_proj_3", userId = uid, name = "EVG Companion", clientName = "MetaLoop", clientAvatarColor = "#F59E0B", progress = 30L, dueDate = "Apr 8, 2026", budget = 2950L, status = "Active", category = "Development"),
            ProjectItem(id = "example_${safeUid}_proj_4", userId = uid, name = "Maniec Idowec", clientName = "Chroma Labs", clientAvatarColor = "#8B5CF6", progress = 50L, dueDate = "Apr 8, 2026", budget = 4100L, status = "Active", category = "Strategy"),
            ProjectItem(id = "example_${safeUid}_proj_5", userId = uid, name = "Emh Companion", clientName = "Cloud Partner", clientAvatarColor = "#F59E0B", progress = 72L, dueDate = "Apr 7, 2026", budget = 6400L, status = "Active", category = "Development"),
            ProjectItem(id = "example_${safeUid}_proj_6", userId = uid, name = "Web Redesign v2", clientName = "Orbit Studio", clientAvatarColor = "#6366F1", progress = 28L, dueDate = "Jun 15, 2026", budget = 5200L, status = "On Hold", category = "Web Design")
        )
        for (p in demoProjects) {
            createProject(p).getOrThrow()
        }

        // Seed 8 Example Clients
        val demoClients = listOf(
            ClientItem(id = "example_${safeUid}_client_1", userId = uid, name = "Elena Rostova", company = "Orbit Studio", email = "elena@orbit.studio", status = "Active", totalBilled = 5400L, projectsCount = 2L, avatarColor = "#6366F1"),
            ClientItem(id = "example_${safeUid}_client_2", userId = uid, name = "Marcus Vance", company = "Pulse Media", email = "marcus@pulsemedia.io", status = "Active", totalBilled = 3200L, projectsCount = 1L, avatarColor = "#10B981"),
            ClientItem(id = "example_${safeUid}_client_3", userId = uid, name = "Sophia Chen", company = "MetaLoop", email = "sophia@metaloop.dev", status = "Active", totalBilled = 2950L, projectsCount = 1L, avatarColor = "#F59E0B"),
            ClientItem(id = "example_${safeUid}_client_4", userId = uid, name = "Liam Thorne", company = "Chroma Labs", email = "liam@chromalabs.co", status = "Active", totalBilled = 4100L, projectsCount = 2L, avatarColor = "#8B5CF6"),
            ClientItem(id = "example_${safeUid}_client_5", userId = uid, name = "Aria Patel", company = "Cloud Partner", email = "aria@cloudpartner.org", status = "Active", totalBilled = 6400L, projectsCount = 1L, avatarColor = "#EC4899"),
            ClientItem(id = "example_${safeUid}_client_6", userId = uid, name = "Noah Lindqvist", company = "Vertex FinTech", email = "noah@vertexfin.com", status = "Active", totalBilled = 2800L, projectsCount = 1L, avatarColor = "#06B6D4"),
            ClientItem(id = "example_${safeUid}_client_7", userId = uid, name = "Chloe Mercer", company = "Nova Health", email = "chloe@novahealth.med", status = "Active", totalBilled = 1900L, projectsCount = 1L, avatarColor = "#10B981"),
            ClientItem(id = "example_${safeUid}_client_8", userId = uid, name = "Devon Brooks", company = "Aether Creative", email = "devon@aether.design", status = "Active", totalBilled = 1550L, projectsCount = 1L, avatarColor = "#6366F1")
        )
        for (c in demoClients) {
            createClient(c).getOrThrow()
        }

        // Seed Example Invoices
        val demoInvoices = listOf(
            InvoiceItem(id = "example_${safeUid}_inv_1", userId = uid, invoiceNumber = "INV-2026-101", clientName = "Orbit Studio", projectTitle = "Web Redesign", amountCents = 645000L, status = "Paid", issuedDate = "Mar 20, 2026", dueDate = "Apr 3, 2026"),
            InvoiceItem(id = "example_${safeUid}_inv_2", userId = uid, invoiceNumber = "INV-2026-102", clientName = "Cloud Partner", projectTitle = "Emh Companion", amountCents = 600000L, status = "Paid", issuedDate = "Mar 25, 2026", dueDate = "Apr 8, 2026"),
            InvoiceItem(id = "example_${safeUid}_inv_3", userId = uid, invoiceNumber = "INV-2026-103", clientName = "Pulse Media", projectTitle = "Brand Identity", amountCents = 12783L, status = "Pending", issuedDate = "Apr 2, 2026", dueDate = "Apr 16, 2026"),
            InvoiceItem(id = "example_${safeUid}_inv_4", userId = uid, invoiceNumber = "INV-2026-104", clientName = "MetaLoop", projectTitle = "EVG Companion", amountCents = 145000L, status = "Pending", issuedDate = "Apr 5, 2026", dueDate = "Apr 19, 2026"),
            InvoiceItem(id = "example_${safeUid}_inv_5", userId = uid, invoiceNumber = "INV-2026-105", clientName = "Chroma Labs", projectTitle = "Maniec Idowec", amountCents = 89000L, status = "Pending", issuedDate = "Apr 7, 2026", dueDate = "Apr 21, 2026")
        )
        for (inv in demoInvoices) {
            createInvoice(inv).getOrThrow()
        }

        // Seed Example Tasks
        val demoTasks = listOf(
            TaskItem(id = "example_${safeUid}_task_1", userId = uid, title = "Upcoming task checklist: Hero UI polish", projectName = "Web Redesign", status = "Todo", priority = "High", completed = false, dueDate = "Apr 8, 2026"),
            TaskItem(id = "example_${safeUid}_task_2", userId = uid, title = "Upcoming task checklist: Brand color tokens", projectName = "Brand Identity", status = "Done", priority = "High", completed = true, dueDate = "Apr 8, 2026"),
            TaskItem(id = "example_${safeUid}_task_3", userId = uid, title = "Upcoming checklist: API schema review", projectName = "EVG Companion", status = "In Progress", priority = "Medium", completed = false, dueDate = "Apr 9, 2026"),
            TaskItem(id = "example_${safeUid}_task_4", userId = uid, title = "Check in client interaction & sign-off", projectName = "Maniec Idowec", status = "Review", priority = "Low", completed = false, dueDate = "Apr 10, 2026"),
            TaskItem(id = "example_${safeUid}_task_5", userId = uid, title = "Responsive breakpoint QA audit", projectName = "Emh Companion", status = "In Progress", priority = "High", completed = false, dueDate = "Apr 11, 2026"),
            TaskItem(id = "example_${safeUid}_task_6", userId = uid, title = "Export SVG iconography bundle", projectName = "Web Redesign", status = "Done", priority = "Medium", completed = true, dueDate = "Apr 7, 2026")
        )
        for (t in demoTasks) {
            createTask(t).getOrThrow()
        }

        // Seed Example Time Logs
        val demoLogs = listOf(
            TimeLogItem(id = "example_${safeUid}_log_1", userId = uid, projectName = "Web Redesign", taskDescription = "Design system architecture & component library", durationMinutes = 840L, hourlyRate = 110L, loggedDate = "Apr 5, 2026", billable = true),
            TimeLogItem(id = "example_${safeUid}_log_2", userId = uid, projectName = "Emh Companion", taskDescription = "Frontend interactive views & SVG charts", durationMinutes = 750L, hourlyRate = 115L, loggedDate = "Apr 6, 2026", billable = true),
            TimeLogItem(id = "example_${safeUid}_log_3", userId = uid, projectName = "Brand Identity", taskDescription = "Logo exploration, typography & swatch deck", durationMinutes = 540L, hourlyRate = 95L, loggedDate = "Apr 7, 2026", billable = true),
            TimeLogItem(id = "example_${safeUid}_log_4", userId = uid, projectName = "EVG Companion", taskDescription = "Client discovery workshop & wireframes", durationMinutes = 420L, hourlyRate = 100L, loggedDate = "Apr 8, 2026", billable = true)
        )
        for (log in demoLogs) {
            createTimeLog(log).getOrThrow()
        }

        val settingsSnap = db.collection("userSettings").document(uid).get().await()
        if (!settingsSnap.exists() || forceReset) {
            saveUserSettings(UserSettingsItem(id = uid, userId = uid)).getOrThrow()
        }
    }
}
