package com.evernoteclone.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val content: String = "",
    val notebookId: String? = null,
    val tagIds: List<String> = emptyList(),
    val resources: List<String> = emptyList(), // "type|name|uri"
    val reminderAt: Long? = null,
    val pinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val inTrash: Boolean = false,
    val trashedAt: Long? = null,
)

@Entity(tableName = "notebooks")
data class Notebook(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val color: Long,          // ARGB
    val stack: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "recent_searches")
data class RecentSearch(
    @PrimaryKey val keyword: String,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "message_threads")
data class MessageThread(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val color: Long,
    val lastMessage: String = "",
    val lastAt: Long = System.currentTimeMillis(),
    val unread: Int = 0,
    val members: List<String> = emptyList(),
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val threadId: String,
    val sender: String,
    val text: String,
    val sentAt: Long = System.currentTimeMillis(),
    val isMine: Boolean = false,
    val read: Boolean = true,
    val savedToNote: Boolean = false,
)

@Entity(tableName = "change_log")
data class ChangeLog(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val entity: String,       // note / notebook / tag / message
    val entityId: String,
    val op: String,           // create / update / delete
    val timestamp: Long = System.currentTimeMillis(),
    val synced: Boolean = false,
)

@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey val id: String = "default",
    val darkMode: Boolean = false,
    val pinEnabled: Boolean = false,
    val pinCode: String = "",
    val notificationsEnabled: Boolean = true,
    val lastSyncAt: Long? = null,
    val pendingSyncCount: Int = 0,
    val viewMode: String = "list",   // list / grid / reminders
    val sortMode: String = "updated",// updated / created / title / reminder
    val fontScale: Float = 1f,
)
