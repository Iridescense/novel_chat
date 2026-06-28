package com.novelchat.ui.creation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novelchat.data.model.Novel
import com.novelchat.ui.bookshelf.BookshelfViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreationListScreen(
    onOpenEditor: (Long) -> Unit,
    viewModel: BookshelfViewModel = viewModel()
) {
    val novels by viewModel.novels.collectAsState()
    val showNewDialog by viewModel.showNewNovelDialog.collectAsState()

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
                        Icons.Default.EditNote,
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
                        onClick = { onOpenEditor(novel.id) }
                    )
                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }

    // 新建对话框复用书架的逻辑
    if (showNewDialog) {
        // 对话框的 UI 在 BookshelfScreen 中已定义，
        // 此处用简单方式触发
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NovelListItem(
    novel: Novel,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
