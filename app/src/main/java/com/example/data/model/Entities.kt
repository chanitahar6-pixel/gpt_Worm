package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val model: String = "AI 3.8 Flash"
)

@Entity(
    tableName = "messages",
    indices = [Index(value = ["conversationId"])]
)
data class MessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val role: String, // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val attachedFileName: String? = null,
    val attachedFileSize: Long? = null,
    val isError: Boolean = false
)

@Entity(tableName = "user_files")
data class UserFileEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val conversationId: String = "",
    val name: String,
    val size: Long,
    val mimeType: String,
    val contentSnippet: String,
    val fullContent: String,
    val uploadedAt: Long = System.currentTimeMillis()
)
