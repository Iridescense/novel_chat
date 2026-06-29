package com.novelchat.ui.creation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novelchat.data.model.Chapter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterListScreen(
    novelId: Long,
    novelTitle: String,
    readOnly: Boolean = false,
    onBack: () -> Unit,
    onOpenChapter: (Long, Long, String) -> Unit,
    viewModel: CreationViewModel = viewModel()
) {
    LaunchedEffect(novelId) {
        viewModel.loadNovel(novelId)
    }

    val chapters by viewModel.chapters.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var menuChapter by remember { mutableStateOf<Chapter?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(novelTitle, style = MaterialTheme.typography.titleMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (!readOnly) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "新建章节")
                }
            }
        }
    ) { padding ->
        if (chapters.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
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
                    Text("还没有章节", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("点击右下角 + 新建章节",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(chapters, key = { it.id }) { chapter ->
                    ChapterItem(
                        chapter = chapter,
                        onClick = {
                            onOpenChapter(novelId, chapter.id, chapter.title)
                        },
                        onLongClick = if (readOnly) ({}) else ({ menuChapter = chapter })
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        var chapterTitle by remember { mutableStateOf("第${chapters.size + 1}章") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("新建章节") },
            text = {
                OutlinedTextField(
                    value = chapterTitle,
                    onValueChange = { chapterTitle = it },
                    label = { Text("章节名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addChapter(chapterTitle.trim())
                    showAddDialog = false
                }) { Text("创建并进入") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("取消") }
            }
        )
    }

    // 长按章节菜单（只读模式下不显示）
    if (!readOnly) { menuChapter?.let { chapter ->
        var renameText by remember { mutableStateOf(chapter.title) }
        AlertDialog(
            onDismissRequest = { menuChapter = null },
            title = { Text(chapter.title) },
            text = {
                Column {
                    OutlinedTextField(value = renameText, onValueChange = { renameText = it },
                        label = { Text("章节名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = {
                        viewModel.deleteChapter(chapter)
                        menuChapter = null
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp)); Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameChapter(chapter, renameText.trim())
                    menuChapter = null
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { menuChapter = null }) { Text("取消") } }
        )
    }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChapterItem(
    chapter: Chapter,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = chapter.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}
