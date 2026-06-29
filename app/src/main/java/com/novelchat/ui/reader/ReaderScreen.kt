package com.novelchat.ui.reader

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novelchat.ui.creation.components.MessageBubble
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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

    val items by viewModel.items.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val showSettings by viewModel.showSettings.collectAsState()
    val isAutoPlaying by viewModel.isAutoPlaying.collectAsState()
    val autoPlaySpeed by viewModel.autoPlaySpeed.collectAsState()

    val listState = rememberLazyListState()
    val bgColor = if (isDarkMode) {
        MaterialTheme.colorScheme.background
    } else {
        MaterialTheme.colorScheme.surface
    }

    LaunchedEffect(currentIndex) {
        if (currentIndex > 0 && items.isNotEmpty()) {
            listState.animateScrollToItem(currentIndex.coerceAtMost(items.size - 1))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        viewModel.novel.collectAsState().value?.title ?: "阅读",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleSettings() }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("这个剧本还没有内容", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = onBack) { Text("返回书架") }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 56.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(items, key = { index, _ -> index }) { index, item ->
                        MessageBubble(
                            message = item.message,
                            role = item.role,
                            isProtagonist = item.isProtagonist,
                            onDoubleTap = {
                                if (item.message.hasHiddenNote) {
                                    viewModel.toggleHiddenNote(item.message.id)
                                }
                            }
                        )
                    }
                }
            }

            if (items.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    color = bgColor.copy(alpha = 0.95f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        if (isAutoPlaying) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(2.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isAutoPlaying) {
                                    IconButton(onClick = { viewModel.toggleAutoPlay() },
                                        modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Pause, contentDescription = "暂停",
                                            modifier = Modifier.size(18.dp))
                                    }
                                } else {
                                    IconButton(onClick = { viewModel.toggleAutoPlay() },
                                        modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "自动播放",
                                            modifier = Modifier.size(18.dp))
                                    }
                                }
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
                    Text("字号", style = MaterialTheme.typography.labelLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("A", style = MaterialTheme.typography.bodySmall)
                        Slider(value = fontSize, onValueChange = { viewModel.setFontSize(it) },
                            valueRange = 12f..28f, modifier = Modifier.weight(1f))
                        Text("A", style = MaterialTheme.typography.headlineSmall)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("夜间模式")
                        Switch(checked = isDarkMode,
                            onCheckedChange = { viewModel.toggleDarkMode() })
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("自动播放")
                        Switch(checked = isAutoPlaying,
                            onCheckedChange = { viewModel.toggleAutoPlay() })
                    }
                    if (isAutoPlaying) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("速度: ${autoPlaySpeed.toInt()}秒/条",
                            style = MaterialTheme.typography.bodySmall)
                        Slider(value = autoPlaySpeed,
                            onValueChange = { viewModel.setAutoPlaySpeed(it) },
                            valueRange = 1f..10f)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.hideSettings() }) { Text("关闭") }
            }
        )
    }
}
