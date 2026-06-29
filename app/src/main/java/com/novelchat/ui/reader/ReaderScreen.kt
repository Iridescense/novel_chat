package com.novelchat.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novelchat.ui.creation.components.MessageBubble
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    novelId: Long,
    startMessageIndex: Int,
    chapterId: Long? = null,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = viewModel()
) {
    LaunchedEffect(novelId, startMessageIndex) {
        viewModel.loadNovel(novelId, startMessageIndex, chapterId)
    }

    val items by viewModel.items.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val showSettings by viewModel.showSettings.collectAsState()
    val isAutoPlaying by viewModel.isAutoPlaying.collectAsState()
    val autoPlaySpeed by viewModel.autoPlaySpeed.collectAsState()
    val showAll by viewModel.showAllMessages.collectAsState()

    val listState = rememberLazyListState()
    val bgColor = if (isDarkMode) MaterialTheme.colorScheme.background
                  else MaterialTheme.colorScheme.surface

    // 可见消息数量：默认从第1条开始，点击递增
    var visibleCount by remember { mutableStateOf(1) }

    // 切换到 showAll 时显示全部
    LaunchedEffect(showAll) {
        if (showAll) visibleCount = items.size
    }

    // 加载完重新设置
    LaunchedEffect(items.size) {
        if (items.isNotEmpty()) {
            visibleCount = 1.coerceAtLeast(startMessageIndex + 1).coerceAtMost(items.size)
        }
    }

    // 自动滚动到最新
    LaunchedEffect(visibleCount) {
        if (visibleCount > 0 && items.isNotEmpty()) {
            listState.animateScrollToItem((visibleCount - 1).coerceAtLeast(0))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viewModel.novel.collectAsState().value?.title ?: "阅读",
                    style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回") } },
                actions = { IconButton(onClick = { viewModel.toggleSettings() }) {
                    Icon(Icons.Default.Settings, contentDescription = "设置") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.MenuBook, contentDescription = null,
                            Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.height(16.dp))
                        Text("这个剧本还没有内容")
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = onBack) { Text("返回书架") }
                    }
                }
            } else {
                // 点触推进模式：点击增加可见消息数
                val displayCount = if (showAll) items.size else visibleCount

                // 带节分割线的显示列表
                val displayItems = remember(items, displayCount) {
                    val result = mutableListOf<Any>()
                    var lastSegId = -1L
                    items.take(displayCount).forEach { item ->
                        if (item.segmentId != lastSegId) {
                            result.add("divider_${item.segmentId}")
                            lastSegId = item.segmentId
                        }
                        result.add(item)
                    }
                    result
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 56.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    if (!showAll && visibleCount < items.size) {
                                        visibleCount++
                                    }
                                }
                            )
                        },
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(displayItems, key = { _, item ->
                        when (item) {
                            is String -> item
                            is ReaderItem -> "msg_${item.message.id}"
                            else -> "unknown"
                        }
                    }) { _, item ->
                        when (item) {
                            is String -> {
                                // 分割线
                                Box(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp)) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                }
                            }
                            is ReaderItem -> {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = slideInVertically { it }
                                ) {
                                    MessageBubble(
                                        message = item.message,
                                        role = item.role,
                                        isProtagonist = item.isProtagonist,
                                        onDoubleTap = {
                                            if (item.message.hasHiddenNote)
                                                viewModel.toggleHiddenNote(item.message.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 底部进度
                Surface(Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                    color = bgColor.copy(alpha = 0.95f)) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        if (isAutoPlaying) {
                            LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp),
                                color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                        }
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("第${visibleCount}条 / 共${totalCount}条",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline)
                            IconButton(onClick = { viewModel.toggleAutoPlay() },
                                modifier = Modifier.size(28.dp)) {
                                Icon(if (isAutoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isAutoPlaying) "暂停" else "自动播放",
                                    modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { viewModel.hideSettings() },
            title = { Text("阅读设置") },
            text = {
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("全部信息展示")
                        Switch(checked = showAll, onCheckedChange = { viewModel.toggleShowAllMessages() })
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("字号")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("A", style = MaterialTheme.typography.bodySmall)
                        Slider(value = fontSize, onValueChange = { viewModel.setFontSize(it) },
                            valueRange = 12f..28f, modifier = Modifier.weight(1f))
                        Text("A", style = MaterialTheme.typography.headlineSmall)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("夜间模式")
                        Switch(checked = isDarkMode, onCheckedChange = { viewModel.toggleDarkMode() })
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("自动播放")
                        Switch(checked = isAutoPlaying, onCheckedChange = { viewModel.toggleAutoPlay() })
                    }
                    if (isAutoPlaying) {
                        Spacer(Modifier.height(8.dp))
                        Text("速度: ${autoPlaySpeed.toInt()}秒/条")
                        Slider(value = autoPlaySpeed, onValueChange = { viewModel.setAutoPlaySpeed(it) },
                            valueRange = 1f..10f)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.hideSettings() }) { Text("关闭") } }
        )
    }
}
