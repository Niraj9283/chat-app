package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class Chat(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val type: String, // DIRECT, GROUP, CHANNEL, BOT
    val encryptionKeyHex: String,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val disappearingTimerSeconds: Int = 0 // 0 means disabled, otherwise timer in seconds
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val chatId: Long,
    val senderName: String,
    val encryptedPayload: String, // Base64 ciphertext
    val ivHex: String, // Initialization vector
    val messageType: String, // TEXT, VOICE_NOTE, PHOTO, FILE, STICKER
    val attachmentUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val selfDestructAt: Long? = null // Epoch ms threshold for auto-deletion
)
