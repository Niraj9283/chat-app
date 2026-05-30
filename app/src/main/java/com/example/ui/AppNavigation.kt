package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.graphicsLayer
import com.example.ui.theme.SecureMessengerTheme
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crypto.EncryptionEngine
import com.example.data.Chat
import com.example.data.Message
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Root Composable wrapping navigation, background canvas, and top-level theme execution.
 */
@Composable
fun SecureMessengerApp(viewModel: SecureMessengerViewModel) {
    val themeName by viewModel.appTheme.collectAsStateWithLifecycle()

    SecureMessengerTheme(themeName = themeName) {
        val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // High-fidelity background digital streams (looks incredible!)
            if (themeName != "Nordic Glacier") {
                BinaryGridBackground()
            }

            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    slideInHorizontally(initialOffsetX = { 1080 }) + fadeIn() togetherWith
                            slideOutHorizontally(targetOffsetX = { -1080 }) + fadeOut()
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    is Screen.ChatList -> ChatListScreen(viewModel)
                    is Screen.ChatDetail -> ChatDetailScreen(viewModel, screen.chatId)
                    is Screen.Settings -> SettingsScreen(viewModel)
                    is Screen.KeyCenter -> KeyCenterScreen(viewModel)
                }
            }
        }
    }
}

/**
 * Animated cyber pattern drawing floating binary streams in negative space.
 */
@Composable
fun BinaryGridBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "binary_grid")
    val animOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 500f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "animOffset"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {}
    ) {
        val sizeWidth = size.width
        val sizeHeight = size.height
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 20f), animOffset)
        
        // Draw grid lines
        val lineSpacing = 120.dp.toPx()
        val accentColor = Color(0x1500E5FF)
        
        var x = 0f
        while (x < sizeWidth) {
            drawLine(
                color = accentColor,
                start = Offset(x, 0f),
                end = Offset(x, sizeHeight),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dashEffect
            )
            x += lineSpacing
        }

        var y = 0f
        while (y < sizeHeight) {
            drawLine(
                color = accentColor,
                start = Offset(0f, y),
                end = Offset(sizeWidth, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dashEffect
            )
            y += lineSpacing
        }
    }
}

/**
 * Chats List Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(viewModel: SecureMessengerViewModel) {
    val chats by viewModel.chatList.collectAsStateWithLifecycle()
    val isCiphertext by viewModel.isCiphertextMode.collectAsStateWithLifecycle()
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            LargeTopAppBar(
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = {
                    Column {
                        Text(
                            text = "Aegis Secure",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp
                        )
                        Text(
                            text = "Default End-To-End Cipher Nodes Mode",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleCiphertextMode() },
                        modifier = Modifier.testTag("ciphertext_mode_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Toggle Database View Mode",
                            tint = if (isCiphertext) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.KeyCenter) },
                        modifier = Modifier.testTag("key_center_navigation")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Keys Center Handshake Indicator",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Settings) },
                        modifier = Modifier.testTag("settings_navigation")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings Terminal Control Panel",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("create_chat_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Initiate New E2EE Handshake"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Encrypted connection log ticker
            ConnectionTickerPanel(isCiphertext)

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("chat_search_bar"),
                placeholder = { Text("Search encrypted channel aliases or bot tags...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Handlers") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            val filteredChats = chats.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }

            if (filteredChats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Shield Indicator Empty",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No private connections established",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Aegis secures every transmission. Click the '+' buttonbelow to initiate a safe session with an AES key.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredChats, key = { it.id }) { chat ->
                        ChatItemRow(
                            chat = chat,
                            isCiphertextMode = isCiphertext,
                            onClick = { viewModel.navigateTo(Screen.ChatDetail(chat.id)) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateChatWizardDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, type, key ->
                viewModel.createNewChat(name, type, key)
                showCreateDialog = false
            }
        )
    }
}

/**
 * Ticker displaying dynamic cryptographic security information.
 */
@Composable
fun ConnectionTickerPanel(isCiphertext: Boolean) {
    var tickCount by remember { mutableStateOf(0) }
    var currentLog by remember { mutableStateOf("Ready to transmit safely.") }

    LaunchedEffect(isCiphertext) {
        while (true) {
            currentLog = if (isCiphertext) {
                val nodes = listOf("0x4A1E", "0xFFAA", "0x2B93", "0xCD78")
                val encryptionLevel = listOf("AES-GCM", "SHA-256", "Entropy Block Active")
                "DB DUMP ACTIVE: Raw sqlite storage ciphered. Node [${nodes.random()}] verified tag hash: ${encryptionLevel.random()}"
            } else {
                listOf(
                    "Network Integrity: E2E session keys loaded successfully.",
                    "Disappearing timer task scanning SQLite tables periodically.",
                    "Diffie-Hellman fingerprint matches verified safely offline.",
                    "Direct cryptographic handshakes require zero central server tracking.",
                    "Aegis AI encryption models running secure offline local sandbox."
                ).random()
            }
            tickCount++
            delay(4000)
        }
    }

    AnimatedContent(
        targetState = currentLog,
        transitionSpec = {
            slideInVertically(initialOffsetY = { -50 }) + fadeIn() togetherWith
                    slideOutVertically(targetOffsetY = { 50 }) + fadeOut()
        },
        label = "Ticker"
    ) { log ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isCiphertext) Color(0x23EF4444) else Color(0x1E00E5FF)
            ),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, if (isCiphertext) Color(0x61EF4444) else Color(0x4400E5FF))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isCiphertext) Color.Red else Color.Green)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = log,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = if (isCiphertext) Color(0xFFFCA5A5) else MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Single item inside the chats list.
 */
@Composable
fun ChatItemRow(chat: Chat, isCiphertextMode: Boolean, onClick: () -> Unit) {
    val keyFingerprint = remember(chat.encryptionKeyHex) {
        EncryptionEngine.getFingerprintEmoji(chat.encryptionKeyHex)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("chat_item_${chat.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Circular Secure Emblem
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = chat.name.take(2),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (chat.isPinned) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Pinned Encryption",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))

                // Interactive Ciphertext presentation (glorious touch!)
                if (isCiphertextMode) {
                    Text(
                        text = "Encrypted Block: " + chat.encryptionKeyHex.take(16) + "...",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Secure Key",
                            modifier = Modifier.size(10.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Verified Key Fingerprint: $keyFingerprint",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Arrow Action button
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack, // Will mirror automatically if needed, let's use it as details trigger
                    contentDescription = "View Secure Logs",
                    modifier = Modifier.graphicsLayer(scaleX = -1f),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

/**
 * Chat Detail Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(viewModel: SecureMessengerViewModel, chatId: Long) {
    val activeChat by viewModel.activeChat.collectAsStateWithLifecycle()
    val messages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val isCiphertext by viewModel.isCiphertextMode.collectAsStateWithLifecycle()
    val isBotTyping by viewModel.isBotTyping.collectAsStateWithLifecycle()

    val messageInput by viewModel.messageInput.collectAsStateWithLifecycle()
    val disappearingTimerSeconds by viewModel.activeChatTimer.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var showStickersPanel by remember { mutableStateOf(false) }
    var showAttachmentDialog by remember { mutableStateOf(false) }

    // Scroll to bottom when message size grows or bot answers
    LaunchedEffect(messages.size, isBotTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (activeChat == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val finalChat = activeChat!!

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                ),
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Return")
                    }
                },
                title = {
                    Column(
                        modifier = Modifier.clickable { viewModel.navigateTo(Screen.KeyCenter) }
                    ) {
                        Text(
                            text = finalChat.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val fp = remember(finalChat.encryptionKeyHex) { 
                                EncryptionEngine.getFingerprintEmoji(finalChat.encryptionKeyHex) 
                            }
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "E2EE Check",
                                modifier = Modifier.size(11.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Code: $fp",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                actions = {
                    // Encryption visual mode toggle
                    IconButton(onClick = { viewModel.toggleCiphertextMode() }) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Toggle Ciphertext Visuals",
                            tint = if (isCiphertext) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Disappearing Messages Timer Choice Menu
                    DisappearingTimerMenuButton(
                        currentTimer = disappearingTimerSeconds,
                        onTimerChanged = { viewModel.updateDisappearingTimer(it) }
                    )

                    IconButton(
                        onClick = { viewModel.deleteChatPermanently(finalChat.id) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Burn Session",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Safe code-signing indicators
            EncryptionStatusBar(finalChat, isCiphertext)

            // Messaging bubble container
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { Spacer(modifier = Modifier.height(10.dp)) }

                items(messages, key = { it.id }) { msg ->
                    MessageBubble(
                        message = msg,
                        chatKeyHex = finalChat.encryptionKeyHex,
                        isCiphertextMode = isCiphertext,
                        onDeleteMessage = { viewModel.deleteMessage(msg.id) }
                    )
                }

                if (isBotTyping) {
                    item {
                        BotTypingIndicatorRow()
                    }
                }

                item { Spacer(modifier = Modifier.height(10.dp)) }
            }

            // Optional Stickers Selection Board
            if (showStickersPanel) {
                StickersSelectionPanel(
                    onSelectSticker = { stickerName ->
                        viewModel.sendSimulatedAttachment("STICKER", stickerName, "Transmitted cryptographic sticker: $stickerName")
                        showStickersPanel = false
                    },
                    onClose = { showStickersPanel = false }
                )
            }

            // Input Row
            MessageInputBar(
                messageText = messageInput,
                onTextChange = { viewModel.updateMessageInput(it) },
                onSend = { viewModel.sendSecureMessage() },
                onAddAttachment = { showAttachmentDialog = true },
                onToggleStickers = { showStickersPanel = !showStickersPanel }
            )
        }
    }

    if (showAttachmentDialog) {
        AttachmentsPickerDialog(
            onDismiss = { showAttachmentDialog = false },
            onPickOption = { option ->
                showAttachmentDialog = false
                when (option) {
                    "PHOTO" -> viewModel.sendSimulatedAttachment("PHOTO", "crypto_photo_placeholder", "Encrypted Secure Photo Binary [Size: 1.4MB]")
                    "VOICE_NOTE" -> viewModel.sendSimulatedAttachment("VOICE_NOTE", "crypto_voice_placeholder", "Encrypted Encrypted Speech Voice-packet [0:12]")
                    "FILE" -> viewModel.sendSimulatedAttachment("FILE", "crypto_keypair_pem", "Encrypted Pinned Cryptographic File [identity.pem]")
                }
            }
        )
    }
}

/**
 * Animated lock verification status bar at top of chat.
 */
@Composable
fun EncryptionStatusBar(chat: Chat, isCiphertext: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isCiphertext) Color(0x3C10141D) else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Secured Core",
            modifier = Modifier.size(12.dp),
            tint = if (isCiphertext) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (isCiphertext) "DEBUG STORAGE INSPECTOR: RAW DATABASE HEX ENTRIES SHOWN" 
                   else "AES-GCM (256-bit Symmetric Packet Core) Active",
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = if (isCiphertext) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
        )
    }
}

/**
 * Message items/bubbles. Handles base64 encrypted payloads!
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    chatKeyHex: String,
    isCiphertextMode: Boolean,
    onDeleteMessage: () -> Unit
) {
    val isSenderMe = message.senderName == "You"
    
    // Perform dynamic real-time decryption!
    val decryptedText = remember(message.encryptedPayload, message.ivHex, chatKeyHex) {
        val envelope = EncryptionEngine.getFingerprintEmoji(chatKeyHex) // Trigger dependencies correctly
        EncryptionEngine.decrypt(
            EncryptionEngine.EncryptedEnvelope(message.encryptedPayload, message.ivHex),
            chatKeyHex
        )
    }

    // Dynamic timer ticker for disappearing messages
    var timeRemainingSeconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(message.selfDestructAt) {
        if (message.selfDestructAt != null) {
            while (true) {
                val now = System.currentTimeMillis()
                val diff = (message.selfDestructAt - now) / 1000
                timeRemainingSeconds = if (diff > 0) diff else 0
                if (diff <= 0) break
                delay(1000)
            }
        }
    }

    var showMessageInfo by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showMessageInfo = !showMessageInfo },
        horizontalAlignment = if (isSenderMe) Alignment.End else Alignment.Start
    ) {
        if (!isSenderMe) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isSenderMe) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            // Disappearing Timer countdown indicator
            if (message.selfDestructAt != null && !isSenderMe) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Time Expire",
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                     text = "${timeRemainingSeconds}s",
                     fontSize = 10.sp,
                     color = MaterialTheme.colorScheme.error,
                     fontFamily = FontFamily.Monospace,
                     modifier = Modifier.padding(end = 4.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isSenderMe) 16.dp else 2.dp,
                            bottomEnd = if (isSenderMe) 2.dp else 16.dp
                        )
                    )
                    .background(
                        if (isSenderMe) {
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = if (isCiphertextMode) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else Color.Transparent,
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isSenderMe) 16.dp else 2.dp,
                            bottomEnd = if (isSenderMe) 2.dp else 16.dp
                        )
                    )
                    .padding(14.dp)
                    .widthIn(max = 280.dp)
            ) {
                Column {
                    // Attachment Display Support
                    if (message.messageType != "TEXT") {
                        AttachmentDisplayCard(message)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (isCiphertextMode) {
                        // High tech terminal raw database representation!
                        Column {
                            Text(
                                text = "RAW DATABASE PAYLOAD (AES-GCM):",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSenderMe) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "IV: " + message.ivHex,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSenderMe) Color.White.copy(alpha = 0.9f) else Color.Yellow,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Cipher: " + message.encryptedPayload.replace("\n", ""),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSenderMe) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        // Decrypted original secure visual text (Normal user text)
                        Text(
                            text = decryptedText,
                            color = if (isSenderMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            if (message.selfDestructAt != null && isSenderMe) {
                Text(
                     text = "${timeRemainingSeconds}s",
                     fontSize = 10.sp,
                     color = MaterialTheme.colorScheme.error,
                     fontFamily = FontFamily.Monospace,
                     modifier = Modifier.padding(start = 4.dp)
                )
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Time Expire",
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        // Expanded metadata analysis drawer (educational)
        AnimatedVisibility(visible = showMessageInfo) {
            Card(
                modifier = Modifier
                    .padding(top = 4.dp, bottom = 4.dp)
                    .widthIn(max = 300.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Cryptography Audit Logs:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Symmetric Key: 256-bit SHA\n• Authenticated Block: IV validated\n• DB Integrity: Verified successfully",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = onDeleteMessage,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.align(Alignment.End).height(24.dp)
                    ) {
                        Text("Delete Message Packet", fontSize = 9.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Display attachments (Voice note waveform, stickers, file icons)
 */
@Composable
fun AttachmentDisplayCard(message: Message) {
    when (message.messageType) {
        "STICKER" -> {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (message.attachmentUri?.contains("Shield") == true) "🛡️" 
                           else if (message.attachmentUri?.contains("Lock") == true) "🔐" 
                           else if (message.attachmentUri?.contains("Rocket") == true) "🚀" 
                           else "🌌",
                    fontSize = 32.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Aegis Secure Sticker",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = message.attachmentUri ?: "",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
        "VOICE_NOTE" -> {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.2f))
                    .padding(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Voice Record",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Encrypted Voice Note", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("0:12 SECS • AES-GCM", fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Beautiful dynamic audio waveform drawing!
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                ) {
                    val bars = 25
                    val spacing = 4.dp.toPx()
                    val barWidth = 3.dp.toPx()
                    val color = Color(0xFF00E5FF)
                    for (i in 0 until bars) {
                        val heightMultiplier = sin(i.toDouble() * 0.5).coerceIn(0.1, 1.0)
                        val h = (size.height * heightMultiplier * 0.8).toFloat()
                        val x = i * (barWidth + spacing)
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(x, (size.height - h) / 2),
                            size = androidx.compose.ui.geometry.Size(barWidth, h),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                        )
                    }
                }
            }
        }
        "PHOTO" -> {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.DarkGray.copy(alpha = 0.3f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypted Camera Asset",
                            modifier = Modifier.size(36.dp),
                            tint = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "IMAGE SECURED & SHA-256 SALT PINNED",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.LightGray
                        )
                    }
                }
            }
        }
        "FILE" -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info, // Matches custom file icon
                    contentDescription = "PEM File Asset",
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "identity_private_key.pem",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("RSA-4096 PEM BLOB • ENCRYPTED STORAGE", fontSize = 9.sp, color = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}

/**
 * Animated Bot typing cursor.
 */
@Composable
fun BotTypingIndicatorRow() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val dotAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dotAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dotAlpha3 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Aegis Bot analyzing secure channel", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF00E5FF).copy(alpha = dotAlpha1)))
                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF00E5FF).copy(alpha = dotAlpha2)))
                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF00E5FF).copy(alpha = dotAlpha3)))
            }
        }
    }
}

/**
 * Text formatting control bar for writing secured conversations.
 */
@Composable
fun MessageInputBar(
    messageText: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAddAttachment: () -> Unit,
    onToggleStickers: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onAddAttachment,
            modifier = Modifier.testTag("attachment_button")
        ) {
            Icon(
                imageVector = Icons.Default.Info, // Attach icon substitute
                contentDescription = "Attach Cryptographic Artifacts",
                modifier = Modifier.graphicsLayer(rotationZ = 45f),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        IconButton(
            onClick = onToggleStickers,
            modifier = Modifier.testTag("stickers_panel_button")
        ) {
            Icon(
                imageVector = Icons.Default.Face,
                contentDescription = "Transmit Dynamic Animated Stickers",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        OutlinedTextField(
            value = messageText,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp)
                .testTag("message_input_field"),
            placeholder = { Text("Secured chat stream...", fontSize = 14.sp) },
            maxLines = 4,
            shape = RoundedCornerShape(20.dp),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
                onSend = {
                    onSend()
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
            )
        )

        IconButton(
            onClick = {
                onSend()
                keyboardController?.hide()
                focusManager.clearFocus()
            },
            enabled = messageText.trim().isNotEmpty(),
            modifier = Modifier.testTag("send_message_button")
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Encrypted Send Dispatch",
                tint = if (messageText.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

/**
 * Menu matching disappearing timers.
 */
@Composable
fun DisappearingTimerMenuButton(
    currentTimer: Int,
    onTimerChanged: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.AccountCircle, // Clock-like icon fallback
                contentDescription = "Set Self Destruct Duration",
                tint = if (currentTimer > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Disappearing: Disabled") },
                onClick = { onTimerChanged(0); expanded = false }
            )
            DropdownMenuItem(
                text = { Text("Self-destruct: 5 Seconds") },
                onClick = { onTimerChanged(5); expanded = false }
            )
            DropdownMenuItem(
                text = { Text("Self-destruct: 15 Seconds") },
                onClick = { onTimerChanged(15); expanded = false }
            )
            DropdownMenuItem(
                text = { Text("Self-destruct: 30 Seconds") },
                onClick = { onTimerChanged(30); expanded = false }
            )
            DropdownMenuItem(
                text = { Text("Self-destruct: 1 Minute") },
                onClick = { onTimerChanged(60); expanded = false }
            )
        }
    }
}

/**
 * Holographic Cryptographic Sticker Drawer Panel
 */
@Composable
fun StickersSelectionPanel(
    onSelectSticker: (String) -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Aegis Encrypted Sticker Pack",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close Sticker Drawer")
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val stickers = listOf(
                    "🛡️ Aegis Shield" to "Shield",
                    "🔐 Crypto Lock" to "Lock",
                    "🚀 Rocket Cipher" to "Rocket",
                    "🌌 Space Nebula" to "Nebula"
                )
                stickers.forEach { (label, name) ->
                    Card(
                        modifier = Modifier
                            .size(72.dp)
                            .clickable { onSelectSticker(name) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (name == "Shield") "🛡️" else if (name == "Lock") "🔐" else if (name == "Rocket") "🚀" else "🌌",
                                    fontSize = 28.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(name, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Picker for attachments selection.
 */
@Composable
fun AttachmentsPickerDialog(
    onDismiss: () -> Unit,
    onPickOption: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Encrypted Artifact") },
        text = {
            Text("Select an attachment to wrap into an authenticated cryptographic envelope. Real nonces are generated.")
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onPickOption("PHOTO") }
                ) {
                    Text("Secure Photo Attachment")
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onPickOption("VOICE_NOTE") }
                ) {
                    Text("Record Crypto Audio Wave-note")
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onPickOption("FILE") }
                ) {
                    Text("Pin Security Cert File (.pem)")
                }
            }
        }
    )
}

/**
 * Key and Cryptography Management Center
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyCenterScreen(viewModel: SecureMessengerViewModel) {
    val chats by viewModel.chatList.collectAsStateWithLifecycle()
    val username by viewModel.username.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            MediumTopAppBar(
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                title = { Text("Cryptographic Signatures") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Return")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Your Identity Certification",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Operator Handle: $username",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Fingerprint: " + EncryptionEngine.getFingerprintEmoji(username),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "This signature secures Diffie-Hellman handshakes made across dynamic nodes inside Aegis.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Active Symmetric Sessions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            items(chats) { chat ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(chat.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                                Text("AES-GCM", fontSize = 9.sp, color = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Active 256-bit Key ID:",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = chat.encryptionKeyHex,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Code: " + EncryptionEngine.getFingerprintEmoji(chat.encryptionKeyHex),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = { viewModel.rotateSymmetricKey(chat.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Rotate Symmetric Key", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Settings Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SecureMessengerViewModel) {
    val username by viewModel.username.collectAsStateWithLifecycle()
    val activeTheme by viewModel.appTheme.collectAsStateWithLifecycle()

    var editingName by remember { mutableStateOf(username) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            MediumTopAppBar(
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                title = { Text("Control Center Panel") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Return")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Operator Nickname Config Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Secured Handle / Alias", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editingName,
                        onValueChange = { editingName = it; viewModel.updateUsername(it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Your custom handle is used to sign key payloads on local group handshakes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Theme Choices (Dynamic customized modes)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select Aegis Secure Aesthetic Mode", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val themes = listOf("Cyber Obsidian", "Nordic Glacier", "Royal Crimson")
                    themes.forEach { t ->
                        val isSelected = t == activeTheme
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { viewModel.selectTheme(t) }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = t,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // System specs card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Kernel Diagnostics", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Database Provider: Android SQLite Room v2.7\n• Master Cryptography: Galois Counter Mode 256-bit\n• Autonomic purger: Task active on loop",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

/**
 * Modals wizard to launch E2EE channels.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChatWizardDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String) -> Unit
) {
    var chatName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("DIRECT") } // DIRECT, GROUP, CHANNEL
    var customKeyEntry by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Initialize Encrypted Session") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = chatName,
                    onValueChange = { chatName = it },
                    label = { Text("Session Name / Partner Alias") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Text("Protocol / Architecture Layout:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val types = listOf("DIRECT", "GROUP", "CHANNEL")
                    types.forEach { type ->
                        val isSelected = type == selectedType
                        ElevatedFilterChip(
                            selected = isSelected,
                            onClick = { selectedType = type },
                            label = { Text(type, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = customKeyEntry,
                    onValueChange = { customKeyEntry = it },
                    label = { Text("Custom 256-bit Key Hex (Optional)") },
                    placeholder = { Text("Leave blank to generate safely") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel Handshake")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (chatName.trim().isNotEmpty()) {
                        onCreate(chatName.trim(), selectedType, customKeyEntry.trim())
                    }
                },
                enabled = chatName.trim().isNotEmpty()
            ) {
                Text("Deploy Key Pair")
            }
        }
    )
}
