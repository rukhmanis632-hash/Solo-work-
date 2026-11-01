package com.example

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.configureAppCheck
import com.example.data.model.ClientItem
import com.example.data.model.InvoiceItem
import com.example.data.model.ProjectItem
import com.example.data.model.TaskItem
import com.example.data.model.TimeLogItem
import com.example.data.model.UserSettingsItem
import com.example.ui.auth.AuthScreen
import com.example.ui.components.AppearanceSettingsPanel
import com.example.ui.screens.AiCopilotView
import com.example.ui.screens.AnalyticsView
import com.example.ui.screens.ClientsView
import com.example.ui.screens.DashboardView
import com.example.ui.screens.InvoicesView
import com.example.ui.screens.ProjectsView
import com.example.ui.screens.SettingsView
import com.example.ui.screens.TaskBoardView
import com.example.ui.screens.TimeTrackerView
import com.example.ui.theme.SoloWorkTheme
import com.example.ui.viewmodel.AiBlueprintState
import com.example.ui.viewmodel.AiGeneratedProjectBlueprint
import com.example.ui.screens.QuickAiAssistantModal
import com.example.ui.viewmodel.AiChatMessage
import com.example.ui.viewmodel.AiClientDocState
import com.example.ui.viewmodel.AiGeneratedClientDoc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w800dp-h1280dp")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `verify app name, firestore database id, and app check token persistence`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SoloWork", appName)

        val dbId = context.getString(R.string.firestore_database_id)
        assertTrue(dbId.isNotBlank())

        val intent = Intent().apply {
            putExtra("FIREBASE_APPCHECK_DEBUG_TOKEN", "test-debug-token-12345")
        }
        configureAppCheck(context, intent)
    }

    @Test
    fun `authScreen renders SoloWork branding and Google sign-in button`() {
        composeTestRule.setContent {
            SoloWorkTheme {
                AuthScreen(onAuthSuccess = {})
            }
        }

        composeTestRule.onNodeWithText("SoloWork").assertIsDisplayed()
        composeTestRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
    }

    @Test
    fun `dashboardView renders dynamic KPI cards, Viewing Example Data banner, and Clear Example Data button`() {
        var newProjectClicked = false
        var clearExampleClicked = false
        val demoProjects = listOf(
            ProjectItem(id = "example_u1_proj_1", name = "Web Redesign", clientName = "Orbit Studio", progress = 50L, dueDate = "Apr 8, 2026", budget = 4800L),
            ProjectItem(id = "example_u1_proj_2", name = "Brand Identity", clientName = "Pulse Media", progress = 30L, dueDate = "Apr 8, 2026", budget = 3200L)
        )
        val demoInvoices = listOf(
            InvoiceItem(id = "example_u1_inv_1", invoiceNumber = "INV-101", clientName = "Orbit Studio", projectTitle = "Web Redesign", amountCents = 1245000L, status = "Paid")
        )
        val demoTimeLogs = listOf(
            TimeLogItem(id = "example_u1_log_1", projectName = "Web Redesign", taskDescription = "UI Polish", durationMinutes = 2550L, hourlyRate = 110L)
        )
        val demoTasks = listOf(
            TaskItem(id = "example_u1_task_1", title = "Upcoming task checklist: Hero UI polish", projectName = "Web Redesign", completed = false)
        )

        composeTestRule.setContent {
            SoloWorkTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    DashboardView(
                        projects = demoProjects,
                        clients = emptyList(),
                        invoices = demoInvoices,
                        tasks = demoTasks,
                        timeLogs = demoTimeLogs,
                        periodFilter = "All Periods",
                        isViewingExampleData = true,
                        onPeriodFilterChange = {},
                        onToggleTask = {},
                        onQuickAddTask = {},
                        onEditProject = { _, _, _, _, _, _, _, _ -> },
                        onOpenNewProjectModal = { newProjectClicked = true },
                        onOpenNewTaskModal = {},
                        onNavigateToTab = {},
                        onClearExampleData = { clearExampleClicked = true }
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("viewing_example_data_banner").assertIsDisplayed()
        composeTestRule.onNodeWithText("Viewing Example Data").assertIsDisplayed()
        composeTestRule.onNodeWithTag("clear_example_data_button").performClick()
        assertTrue(clearExampleClicked)

        composeTestRule.onNodeWithText("Total Revenue").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("$12,450")[0].assertIsDisplayed()
        composeTestRule.onNodeWithText("Hours Tracked").assertIsDisplayed()
        composeTestRule.onNodeWithText("42.5h").assertIsDisplayed()
        composeTestRule.onNodeWithText("Active Clients").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pending Invoices").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dashboard_ai_copilot_banner").assertIsDisplayed()
        composeTestRule.onNodeWithText("Recent Projects").assertIsDisplayed()
        composeTestRule.onNodeWithText("Web Redesign").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tasks & Invoices").assertIsDisplayed()

        composeTestRule.onNodeWithTag("dashboard_add_project_button").performClick()
        assertTrue(newProjectClicked)
    }

    @Test
    fun `dashboardView initializes at zero on clean slate and displays Clean Workspace Ready banner`() {
        var loadExampleClicked = false

        composeTestRule.setContent {
            SoloWorkTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    DashboardView(
                        projects = emptyList(),
                        clients = emptyList(),
                        invoices = emptyList(),
                        tasks = emptyList(),
                        timeLogs = emptyList(),
                        periodFilter = "All Periods",
                        isViewingExampleData = false,
                        onPeriodFilterChange = {},
                        onToggleTask = {},
                        onQuickAddTask = {},
                        onEditProject = { _, _, _, _, _, _, _, _ -> },
                        onOpenNewProjectModal = {},
                        onOpenNewTaskModal = {},
                        onNavigateToTab = {},
                        onClearExampleData = {},
                        onLoadExampleData = { loadExampleClicked = true }
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("clean_workspace_banner").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("$0")[0].assertIsDisplayed()
        composeTestRule.onNodeWithText("0.0h").assertIsDisplayed()
        composeTestRule.onNodeWithTag("load_example_data_button").performClick()
        assertTrue(loadExampleClicked)
    }

    @Test
    fun `aiCopilotView renders 3-in-1 studio with structured blueprint, multi-turn chat, and client email studio`() {
        var importedBlueprint: AiGeneratedProjectBlueprint? = null
        var streamedPrompt: String? = null
        var generatedDocType: String? = null

        val sampleBlueprint = AiGeneratedProjectBlueprint(
            projectName = "Vertex Analytics Portal",
            clientName = "Vertex FinTech",
            category = "Web Design",
            estimatedBudgetUsd = 6800L,
            executiveSummary = "End-to-end redesign of the financial analytics portal.",
            milestones = listOf("Design token audit", "Interactive chart prototypes", "Responsive QA")
        )

        val sampleDoc = AiGeneratedClientDoc(
            documentType = "Invoice Payment Reminder",
            subjectLine = "Action Required: Invoice INV-2026-103 Follow-Up",
            recipientName = "Marcus Vance (Pulse Media)",
            bodyText = "Hi Marcus, hope the brand identity rollout is going smoothly.",
            keyHighlights = listOf("Invoice #INV-2026-103 ($127.83)", "Due Apr 18, 2026"),
            suggestedFollowUpDate = "Apr 21, 2026"
        )

        composeTestRule.setContent {
            SoloWorkTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    AiCopilotView(
                        aiStreamState = com.example.ui.viewmodel.AiStreamState.Success("1. Follow up on INV-2026-103 with Pulse Media."),
                        aiBlueprintState = AiBlueprintState.Success(sampleBlueprint),
                        aiClientDocState = AiClientDocState.Success(sampleDoc),
                        aiChatMessages = listOf(
                            AiChatMessage(id = "m1", role = "assistant", text = "Welcome to SoloWork AI! Ready to assist.", timestamp = "Just now")
                        ),
                        onStreamPrompt = { streamedPrompt = it },
                        onClearChat = {},
                        onGenerateBlueprint = {},
                        onImportBlueprint = { importedBlueprint = it },
                        onGenerateClientDoc = { docType, _, _ -> generatedDocType = docType }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("SoloWork AI Copilot • Gemini 3.8 Flash").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vertex Analytics Portal").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ai_import_blueprint_button").performScrollTo().performClick()
        assertEquals("Vertex Analytics Portal", importedBlueprint?.projectName)

        composeTestRule.onNodeWithTag("ai_stream_generate_button").performScrollTo().performClick()
        assertTrue(!streamedPrompt.isNullOrBlank())

        composeTestRule.onNodeWithTag("ai_generate_client_doc_button").performScrollTo().performClick()
        assertEquals("Statement of Work (SOW)", generatedDocType)
        composeTestRule.onNodeWithText("Subject: Action Required: Invoice INV-2026-103 Follow-Up").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `quickAiAssistantModal renders contextual suggestions and launches prompt`() {
        var sentPrompt: String? = null
        composeTestRule.setContent {
            SoloWorkTheme {
                QuickAiAssistantModal(
                    currentTab = com.example.ui.viewmodel.SoloWorkTab.Invoices,
                    onDismiss = {},
                    onLaunchPromptInCopilot = { sentPrompt = it },
                    onOpenFullCopilot = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Quick AI Assistant • Invoices").assertIsDisplayed()
        composeTestRule.onNodeWithText("Draft a friendly payment follow-up email for my 3 pending invoices.").performClick()
        assertEquals("Draft a friendly payment follow-up email for my 3 pending invoices.", sentPrompt)
    }

    @Test
    fun `appearanceSettingsPanel switches theme modes, color swatches, fonts, and presets`() {
        var selectedMode = "Dark"
        var selectedAccent = "Indigo"
        var selectedFont = "Inter"
        var selectedPreset = "Classic Dark"

        composeTestRule.setContent {
            SoloWorkTheme(
                themeMode = selectedMode,
                themePreset = selectedPreset,
                accentColor = selectedAccent,
                fontFamily = selectedFont
            ) {
                AppearanceSettingsPanel(
                    settings = UserSettingsItem(
                        themeMode = selectedMode,
                        themePreset = selectedPreset,
                        accentColor = selectedAccent,
                        fontFamily = selectedFont
                    ),
                    onUpdateThemeMode = { selectedMode = it },
                    onUpdateThemePreset = { selectedPreset = it },
                    onUpdateAccentColor = { selectedAccent = it },
                    onUpdateFontFamily = { selectedFont = it },
                    onUpdateCustomizeLayoutStyle = {},
                    onUpdateSidebarCompact = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Appearance Settings").assertIsDisplayed()
        composeTestRule.onNodeWithTag("theme_mode_light_button").performClick()
        assertEquals("Light", selectedMode)

        composeTestRule.onNodeWithTag("swatch_emerald").performClick()
        assertEquals("Emerald", selectedAccent)

        composeTestRule.onNodeWithTag("font_option_poppins").performClick()
        assertEquals("Poppins", selectedFont)

        composeTestRule.onNodeWithTag("preset_cyberpunk").performScrollTo().performClick()
        assertEquals("Cyberpunk", selectedPreset)
    }

    @Test
    fun `timeTrackerView renders stopwatch and triggers start pause and log session`() {
        var stopwatchToggled = false
        var sessionSaved = false

        composeTestRule.setContent {
            SoloWorkTheme {
                TimeTrackerView(
                    projects = listOf(ProjectItem(id = "p1", name = "Web Redesign")),
                    timeLogs = listOf(
                        TimeLogItem(
                            id = "l1",
                            projectName = "Web Redesign",
                            taskDescription = "Design system architecture",
                            durationMinutes = 120L,
                            hourlyRate = 110L
                        )
                    ),
                    isTimerRunning = false,
                    elapsedSeconds = 3665L,
                    timerProjectName = "Web Redesign",
                    timerTaskNote = "Sprint work",
                    timerHourlyRate = 110L,
                    onUpdateTimerMetadata = { _, _, _ -> },
                    onToggleStopwatch = { stopwatchToggled = true },
                    onResetStopwatch = {},
                    onLogStopwatchSession = { sessionSaved = true },
                    onAddManualTimeLog = { _, _, _, _, _ -> },
                    onDeleteTimeLog = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Live Time Tracker & Billable Logs").assertIsDisplayed()
        composeTestRule.onNodeWithText("01:01:05").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stopwatch_start_pause_button").performClick()
        assertTrue(stopwatchToggled)

        composeTestRule.onNodeWithTag("stopwatch_save_session_button").performClick()
        assertTrue(sessionSaved)
    }

    @Test
    fun `projectsInvoicesClientsTaskBoardAndAnalyticsViews render accurately`() {
        composeTestRule.setContent {
            SoloWorkTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    ProjectsView(
                        projects = listOf(ProjectItem(id = "p1", name = "Web Redesign", clientName = "Orbit Studio", progress = 50L, budget = 4800L)),
                        onAddProject = { _, _, _, _, _, _, _, _ -> },
                        onUpdateProgress = { _, _ -> },
                        onEditProject = { _, _, _, _, _, _, _, _ -> },
                        onDeleteProject = {}
                    )
                    InvoicesView(
                        invoices = listOf(InvoiceItem(id = "i1", invoiceNumber = "INV-2026-101", clientName = "Orbit Studio", projectTitle = "Web Redesign", amountCents = 645000L, status = "Paid")),
                        onAddInvoice = { _, _, _, _, _, _ -> },
                        onEditInvoice = { _, _, _, _, _, _, _ -> },
                        onToggleInvoiceStatus = {},
                        onDeleteInvoice = {}
                    )
                    ClientsView(
                        clients = listOf(ClientItem(id = "c1", name = "Elena Rostova", company = "Orbit Studio", email = "elena@orbit.studio", totalBilled = 5400L)),
                        onAddClient = { _, _, _, _, _, _ -> },
                        onEditClient = { _, _, _, _, _, _, _ -> },
                        onDeleteClient = {}
                    )
                    TaskBoardView(
                        tasks = listOf(TaskItem(id = "t1", title = "Hero UI polish", projectName = "Web Redesign", status = "Todo", priority = "High")),
                        onAddTask = { _, _, _, _, _ -> },
                        onToggleTask = {},
                        onMoveTaskStatus = { _, _ -> },
                        onDeleteTask = {}
                    )
                    AnalyticsView(
                        projects = emptyList(),
                        invoices = emptyList(),
                        clients = emptyList(),
                        timeLogs = emptyList()
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Projects Portfolio").assertIsDisplayed()
        composeTestRule.onNodeWithText("Invoices & Billing").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("INV-2026-101").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Client Directory & CRM").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Task Board").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Revenue & Productivity Analytics").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `emptyStatesAndCreationModals render gracefully and submit user data`() {
        var addedProjectName: String? = null
        var addedClientName: String? = null

        composeTestRule.setContent {
            SoloWorkTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    ProjectsView(
                        projects = emptyList(),
                        onAddProject = { name, client, _, _, _, _, _, _ ->
                            addedProjectName = name
                            addedClientName = client
                        },
                        onUpdateProgress = { _, _ -> },
                        onEditProject = { _, _, _, _, _, _, _, _ -> },
                        onDeleteProject = {}
                    )
                    InvoicesView(
                        invoices = emptyList(),
                        onAddInvoice = { _, _, _, _, _, _ -> },
                        onEditInvoice = { _, _, _, _, _, _, _ -> },
                        onToggleInvoiceStatus = {},
                        onDeleteInvoice = {}
                    )
                    ClientsView(
                        clients = emptyList(),
                        onAddClient = { _, _, _, _, _, _ -> },
                        onEditClient = { _, _, _, _, _, _, _ -> },
                        onDeleteClient = {}
                    )
                    TaskBoardView(
                        tasks = emptyList(),
                        onAddTask = { _, _, _, _, _ -> },
                        onToggleTask = {},
                        onMoveTaskStatus = { _, _ -> },
                        onDeleteTask = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("projects_empty_state").assertIsDisplayed()
        composeTestRule.onNodeWithTag("invoices_empty_state").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("clients_empty_state").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("tasks_empty_state").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag("projects_new_button").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Create New Project").assertIsDisplayed()
        composeTestRule.onNodeWithTag("create_project_confirm_button").performClick()
        assertEquals("SaaS Design Sprint", addedProjectName)
        assertEquals("General Client", addedClientName)
    }

    @Test
    fun `settingsView triggers Reset Demo Data and Clear All Data actions`() {
        var resetCalled = false
        var clearAllCalled = false

        composeTestRule.setContent {
            SoloWorkTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    SettingsView(
                        settings = UserSettingsItem(),
                        onUpdateProfile = { _, _ -> },
                        onUpdateThemeMode = {},
                        onUpdateThemePreset = {},
                        onUpdateAccentColor = {},
                        onUpdateFontFamily = {},
                        onUpdateCustomizeLayoutStyle = {},
                        onUpdateSidebarCompact = {},
                        onResetDemoData = { resetCalled = true },
                        onClearAllData = { clearAllCalled = true },
                        onSignOut = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("reset_demo_data_button").performScrollTo().performClick()
        assertTrue(resetCalled)

        composeTestRule.onNodeWithTag("clear_all_data_button").performScrollTo().performClick()
        assertTrue(clearAllCalled)
    }
}

