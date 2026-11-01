package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClientItem
import com.example.data.model.InvoiceItem
import com.example.data.model.ProjectItem
import com.example.data.model.TaskItem
import com.example.data.model.TimeLogItem
import com.example.data.model.UserSettingsItem
import com.example.ui.components.SparklineChart
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCrimson
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentViolet
import com.example.ui.theme.ClassicDarkBg
import com.example.ui.theme.CyberpunkBg
import com.example.ui.theme.DaylightBg
import com.example.ui.theme.LocalSoloWorkPalette
import com.example.ui.theme.SolarizedDarkBg
import java.text.NumberFormat
import java.util.Locale

// ============================================================================
// 1. PROJECTS DEDICATED SCREEN
// ============================================================================
@Composable
fun ProjectsView(
    projects: List<ProjectItem>,
    onAddProject: (String, String, Long, Long, String, String, String, String) -> Unit,
    onUpdateProgress: (ProjectItem, Long) -> Unit,
    onEditProject: (ProjectItem, String, String, Long, Long, String, String, String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onAiAction: (String) -> Unit = {}
) {
    val palette = LocalSoloWorkPalette.current
    var selectedStatus by remember { mutableStateOf("All") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showCreateModal by remember { mutableStateOf(false) }
    var projectToEdit by remember { mutableStateOf<ProjectItem?>(null) }

    val filtered = remember(projects, selectedStatus, selectedCategory) {
        projects.filter { p ->
            (selectedStatus == "All" || p.status == selectedStatus) &&
                (selectedCategory == "All" || p.category.equals(selectedCategory, ignoreCase = true))
        }
    }

    val totalBudget = filtered.sumOf { it.budget }
    val avgProgress = if (filtered.isEmpty()) 0L else filtered.sumOf { it.progress } / filtered.size
    val numFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Projects Portfolio",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.textPrimary
                )
                Text(
                    text = "${filtered.size} deliverables • $${numFormat.format(totalBudget)} portfolio value • $avgProgress% avg completion",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        onAiAction(
                            "Analyze my active project portfolio (${filtered.joinToString { "${it.name} at ${it.progress}%" }}) and suggest the top 3 delivery priorities for this week."
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("projects_ai_audit_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI Audit", tint = palette.accent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI Audit")
                }
                Button(
                    onClick = { showCreateModal = true },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("projects_new_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Project", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Project")
                }
            }
        }

        // Status & Category Filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Active", "Completed", "On Hold").forEach { status ->
                FilterChip(
                    selected = selectedStatus == status,
                    onClick = { selectedStatus = status },
                    label = { Text(status) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = palette.accent.copy(alpha = 0.22f),
                        selectedLabelColor = palette.accent
                    )
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            listOf("All", "Web Design", "Branding", "Development", "Strategy").forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(if (cat == "All") "All Disciplines" else cat) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentEmerald.copy(alpha = 0.20f),
                        selectedLabelColor = AccentEmerald
                    )
                )
            }
        }

        if (filtered.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.cardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.border, RoundedCornerShape(16.dp))
                    .testTag("projects_empty_state")
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "No projects in your workspace yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Create your first client project to track budgets, deadlines, and progress in Cloud Firestore.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                    Button(
                        onClick = { showCreateModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("+ Create First Project")
                    }
                }
            }
        }

        filtered.forEach { project ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.cardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.border, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = project.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = palette.textPrimary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                StatusBadge(
                                    text = project.status,
                                    color = when (project.status) {
                                        "Completed" -> AccentEmerald
                                        "On Hold" -> AccentAmber
                                        else -> palette.accent
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Client: ${project.clientName} • ${project.category} • Due ${project.dueDate}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                        }
                        Text(
                            text = "$${numFormat.format(project.budget)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = AccentEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Interactive Progress Slider + Actions
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Slider(
                            value = project.progress.toFloat(),
                            onValueChange = { newVal ->
                                onUpdateProgress(project, newVal.toLong())
                            },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = palette.accent,
                                activeTrackColor = palette.accent,
                                inactiveTrackColor = palette.cardElevatedBg
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${project.progress}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.accent,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(onClick = { projectToEdit = project }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit project",
                                tint = palette.textSecondary
                            )
                        }
                        IconButton(onClick = { onDeleteProject(project.id) }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete project",
                                tint = palette.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateModal) {
        NewProjectDialog(
            onDismiss = { showCreateModal = false },
            onConfirm = { name, client, budget, progress, due, status, category ->
                onAddProject(name, client, budget, progress, due, status, category, "#6366F1")
                showCreateModal = false
            }
        )
    }

    projectToEdit?.let { proj ->
        EditProjectDialog(
            project = proj,
            onDismiss = { projectToEdit = null },
            onSave = { name, client, budget, progress, due, status, category ->
                onEditProject(proj, name, client, budget, progress, due, status, category)
                projectToEdit = null
            }
        )
    }
}

@Composable
fun NewProjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long, Long, String, String, String) -> Unit
) {
    val palette = LocalSoloWorkPalette.current
    var name by remember { mutableStateOf("") }
    var client by remember { mutableStateOf("") }
    var budgetStr by remember { mutableStateOf("4500") }
    var progressFloat by remember { mutableStateOf(25f) }
    var dueDate by remember { mutableStateOf("May 12, 2026") }
    var category by remember { mutableStateOf("Web Design") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Project") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project Title") },
                    placeholder = { Text("e.g., SaaS Design Sprint") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = client,
                    onValueChange = { client = it },
                    label = { Text("Client Name") },
                    placeholder = { Text("e.g., Orbit Studio") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = budgetStr,
                    onValueChange = { budgetStr = it },
                    label = { Text("Budget (USD)") },
                    placeholder = { Text("4500") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Initial Progress", style = MaterialTheme.typography.labelLarge)
                        Text("${progressFloat.toInt()}%", style = MaterialTheme.typography.labelLarge, color = palette.accent, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = progressFloat,
                        onValueChange = { progressFloat = it },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = palette.accent, activeTrackColor = palette.accent)
                    )
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
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Due Date") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        name.ifBlank { "SaaS Design Sprint" },
                        client.ifBlank { "General Client" },
                        budgetStr.toLongOrNull() ?: 4500L,
                        progressFloat.toLong(),
                        dueDate.ifBlank { "Upcoming" },
                        "Active",
                        category
                    )
                },
                modifier = Modifier.testTag("create_project_confirm_button")
            ) {
                Text("Create Project")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ============================================================================
// 2. INVOICES DEDICATED SCREEN (WITH INVOICE PREVIEW & EDIT MODAL)
// ============================================================================
@Composable
fun InvoicesView(
    invoices: List<InvoiceItem>,
    onAddInvoice: (String, String, String, Double, String, String) -> Unit,
    onEditInvoice: (InvoiceItem, String, String, String, Double, String, String) -> Unit,
    onToggleInvoiceStatus: (InvoiceItem) -> Unit,
    onDeleteInvoice: (String) -> Unit,
    onAiAction: (String) -> Unit = {}
) {
    val palette = LocalSoloWorkPalette.current
    var statusFilter by remember { mutableStateOf("All") }
    var showCreateModal by remember { mutableStateOf(false) }
    var selectedInvoiceForPreview by remember { mutableStateOf<InvoiceItem?>(null) }

    val filtered = remember(invoices, statusFilter) {
        if (statusFilter == "All") invoices else invoices.filter { it.status == statusFilter }
    }

    val totalPaid = invoices.filter { it.status == "Paid" }.sumOf { it.amountCents } / 100.0
    val totalPending = invoices.filter { it.status == "Pending" || it.status == "Overdue" }.sumOf { it.amountCents } / 100.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Invoices & Billing",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.textPrimary
                )
                Text(
                    text = String.format(Locale.US, "Paid: $%,.2f • Outstanding: $%,.2f", totalPaid, totalPending),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )
            }
            Button(
                onClick = { showCreateModal = true },
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("invoices_new_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Invoice", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Invoice")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Paid", "Pending", "Overdue").forEach { status ->
                FilterChip(
                    selected = statusFilter == status,
                    onClick = { statusFilter = status },
                    label = { Text(status) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = palette.accent.copy(alpha = 0.22f),
                        selectedLabelColor = palette.accent
                    )
                )
            }
        }

        if (filtered.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = palette.cardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                    .testTag("invoices_empty_state")
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "No invoices recorded",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Issue an invoice to track paid revenue and outstanding balances in real time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                    Button(
                        onClick = { showCreateModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("+ Issue First Invoice")
                    }
                }
            }
        }

        filtered.forEach { inv ->
            val statusColor = when (inv.status) {
                "Paid" -> AccentEmerald
                "Overdue" -> AccentCrimson
                else -> AccentAmber
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = palette.cardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                    .clickable { selectedInvoiceForPreview = inv }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                    contentDescription = null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = inv.invoiceNumber,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.accent,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = inv.clientName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.textPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${inv.projectTitle} • Issued ${inv.issuedDate} • Due ${inv.dueDate}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                        }

                        Text(
                            text = String.format(Locale.US, "$%,.2f", inv.amountCents / 100.0),
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(statusColor.copy(alpha = 0.18f))
                                    .border(1.dp, statusColor, RoundedCornerShape(8.dp))
                                    .clickable { onToggleInvoiceStatus(inv) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = inv.status,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (inv.status != "Paid") {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(palette.accent.copy(alpha = 0.15f))
                                        .border(1.dp, palette.accent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            onAiAction(
                                                "Draft a polite, high-converting payment reminder email for invoice ${inv.invoiceNumber} to ${inv.clientName} (${inv.projectTitle}) for $${String.format(Locale.US, "%.2f", inv.amountCents / 100.0)} due on ${inv.dueDate}."
                                            )
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "✨ AI Remind",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.accent,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        IconButton(onClick = { onDeleteInvoice(inv.id) }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete invoice",
                                tint = palette.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateModal) {
        var number by remember { mutableStateOf("INV-2026-${(106..999).random()}") }
        var client by remember { mutableStateOf("") }
        var project by remember { mutableStateOf("") }
        var amountStr by remember { mutableStateOf("1250.00") }
        var due by remember { mutableStateOf("May 20, 2026") }

        AlertDialog(
            onDismissRequest = { showCreateModal = false },
            title = { Text("Create Client Invoice") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(value = number, onValueChange = { number = it }, label = { Text("Invoice #") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = client, onValueChange = { client = it }, label = { Text("Client Name") }, placeholder = { Text("e.g., Orbit Studio") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = project, onValueChange = { project = it }, label = { Text("Project Title") }, placeholder = { Text("e.g., Web Redesign") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = amountStr, onValueChange = { amountStr = it }, label = { Text("Amount (USD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = due, onValueChange = { due = it }, label = { Text("Due Date") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAddInvoice(
                        number.ifBlank { "INV-2026-100" },
                        client.ifBlank { "Client" },
                        project.ifBlank { "Deliverable" },
                        amountStr.toDoubleOrNull() ?: 1250.0,
                        "Pending",
                        due.ifBlank { "Due on Receipt" }
                    )
                    showCreateModal = false
                }) {
                    Text("Issue Invoice")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateModal = false }) { Text("Cancel") }
            }
        )
    }

    selectedInvoiceForPreview?.let { inv ->
        var amountStr by remember(inv.id) { mutableStateOf(String.format(Locale.US, "%.2f", inv.amountCents / 100.0)) }
        var client by remember(inv.id) { mutableStateOf(inv.clientName) }
        var project by remember(inv.id) { mutableStateOf(inv.projectTitle) }
        var due by remember(inv.id) { mutableStateOf(inv.dueDate) }
        var status by remember(inv.id) { mutableStateOf(inv.status) }

        AlertDialog(
            onDismissRequest = { selectedInvoiceForPreview = null },
            title = { Text("Invoice ${inv.invoiceNumber}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Billed From: Alex Miller (SoloWork Studio)",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.accent
                    )
                    HorizontalDivider()
                    OutlinedTextField(value = client, onValueChange = { client = it }, label = { Text("Billed To (Client)") }, singleLine = true)
                    OutlinedTextField(value = project, onValueChange = { project = it }, label = { Text("Project Deliverable") }, singleLine = true)
                    OutlinedTextField(value = amountStr, onValueChange = { amountStr = it }, label = { Text("Total Amount (USD)") }, singleLine = true)
                    OutlinedTextField(value = due, onValueChange = { due = it }, label = { Text("Due Date") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Paid", "Pending", "Overdue").forEach { s ->
                            FilterChip(
                                selected = status == s,
                                onClick = { status = s },
                                label = { Text(s) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onEditInvoice(
                        inv,
                        inv.invoiceNumber,
                        client,
                        project,
                        amountStr.toDoubleOrNull() ?: (inv.amountCents / 100.0),
                        status,
                        due
                    )
                    selectedInvoiceForPreview = null
                }) {
                    Text("Save Invoice")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedInvoiceForPreview = null }) { Text("Close") }
            }
        )
    }
}

// ============================================================================
// 3. CLIENTS DEDICATED SCREEN
// ============================================================================
@Composable
fun ClientsView(
    clients: List<ClientItem>,
    onAddClient: (String, String, String, String, Long, String) -> Unit,
    onEditClient: (ClientItem, String, String, String, String, Long, Long) -> Unit,
    onDeleteClient: (String) -> Unit,
    onAiAction: (String) -> Unit = {}
) {
    val palette = LocalSoloWorkPalette.current
    var statusFilter by remember { mutableStateOf("All") }
    var showCreateModal by remember { mutableStateOf(false) }
    var clientToEdit by remember { mutableStateOf<ClientItem?>(null) }

    val filtered = remember(clients, statusFilter) {
        if (statusFilter == "All") clients else clients.filter { it.status == statusFilter }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Client Directory & CRM",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.textPrimary
                )
                Text(
                    text = "${filtered.size} partner accounts • Tap any client card to edit CRM details",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )
            }
            Button(
                onClick = { showCreateModal = true },
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("clients_new_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Client", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Client")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Active", "Lead", "Archived").forEach { s ->
                FilterChip(
                    selected = statusFilter == s,
                    onClick = { statusFilter = s },
                    label = { Text(s) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = palette.accent.copy(alpha = 0.22f),
                        selectedLabelColor = palette.accent
                    )
                )
            }
        }

        if (filtered.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.cardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.border, RoundedCornerShape(16.dp))
                    .testTag("clients_empty_state")
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "No clients in your CRM directory",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Add your freelance clients and leads to manage contacts and billed value.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                    Button(
                        onClick = { showCreateModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("+ Add First Client")
                    }
                }
            }
        }

        filtered.forEachIndexed { idx, client ->
            val badgeColor = when (idx % 5) {
                0 -> AccentIndigo
                1 -> AccentEmerald
                2 -> AccentAmber
                3 -> AccentViolet
                else -> AccentCrimson
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.cardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.border, RoundedCornerShape(16.dp))
                    .clickable { clientToEdit = client }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(badgeColor.copy(alpha = 0.22f))
                                    .border(1.5.dp, badgeColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = client.company.take(2).uppercase(),
                                    color = badgeColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = client.company,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    StatusBadge(
                                        text = client.status,
                                        color = when (client.status) {
                                            "Active" -> AccentEmerald
                                            "Lead" -> AccentAmber
                                            else -> palette.textSecondary
                                        }
                                    )
                                }
                                Text(
                                    text = "${client.name} • ${client.email}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$${NumberFormat.getNumberInstance(Locale.US).format(client.totalBilled)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${client.projectsCount} projects",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.accent.copy(alpha = 0.15f))
                                .border(1.dp, palette.accent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .clickable {
                                    onAiAction(
                                        "Draft a personalized quarterly check-in and retainer upsell email for ${client.name} at ${client.company} (${client.email})."
                                    )
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "✨ AI Pitch",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.accent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        IconButton(onClick = { onDeleteClient(client.id) }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Remove client",
                                tint = palette.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateModal) {
        var name by remember { mutableStateOf("") }
        var company by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var status by remember { mutableStateOf("Active") }
        var billedStr by remember { mutableStateOf("3500") }

        AlertDialog(
            onDismissRequest = { showCreateModal = false },
            title = { Text("Add Freelance Client") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(value = company, onValueChange = { company = it }, label = { Text("Company / Studio Name") }, placeholder = { Text("e.g., Horizon Labs") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Primary Contact Name") }, placeholder = { Text("e.g., Jordan Lee") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Contact Email") }, placeholder = { Text("e.g., jordan@horizonlabs.io") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = billedStr, onValueChange = { billedStr = it }, label = { Text("Total Billed (USD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Active", "Lead", "Archived").forEach { s ->
                            FilterChip(
                                selected = status == s,
                                onClick = { status = s },
                                label = { Text(s) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAddClient(
                        name.ifBlank { "Jordan Lee" },
                        company.ifBlank { "Horizon Labs" },
                        email.ifBlank { "jordan@horizonlabs.io" },
                        status,
                        billedStr.toLongOrNull() ?: 3500L,
                        "#6366F1"
                    )
                    showCreateModal = false
                }) {
                    Text("Save Client")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateModal = false }) { Text("Cancel") }
            }
        )
    }

    clientToEdit?.let { client ->
        var name by remember(client.id) { mutableStateOf(client.name) }
        var company by remember(client.id) { mutableStateOf(client.company) }
        var email by remember(client.id) { mutableStateOf(client.email) }
        var status by remember(client.id) { mutableStateOf(client.status) }
        var billedStr by remember(client.id) { mutableStateOf(client.totalBilled.toString()) }
        var projectsCountStr by remember(client.id) { mutableStateOf(client.projectsCount.toString()) }

        AlertDialog(
            onDismissRequest = { clientToEdit = null },
            title = { Text("Edit Client Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = company, onValueChange = { company = it }, label = { Text("Company") }, singleLine = true)
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Contact Name") }, singleLine = true)
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true)
                    OutlinedTextField(value = billedStr, onValueChange = { billedStr = it }, label = { Text("Total Billed (USD)") }, singleLine = true)
                    OutlinedTextField(value = projectsCountStr, onValueChange = { projectsCountStr = it }, label = { Text("Projects Count") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Active", "Lead", "Archived").forEach { s ->
                            FilterChip(
                                selected = status == s,
                                onClick = { status = s },
                                label = { Text(s) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onEditClient(
                        client,
                        name,
                        company,
                        email,
                        status,
                        billedStr.toLongOrNull() ?: client.totalBilled,
                        projectsCountStr.toLongOrNull() ?: client.projectsCount
                    )
                    clientToEdit = null
                }) {
                    Text("Update Client")
                }
            },
            dismissButton = {
                TextButton(onClick = { clientToEdit = null }) { Text("Cancel") }
            }
        )
    }
}

// ============================================================================
// 4. TASK BOARD (KANBAN + CHECKLIST) DEDICATED SCREEN
// ============================================================================
@Composable
fun TaskBoardView(
    tasks: List<TaskItem>,
    onAddTask: (String, String, String, String, String) -> Unit,
    onToggleTask: (TaskItem) -> Unit,
    onMoveTaskStatus: (TaskItem, String) -> Unit,
    onDeleteTask: (String) -> Unit
) {
    val palette = LocalSoloWorkPalette.current
    var viewMode by remember { mutableStateOf("Kanban") }
    var priorityFilter by remember { mutableStateOf("All") }
    var showCreateModal by remember { mutableStateOf(false) }

    val filteredTasks = remember(tasks, priorityFilter) {
        if (priorityFilter == "All") tasks else tasks.filter { it.priority == priorityFilter }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Task Board",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.textPrimary
                )
                Text(
                    text = "Completing tasks automatically updates project progress",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { viewMode = if (viewMode == "Kanban") "List" else "Kanban" },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (viewMode == "Kanban") "List View" else "Kanban View")
                }
                Button(
                    onClick = { showCreateModal = true },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("tasks_new_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Task")
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "High", "Medium", "Low").forEach { prio ->
                FilterChip(
                    selected = priorityFilter == prio,
                    onClick = { priorityFilter = prio },
                    label = { Text(if (prio == "All") "All Priorities" else "$prio Priority") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = palette.accent.copy(alpha = 0.22f),
                        selectedLabelColor = palette.accent
                    )
                )
            }
        }

        if (filteredTasks.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.cardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.border, RoundedCornerShape(16.dp))
                    .testTag("tasks_empty_state")
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Your Task Board is empty",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Add tasks to organize your workflow across Todo, In Progress, Review, and Done.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                    Button(
                        onClick = { showCreateModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("+ Add First Task")
                    }
                }
            }
        }

        if (viewMode == "Kanban") {
            val columns = listOf("Todo", "In Progress", "Review", "Done")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                columns.forEach { colStatus ->
                    val colTasks = filteredTasks.filter { it.status == colStatus }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = palette.cardBg,
                        modifier = Modifier
                            .width(275.dp)
                            .border(1.dp, palette.border, RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = colStatus,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                StatusBadge(text = colTasks.size.toString(), color = palette.accent)
                            }
                            HorizontalDivider(color = palette.border)

                            colTasks.forEach { task ->
                                val prioColor = when (task.priority) {
                                    "High" -> AccentCrimson
                                    "Medium" -> AccentAmber
                                    else -> AccentEmerald
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = palette.cardElevatedBg,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            StatusBadge(text = task.priority, color = prioColor)
                                            IconButton(
                                                onClick = { onDeleteTask(task.id) },
                                                modifier = Modifier.size(22.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Delete task",
                                                    tint = palette.textSecondary,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = task.completed,
                                                onCheckedChange = { onToggleTask(task) },
                                                colors = CheckboxDefaults.colors(checkedColor = palette.accent),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = task.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = palette.textPrimary,
                                                textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "${task.projectName} • ${task.dueDate}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.textSecondary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            columns.filter { it != colStatus }.forEach { target ->
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(palette.cardBg)
                                                        .clickable { onMoveTaskStatus(task, target) }
                                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                                ) {
                                                    Text(
                                                        text = "→ $target",
                                                        fontSize = 10.sp,
                                                        color = palette.textSecondary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            filteredTasks.forEach { task ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.cardBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Checkbox(
                                checked = task.completed,
                                onCheckedChange = { onToggleTask(task) },
                                colors = CheckboxDefaults.colors(checkedColor = palette.accent)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.textPrimary,
                                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                                )
                                Text(
                                    text = "${task.projectName} • ${task.status} • Due ${task.dueDate}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                        IconButton(onClick = { onDeleteTask(task.id) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = palette.textSecondary)
                        }
                    }
                }
            }
        }
    }

    if (showCreateModal) {
        NewTaskDialog(
            onDismiss = { showCreateModal = false },
            onConfirm = { title, proj, prio, status, due ->
                onAddTask(title, proj, prio, status, due)
                showCreateModal = false
            }
        )
    }
}

@Composable
fun NewTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String) -> Unit
) {
    val palette = LocalSoloWorkPalette.current
    var title by remember { mutableStateOf("") }
    var project by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("High") }
    var status by remember { mutableStateOf("Todo") }
    var dueDate by remember { mutableStateOf("Apr 18, 2026") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Task to Board") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Task Title") }, placeholder = { Text("e.g., Finalize responsive hero layout") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = project, onValueChange = { project = it }, label = { Text("Project Name") }, placeholder = { Text("e.g., Web Redesign") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Priority", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("High", "Medium", "Low").forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p) }
                        )
                    }
                }
                Text("Column Status", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Todo", "In Progress", "Review", "Done").forEach { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = { Text(s) }
                        )
                    }
                }
                OutlinedTextField(value = dueDate, onValueChange = { dueDate = it }, label = { Text("Due Date") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(
                    title.ifBlank { "Upcoming task checklist item" },
                    project.ifBlank { "General" },
                    priority,
                    status,
                    dueDate.ifBlank { "Upcoming" }
                )
            }) {
                Text("Add Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ============================================================================
// 5. TIME TRACKER (LIVE STOPWATCH + MANUAL LOG + BILLABLE LOGS)
// ============================================================================
@Composable
fun TimeTrackerView(
    projects: List<ProjectItem>,
    timeLogs: List<TimeLogItem>,
    isTimerRunning: Boolean,
    elapsedSeconds: Long,
    timerProjectName: String,
    timerTaskNote: String,
    timerHourlyRate: Long,
    onUpdateTimerMetadata: (String, String, Long) -> Unit,
    onToggleStopwatch: () -> Unit,
    onResetStopwatch: () -> Unit,
    onLogStopwatchSession: () -> Unit,
    onAddManualTimeLog: (String, String, Long, Long, Boolean) -> Unit,
    onDeleteTimeLog: (String) -> Unit
) {
    val palette = LocalSoloWorkPalette.current
    var showManualModal by remember { mutableStateOf(false) }

    val hours = elapsedSeconds / 3600
    val minutes = (elapsedSeconds % 3600) / 60
    val seconds = elapsedSeconds % 60
    val formattedClock = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    val liveEarnings = (elapsedSeconds / 3600.0) * timerHourlyRate

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Live Time Tracker & Billable Logs",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.textPrimary
                )
                Text(
                    text = "Track billable sprints in real time or log manual sessions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )
            }
            OutlinedButton(
                onClick = { showManualModal = true },
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Manual Entry", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Manual Entry")
            }
        }

        // Live Stopwatch Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.cardBg,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, if (isTimerRunning) AccentEmerald else palette.border, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column {
                        Text(
                            text = if (isTimerRunning) "RECORDING BILLABLE SESSION • $${timerHourlyRate}/hr" else "STOPWATCH READY • $${timerHourlyRate}/hr",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isTimerRunning) AccentEmerald else palette.textSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = formattedClock,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = String.format(Locale.US, "$%.2f earned", liveEarnings),
                                style = MaterialTheme.typography.titleMedium,
                                color = AccentEmerald,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onToggleStopwatch,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isTimerRunning) AccentAmber else AccentEmerald
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("stopwatch_start_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isTimerRunning) "Pause" else "Start"
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isTimerRunning) "Pause" else "Start")
                        }

                        OutlinedButton(
                            onClick = onResetStopwatch,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("stopwatch_reset_button")
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = "Reset")
                        }

                        Button(
                            onClick = onLogStopwatchSession,
                            colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("stopwatch_save_session_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save Session", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Log Session")
                        }
                    }
                }

                // Quick Project Selector Chips
                if (projects.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        projects.take(5).forEach { proj ->
                            FilterChip(
                                selected = timerProjectName == proj.name,
                                onClick = { onUpdateTimerMetadata(proj.name, timerTaskNote, timerHourlyRate) },
                                label = { Text(proj.name) }
                            )
                        }
                    }
                }

                // QuickHourly Rate Selector Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Rate:", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    listOf(75L, 110L, 150L, 200L).forEach { rateOpt ->
                        FilterChip(
                            selected = timerHourlyRate == rateOpt,
                            onClick = { onUpdateTimerMetadata(timerProjectName, timerTaskNote, rateOpt) },
                            label = { Text("$$rateOpt/hr") }
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = timerProjectName,
                        onValueChange = { onUpdateTimerMetadata(it, timerTaskNote, timerHourlyRate) },
                        label = { Text("Active Project") },
                        placeholder = { Text("e.g., Web Redesign") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = timerTaskNote,
                        onValueChange = { onUpdateTimerMetadata(timerProjectName, it, timerHourlyRate) },
                        label = { Text("Task Description") },
                        placeholder = { Text("What are you working on right now?") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        val totalHours = timeLogs.sumOf { it.durationMinutes } / 60.0
        val totalBillable = timeLogs.filter { it.billable }.sumOf { (it.durationMinutes / 60.0) * it.hourlyRate }

        Text(
            text = String.format(Locale.US, "Logged Sessions (%.1fh Total • $%,.0f Billable Value)", totalHours, totalBillable),
            style = MaterialTheme.typography.titleLarge,
            color = palette.textPrimary
        )

        if (timeLogs.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = palette.cardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                    .testTag("timelogs_empty_state")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "No time sessions logged yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Start the live stopwatch above or tap 'Manual Entry' to record your billable hours.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
            }
        }

        timeLogs.forEach { log ->
            val hoursVal = log.durationMinutes / 60.0
            val earned = hoursVal * log.hourlyRate
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = palette.cardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.border, RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = log.projectName,
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.accent,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${log.taskDescription} • ${log.loggedDate}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = String.format(Locale.US, "%.1fh ($%.0f)", hoursVal, earned),
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$${log.hourlyRate}/hr",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentEmerald
                            )
                        }
                        IconButton(onClick = { onDeleteTimeLog(log.id) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete log", tint = palette.textSecondary)
                        }
                    }
                }
            }
        }
    }

    if (showManualModal) {
        var proj by remember { mutableStateOf(timerProjectName) }
        var desc by remember { mutableStateOf("Client design review & handoff") }
        var minsStr by remember { mutableStateOf("90") }
        var rateStr by remember { mutableStateOf("110") }

        AlertDialog(
            onDismissRequest = { showManualModal = false },
            title = { Text("Log Manual Time Entry") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = proj, onValueChange = { proj = it }, label = { Text("Project Name") }, singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Work Description") }, singleLine = true)
                    OutlinedTextField(value = minsStr, onValueChange = { minsStr = it }, label = { Text("Duration (Minutes)") }, singleLine = true)
                    OutlinedTextField(value = rateStr, onValueChange = { rateStr = it }, label = { Text("Hourly Rate (USD)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAddManualTimeLog(
                        proj,
                        desc,
                        minsStr.toLongOrNull() ?: 90L,
                        rateStr.toLongOrNull() ?: 110L,
                        true
                    )
                    showManualModal = false
                }) {
                    Text("Save Time Entry")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualModal = false }) { Text("Cancel") }
            }
        )
    }
}

// ============================================================================
// 6. ANALYTICS DEDICATED SCREEN
// ============================================================================
@Composable
fun AnalyticsView(
    projects: List<ProjectItem>,
    invoices: List<InvoiceItem>,
    clients: List<ClientItem>,
    timeLogs: List<TimeLogItem>
) {
    val palette = LocalSoloWorkPalette.current
    var selectedMetric by remember { mutableStateOf("Revenue ($)") }

    val totalMinutes = timeLogs.sumOf { it.durationMinutes }
    val totalHours = totalMinutes / 60.0
    val totalBillableValue = timeLogs.filter { it.billable }.sumOf { (it.durationMinutes / 60.0) * it.hourlyRate }
    val effectiveRate = if (totalHours > 0.0) (totalBillableValue / totalHours).toLong() else 0L

    val avgCompletion = if (projects.isNotEmpty()) projects.sumOf { it.progress } / projects.size else 0L
    val totalInvoicedCents = invoices.sumOf { it.amountCents }
    val paidInvoicedCents = invoices.filter { it.status == "Paid" }.sumOf { it.amountCents }
    val collectionRate = if (totalInvoicedCents > 0L) (paidInvoicedCents * 100.0 / totalInvoicedCents) else 0.0

    val chartPoints = remember(selectedMetric, invoices, timeLogs, clients) {
        when (selectedMetric) {
            "Hours Tracked (h)" -> {
                if (timeLogs.isEmpty()) listOf(0f, 0f, 0f, 0f, 0f, 0f)
                else timeLogs.take(6).reversed().map { (it.durationMinutes / 60f) }.let {
                    if (it.size < 2) listOf(0f, it.firstOrNull() ?: 0f) else it
                }
            }
            "Active Clients" -> {
                val activeCount = clients.count { it.status == "Active" }.toFloat()
                if (activeCount == 0f) listOf(0f, 0f, 0f, 0f, 0f, 0f)
                else listOf(
                    (activeCount * 0.4f).coerceAtLeast(1f),
                    (activeCount * 0.6f).coerceAtLeast(1f),
                    (activeCount * 0.8f).coerceAtLeast(1f),
                    activeCount
                )
            }
            else -> {
                val paidList = invoices.filter { it.status == "Paid" }
                if (paidList.isEmpty()) listOf(0f, 0f, 0f, 0f, 0f, 0f)
                else paidList.reversed().map { (it.amountCents / 100f) }.let {
                    if (it.size < 2) listOf(0f, it.firstOrNull() ?: 0f) else it
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Revenue & Productivity Analytics",
            style = MaterialTheme.typography.headlineMedium,
            color = palette.textPrimary
        )

        // Freelance Efficiency KPI Strip derived from live user data
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnalyticsMetricTile("Effective Rate", "$$effectiveRate/hr", AccentEmerald, Modifier.weight(1f))
            AnalyticsMetricTile("Avg Completion", "$avgCompletion%", AccentIndigo, Modifier.weight(1f))
            AnalyticsMetricTile("Collection Rate", String.format(Locale.US, "%.0f%%", collectionRate), AccentAmber, Modifier.weight(1f))
        }

        // Interactive Performance Trajectory Chart Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = palette.cardBg,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.border, RoundedCornerShape(18.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Workspace Performance Trajectory",
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Real-time metrics calculated from your invoices, time logs, and clients",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Revenue ($)", "Hours Tracked (h)", "Active Clients").forEach { metric ->
                        FilterChip(
                            selected = selectedMetric == metric,
                            onClick = { selectedMetric = metric },
                            label = { Text(metric) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                SparklineChart(
                    points = chartPoints,
                    lineColor = palette.accent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
            }
        }

        // Category Distribution & Top Clients Row
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isWide = maxWidth >= 650.dp
            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CategoryDonutCard(projects = projects, modifier = Modifier.weight(1f))
                    ClientLeaderboardCard(clients = clients, modifier = Modifier.weight(1f))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CategoryDonutCard(projects = projects, modifier = Modifier.fillMaxWidth())
                    ClientLeaderboardCard(clients = clients, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        // Logged Sessions Distribution Bar Chart Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = palette.cardBg,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.border, RoundedCornerShape(18.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Recent Time Log Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))
                val sessionBars = remember(timeLogs) {
                    if (timeLogs.isEmpty()) {
                        listOf("Log 1" to 0f, "Log 2" to 0f, "Log 3" to 0f, "Log 4" to 0f, "Log 5" to 0f)
                    } else {
                        timeLogs.take(7).map { log ->
                            log.projectName.take(8) to (log.durationMinutes / 60f)
                        }
                    }
                }
                val maxBarHours = (sessionBars.maxOfOrNull { it.second } ?: 1f).coerceAtLeast(1f)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                ) {
                    val barCount = sessionBars.size.coerceAtLeast(1)
                    val slotWidth = size.width / barCount
                    val barWidth = (slotWidth * 0.45f).coerceAtMost(36.dp.toPx())
                    sessionBars.forEachIndexed { index, (_, hrs) ->
                        val ratio = if (hrs <= 0f) 0.04f else (hrs / maxBarHours).coerceIn(0.08f, 1f)
                        val barH = size.height * ratio
                        val left = index * slotWidth + (slotWidth - barWidth) / 2f
                        drawRoundRect(
                            color = if (hrs > 0f) palette.accent else palette.border,
                            topLeft = Offset(left, size.height - barH),
                            size = Size(barWidth, barH),
                            cornerRadius = CornerRadius(10f, 10f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    sessionBars.forEach { (label, hrs) ->
                        Text(
                            text = String.format(Locale.US, "%s (%.1fh)", label, hrs),
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsMetricTile(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val palette = LocalSoloWorkPalette.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.cardBg,
        modifier = modifier.border(1.dp, palette.border, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CategoryDonutCard(projects: List<ProjectItem>, modifier: Modifier = Modifier) {
    val palette = LocalSoloWorkPalette.current
    val totalBudget = projects.sumOf { it.budget }.toFloat()

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = palette.cardBg,
        modifier = modifier.border(1.dp, palette.border, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Portfolio Budget by Discipline",
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (projects.isEmpty() || totalBudget <= 0f) {
                Text(
                    text = "No projects yet. Add projects to see discipline breakdown.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            } else {
                val grouped = projects.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.budget }.toFloat() }
                val colors = listOf(AccentIndigo, AccentEmerald, AccentAmber, AccentViolet, AccentCrimson)
                Canvas(modifier = Modifier.size(130.dp)) {
                    val stroke = 24.dp.toPx()
                    var startAngle = -90f
                    grouped.entries.forEachIndexed { idx, (_, catBudget) ->
                        val sweep = (catBudget / totalBudget) * 360f
                        drawArc(
                            color = colors[idx % colors.size],
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(stroke, cap = StrokeCap.Butt)
                        )
                        startAngle += sweep
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                val summary = grouped.entries.joinToString(" • ") { (cat, budget) ->
                    val pct = ((budget / totalBudget) * 100).toInt()
                    "$cat ($pct%)"
                }
                Text(
                    text = summary,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
                )
            }
        }
    }
}

@Composable
private fun ClientLeaderboardCard(clients: List<ClientItem>, modifier: Modifier = Modifier) {
    val palette = LocalSoloWorkPalette.current
    val maxBilled = (clients.maxOfOrNull { it.totalBilled } ?: 1L).coerceAtLeast(1L).toFloat()
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = palette.cardBg,
        modifier = modifier.border(1.dp, palette.border, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Top Clients by Billed Value",
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary
            )
            if (clients.isEmpty()) {
                Text(
                    text = "No client billing records yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            } else {
                clients.sortedByDescending { it.totalBilled }.take(4).forEach { client ->
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(client.company, style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary)
                            Text("$${client.totalBilled}", style = MaterialTheme.typography.labelLarge, color = AccentEmerald)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (client.totalBilled / maxBilled).coerceIn(0.05f, 1f) },
                            color = palette.accent,
                            trackColor = palette.cardElevatedBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 7. SETTINGS DEDICATED SCREEN (PROFILE + LIVE THEME ENGINE + DATA CONTROLS)
// ============================================================================
@Composable
fun SettingsView(
    settings: UserSettingsItem,
    isViewingExampleData: Boolean = false,
    onUpdateProfile: (String, String) -> Unit,
    onUpdateThemeMode: (String) -> Unit,
    onUpdateThemePreset: (String) -> Unit,
    onUpdateAccentColor: (String) -> Unit,
    onUpdateFontFamily: (String) -> Unit,
    onUpdateCustomizeLayoutStyle: (Boolean) -> Unit,
    onUpdateSidebarCompact: (Boolean) -> Unit,
    onResetDemoData: () -> Unit,
    onClearAllData: () -> Unit = {},
    onSignOut: () -> Unit
) {
    val palette = LocalSoloWorkPalette.current
    var displayName by remember(settings.displayName) { mutableStateOf(settings.displayName) }
    var roleTitle by remember(settings.roleTitle) { mutableStateOf(settings.roleTitle) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Workspace, Theme & Profile Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = palette.textPrimary
        )

        // Embedded Live Theme & Layout Customization Engine Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = palette.cardBg,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.border, RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Live Appearance & Theme Engine",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )

                // Theme Mode + Presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Classic Dark", "Daylight", "Solarized", "Cyberpunk").forEach { preset ->
                        FilterChip(
                            selected = settings.themePreset == preset,
                            onClick = { onUpdateThemePreset(preset) },
                            label = { Text(preset) }
                        )
                    }
                }

                // 5 Color Accents
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        "Indigo" to AccentIndigo,
                        "Emerald" to AccentEmerald,
                        "Violet" to AccentViolet,
                        "Amber" to AccentAmber,
                        "Crimson" to AccentCrimson
                    ).forEach { (name, color) ->
                        val selected = settings.accentColor == name
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onUpdateAccentColor(name) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(if (selected) 2.5.dp else 1.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(name, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        }
                    }
                }

                // Font Family Switcher
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Inter", "Roboto", "Poppins").forEach { font ->
                        FilterChip(
                            selected = settings.fontFamily == font,
                            onClick = { onUpdateFontFamily(font) },
                            label = { Text("Font: $font") }
                        )
                    }
                }

                // Layout & Sidebar checkboxes
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = settings.customizeLayoutStyle,
                            onCheckedChange = onUpdateCustomizeLayoutStyle
                        )
                        Text("Elevated Rounded Cards", style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = settings.sidebarCompact,
                            onCheckedChange = onUpdateSidebarCompact
                        )
                        Text("Compact Sidebar", style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary)
                    }
                }
            }
        }

        // Freelancer Profile Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = palette.cardBg,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.border, RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Freelancer Profile",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = roleTitle,
                    onValueChange = { roleTitle = it },
                    label = { Text("Role / Headline") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { onUpdateProfile(displayName, roleTitle) },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Profile Changes")
                }
            }
        }

        // Workspace Data & Example Mode Controls Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = palette.cardBg,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.border, RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Workspace Data & Example Mode",
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.textPrimary
                    )
                    if (isViewingExampleData) {
                        StatusBadge(text = "Viewing Example Data", color = AccentAmber)
                    }
                }
                Text(
                    text = "Start with a clean, zeroed-out workspace for your own freelance records or load the optional SoloWork example dataset synced to Cloud Firestore.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onClearAllData,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCrimson),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("clear_all_data_button")
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Clear All Data", modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear All Data & Start Fresh", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = onResetDemoData,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("reset_demo_data_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Load Example Data", modifier = Modifier.size(18.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Load Example Data", color = Color.Black, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Sign Out of Workspace")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}
