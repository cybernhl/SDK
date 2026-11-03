package chat.im.trilivechat.chatedin.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.im.trilivechat.chatedin.state.SessionState
import idv.neo.entity.cs.trilivechat.response.ChatMessageItem
import chat.im.trilivechat.chatedin.ui.theme.LiveChatTheme
import chat.im.trilivechat.chatedin.viewmodel.TriliveChatViewModel
import kotlinx.coroutines.launch

/**
 * TriliveChat 客服對話主畫面 Composable。
 *
 * 具備預設不安裝連線邏輯：
 * 1. 預設先不自動發起連線。
 * 2. 檢查確定有 `site_id` 與 `agent_id` 才嘗試呼叫 `startSession` 發起連線。
 * 3. 若無傳入 `site_id` 與 `agent_id`，彈出對話框供使用者手動輸入 `site_id` 與 `agent_id`，以及可選填的新 `domain` 後才觸發連線。
 * 4. 連線建立後與身份綁定 (`bindToken`) 流程維持一致。
 *
 * @param viewModel 客服 View Model。
 * @param siteId 站台商戶識別碼（選填/預設空字串）。
 * @param agentId 客服專員/佇列識別碼（選填/預設空字串）。
 * @param domain 客服系統網址 Domain（選填/預設 https://trilivechat.chat/）。
 * @param initialToken 玩家驗證 JWT Token（選填，登入狀態傳入；未登入傳入 null）。
 * @param onCloseClick 點擊關閉按鈕回調（選填）。
 * @param onPickImageClick 點擊圖片選擇/發送按鈕回調（選填）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveChatScreen(
    viewModel: TriliveChatViewModel,
    siteId: String = "",
    agentId: String = "",
    domain: String = "https://trilivechat.chat/",
    initialToken: String? = null,
    onCloseClick: (() -> Unit)? = null,
    onPickImageClick: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }

    var activeSiteId by remember(siteId) { mutableStateOf(siteId) }
    var activeAgentId by remember(agentId) { mutableStateOf(agentId) }
    var activeDomain by remember(domain) { mutableStateOf(domain) }

    var showConfigDialog by remember { mutableStateOf(false) }
    var configInputSiteId by remember { mutableStateOf(activeSiteId) }
    var configInputAgentId by remember { mutableStateOf(activeAgentId) }
    var configInputDomain by remember { mutableStateOf(activeDomain) }

    var showBindDialog by remember { mutableStateOf(false) }
    var bindTokenInput by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val hasValidConfig = activeSiteId.isNotBlank() && activeAgentId.isNotBlank()

    // 1. 檢查有 site_id 與 agent_id 才嘗試連線；否則開啟 Config Dialog 供 User 填寫
    LaunchedEffect(activeSiteId, activeAgentId, initialToken) {
        if (hasValidConfig) {
            viewModel.startSession(
                siteId = activeSiteId,
                agentId = activeAgentId,
                token = initialToken
            )
        } else {
            showConfigDialog = true
        }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(uiState.messages.size - 1)
            }
        }
    }

    val isAnonymous = (uiState.sessionState as? SessionState.Active)?.isBound == false

    LiveChatTheme {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    windowInsets = WindowInsets.statusBars,
                    title = {
                        Column {
                            Text(
                                text = uiState.siteName ?: "線上客服",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val statusText = if (!hasValidConfig) {
                                "未設定連線參數 (點擊⚙️設定)"
                            } else {
                                when (val state = uiState.sessionState) {
                                    is SessionState.Active -> if (state.isBound) "已登入身分 (${state.username ?: "已驗證"})" else "匿名對話中"
                                    is SessionState.Initializing -> "連線建立中..."
                                    is SessionState.Error -> "連線異常: ${state.message}"
                                    SessionState.Uninitialized -> "未初始化"
                                }
                            }
                            Text(
                                text = statusText,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            configInputSiteId = activeSiteId
                            configInputAgentId = activeAgentId
                            configInputDomain = activeDomain
                            showConfigDialog = true
                        }) {
                            Text(text = "⚙️", fontSize = 20.sp)
                        }
                        IconButton(onClick = { showBindDialog = true }) {
                            Text(text = "🔑", fontSize = 20.sp)
                        }
                        if (onCloseClick != null) {
                            IconButton(onClick = onCloseClick) {
                                Text(text = "✕", fontSize = 20.sp)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            },
            floatingActionButton = {
                if (isAnonymous && hasValidConfig) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            showBindDialog = true
                        },
                        icon = { Text("🔑", fontSize = 18.sp) },
                        text = { Text("登入綁定帳號", fontSize = 14.sp) },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Banner Message Notification
                AnimatedVisibility(visible = uiState.bindingMessage != null || uiState.errorMessage != null) {
                    val isError = uiState.errorMessage != null
                    val text = uiState.errorMessage ?: uiState.bindingMessage ?: ""
                    Surface(
                        color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = text,
                            color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Chat Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.messages) { message ->
                        ChatMessageBubble(message = message)
                    }

                    if (uiState.isSending || uiState.isUploadingImage) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (uiState.isUploadingImage) "圖片上傳中..." else "訊息傳送中...",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Input Bar
                Surface(
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onPickImageClick != null) {
                            IconButton(onClick = onPickImageClick) {
                                Text("📷", fontSize = 20.sp)
                            }
                        }

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text(if (hasValidConfig) "請輸入您的問題..." else "請先設定 site_id 與 agent_id...") },
                            modifier = Modifier.weight(1f),
                            maxLines = 3,
                            enabled = hasValidConfig,
                            shape = RoundedCornerShape(24.dp)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Button(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    val textToSend = inputText
                                    inputText = ""
                                    viewModel.sendMessage(textToSend)
                                }
                            },
                            enabled = hasValidConfig && inputText.isNotBlank() && !uiState.isSending && !uiState.isUploadingImage,
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text("傳送")
                        }
                    }
                }
            }
        }

        // Config Dialog (當缺少 site_id 或 agent_id 或使用者點擊⚙️時顯示)
        if (showConfigDialog) {
            AlertDialog(
                onDismissRequest = {
                    if (hasValidConfig) showConfigDialog = false
                },
                title = { Text("設定客服連線參數") },
                text = {
                    Column {
                        Text("請填入站台識別碼 (site_id) 與專員識別碼 (agent_id) 以發起客服連線：", fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = configInputSiteId,
                            onValueChange = { configInputSiteId = it },
                            label = { Text("Site ID (站台識別碼 / 必填)") },
                            placeholder = { Text("請輸入站台識別碼") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = configInputAgentId,
                            onValueChange = { configInputAgentId = it },
                            label = { Text("Agent ID (專員識別碼 / 必填)") },
                            placeholder = { Text("請輸入專員識別碼") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = configInputDomain,
                            onValueChange = { configInputDomain = it },
                            label = { Text("Domain (客服網址 / 選填)") },
                            placeholder = { Text("預設: https://trilivechat.chat/") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (configInputSiteId.isNotBlank() && configInputAgentId.isNotBlank()) {
                                activeSiteId = configInputSiteId.trim()
                                activeAgentId = configInputAgentId.trim()
                                activeDomain = configInputDomain.ifBlank { "https://trilivechat.chat/" }.trim()
                                showConfigDialog = false
                            }
                        },
                        enabled = configInputSiteId.isNotBlank() && configInputAgentId.isNotBlank()
                    ) {
                        Text("確定連線")
                    }
                },
                dismissButton = {
                    if (hasValidConfig) {
                        TextButton(onClick = { showConfigDialog = false }) {
                            Text("取消")
                        }
                    }
                }
            )
        }

        // Token Binding Dialog
        if (showBindDialog) {
            AlertDialog(
                onDismissRequest = { showBindDialog = false },
                title = { Text("登入綁定 Token") },
                text = {
                    Column {
                        Text("請輸入玩家 Auth JWT Token 進行身分綁定：", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = bindTokenInput,
                            onValueChange = { bindTokenInput = it },
                            label = { Text("Player Token") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 4
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (bindTokenInput.isNotBlank()) {
                                viewModel.bindToken(bindTokenInput)
                                showBindDialog = false
                            }
                        }
                    ) {
                        Text("進行綁定")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBindDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

@Composable
fun ChatMessageBubble(message: ChatMessageItem) {
    val isUser = message.senderType == "customer"
    val senderName = message.senderName
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            if (!isUser && !senderName.isNullOrBlank()) {
                Text(
                    text = senderName,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
                )
            }

            Surface(
                color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                tonalElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = message.message,
                        fontSize = 15.sp
                    )
                    if (!message.imageUrl.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "📷 [圖片: ${message.imageUrl}]",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
