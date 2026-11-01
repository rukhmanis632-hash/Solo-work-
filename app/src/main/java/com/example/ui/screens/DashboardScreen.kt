package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PeopleOutline
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ClientItem
import com.example.data.model.InvoiceItem
import com.example.data.model.ProjectItem
import com.example.data.model.TaskItem
import com.example.data.model.TimeLogItem
import com.example.ui.components.KpiSummaryCard
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCrimson
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentViolet
import com.example.ui.theme.LocalSoloWorkPalette
import com.example.ui.viewmodel.SoloWorkTab
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DashboardView(
    projects: List<ProjectItem>,
    clients: List<ClientItem>,
    invoices: List<InvoiceItem>,
    tasks: List<TaskItem>,
    timeLogs: List<TimeLogItem>,
    periodFilter: String,
    isViewingExampleData: Boolean = false,
    onPeriodFilterChange: (String) -> Unit,
    onToggleTask: (TaskItem) -> Unit,
    onQuickAddTask: (String) -> Unit,
    onEditProject: (ProjectItem, String, String, Long, Long, String, String, String) -> Unit,
    onOpenNewProjectModal: () -> Unit,
    onOpenNewTaskModal: () -> Unit,
    onNavigateToTab: (SoloWorkTab) -> Unit,
    onClearExampleData: () -> Unit = {},
    onLoadExampleData: () -> Unit = {}
) {
    val palette = LocalSoloWorkPalette.current
    val cornerRadius = if (palette.customizeLayoutStyle) 18.dp else 10.dp

    var selectedProjectForEdit by remember { mutableStateOf<ProjectItem?>(null) }

    // Compute 100% real dynamic KPI metrics directly from Firestore records (zero when workspace is empty)
    val paidInvoices = invoices.filter { it.status == "Paid" }
    val paidCents = paidInvoices.sumOf { it.amountCents }
    val pendingInvoicesList = invoices.filter { it.status == "Pending" || it.status == "Overdue" }
    val pendingCents = pendingInvoicesList.sumOf { it.amountCents }

    val totalRevenueDollars = paidCents / 100L
    val totalMinutes = timeLogs.sumOf { it.durationMinutes }
    val hoursTrackedFormatted = String.format(Locale.US, "%.1fh", totalMinutes / 60.0)
    val activeClientsCount = clients.count { it.status == "Active" }
    val pendingInvoicesCount = pendingInvoicesList.size

    // Dynamic sparkline points derived strictly from real records (flat zero line when empty)
    val revenueSparkline = remember(paidInvoices) {
        if (paidInvoices.isEmpty()) listOf(0f, 0f, 0f, 0f)
        else paidInvoices.map { (it.amountCents / 100f).coerceAtLeast(1f) }.let { pts ->
            if (pts.size == 1) listOf(0f, pts.first() * 0.5f, pts.first() * 0.8f, pts.first()) else pts
        }
    }
    val hoursSparkline = remember(timeLogs) {
        if (timeLogs.isEmpty()) listOf(0f, 0f, 0f, 0f)
        else timeLogs.map { (it.durationMinutes / 60f).coerceAtLeast(0.5f) }.let { pts ->
            if (pts.size == 1) listOf(0f, pts.first() * 0.5f, pts.first() * 0.8f, pts.first()) else pts
        }
    }
    val clientsSparkline = remember(clients) {
        if (clients.isEmpty()) listOf(0f, 0f, 0f, 0f)
        else clients.map { it.totalBilled.toFloat().coerceAtLeast(1f) }.let { pts ->
            if (pts.size == 1) listOf(0f, pts.first() * 0.5f, pts.first() * 0.8f, pts.first()) else pts
        }
    }
    val pendingSparkline = remember(pendingInvoicesList) {
        if (pendingInvoicesList.isEmpty()) listOf(0f, 0f, 0f, 0f)
        else pendingInvoicesList.map { (it.amountCents / 100f).coerceAtLeast(1f) }.let { pts ->
            if (pts.size == 1) listOf(0f, pts.first() * 0.5f, pts.first() * 0.8f, pts.first()) else pts
        }
    }

    val currencyFormatter = remember { NumberFormat.getNumberInstance(Locale.US) }
    val isWorkspaceCompletelyEmpty = projects.isEmpty() && clients.isEmpty() && invoices.isEmpty() && tasks.isEmpty() && timeLogs.isEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Optional Demo / Example Mode Banner with 1-tap "Clear Example Data & Start Fresh"
        if (isViewingExampleData) {
            Surface(
                shape = RoundedCornerShape(cornerRadius),
                color = AccentAmber.copy(alpha = 0.14f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, AccentAmber, RoundedCornerShape(cornerRadius))
                    .testTag("viewing_example_data_banner")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AccentAmber)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Viewing Example Data",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Interactive Starter Tour",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "You are exploring sample projects, clients, and invoices. Clear them anytime in 1 tap to start with a clean workspace at zero.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onClearExampleData,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("clear_example_data_button")
                        ) {
                            Text(
                                text = "Clear Example Data & Start Fresh",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else if (isWorkspaceCompletelyEmpty) {
            // Clean Slate Onboarding Banner when workspace has 0 records
            Surface(
                shape = RoundedCornerShape(cornerRadius),
                color = palette.cardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.border, RoundedCornerShape(cornerRadius))
                    .testTag("clean_workspace_banner")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Clean Workspace Ready",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Your workspace is empty and all counters are initialized at zero. Create your first real project or load the optional starter example tour.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onLoadExampleData,
                            modifier = Modifier.testTag("load_example_data_button")
                        ) {
                            Text("Load Example Data")
                        }
                        Button(
                            onClick = onOpenNewProjectModal,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+ First Project")
                        }
                    }
                }
            }
        }

        // 1. 4 KPI Summary Cards with colored SVG-style Sparklines (tappable to drill down)
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isWide = maxWidth >= 680.dp
            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiSummaryCard(
                        title = "Total Revenue",
                        value = "$${currencyFormatter.format(totalRevenueDollars)}",
                        icon = Icons.Default.AttachMoney,
                        sparklineColor = AccentIndigo,
                        sparklinePoints = revenueSparkline,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToTab(SoloWorkTab.Invoices) }
                    )
                    KpiSummaryCard(
                        title = "Hours Tracked",
                        value = hoursTrackedFormatted,
                        icon = Icons.Default.AccessTime,
                        sparklineColor = AccentEmerald,
                        sparklinePoints = hoursSparkline,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToTab(SoloWorkTab.TimeTracker) }
                    )
                    KpiSummaryCard(
                        title = "Active Clients",
                        value = activeClientsCount.toString(),
                        icon = Icons.Default.PeopleOutline,
                        sparklineColor = AccentAmber,
                        sparklinePoints = clientsSparkline,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToTab(SoloWorkTab.Clients) }
                    )
                    KpiSummaryCard(
                        title = "Pending Invoices",
                        value = pendingInvoicesCount.toString(),
                        icon = Icons.Default.Description,
                        sparklineColor = AccentCrimson,
                        sparklinePoints = pendingSparkline,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToTab(SoloWorkTab.Invoices) }
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KpiSummaryCard(
                            title = "Total Revenue",
                            value = "$${currencyFormatter.format(totalRevenueDollars)}",
                            icon = Icons.Default.AttachMoney,
                            sparklineColor = AccentIndigo,
                            sparklinePoints = revenueSparkline,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToTab(SoloWorkTab.Invoices) }
                        )
                        KpiSummaryCard(
                            title = "Hours Tracked",
                            value = hoursTrackedFormatted,
                            icon = Icons.Default.AccessTime,
                            sparklineColor = AccentEmerald,
                            sparklinePoints = hoursSparkline,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToTab(SoloWorkTab.TimeTracker) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KpiSummaryCard(
                            title = "Active Clients",
                            value = activeClientsCount.toString(),
                            icon = Icons.Default.PeopleOutline,
                            sparklineColor = AccentAmber,
                            sparklinePoints = clientsSparkline,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToTab(SoloWorkTab.Clients) }
                        )
                        KpiSummaryCard(
                            title = "Pending Invoices",
                            value = pendingInvoicesCount.toString(),
                            icon = Icons.Default.Description,
                            sparklineColor = AccentCrimson,
                            sparklinePoints = pendingSparkline,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToTab(SoloWorkTab.Invoices) }
                        )
                    }
                }
            }
        }

        // 1B. Gemini AI Copilot Quick-Launch & 1-Tap Quick-Start Studio Bar
        Surface(
            shape = RoundedCornerShape(cornerRadius),
            color = palette.cardBg,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.accent.copy(alpha = 0.5f), RoundedCornerShape(cornerRadius))
                .clickable { onNavigateToTab(SoloWorkTab.AiCopilot) }
                .testTag("dashboard_ai_copilot_banner")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.accent.copy(alpha = 0.20f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "SoloWork AI Copilot",
                                tint = palette.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "SoloWork AI Copilot • Powered by Gemini 3.8 Flash",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "1-click project scoping, multi-turn AI chat, SOW proposals & invoice reminders",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.accent)
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "Open AI Studio",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // 1-Tap Quick-Start Workflow Pills (horizontally scrollable on narrow screens so labels never truncate)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "✨ AI Scope Project" to SoloWorkTab.AiCopilot,
                        "⏱️ Track Time" to SoloWorkTab.TimeTracker,
                        "🧾 Issue Invoice" to SoloWorkTab.Invoices,
                        "📋 Kanban Board" to SoloWorkTab.TaskBoard
                    ).forEach { (label, targetTab) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.cardElevatedBg)
                                .border(1.dp, palette.border, RoundedCornerShape(8.dp))
                                .clickable { onNavigateToTab(targetTab) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // 2. Dashboard Main Widgets Row (Recent Projects Table + Tasks & Invoices Card)
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isTwoColumn = maxWidth >= 740.dp
            if (isTwoColumn) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    RecentProjectsWidget(
                        projects = projects,
                        periodFilter = periodFilter,
                        onPeriodFilterChange = onPeriodFilterChange,
                        onSelectProject = { selectedProjectForEdit = it },
                        onOpenNewProjectModal = onOpenNewProjectModal,
                        cornerRadius = cornerRadius,
                        modifier = Modifier.weight(1.45f)
                    )
                    TasksAndInvoicesWidget(
                        tasks = tasks,
                        paidDollarsFormatted = "$${currencyFormatter.format(totalRevenueDollars)}",
                        pendingFormatted = String.format(Locale.US, "$%.2f", pendingCents / 100.0),
                        onToggleTask = onToggleTask,
                        onQuickAddTask = onQuickAddTask,
                        onOpenNewTaskModal = onOpenNewTaskModal,
                        onNavigateToInvoices = { onNavigateToTab(SoloWorkTab.Invoices) },
                        cornerRadius = cornerRadius,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    RecentProjectsWidget(
                        projects = projects,
                        periodFilter = periodFilter,
                        onPeriodFilterChange = onPeriodFilterChange,
                        onSelectProject = { selectedProjectForEdit = it },
                        onOpenNewProjectModal = onOpenNewProjectModal,
                        cornerRadius = cornerRadius,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TasksAndInvoicesWidget(
                        tasks = tasks,
                        paidDollarsFormatted = "$${currencyFormatter.format(totalRevenueDollars)}",
                        pendingFormatted = String.format(Locale.US, "$%.2f", pendingCents / 100.0),
                        onToggleTask = onToggleTask,
                        onQuickAddTask = onQuickAddTask,
                        onOpenNewTaskModal = onOpenNewTaskModal,
                        onNavigateToInvoices = { onNavigateToTab(SoloWorkTab.Invoices) },
                        cornerRadius = cornerRadius,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    selectedProjectForEdit?.let { project ->
        EditProjectDialog(
            project = project,
            onDismiss = { selectedProjectForEdit = null },
            onSave = { name, client, budget, progress, due, status, category ->
                onEditProject(project, name, client, budget, progress, due, status, category)
                selectedProjectForEdit = null
            }
        )
    }
}

@Composable
private fun RecentProjectsWidget(
    projects: List<ProjectItem>,
    periodFilter: String,
    onPeriodFilterChange: (String) -> Unit,
    onSelectProject: (ProjectItem) -> Unit,
    onOpenNewProjectModal: () -> Unit,
    cornerRadius: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val palette = LocalSoloWorkPalette.current
    var dropdownExpanded by remember { mutableStateOf(false) }

    val filteredProjects = remember(projects, periodFilter) {
        when (periodFilter) {
            "Active Only" -> projects.filter { it.status == "Active" }
            "High Progress" -> projects.filter { it.progress >= 50L }
            else -> projects
        }
    }

    Surface(
        modifier = modifier
            .border(1.dp, palette.border, RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        color = palette.cardBg,
        tonalElevation = if (palette.customizeLayoutStyle) 4.dp else 0.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Projects",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.cardElevatedBg)
                                .border(1.dp, palette.border, RoundedCornerShape(8.dp))
                                .clickable { dropdownExpanded = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = periodFilter,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = "Filter period",
                                tint = palette.textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            listOf("All Periods", "Active Only", "High Progress").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        onPeriodFilterChange(option)
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.accent)
                            .clickable { onOpenNewProjectModal() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("dashboard_add_project_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Project",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "New",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Table Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.cardElevatedBg)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Project",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    modifier = Modifier.weight(1.5f)
                )
                Text(
                    text = "Client",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    modifier = Modifier.weight(0.7f)
                )
                Text(
                    text = "Progress",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    modifier = Modifier.weight(1.2f)
                )
                Text(
                    text = "Date",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    modifier = Modifier.weight(0.9f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (filteredProjects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No projects yet. Tap '+ New' above to create your first project.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
            }

            filteredProjects.take(6).forEachIndexed { idx, project ->
                val badgeColor = when (idx % 4) {
                    0 -> AccentIndigo
                    1 -> AccentEmerald
                    2 -> AccentAmber
                    else -> AccentViolet
                }
                val badgeIcon = when (project.category) {
                    "Branding" -> Icons.Default.Palette
                    "Development" -> Icons.Default.Code
                    "Strategy" -> Icons.Default.Hub
                    else -> Icons.Default.Computer
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectProject(project) }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Project Icon + Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(badgeColor.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = badgeIcon,
                                contentDescription = project.name,
                                tint = badgeColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Client Avatar Badge
                    Box(
                        modifier = Modifier.weight(0.7f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(badgeColor.copy(alpha = 0.25f))
                                .border(1.5.dp, badgeColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = project.clientName.take(1).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Progress Bar + Percentage
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1.2f)
                            .padding(end = 8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { (project.progress / 100f).coerceIn(0f, 1f) },
                            color = badgeColor,
                            trackColor = palette.cardElevatedBg,
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${project.progress}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Due Date
                    Text(
                        text = project.dueDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(0.9f)
                    )
                }

                if (idx < filteredProjects.take(6).lastIndex) {
                    HorizontalDivider(color = palette.border.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun TasksAndInvoicesWidget(
    tasks: List<TaskItem>,
    paidDollarsFormatted: String,
    pendingFormatted: String,
    onToggleTask: (TaskItem) -> Unit,
    onQuickAddTask: (String) -> Unit,
    onOpenNewTaskModal: () -> Unit,
    onNavigateToInvoices: () -> Unit,
    cornerRadius: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val palette = LocalSoloWorkPalette.current
    var quickWidgetExpanded by remember { mutableStateOf(true) }
    var invoiceStatusExpanded by remember { mutableStateOf(true) }
    var quickTaskTitle by remember { mutableStateOf("") }

    Surface(
        modifier = modifier
            .border(1.dp, palette.border, RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        color = palette.cardBg,
        tonalElevation = if (palette.customizeLayoutStyle) 4.dp else 0.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tasks & Invoices",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "Add quick task",
                    tint = palette.textSecondary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onOpenNewTaskModal() }
                        .padding(4.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Widget Collapsible Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { quickWidgetExpanded = !quickWidgetExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick widget",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary
                )
                Icon(
                    imageVector = if (quickWidgetExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle Quick Widget",
                    tint = palette.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = quickWidgetExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (tasks.isEmpty()) {
                        Text(
                            text = "No tasks yet. Add your first checklist item below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    tasks.take(4).forEach { task ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onToggleTask(task) }
                                .padding(vertical = 3.dp)
                        ) {
                            Checkbox(
                                checked = task.completed,
                                onCheckedChange = { onToggleTask(task) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = palette.accent,
                                    uncheckedColor = palette.textSecondary
                                ),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (task.completed) palette.textSecondary else palette.textPrimary,
                                textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            val dotColor = when (task.priority) {
                                "High" -> AccentCrimson
                                "Medium" -> AccentAmber
                                else -> palette.border
                            }
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                        }
                    }

                    // Inline Quick-Add Task Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = quickTaskTitle,
                            onValueChange = { quickTaskTitle = it },
                            placeholder = {
                                Text("Add checklist item...", style = MaterialTheme.typography.labelSmall)
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                if (quickTaskTitle.isNotBlank()) {
                                    onQuickAddTask(quickTaskTitle)
                                    quickTaskTitle = ""
                                } else {
                                    onOpenNewTaskModal()
                                }
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.accent)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Quick add task",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = palette.border)
            Spacer(modifier = Modifier.height(12.dp))

            // Invoice Status Collapsible Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { invoiceStatusExpanded = !invoiceStatusExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Invoice Status",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary
                )
                Icon(
                    imageVector = if (invoiceStatusExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle Invoice Status",
                    tint = palette.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = invoiceStatusExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Paid Badge Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.cardElevatedBg)
                            .clickable { onNavigateToInvoices() }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Paid",
                                tint = AccentEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Paid",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textPrimary
                            )
                        }
                        Text(
                            text = paidDollarsFormatted,
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Pending Badge Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.cardElevatedBg.copy(alpha = 0.6f))
                            .clickable { onNavigateToInvoices() }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.RemoveCircle,
                                contentDescription = "Pending",
                                tint = AccentAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pending",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textPrimary
                            )
                        }
                        Text(
                            text = pendingFormatted,
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditProjectDialog(
    project: ProjectItem,
    onDismiss: () -> Unit,
    onSave: (String, String, Long, Long, String, String, String) -> Unit
) {
    val palette = LocalSoloWorkPalette.current
    var name by remember(project.id) { mutableStateOf(project.name) }
    var client by remember(project.id) { mutableStateOf(project.clientName) }
    var budgetStr by remember(project.id) { mutableStateOf(project.budget.toString()) }
    var progressFloat by remember(project.id) { mutableFloatStateOf(project.progress.toFloat()) }
    var dueDate by remember(project.id) { mutableStateOf(project.dueDate) }
    var status by remember(project.id) { mutableStateOf(project.status) }
    var category by remember(project.id) { mutableStateOf(project.category) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Project & Milestone Progress") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Project Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = client, onValueChange = { client = it }, label = { Text("Client Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = budgetStr, onValueChange = { budgetStr = it }, label = { Text("Budget (USD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Completion Progress", style = MaterialTheme.typography.labelLarge)
                        Text("${progressFloat.toInt()}%", style = MaterialTheme.typography.labelLarge, color = palette.accent, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = progressFloat,
                        onValueChange = { progressFloat = it },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = palette.accent, activeTrackColor = palette.accent)
                    )
                }

                Text("Status", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Active", "Completed", "On Hold").forEach { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = { Text(s) }
                        )
                    }
                }

                Text("Discipline", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Web Design", "Branding", "Development", "Strategy").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                OutlinedTextField(value = dueDate, onValueChange = { dueDate = it }, label = { Text("Due Date") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name.ifBlank { project.name },
                        client.ifBlank { project.clientName },
                        budgetStr.toLongOrNull() ?: project.budget,
                        progressFloat.toLong(),
                        dueDate.ifBlank { project.dueDate },
                        status,
                        category
                    )
                }
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
