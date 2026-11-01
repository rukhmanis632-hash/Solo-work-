package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCrimson
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentViolet
import com.example.ui.theme.LocalSoloWorkPalette
import com.example.ui.viewmodel.AiBlueprintState
import com.example.ui.viewmodel.AiChatMessage
import com.example.ui.viewmodel.AiClientDocState
import com.example.ui.viewmodel.AiGeneratedClientDoc
import com.example.ui.viewmodel.AiGeneratedProjectBlueprint
import com.example.ui.viewmodel.AiStreamState
import com.example.ui.viewmodel.SoloWorkTab
import java.text.NumberFormat
import java.util.Locale

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
}

@Composable
fun AiCopilotView(
    aiStreamState: AiStreamState,
    aiBlueprintState: AiBlueprintState,
    aiClientDocState: AiClientDocState = AiClientDocState.Idle,
    aiChatMessages: List<AiChatMessage> = emptyList(),
    onStreamPrompt: (String) -> Unit,
    onClearChat: () -> Unit = {},
    onGenerateBlueprint: (String) -> Unit,
    onImportBlueprint: (AiGeneratedProjectBlueprint) -> Unit,
    onGenerateClientDoc: (String, String, String) -> Unit = { _, _, _ -> }
) {
    val palette = LocalSoloWorkPalette.current
    val context = LocalContext.current
    val cornerRadius = if (palette.customizeLayoutStyle) 18.dp else 10.dp
    val numFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    var activeStudioFilter by remember { mutableStateOf("All Studios") }
    var copiedNotice by remember { mutableStateOf<String?>(null) }

    var blueprintBrief by remember {
        mutableStateOf("Redesign the customer analytics portal and design system for FinTech client Vertex FinTech with dark mode and interactive charts.")
    }
    var customStreamPrompt by remember {
        mutableStateOf("Analyze my active SoloWork projects, pending invoices, and billable hours, then give me 3 high-impact actions to increase revenue this week.")
    }

    var selectedDocType by remember { mutableStateOf("Statement of Work (SOW)") }
    var docRecipient by remember { mutableStateOf("Orbit Studio (Elena Rostova)") }
    var docCustomNotes by remember {
        mutableStateOf("Include 3 milestone payments, responsive design deliverables, and 14-day net payment terms.")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Hero Banner Header + Studio Mode Filter Bar
        Surface(
            shape = RoundedCornerShape(cornerRadius),
            color = palette.cardBg,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.accent.copy(alpha = 0.6f), RoundedCornerShape(cornerRadius))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                palette.accent.copy(alpha = 0.20f),
                                AccentViolet.copy(alpha = 0.14f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.linearGradient(listOf(palette.accent, AccentViolet))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "SoloWork AI Copilot",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "SoloWork AI Copilot • Gemini 3.8 Flash",
                                style = MaterialTheme.typography.headlineMedium,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "1-Click Project Scoper, Multi-Turn Workspace Advisor & Client Proposal Studio",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                        }
                    }
                }

                // Studio Mode Selector Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "All Studios",
                        "🚀 Project & Task Scoper",
                        "💬 AI Advisor Chat",
                        "✉️ Client Email & SOW Studio"
                    ).forEach { mode ->
                        val selected = activeStudioFilter == mode
                        FilterChip(
                            selected = selected,
                            onClick = { activeStudioFilter = mode },
                            label = { Text(mode) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.accent,
                                selectedLabelColor = Color.White,
                                containerColor = palette.cardBg,
                                labelColor = palette.textSecondary
                            )
                        )
                    }
                }

                if (copiedNotice != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AccentEmerald.copy(alpha = 0.2f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AccentEmerald, RoundedCornerShape(10.dp))
                            .clickable { copiedNotice = null }
                    ) {
                        Text(
                            text = copiedNotice ?: "",
                            style = MaterialTheme.typography.labelLarge,
                            color = AccentEmerald,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // =========================================================================
        // STUDIO 1: Structured JSON Project & Milestone Blueprint Generator
        // =========================================================================
        if (activeStudioFilter == "All Studios" || activeStudioFilter == "🚀 Project & Task Scoper") {
            Surface(
                shape = RoundedCornerShape(cornerRadius),
                color = palette.cardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.border, RoundedCornerShape(cornerRadius))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "1. AI Project Scope & Kanban Blueprint Generator",
                                style = MaterialTheme.typography.titleLarge,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Pick a template or describe any client project—Gemini generates the budget, summary, and Kanban tasks ready to import in 1 tap.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                        }
                    }

                    // Quick Brief Templates
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sampleBriefs = listOf(
                            "FinTech Portal Redesign" to "Redesign the customer analytics portal and design system for FinTech client Vertex FinTech with dark mode and interactive charts.",
                            "Brand Identity & Guidelines" to "Create a complete visual brand identity, logo suite, typography system, and social kit for Nova Health.",
                            "SaaS Onboarding & Checkout" to "Build a high-converting onboarding flow and Stripe billing dashboard for Orbit Studio.",
                            "AI Mobile App MVP" to "Design and prototype an AI fitness coaching mobile application for Pulse Media."
                        )
                        sampleBriefs.forEach { (label, fullBrief) ->
                            FilterChip(
                                selected = blueprintBrief == fullBrief,
                                onClick = { blueprintBrief = fullBrief },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = palette.accent.copy(alpha = 0.22f),
                                    selectedLabelColor = palette.accent
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = blueprintBrief,
                        onValueChange = { blueprintBrief = it },
                        label = { Text("Client Brief or Project Idea") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_blueprint_brief_input")
                    )

                    Button(
                        onClick = { onGenerateBlueprint(blueprintBrief) },
                        enabled = aiBlueprintState !is AiBlueprintState.Loading && blueprintBrief.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("ai_generate_blueprint_button")
                    ) {
                        if (aiBlueprintState is AiBlueprintState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating Structured Blueprint...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Project Blueprint")
                        }
                    }

                    when (aiBlueprintState) {
                        is AiBlueprintState.Idle -> Unit
                        is AiBlueprintState.Loading -> Unit
                        is AiBlueprintState.Error -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AccentCrimson.copy(alpha = 0.14f))
                                    .border(1.dp, AccentCrimson.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                                    .testTag("ai_blueprint_error_box")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Error",
                                    tint = AccentCrimson
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = aiBlueprintState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textPrimary
                                )
                            }
                        }
                        is AiBlueprintState.Success -> {
                            val bp = aiBlueprintState.blueprint
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = palette.cardElevatedBg,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, palette.accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                    .testTag("ai_blueprint_result_card")
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = bp.projectName,
                                                style = MaterialTheme.typography.titleLarge,
                                                color = palette.textPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Client: ${bp.clientName} • Discipline: ${bp.category}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = palette.accent
                                            )
                                        }
                                        Text(
                                            text = "$${numFormat.format(bp.estimatedBudgetUsd)}",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = AccentEmerald,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Text(
                                        text = bp.executiveSummary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.textSecondary
                                    )

                                    HorizontalDivider(color = palette.border)

                                    Text(
                                        text = "Generated Kanban Milestones (${bp.milestones.size})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    bp.milestones.forEachIndexed { idx, milestone ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(palette.accent.copy(alpha = 0.22f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${idx + 1}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = palette.accent,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = milestone,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = palette.textPrimary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = { onImportBlueprint(bp) },
                                            colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.testTag("ai_import_blueprint_button")
                                        ) {
                                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Import Project & Tasks to Cloud Firestore")
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                val summaryText = "${bp.projectName} (${bp.clientName}) - $${bp.estimatedBudgetUsd}\n${bp.executiveSummary}\nMilestones:\n" +
                                                    bp.milestones.joinToString("\n") { "- $it" }
                                                copyToClipboard(context, "Project Blueprint", summaryText)
                                                copiedNotice = "Copied project blueprint to clipboard!"
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Blueprint", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Copy")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // STUDIO 2: Interactive Multi-Turn AI Workspace Chat & Live Streaming
        // =========================================================================
        if (activeStudioFilter == "All Studios" || activeStudioFilter == "💬 AI Advisor Chat") {
            Surface(
                shape = RoundedCornerShape(cornerRadius),
                color = palette.cardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.border, RoundedCornerShape(cornerRadius))
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "2. Interactive AI Workspace Chat & Live Advisor",
                                style = MaterialTheme.typography.titleLarge,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ask anything about your projects, invoices, clients, or freelance strategy with live token streaming.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                        }
                        if (aiChatMessages.size > 1) {
                            IconButton(
                                onClick = onClearChat,
                                modifier = Modifier.testTag("ai_clear_chat_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear conversation",
                                    tint = palette.textSecondary
                                )
                            }
                        }
                    }

                    // 1-Tap Quick-Action Prompt Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val quickPrompts = listOf(
                            "📊 Workspace Revenue Audit" to "Analyze my active SoloWork projects, pending invoices, and billable hours, then give me 3 high-impact actions to increase revenue this week.",
                            "💸 Collect Pending Invoices" to "Draft a polite, high-converting payment reminder email for my pending invoices (including Pulse Media and MetaLoop) with clear settlement steps.",
                            "📑 Client Proposal & SOW" to "Draft an executive Statement of Work (SOW) and milestone payment schedule for the Web Redesign v2 project with Orbit Studio.",
                            "⏱️ Rate & Retainer Strategy" to "Review my logged billable hours and hourly rates and recommend how to structure monthly retainer packages for my top clients."
                        )
                        quickPrompts.forEach { (chipLabel, promptText) ->
                            FilterChip(
                                selected = customStreamPrompt == promptText,
                                onClick = {
                                    customStreamPrompt = promptText
                                    onStreamPrompt(promptText)
                                },
                                label = { Text(chipLabel) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentIndigo.copy(alpha = 0.22f),
                                    selectedLabelColor = AccentIndigo
                                )
                            )
                        }
                    }

                    // Conversation History Bubbles
                    if (aiChatMessages.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(palette.background.copy(alpha = 0.55f))
                                .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            aiChatMessages.takeLast(6).forEach { msg ->
                                val isUser = msg.role == "user"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(
                                            topStart = 14.dp,
                                            topEnd = 14.dp,
                                            bottomStart = if (isUser) 14.dp else 4.dp,
                                            bottomEnd = if (isUser) 4.dp else 14.dp
                                        ),
                                        color = if (isUser) palette.accent else palette.cardElevatedBg,
                                        modifier = Modifier.fillMaxWidth(0.92f)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (isUser) "You • ${msg.timestamp}" else "SoloWork AI • ${msg.timestamp}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isUser) Color.White.copy(alpha = 0.8f) else palette.accent,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (!isUser) {
                                                    Icon(
                                                        imageVector = Icons.Default.ContentCopy,
                                                        contentDescription = "Copy message",
                                                        tint = palette.textSecondary,
                                                        modifier = Modifier
                                                            .size(16.dp)
                                                            .clickable {
                                                                copyToClipboard(context, "AI Response", msg.text)
                                                                copiedNotice = "Copied AI response to clipboard!"
                                                            }
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = msg.text,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isUser) Color.White else palette.textPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = customStreamPrompt,
                            onValueChange = { customStreamPrompt = it },
                            label = { Text("Ask SoloWork AI Copilot...") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_stream_prompt_input")
                        )
                        Button(
                            onClick = { onStreamPrompt(customStreamPrompt) },
                            enabled = aiStreamState !is AiStreamState.Loading && customStreamPrompt.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("ai_stream_generate_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send prompt", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ask AI")
                        }
                    }

                    when (aiStreamState) {
                        is AiStreamState.Idle -> Unit
                        is AiStreamState.Loading -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = palette.accent,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Connecting to Gemini 3.8 Flash...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary
                                )
                            }
                        }
                        is AiStreamState.Streaming -> {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = palette.cardElevatedBg,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, AccentAmber.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            color = AccentAmber,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Streaming live from Gemini 3.8 Flash...",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AccentAmber,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = aiStreamState.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.textPrimary
                                    )
                                }
                            }
                        }
                        is AiStreamState.Success -> {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = palette.cardElevatedBg,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, AccentEmerald.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                    .testTag("ai_stream_result_card")
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Complete",
                                                tint = AccentEmerald,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Latest Gemini 3.8 Flash Response",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = AccentEmerald,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        TextButton(
                                            onClick = {
                                                copyToClipboard(context, "Gemini Response", aiStreamState.text)
                                                copiedNotice = "Copied response to clipboard!"
                                            }
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Copy")
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = aiStreamState.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.textPrimary
                                    )
                                }
                            }
                        }
                        is AiStreamState.Error -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AccentCrimson.copy(alpha = 0.14f))
                                    .border(1.dp, AccentCrimson.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                                    .testTag("ai_stream_error_box")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Error",
                                    tint = AccentCrimson
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = aiStreamState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // STUDIO 3: AI Client Email, Proposal & Invoice Follow-Up Studio (Schema.obj)
        // =========================================================================
        if (activeStudioFilter == "All Studios" || activeStudioFilter == "✉️ Client Email & SOW Studio") {
            Surface(
                shape = RoundedCornerShape(cornerRadius),
                color = palette.cardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.border, RoundedCornerShape(cornerRadius))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "3. AI Client Email, Proposal & Invoice Follow-Up Studio",
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Generate ready-to-send client emails, Statements of Work, and payment reminders with structured subject lines and key highlights.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )

                    // Document Type Selector Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Statement of Work (SOW)",
                            "Invoice Payment Reminder",
                            "Weekly Progress Update",
                            "Retainer Upsell Pitch"
                        ).forEach { docType ->
                            FilterChip(
                                selected = selectedDocType == docType,
                                onClick = { selectedDocType = docType },
                                label = { Text(docType) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentViolet.copy(alpha = 0.22f),
                                    selectedLabelColor = AccentViolet
                                )
                            )
                        }
                    }

                    // Quick Client / Invoice Target Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Orbit Studio (Elena Rostova)",
                            "Pulse Media • INV-2026-103 ($127.83)",
                            "MetaLoop • INV-2026-104 ($1,450.00)",
                            "Chroma Labs • INV-2026-105 ($890.00)"
                        ).forEach { target ->
                            FilterChip(
                                selected = docRecipient == target,
                                onClick = { docRecipient = target },
                                label = { Text(target) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = docRecipient,
                        onValueChange = { docRecipient = it },
                        label = { Text("Client or Invoice Recipient") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = docCustomNotes,
                        onValueChange = { docCustomNotes = it },
                        label = { Text("Key Points or Custom Instructions") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { onGenerateClientDoc(selectedDocType, docRecipient, docCustomNotes) },
                        enabled = aiClientDocState !is AiClientDocState.Loading && docRecipient.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentViolet),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("ai_generate_client_doc_button")
                    ) {
                        if (aiClientDocState is AiClientDocState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Drafting Client Document...")
                        } else {
                            Icon(Icons.Default.MarkEmailRead, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate $selectedDocType")
                        }
                    }

                    when (aiClientDocState) {
                        is AiClientDocState.Idle -> Unit
                        is AiClientDocState.Loading -> Unit
                        is AiClientDocState.Error -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AccentCrimson.copy(alpha = 0.14f))
                                    .border(1.dp, AccentCrimson.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = "Error", tint = AccentCrimson)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = aiClientDocState.message, color = palette.textPrimary)
                            }
                        }
                        is AiClientDocState.Success -> {
                            val doc = aiClientDocState.doc
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = palette.cardElevatedBg,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, AccentViolet.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                    .testTag("ai_client_doc_result_card")
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Subject: ${doc.subjectLine}",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = palette.textPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "To: ${doc.recipientName} • Follow-up: ${doc.suggestedFollowUpDate}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = AccentViolet
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                val fullEmail = "Subject: ${doc.subjectLine}\n\n${doc.bodyText}"
                                                copyToClipboard(context, "Client Email", fullEmail)
                                                copiedNotice = "Copied '${doc.subjectLine}' to clipboard!"
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Copy Email")
                                        }
                                    }

                                    HorizontalDivider(color = palette.border)

                                    Text(
                                        text = doc.bodyText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.textPrimary
                                    )

                                    if (doc.keyHighlights.isNotEmpty()) {
                                        HorizontalDivider(color = palette.border)
                                        Text(
                                            text = "Key Highlights Included:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.textSecondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        doc.keyHighlights.forEach { bullet ->
                                            Text(
                                                text = "• $bullet",
                                                style = MaterialTheme.typography.bodySmall,
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
}

@Composable
fun QuickAiAssistantModal(
    currentTab: SoloWorkTab,
    onDismiss: () -> Unit,
    onLaunchPromptInCopilot: (String) -> Unit,
    onOpenFullCopilot: () -> Unit
) {
    val palette = LocalSoloWorkPalette.current
    var customQuestion by remember { mutableStateOf("") }

    val contextualSuggestions = remember(currentTab) {
        when (currentTab) {
            SoloWorkTab.Dashboard -> listOf(
                "Give me a 60-second executive summary of my freelance business health today.",
                "Which active project and pending invoice should I prioritize right now?",
                "Suggest 3 ways to increase my monthly freelance recurring revenue."
            )
            SoloWorkTab.Projects -> listOf(
                "Audit my active projects and identify potential delivery bottlenecks.",
                "Draft a milestone completion message for Orbit Studio's Web Redesign.",
                "Recommend how to price my next Web Design & Branding package."
            )
            SoloWorkTab.Invoices -> listOf(
                "Draft a friendly payment follow-up email for my 3 pending invoices.",
                "Create a late-payment policy clause I can add to future invoices.",
                "Summarize my paid vs. pending cash flow and collection rate."
            )
            SoloWorkTab.Clients -> listOf(
                "Draft a warm check-in and retainer upsell email for my top clients.",
                "Write a new-client welcome & onboarding checklist email.",
                "Which clients have the highest billed value and expansion potential?"
            )
            SoloWorkTab.TaskBoard -> listOf(
                "Prioritize my open tasks so I finish high-impact deliverables first.",
                "Break down 'Hero UI polish' and 'API schema review' into sub-tasks.",
                "Suggest a daily sprint schedule for my In Progress tasks."
            )
            SoloWorkTab.TimeTracker -> listOf(
                "Analyze my 42.5h logged hours and suggest how to raise my effective hourly rate.",
                "Rewrite my time log notes so they sound executive-ready on client invoices.",
                "Recommend an optimal weekly billable hour target to hit $15,000/mo."
            )
            else -> listOf(
                "Analyze my freelance workspace metrics and recommend 3 growth actions.",
                "Draft a client proposal & Statement of Work for a new SaaS project.",
                "Write a polite payment reminder for overdue or pending invoices."
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Quick AI Assistant • ${currentTab.title}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Tap any 1-click suggestion for ${currentTab.title} or ask your own question:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )

                contextualSuggestions.forEach { suggestion ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.cardElevatedBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                            .clickable {
                                onLaunchPromptInCopilot(suggestion)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = palette.accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = customQuestion,
                    onValueChange = { customQuestion = it },
                    label = { Text("Or ask a custom question...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (customQuestion.isNotBlank()) {
                        onLaunchPromptInCopilot(customQuestion)
                    } else {
                        onOpenFullCopilot()
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent)
            ) {
                Text(if (customQuestion.isNotBlank()) "Ask Gemini AI" else "Open Full AI Studio")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
