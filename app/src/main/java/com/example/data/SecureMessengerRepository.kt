package com.example.data

import com.example.crypto.EncryptionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class SecureMessengerRepository(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao
) {
    val allChats: Flow<List<Chat>> = chatDao.getChatsFlow()

    fun getMessagesForChat(chatId: Long): Flow<List<Message>> {
        return messageDao.getMessagesForChat(chatId)
    }

    suspend fun getChatById(chatId: Long): Chat? {
        return chatDao.getChatById(chatId)
    }

    suspend fun insertChat(chat: Chat): Long {
        return chatDao.insertChat(chat)
    }

    suspend fun updateChat(chat: Chat) {
        chatDao.updateChat(chat)
    }

    suspend fun deleteChat(chatId: Long) {
        chatDao.deleteChatById(chatId)
        messageDao.clearChatMessages(chatId)
    }

    suspend fun deleteMessage(id: Long) {
        messageDao.deleteMessageById(id)
    }

    suspend fun sendSecureMessage(
        chatId: Long,
        senderName: String,
        plainText: String,
        messageType: String = "TEXT",
        attachmentUri: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val chat = chatDao.getChatById(chatId) ?: return@withContext -1L
        
        // Encrypt plainText
        val envelope = EncryptionEngine.encrypt(plainText, chat.encryptionKeyHex)
        
        val now = System.currentTimeMillis()
        val selfDestructDelay = chat.disappearingTimerSeconds
        val selfDestructAt = if (selfDestructDelay > 0) {
            now + (selfDestructDelay * 1000)
        } else {
            null
        }

        val message = Message(
            chatId = chatId,
            senderName = senderName,
            encryptedPayload = envelope.base64Payload,
            ivHex = envelope.ivHex,
            messageType = messageType,
            attachmentUri = attachmentUri,
            timestamp = now,
            selfDestructAt = selfDestructAt
        )

        messageDao.insertMessage(message)
    }

    suspend fun insertRawMessage(message: Message) = withContext(Dispatchers.IO) {
        messageDao.insertMessage(message)
    }

    suspend fun cleanExpiredMessages(): Int = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        messageDao.deleteExpiredMessages(now)
    }

    /**
     * Pre-populates the database with realistic encrypted messages for Alice, Bot, and Groups.
     */
    suspend fun populateInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val existing = chatDao.getChatsFlow().first()
        if (existing.isNotEmpty()) return@withContext

        // 1. Create Chats
        val aliceKey = EncryptionEngine.generateHexKey()
        val aliceChatId = chatDao.insertChat(Chat(
            name = "Alice 🔐",
            type = "DIRECT",
            encryptionKeyHex = aliceKey,
            isPinned = true
        ))

        val secureGroupKey = EncryptionEngine.generateHexKey()
        val groupId = chatDao.insertChat(Chat(
            name = "Syndicate Dev Group 🛸",
            type = "GROUP",
            encryptionKeyHex = secureGroupKey
        ))

        val channelKey = EncryptionEngine.generateHexKey()
        val channelId = chatDao.insertChat(Chat(
            name = "Aegis Cryptography Feed 📡",
            type = "CHANNEL",
            encryptionKeyHex = channelKey
        ))

        val botKey = EncryptionEngine.generateHexKey()
        val botId = chatDao.insertChat(Chat(
            name = "Aegis AI Bot 🤖",
            type = "BOT",
            encryptionKeyHex = botKey
        ))

        // 2. Pre-populate Alice Encrypted Chat
        insertPrepopulatedMessage(aliceChatId, "Alice", "Hey there! This chat is encrypted using end-to-end AES-GCM 256. Click the toggle above to see the raw binary ciphertext in our Room Database!", aliceKey, -200000)
        insertPrepopulatedMessage(aliceChatId, "You", "Wow! This is amazing. So what we store is fully secure, in Base64 encrypted format, and decrypted in real-time?", aliceKey, -180000)
        insertPrepopulatedMessage(aliceChatId, "Alice", "Exactly! If someone pulls the sqlite db of this messenger from the phone, they will only see random garbage bytes. Check the fingerprint emoji representation of our key, it matches perfectly!", aliceKey, -160000)

        // 3. Pre-populate Group Chat
        insertPrepopulatedMessage(groupId, "Bob", "Hello team! Confirming that our chat payload is encrypted.", secureGroupKey, -500000)
        insertPrepopulatedMessage(groupId, "Dave", "Excellent, group messaging key exchange succeeded.", secureGroupKey, -480000)
        insertPrepopulatedMessage(groupId, "You", "Perfect! Let's build the secure UI now.", secureGroupKey, -450000)

        // 4. Pre-populate Channel
        insertPrepopulatedMessage(channelId, "Aegis News", "System update v1.4: Dynamic disappearing timers are now operational. Slide the safety indicator in any chat to set custom automatic self-destruct countdowns on all future text, audio notes, and attachments.", channelKey, -800000)
        insertPrepopulatedMessage(channelId, "Aegis News", "Cryptography tip of the day: High-entropy key generation should rely on SecureRandom sources to prevent seed duplication. Aegis implements dynamic GCM nonces for each packet to guarantee protection from replay attacks.", channelKey, -700000)

        // 5. Pre-populate Bot Welcome Message
        insertPrepopulatedMessage(botId, "Aegis AI Bot", "Greetings, Operator. I am the Aegis AI Secure Bot, running with real-time payload protection. Ask me any question about privacy, encryption, cybersecurity, or let's just chat. All queries are fully encrypted. Type /help to see special commands!", botKey, -50000)
    }

    private suspend fun insertPrepopulatedMessage(chatId: Long, sender: String, text: String, hexKey: String, timeOffsetMs: Long) {
        val envelope = EncryptionEngine.encrypt(text, hexKey)
        val message = Message(
            chatId = chatId,
            senderName = sender,
            encryptedPayload = envelope.base64Payload,
            ivHex = envelope.ivHex,
            messageType = "TEXT",
            timestamp = System.currentTimeMillis() + timeOffsetMs
        )
        messageDao.insertMessage(message)
    }
}
