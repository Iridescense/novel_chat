package com.novelchat.ui.reader

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novelchat.data.model.Message
import com.novelchat.ui.creation.components.RoleAvatar
import com.novelchat.ui.theme.HiddenNoteDot
import com.novelchat.ui.theme.NarratorBg
import com.novelchat.ui.theme.NarratorText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    novelId: Long,
    startMessageIndex: Int,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()

    LaunchedEffect(novelId, startMessageIndex) {
        viewModel.loadNovel(novelId, startMessageIndex)
    }

    val novel by viewModel.novel.collectAsState()
    val items by viewModel.items.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val currentItem by viewModel.currentItem.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val showSettings by viewModel.showSettings.collectAsState()
    val isAutoPlaying by viewModel.isAutoPlaying.collectAsState()
    val autoPlaySpeed by viewModel.autoPlaySpeed.collectAsState()
    val visibleHiddenNoteId by viewModel.visibleHiddenNoteId.collectAsState()

    val bgColor = if (isDarkMode) {
        MaterialTheme.colorScheme.background
    } else {
        MaterialTheme.colorScheme.surface
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            // 如果设置面板开着，点击关掉
                            if (showSettings) {
                                viewModel.hideSettings()
                            } else if (currentIndex < items.size - 1) {
                                viewModel.advance()
                            }
                        },
                        onDoubleTap = {
                            viewModel.toggleSettings()
                        },
                        onLongPress = {
                            // 长按空白处也可以进出设置
                        }
                    )
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            // 主要内容区
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (items.isEmpty()) {
                    CircularProgressIndicator()
                } else if (currentIndex >= items.size) {
                    // 阅读完毕
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "阅读完毕",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = onBack) {
                            Text("返回书架")
                        }
                    }
                } else {
                    currentItem?.let { item ->
                        val message = item.message
                        val role = item.role
                        val isPro = item.isProtagonist
                        val isNarrator = message.type == Message.TYPE_NARRATOR

                        // 入场动画
                        AnimatedContent(
                            targetState = currentIndex,
                            transitionSpec = {
                                slideInVertically { it } + fadeIn() togetherWith
                                        slideOutVertically { -it } + fadeOut()
                            },
                            label = "message_transition"
                        ) { _ ->
                            if (isNarrator) {
                                // 旁白居中
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = NarratorBg,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 32.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(
                                            text = message.text,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontSize = fontSize.sp
                                            ),
                                            color = NarratorText,
                                            textAlign = TextAlign.Center
                                        )
                                        HiddenNoteInReader(
                                            message = message,
                                            isVisible = visibleHiddenNoteId == message.id,
                                            onToggle = { viewModel.toggleHiddenNote(message.id) }
                                        )
                                    }
                                }
                            } else {
                                // 对话气泡
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isPro) Arrangement.End
                                    else Arrangement.Start,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    if (!isPro) {
                                        RoleAvatar(role, Modifier.size(40.dp))
                                        Spacer(Modifier.width(8.dp))
                                    }

                                    Column(
                                        horizontalAlignment = if (isPro) Alignment.End
                                        else Alignment.Start,
                                        modifier = Modifier.widthIn(max = 280.dp)
                                    ) {
                                        if (!isPro && role != null) {
                                            Text(
                                                role.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(
                                                topStart = if (isPro) 16.dp else 4.dp,
                                                topEnd = if (isPro) 4.dp else 16.dp,
                                                bottomStart = 16.dp,
                                                bottomEnd = 16.dp
                                            ),
                                            color = if (isPro) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.combinedClickable(
                                                onClick = {},
                                                onLongClick = {
                                                    if (message.hasHiddenNote) {
                                                        viewModel.toggleHiddenNote(message.id)
                                                    }
                                                }
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                                Text(
                                                    text = message.text,
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        fontSize = fontSize.sp
                                                    ),
                                                    color = if (isPro) MaterialTheme.colorScheme.onPrimary
                                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                HiddenNoteInReader(
                                                    message = message,
                                                    isVisible = visibleHiddenNoteId == message.id,
                                                    onToggle = { viewModel.toggleHiddenNote(message.id) }
                                                )
                                            }
                                        }
                                    }

                                    if (isPro) {
                                        Spacer(Modifier.width(8.dp))
                                        RoleAvatar(role, Modifier.size(40.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 底部进度条
            if (items.isNotEmpty() && currentIndex < items.size) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = bgColor.copy(alpha = 0.8f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        // 进度条
                        LinearProgressIndicator(
                            progress = { (currentIndex + 1).toFloat() / totalCount.coerceAtLeast(1) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "第${currentIndex + 1}条 / 共${totalCount}条",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )

                            if (currentItem != null && currentItem!!.chapterTitle.isNotBlank()) {
                                Text(
                                    currentItem!!.chapterTitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }

        // 顶部返回按钮
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .statusBarsPadding()
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        // 设置面板（覆盖层）
        AnimatedVisibility(
            visible = showSettings,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            ReaderSettingsPanel(
                fontSize = fontSize,
                isDarkMode = isDarkMode,
                isAutoPlaying = isAutoPlaying,
                autoPlaySpeed = autoPlaySpeed,
                onFontSizeChange = { viewModel.setFontSize(it) },
                onToggleDarkMode = { viewModel.toggleDarkMode() },
                onToggleAutoPlay = { viewModel.toggleAutoPlay() },
                onSpeedChange = { viewModel.setAutoPlaySpeed(it) },
                onClose = { viewModel.hideSettings() }
            )
        }
    }
}

@Composable
private fun HiddenNoteInReader(
    message: Message,
    isVisible: Boolean,
    onToggle: () -> Unit
) {
    if (message.hasHiddenNote) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (!isVisible) {
                Text(
                    text = "●",
                    color = HiddenNoteDot,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            AnimatedVisibility(visible = isVisible) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Text(
                        text = message.hiddenNote ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderSettingsPanel(
    fontSize: Float,
    isDarkMode: Boolean,
    isAutoPlaying: Boolean,
    autoPlaySpeed: Float,
    onFontSizeChange: (Float) -> Unit,
    onToggleDarkMode: () -> Unit,
    onToggleAutoPlay: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("设置", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 字号
            Text("字号", style = MaterialTheme.typography.labelLarge)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("A", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = fontSize,
                    onValueChange = onFontSizeChange,
                    valueRange = 12f..28f,
                    modifier = Modifier.weight(1f)
                )
                Text("A", style = MaterialTheme.typography.headlineSmall)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // 夜间模式
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("夜间模式")
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { onToggleDarkMode() }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // 自动播放
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("自动播放")
                Switch(
                    checked = isAutoPlaying,
                    onCheckedChange = { onToggleAutoPlay() }
                )
            }

            if (isAutoPlaying) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("慢", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = autoPlaySpeed,
                        onValueChange = onSpeedChange,
                        valueRange = 1f..10f,
                        modifier = Modifier.weight(1f)
                    )
                    Text("快", style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "当前间隔：${autoPlaySpeed.toInt()} 秒",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
