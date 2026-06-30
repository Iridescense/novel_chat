package com.novelchat.ui.creation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novelchat.data.model.Novel
import com.novelchat.ui.bookshelf.BookshelfViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreationListScreen(
    onOpenEditor: (novelId: Long) -> Unit,
    viewModel: BookshelfViewModel = viewModel()
) {
    val novels by viewModel.allNovels.collectAsState()
    val showNewDialog by viewModel.showNewNovelDialog.collectAsState()
    var menuNovel by remember { mutableStateOf<Novel?>(null) }
    val scope = rememberCoroutineScope()
    var bookshelfStatus by remember { mutableStateOf<Map<Long, Boolean>>(emptyMap()) }

    // 检查剧本是否已在书架中
    fun checkBookshelfStatus(novelId: Long) {
        scope.launch {
            bookshelfStatus = bookshelfStatus + (novelId to viewModel.hasBookshelfCopy(novelId))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("创作台") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showNewNovelDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "新建剧本")
            }
        }
    ) { padding ->
        if (novels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Create,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "还没有剧本",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "点击右下角 + 新建剧本，再点选进入编辑",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(novels, key = { it.id }) { novel ->
                    NovelListItem(
                        novel = novel,
                        onClick = { onOpenEditor(novel.id) },
                        onLongClick = { menuNovel = novel }
                    )
                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }

    // 新建剧本对话框
    if (showNewDialog) {
        NovelCreateDialog(
            onConfirm = { title, description ->
                viewModel.createNovel(title, description)
                viewModel.hideNewNovelDialog()
            },
            onDismiss = { viewModel.hideNewNovelDialog() }
        )
    }

    // 长按菜单对话框
    menuNovel?.let { novel ->
        var renameText by remember { mutableStateOf(novel.title) }
        var descText by remember { mutableStateOf(novel.description) }

        AlertDialog(
            onDismissRequest = { menuNovel = null },
            title = { Text(novel.title) },
            text = {
                Column {
                    OutlinedTextField(value = renameText, onValueChange = { renameText = it },
                        label = { Text("剧本名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = descText, onValueChange = { descText = it },
                        label = { Text("简介") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                                val isInBookshelf = bookshelfStatus[novel.id] ?: false
                    // 检查状态（第一次打开时触发检查）
                    LaunchedEffect(novel.id) { checkBookshelfStatus(novel.id) }

                    TextButton(
                        onClick = {
                            viewModel.addToBookshelf(novel.id)
                            bookshelfStatus = bookshelfStatus + (novel.id to true)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isInBookshelf
                    ) {
                        Icon(
                            if (isInBookshelf) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            tint = if (isInBookshelf) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isInBookshelf) "已在书架中" else "添加至书架",
                            color = if (isInBookshelf) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    TextButton(onClick = {
                        viewModel.deleteNovel(novel)
                        menuNovel = null
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp)); Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateNovel(novel.id, renameText.trim(), descText.trim(), novel.coverColor)
                    menuNovel = null
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { menuNovel = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun NovelCreateDialog(
    onConfirm: (title: String, desc: String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建剧本") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("简介") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (title.isNotBlank()) onConfirm(title.trim(), desc.trim()) },
                enabled = title.isNotBlank()
            ) { Text("确认") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun NovelListItem(
    novel: Novel,
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
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 状态标签
            val statusColor = if (novel.status == Novel.STATUS_COMPLETED) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            }
            val statusText = if (novel.status == Novel.STATUS_COMPLETED) "完成" else "草稿"

            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = statusColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 剧本名
            Text(
                text = novel.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 箭头
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}
