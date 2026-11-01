package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PeopleOutline
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserSettingsItem
import com.example.ui.auth.SoloWorkLogoBadge
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCrimson
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentViolet
import com.example.ui.theme.ClassicDarkBg
import com.example.ui.theme.ClassicDarkCard
import com.example.ui.theme.CyberpunkBg
import com.example.ui.theme.CyberpunkCard
import com.example.ui.theme.DaylightBg
import com.example.ui.theme.DaylightCard
import com.example.ui.theme.LocalSoloWorkPalette
import com.example.ui.theme.SolarizedDarkBg
import com.example.ui.theme.SolarizedDarkCard
import com.example.ui.viewmodel.SoloWorkTab

@Composable
fun SparklineChart(
    points: List<Float>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val w = size.width
        val h = size.height
        val minVal = points.minOrNull() ?: 0f
        val maxVal = (points.maxOrNull() ?: 1f).coerceAtLeast(minVal + 0.01f)
        val range = maxVal - minVal

        val stepX = w / (points.size - 1)
        val coords = points.mapIndexed { index, value ->
            val x = index * stepX
            val normalizedY = (value - minVal) / range
            val y = h - (normalizedY * (h * 0.72f) + h * 0.14f)
            Offset(x, y)
        }

        val strokePath = Path().apply {
            moveTo(coords.first().x, coords.first().y)
            for (i in 0 until coords.size - 1) {
                val p0 = coords[i]
                val p1 = coords[i + 1]
                val controlX1 = p0.x + (p1.x - p0.x) * 0.45f
                val controlY1 = p0.y
                val controlX2 = p0.x + (p1.x - p0.x) * 0.55f
                val controlY2 = p1.y
                cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
            }
        }

        val fillPath = Path().apply {
            addPath(strokePath)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0.0f)),
                startY = 0f,
                endY = h
            )
        )

        drawPath(
            path = strokePath,
            color = lineColor,
            style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun KpiSummaryCard(
    title: String,
    value: String,
    icon: ImageVector,
    sparklineColor: Color,
    sparklinePoints: List<Float>,
    modifier: Modifier = Modifier
) {
    val palette = LocalSoloWorkPalette.current
    val cornerRadius = if (palette.customizeLayoutStyle) 18.dp else 10.dp

    Surface(
        modifier = modifier
            .border(1.dp, palette.border, RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        color = palette.cardBg,
        tonalElevation = if (palette.customizeLayoutStyle) 4.dp else 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = sparklineColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = palette.textPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            SparklineChart(
                points = sparklinePoints,
                lineColor = sparklineColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
            )
        }
    }
}

@Composable
fun TopHeaderBar(
    userName: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isDark: Boolean,
    onToggleThemeMode: (String) -> Unit,
    hasUnreadNotifications: Boolean,
    onNotificationClick: () -> Unit,
    onToggleAppearancePanel: () -> Unit,
    showMenuButton: Boolean,
    onMenuClick: () -> Unit
) {
    val palette = LocalSoloWorkPalette.current
    val firstName = userName.substringBefore(" ").ifBlank { "Alex" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                if (showMenuButton) {
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier.testTag("open_sidebar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open navigation menu",
                            tint = palette.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = "Welcome back, $firstName! 👋",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Notification Bell with Unread Indicator
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.cardBg)
                        .border(1.dp, palette.border, RoundedCornerShape(10.dp))
                        .clickable { onNotificationClick() }
                        .testTag("notification_bell_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    if (hasUnreadNotifications) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 7.dp, end = 7.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AccentCrimson)
                        )
                    }
                }

                // Quick Light / Dark Switcher Pill
                Row(
                    modifier = Modifier
                        .height(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.cardBg)
                        .border(1.dp, palette.border, RoundedCornerShape(10.dp))
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!isDark) palette.accent.copy(alpha = 0.22f) else Color.Transparent)
                            .clickable { onToggleThemeMode("Light") }
                            .testTag("quick_light_mode_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LightMode,
                            contentDescription = "Light Mode",
                            tint = if (!isDark) AccentAmber else palette.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) palette.cardElevatedBg else Color.Transparent)
                            .clickable { onToggleThemeMode("Dark") }
                            .testTag("quick_dark_mode_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DarkMode,
                            contentDescription = "Dark Mode",
                            tint = if (isDark) palette.accent else palette.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Appearance Panel Toggle Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.accent.copy(alpha = 0.16f))
                        .border(1.dp, palette.accent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .clickable { onToggleAppearancePanel() }
                        .testTag("appearance_panel_toggle_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Appearance Settings",
                        tint = palette.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Global Live Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    text = "Search projects, clients, tasks, invoices...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = palette.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = palette.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = palette.cardBg,
                unfocusedContainerColor = palette.cardBg,
                focusedBorderColor = palette.accent,
                unfocusedBorderColor = palette.border,
                focusedTextColor = palette.textPrimary,
                unfocusedTextColor = palette.textPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("global_search_input")
        )
    }
}

@Composable
fun LeftNavigationSidebar(
    activeTab: SoloWorkTab,
    onSelectTab: (SoloWorkTab) -> Unit,
    userName: String,
    userRole: String,
    compact: Boolean,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalSoloWorkPalette.current
    val sidebarWidth = if (compact) 78.dp else 230.dp

    Column(
        modifier = modifier
            .width(sidebarWidth)
            .fillMaxHeight()
            .background(palette.sidebarBg)
            .border(width = 1.dp, color = palette.border)
            .padding(horizontal = if (compact) 10.dp else 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // SoloWork Brand Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (compact) Arrangement.Center else Arrangement.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp)
            ) {
                SoloWorkLogoBadge(sizeDp = 34)
                if (!compact) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "SoloWork",
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            val navItems = listOf(
                SoloWorkTab.Dashboard to Icons.Default.Home,
                SoloWorkTab.Projects to Icons.Default.FolderOpen,
                SoloWorkTab.Invoices to Icons.Default.Description,
                SoloWorkTab.Clients to Icons.Default.PeopleOutline,
                SoloWorkTab.TaskBoard to Icons.Default.Checklist,
                SoloWorkTab.TimeTracker to Icons.Default.Schedule,
                SoloWorkTab.Analytics to Icons.Default.BarChart,
                SoloWorkTab.AiCopilot to Icons.Default.AutoAwesome,
                SoloWorkTab.Settings to Icons.Default.Settings
            )

            navItems.forEach { (tab, icon) ->
                val isSelected = activeTab == tab
                val bgModifier = if (isSelected) {
                    Modifier.background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(palette.accent, palette.accentSecondary)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier.background(Color.Transparent, RoundedCornerShape(12.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (compact) Arrangement.Center else Arrangement.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .then(bgModifier)
                        .clickable { onSelectTab(tab) }
                        .padding(horizontal = 12.dp, vertical = 11.dp)
                        .testTag("nav_item_${tab.name.lowercase()}")
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = tab.title,
                        tint = if (isSelected) Color.White else palette.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    if (!compact) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) Color.White else palette.textSecondary,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // User Card (Alex Miller) + Log Out Button
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = palette.cardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, palette.border, RoundedCornerShape(14.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (compact) Arrangement.Center else Arrangement.Start,
                    modifier = Modifier.padding(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(palette.accent, AccentAmber)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val initials = userName.split(" ")
                            .mapNotNull { it.firstOrNull()?.uppercase() }
                            .take(2)
                            .joinToString("")
                            .ifEmpty { "AM" }
                        Text(
                            text = initials,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    if (!compact) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userName,
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = userRole,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.cardBg)
                    .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                    .clickable { onSignOut() }
                    .padding(vertical = 10.dp, horizontal = 8.dp)
                    .testTag("sidebar_logout_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Log Out",
                    tint = palette.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
                if (!compact) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Log Out",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.textPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun AppearanceSettingsPanel(
    settings: UserSettingsItem,
    onUpdateThemeMode: (String) -> Unit,
    onUpdateThemePreset: (String) -> Unit,
    onUpdateAccentColor: (String) -> Unit,
    onUpdateFontFamily: (String) -> Unit,
    onUpdateCustomizeLayoutStyle: (Boolean) -> Unit,
    onUpdateSidebarCompact: (Boolean) -> Unit,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalSoloWorkPalette.current

    Column(
        modifier = modifier
            .width(280.dp)
            .fillMaxHeight()
            .background(palette.sidebarBg)
            .border(1.dp, palette.border)
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Appearance Settings",
                    tint = palette.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Appearance Settings",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            if (onClose != null) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Appearance Settings",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = palette.border)
        Spacer(modifier = Modifier.height(16.dp))

        // 1. Theme Mode (Light Theme vs Dark Theme)
        Text(
            text = "Theme Mode",
            style = MaterialTheme.typography.labelLarge,
            color = palette.textPrimary
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(palette.cardBg)
                .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            val isLight = settings.themeMode == "Light"
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isLight) palette.cardElevatedBg else Color.Transparent)
                    .clickable { onUpdateThemeMode("Light") }
                    .padding(vertical = 8.dp)
                    .testTag("theme_mode_light_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Light Theme ☀️",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isLight) palette.textPrimary else palette.textSecondary,
                    fontWeight = if (isLight) FontWeight.SemiBold else FontWeight.Normal
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (!isLight) palette.background else Color.Transparent)
                    .clickable { onUpdateThemeMode("Dark") }
                    .padding(vertical = 8.dp)
                    .testTag("theme_mode_dark_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Dark Theme 🌙",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (!isLight) palette.textPrimary else palette.textSecondary,
                    fontWeight = if (!isLight) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        HorizontalDivider(color = palette.border)
        Spacer(modifier = Modifier.height(16.dp))

        // 2. Color Accents (5 Live Swatches: Indigo, Emerald, Violet, Amber, Crimson)
        Text(
            text = "Color Accents",
            style = MaterialTheme.typography.labelLarge,
            color = palette.textPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        val swatches = listOf(
            "Indigo" to AccentIndigo,
            "Emerald" to AccentEmerald,
            "Violet" to AccentViolet,
            "Amber" to AccentAmber,
            "Crimson" to AccentCrimson
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            swatches.forEach { (name, color) ->
                val selected = settings.accentColor == name
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onUpdateAccentColor(name) }
                        .testTag("swatch_${name.lowercase()}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (selected) 2.5.dp else 1.dp,
                                color = if (selected) Color.White else palette.border,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "$name selected",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) palette.textPrimary else palette.textSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        HorizontalDivider(color = palette.border)
        Spacer(modifier = Modifier.height(16.dp))

        // 3. Menu Settings & Toggles
        Text(
            text = "Menu Settings & Toggles",
            style = MaterialTheme.typography.labelLarge,
            color = palette.textPrimary
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Customize layout style checkbox
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onUpdateCustomizeLayoutStyle(!settings.customizeLayoutStyle) }
                .padding(vertical = 4.dp)
        ) {
            Checkbox(
                checked = settings.customizeLayoutStyle,
                onCheckedChange = { onUpdateCustomizeLayoutStyle(it) },
                colors = CheckboxDefaults.colors(checkedColor = palette.accent),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Customize layout style",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Font Switcher (Inter, Roboto, Poppins)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("Inter", "Roboto", "Poppins").forEach { font ->
                val isSelected = settings.fontFamily == font
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) palette.accent.copy(alpha = 0.22f) else palette.cardBg)
                        .border(
                            1.dp,
                            if (isSelected) palette.accent else palette.border,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onUpdateFontFamily(font) }
                        .padding(vertical = 7.dp)
                        .testTag("font_option_${font.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = font,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) palette.accent else palette.textSecondary,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sidebar: Expanded / Compact toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onUpdateSidebarCompact(!settings.sidebarCompact) }
                .padding(vertical = 4.dp)
        ) {
            Checkbox(
                checked = settings.sidebarCompact,
                onCheckedChange = { onUpdateSidebarCompact(it) },
                colors = CheckboxDefaults.colors(checkedColor = palette.accent),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Sidebar: Expanded / Compact",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
        HorizontalDivider(color = palette.border)
        Spacer(modifier = Modifier.height(16.dp))

        // 4. Custom Theme Presets (4 Visual Thumbnail Cards)
        Text(
            text = "Custom Theme Presets",
            style = MaterialTheme.typography.labelLarge,
            color = palette.textPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        val presets = listOf(
            Triple("Classic Dark", ClassicDarkBg, ClassicDarkCard),
            Triple("Daylight", DaylightBg, DaylightCard),
            Triple("Solarized", SolarizedDarkBg, SolarizedDarkCard),
            Triple("Cyberpunk", CyberpunkBg, CyberpunkCard)
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                presets.take(2).forEach { (presetName, bgCol, cardCol) ->
                    ThemePresetPreviewCard(
                        name = presetName,
                        bgColor = bgCol,
                        cardColor = cardCol,
                        accentColor = palette.accent,
                        isSelected = settings.themePreset == presetName,
                        onClick = { onUpdateThemePreset(presetName) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                presets.drop(2).forEach { (presetName, bgCol, cardCol) ->
                    ThemePresetPreviewCard(
                        name = presetName,
                        bgColor = bgCol,
                        cardColor = cardCol,
                        accentColor = palette.accent,
                        isSelected = settings.themePreset == presetName,
                        onClick = { onUpdateThemePreset(presetName) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemePresetPreviewCard(
    name: String,
    bgColor: Color,
    cardColor: Color,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalSoloWorkPalette.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable { onClick() }
            .testTag("preset_${name.lowercase().replace(" ", "_")}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(bgColor)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) palette.accent else palette.border,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(6.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(50.dp)) {
                val w = size.width
                val h = size.height
                // Mini sidebar
                drawRoundRect(
                    color = cardColor,
                    topLeft = Offset(0f, 0f),
                    size = Size(w * 0.22f, h),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                // Mini active nav pill
                drawRoundRect(
                    color = accentColor,
                    topLeft = Offset(w * 0.03f, h * 0.16f),
                    size = Size(w * 0.16f, h * 0.14f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                // Mini KPI cards
                for (i in 0..2) {
                    val startX = w * 0.28f + i * (w * 0.24f)
                    drawRoundRect(
                        color = cardColor,
                        topLeft = Offset(startX, 0f),
                        size = Size(w * 0.21f, h * 0.42f),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }
                // Mini table card
                drawRoundRect(
                    color = cardColor,
                    topLeft = Offset(w * 0.28f, h * 0.50f),
                    size = Size(w * 0.70f, h * 0.48f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) palette.accent else palette.textSecondary,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
