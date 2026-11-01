package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.ClientItem
import com.example.data.model.InvoiceItem
import com.example.data.model.ProjectItem
import com.example.data.model.TaskItem
import com.example.data.model.TimeLogItem
import com.example.data.model.UserSettingsItem
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class SoloWorkRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveProject_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = SoloWorkRepository(firestore)
        val uniqueId = "proj_${UUID.randomUUID().toString().replace("-", "")}"

        val project = ProjectItem(
            id = uniqueId,
            userId = uid,
            name = "Web Redesign",
            clientName = "Orbit Studio",
            clientAvatarColor = "#6366F1",
            progress = 50L,
            dueDate = "Apr 8, 2026",
            budget = 4800L,
            status = "Active",
            category = "Web Design"
        )

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.createProject(project) }
        assertTrue(createResult.isSuccess)
        assertEquals(uniqueId, createResult.getOrThrow())

        val emittedProjects = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProjects(uid).first { list -> list.any { it.id == uniqueId } }
        }
        val fetched = emittedProjects.first { it.id == uniqueId }
        assertEquals("Web Redesign", fetched.name)
        assertNotNull(fetched.createdAt)

        // Update project progress
        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateProject(fetched.copy(progress = 85L))
        }
        assertTrue(updateResult.isSuccess)

        // Delete project
        val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.deleteProject(uniqueId)
        }
        assertTrue(deleteResult.isSuccess)
    }

    @Test
    fun createUpdateDeleteClientAndInvoice_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = SoloWorkRepository(firestore)

        val clientId = "client_${UUID.randomUUID().toString().replace("-", "")}"
        val client = ClientItem(
            id = clientId,
            userId = uid,
            name = "Elena Rostova",
            company = "Orbit Studio",
            email = "elena@orbit.studio",
            status = "Active",
            totalBilled = 5400L,
            projectsCount = 2L,
            avatarColor = "#6366F1"
        )
        val createClientRes = withTimeout(DEFAULT_TIMEOUT_MS) { repository.createClient(client) }
        assertTrue(createClientRes.isSuccess)

        val observedClients = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeClients(uid).first { list -> list.any { it.id == clientId } }
        }
        val fetchedClient = observedClients.first { it.id == clientId }
        assertNotNull(fetchedClient.createdAt)

        val updateClientRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateClient(fetchedClient.copy(totalBilled = 6800L, projectsCount = 3L))
        }
        assertTrue(updateClientRes.isSuccess)

        val invId = "inv_${UUID.randomUUID().toString().replace("-", "")}"
        val invoice = InvoiceItem(
            id = invId,
            userId = uid,
            invoiceNumber = "INV-2026-101",
            clientName = "Orbit Studio",
            projectTitle = "Web Redesign",
            amountCents = 645000L,
            status = "Pending",
            issuedDate = "Mar 20, 2026",
            dueDate = "Apr 3, 2026"
        )
        val createInvRes = withTimeout(DEFAULT_TIMEOUT_MS) { repository.createInvoice(invoice) }
        assertTrue(createInvRes.isSuccess)

        val observedInvoices = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeInvoices(uid).first { list -> list.any { it.id == invId } }
        }
        val fetchedInvoice = observedInvoices.first { it.id == invId }
        assertNotNull(fetchedInvoice.createdAt)

        val updateInvRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateInvoice(fetchedInvoice.copy(status = "Paid"))
        }
        assertTrue(updateInvRes.isSuccess)
    }

    @Test
    fun createTaskTimeLogAndSaveUserSettings_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = SoloWorkRepository(firestore)
        val taskId = "task_${UUID.randomUUID().toString().replace("-", "")}"

        val task = TaskItem(
            id = taskId,
            userId = uid,
            title = "Upcoming task checklist",
            projectName = "Web Redesign",
            status = "Todo",
            priority = "High",
            completed = false,
            dueDate = "Apr 8, 2026"
        )
        val taskResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.createTask(task) }
        assertTrue(taskResult.isSuccess)

        val observedTasks = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTasks(uid).first { list -> list.any { it.id == taskId } }
        }
        val fetchedTask = observedTasks.first { it.id == taskId }
        val updateTaskRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateTask(fetchedTask.copy(completed = true, status = "Done"))
        }
        assertTrue(updateTaskRes.isSuccess)

        val logId = "log_${UUID.randomUUID().toString().replace("-", "")}"
        val logRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createTimeLog(
                TimeLogItem(
                    id = logId,
                    userId = uid,
                    projectName = "Web Redesign",
                    taskDescription = "Design tokens & hero polish",
                    durationMinutes = 120L,
                    hourlyRate = 110L,
                    loggedDate = "Apr 8, 2026",
                    billable = true
                )
            )
        }
        assertTrue(logRes.isSuccess)

        val settingsResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveUserSettings(
                UserSettingsItem(
                    id = uid,
                    userId = uid,
                    themeMode = "Dark",
                    themePreset = "Cyberpunk",
                    accentColor = "Violet",
                    fontFamily = "Poppins"
                )
            )
        }
        assertTrue(settingsResult.isSuccess)

        // Update existing user settings preserving createdAt
        val settingsUpdateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveUserSettings(
                UserSettingsItem(
                    id = uid,
                    userId = uid,
                    themeMode = "Light",
                    themePreset = "Daylight",
                    accentColor = "Emerald",
                    fontFamily = "Roboto"
                )
            )
        }
        assertTrue(settingsUpdateResult.isSuccess)
    }

    @Test
    fun ensureSeededOrReset_populatesFullDemoDataset_succeeds() = runBlocking {
        val uid = signInTestUser("seed_test_${UUID.randomUUID().toString().take(6)}@solowork.test")
        val repository = SoloWorkRepository(firestore)

        val seedRes = withTimeout(10_000L) {
            repository.ensureSeededOrReset(forceReset = true)
        }
        assertTrue("Expected demo seeding to succeed, got ${seedRes.exceptionOrNull()}", seedRes.isSuccess)

        val projects = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProjects(uid).first { it.size == 6 }
        }
        assertEquals(6, projects.size)

        val clients = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeClients(uid).first { it.size == 8 }
        }
        assertEquals(8, clients.size)

        val invoices = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeInvoices(uid).first { it.size == 5 }
        }
        val paidCents = invoices.filter { it.status == "Paid" }.sumOf { it.amountCents }
        assertEquals(1245000L, paidCents) // $12,450.00

        val timeLogs = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTimeLogs(uid).first { it.size == 4 }
        }
        val totalMinutes = timeLogs.sumOf { it.durationMinutes }
        assertEquals(2550L, totalMinutes) // 42.5 hours

        // Verify clearing all example data leaves a clean empty workspace and does not auto-reseed
        val clearRes = withTimeout(10_000L) {
            repository.clearAllUserData()
        }
        assertTrue("Expected clearAllUserData to succeed", clearRes.isSuccess)

        val emptyProjects = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProjects(uid).first { it.isEmpty() }
        }
        assertTrue(emptyProjects.isEmpty())

        // Calling ensureSeededOrReset(forceReset = false) after clearing must NOT reseed example data
        val checkSeedAfterClear = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureSeededOrReset(forceReset = false)
        }
        assertTrue(checkSeedAfterClear.isSuccess)
        val stillEmptyProjects = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProjects(uid).first()
        }
        assertTrue(stillEmptyProjects.isEmpty())
    }

    @Test
    fun getProjectById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = SoloWorkRepository(firestore)
        val uniqueId = "proj_${UUID.randomUUID().toString().replace("-", "")}"

        val projectId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createProject(
                ProjectItem(
                    id = uniqueId,
                    userId = aliceUid,
                    name = "Alice Secret Project",
                    clientName = "Orbit",
                    progress = 40L,
                    budget = 5000L
                )
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = SoloWorkRepository(firestore)
        val result = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getProjectById(projectId) }
        assertTrue(result.isFailure)
        val firestoreEx = result.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun observeProjects_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = SoloWorkRepository(firestore)

        var caughtException: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeProjects("unauthenticated_uid").first()
            }
        } catch (e: Throwable) {
            caughtException = e
        }
        assertNotNull("Expected exception when observing unauthenticated", caughtException)
        val firestoreException = checkNotNull(caughtException?.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $caughtException"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@solowork.test"
        const val BOB_EMAIL = "bob@solowork.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
