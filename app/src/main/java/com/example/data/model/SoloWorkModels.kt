package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class ProjectItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val clientName: String = "",
    val clientAvatarColor: String = "#6366F1",
    val progress: Long = 0L,
    val dueDate: String = "Apr 8, 2026",
    val budget: Long = 0L,
    val status: String = "Active", // Active, Completed, On Hold
    val category: String = "Web Design",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ClientItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val company: String = "",
    val email: String = "",
    val status: String = "Active", // Active, Lead, Archived
    val totalBilled: Long = 0L,
    val projectsCount: Long = 1L,
    val avatarColor: String = "#6366F1",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class InvoiceItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val invoiceNumber: String = "",
    val clientName: String = "",
    val projectTitle: String = "",
    val amountCents: Long = 0L,
    val status: String = "Pending", // Paid, Pending, Overdue
    val issuedDate: String = "Apr 1, 2026",
    val dueDate: String = "Apr 15, 2026",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TaskItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val projectName: String = "",
    val status: String = "Todo", // Todo, In Progress, Review, Done
    val priority: String = "Medium", // High, Medium, Low
    val completed: Boolean = false,
    val dueDate: String = "Apr 8, 2026",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TimeLogItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectName: String = "",
    val taskDescription: String = "",
    val durationMinutes: Long = 60L,
    val hourlyRate: Long = 95L,
    val loggedDate: String = "Apr 8, 2026",
    val billable: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserSettingsItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val themeMode: String = "Dark", // Dark, Light
    val themePreset: String = "Classic Dark", // Classic Dark, Daylight, Solarized, Cyberpunk
    val accentColor: String = "Indigo", // Indigo, Emerald, Violet, Amber, Crimson
    val fontFamily: String = "Inter", // Inter, Roboto, Poppins
    val customizeLayoutStyle: Boolean = true,
    val sidebarCompact: Boolean = false,
    val displayName: String = "Alex Miller",
    val roleTitle: String = "Principal Freelancer",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
