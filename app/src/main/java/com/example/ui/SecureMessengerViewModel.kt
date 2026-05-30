package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.EncryptionEngine
import com.example.crypto.GeminiClient
import com.example.data.AppDatabase
import com.example.data.Chat
import com.example.data.Message
import com.example.data.SecureMessengerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface Screen {
    object ChatList : Screen
    data class ChatDetail(val chatId: Long) : Screen
    object Settings : Screen
    object KeyCenter : Screen
}

class SecureMessengerViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: SecureMessengerRepository
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = SecureMessengerRepository(database.chatDao(), database.messageDao())
        
        // Populate initial secure mock data if the database is clean
        viewModelScope.launch {
            repository.populateInitialDataIfEmpty()
        }
        
        // Start vanishing messages garbage collector
        startVanishCollector()
    }

    // Screens navigation
    private val _currentScreen = MutableStateFlow<Screen>(Screen.ChatList)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _backStack = MutableStateFlow<List<Screen>>(listOf(Screen.ChatList))

    fun navigateTo(screen: Screen) {
        val currentStack = _backStack.value.toMutableList()
        currentStack.add(screen)
        _backStack.value = currentStack
        _currentScreen.value = screen
        
        if (screen is Screen.ChatDetail) {
            _activeChatId.value = screen.chatId
            // Observe chat disappearing timer
            viewModelScope.launch {
                val chat = repository.getChatById(screen.chatId)
                if (chat != null) {
                    _activeChatTimer.value = chat.disappearingTimerSeconds
                }
            }
        } else {
            _activeChatId.value = null
        }
    }

    fun navigateBack(): Boolean {
        val currentStack = _backStack.value.toMutableList()
        if (currentStack.size > 1) {
            currentStack.removeAt(currentStack.size - 1)
            _backStack.value = currentStack
            val prevScreen = currentStack.last()
            _currentScreen.value = prevScreen
            
            if (prevScreen is Screen.ChatDetail) {
                _activeChatId.value = prevScreen.chatId
            } else {
                _activeChatId.value = null
            }
            return true
        }
        return false
    }

    // Interactive Ciphertext / Decrypted toggle
    private val _isCiphertextMode = MutableStateFlow(false)
    val isCiphertextMode: StateFlow<Boolean> = _isCiphertextMode.asStateFlow()

    fun toggleCiphertextMode() {
        _isCiphertextMode.value = !_isCiphertextMode.value
    }

    // Active Chat Management
    private val _activeChatId = MutableStateFlow<Long?>(null)
    val activeChatId: StateFlow<Long?> = _activeChatId.asStateFlow()

    val chatList: StateFlow<List<Chat>> = repository.allChats
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeChat: StateFlow<Chat?> = _activeChatId.flatMapLatest { id ->
        if (id == null) flowOf<Chat?>(null)
        else {
            flowOf<Chat?>(null) // trigger updates
            // Let's query db directly on flow trigger
            kotlinx.coroutines.flow.flow {
                emit(repository.getChatById(id))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeMessages: StateFlow<List<Message>> = _activeChatId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else repository.getMessagesForChat(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Message Input State
    private val _messageInput = MutableStateFlow("")
    val messageInput: StateFlow<String> = _messageInput.asStateFlow()

    fun updateMessageInput(input: String) {
        _messageInput.value = input
    }

    // Disappearing Messages State
    private val _activeChatTimer = MutableStateFlow(0)
    val activeChatTimer: StateFlow<Int> = _activeChatTimer.asStateFlow()

    fun updateDisappearingTimer(seconds: Int) {
        _activeChatTimer.value = seconds
        val chatId = _activeChatId.value ?: return
        viewModelScope.launch {
            val chat = repository.getChatById(chatId)
            if (chat != null) {
                repository.updateChat(chat.copy(disappearingTimerSeconds = seconds))
            }
        }
    }

    // User Settings
    private val _username = MutableStateFlow("CrypticOperator")
    val username: StateFlow<String> = _username.asStateFlow()

    fun updateUsername(name: String) {
        _username.value = name
    }

    // Theme Customization
    private val _appTheme = MutableStateFlow("Cyber Obsidian") // "Cyber Obsidian", "Nordic Glacier", "Royal Crimson"
    val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    fun selectTheme(themeName: String) {
        _appTheme.value = themeName
    }

    // Dynamic message writing & simulated events
    private var botTypingJob: Job? = null
    
    private val _isBotTyping = MutableStateFlow(false)
    val isBotTyping: StateFlow<Boolean> = _isBotTyping.asStateFlow()

    fun sendSecureMessage() {
        val text = _messageInput.value.trim()
        val chatId = _activeChatId.value ?: return
        if (text.isEmpty()) return

        _messageInput.value = ""

        viewModelScope.launch {
            // Send user message
            repository.sendSecureMessage(
                chatId = chatId,
                senderName = "You",
                plainText = text
            )

            // Trigger Aegis Cryptographic Bot response if this is a bot chat
            val chat = repository.getChatById(chatId)
            if (chat != null && chat.type == "BOT") {
                triggerSafeBotResponse(chatId, text)
            }
        }
    }

    fun sendSimulatedAttachment(type: String, uri: String, previewText: String) {
        val chatId = _activeChatId.value ?: return
        viewModelScope.launch {
            repository.sendSecureMessage(
                chatId = chatId,
                senderName = "You",
                plainText = previewText,
                messageType = type,
                attachmentUri = uri
            )
        }
    }

    private fun triggerSafeBotResponse(chatId: Long, userPrompt: String) {
        botTypingJob?.cancel()
        botTypingJob = viewModelScope.launch {
            _isBotTyping.value = true
            // Simulate cryptographic handshake processing lag (feeling incredibly realistic)
            delay(1500)
            
            val aiResponse = GeminiClient.queryGemini(userPrompt)
            
            repository.sendSecureMessage(
                chatId = chatId,
                senderName = "Aegis AI Bot",
                plainText = aiResponse
            )
            _isBotTyping.value = false
        }
    }

    // Create custom secure chat/group/channel
    fun createNewChat(name: String, type: String, customKey: String = "") {
        viewModelScope.launch {
            val key = if (customKey.length >= 8) customKey else EncryptionEngine.generateHexKey()
            val finalName = if (type == "GROUP") "$name 🛸" else if (type == "CHANNEL") "$name 📡" else "$name 🔐"
            val chatId = repository.insertChat(Chat(
                name = finalName,
                type = type,
                encryptionKeyHex = key
            ))
            
            // Insert introductory encrypted message (E2E handshake complete)
            val introText = when (type) {
                "GROUP" -> "Encrypted multi-party keyset initialized for group $name. All future nodes can communicate locally with AES-GCM tags."
                "CHANNEL" -> "Welcome to broadcast node $name. As creator, your identity public key secures all feeds."
                else -> "Handshake complete. AES-GCM 256 symmetric channel opened for E2E secured direct chat."
            }
            
            val envelope = EncryptionEngine.encrypt(introText, key)
            repository.insertRawMessage(Message(
                chatId = chatId,
                senderName = if (type == "CHANNEL") name else "System Codebook",
                encryptedPayload = envelope.base64Payload,
                ivHex = envelope.ivHex,
                messageType = "TEXT",
                timestamp = System.currentTimeMillis()
            ))

            navigateTo(Screen.ChatDetail(chatId))
        }
    }

    // Delete chat permanently
    fun deleteChatPermanently(chatId: Long) {
        viewModelScope.launch {
            repository.deleteChat(chatId)
            navigateBack()
        }
    }

    // Delete static message log
    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
        }
    }

    // Rotate symmetric encryption keys (Dynamic re-keying)
    fun rotateSymmetricKey(chatId: Long) {
        viewModelScope.launch {
            val chat = repository.getChatById(chatId) ?: return@launch
            val newKey = EncryptionEngine.generateHexKey()
            repository.updateChat(chat.copy(encryptionKeyHex = newKey))
            
            val systemConfirm = "Symmetric key rotated successfully! Future packets will authenticate using the new 256-bit entropy block."
            val envelope = EncryptionEngine.encrypt(systemConfirm, newKey)
            repository.insertRawMessage(Message(
                chatId = chatId,
                senderName = "Aegis Core",
                encryptedPayload = envelope.base64Payload,
                ivHex = envelope.ivHex,
                messageType = "TEXT",
                timestamp = System.currentTimeMillis()
            ))
        }
    }

    // Garbage collector of vanishing packets
    private fun startVanishCollector() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val deletedCount = repository.cleanExpiredMessages()
                if (deletedCount > 0) {
                    // Force state refresh
                }
                delay(1000) // Scan database every 1 second
            }
        }
    }
}
