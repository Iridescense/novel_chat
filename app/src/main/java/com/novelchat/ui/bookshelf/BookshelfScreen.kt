package com.novelchat.ui.bookshelf

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.novelchat.NovelChatApp
import com.novelchat.data.model.Novel
import com.novelchat.util.AppModule
import com.novelchat.util.JsonExporter
import java.io.File
import android.os.Environment
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BookshelfScreen(
    onOpenReader: (Long) -> Unit,
    onOpenCreation: (Long) -> Unit,
    viewModel: BookshelfViewModel = viewModel()
) {
    val novels by viewModel.novels.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showNewDialog by viewModel.showNewNovelDialog.collectAsState()
    val editingNovel by viewModel.editingNovel.collectAsState()

    // 长按菜单状态
    var menuNovel by remember { mutableStateOf<Novel?>(null) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 导出文件选择器
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            menuNovel?.let { novel ->
                scope.launch {
                    viewModel.exportNovelToUri(novel, it)
                }
            }
        }
    }

    // 导入文件选择器
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val app = context.applicationContext as NovelChatApp
                val repo = AppModule.getRepository(app)
                try {
                    JsonExporter.importNovelFromUri(context, repo, it)
                } catch (_: Exception) {
                    // 导入失败忽略
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("书架") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { importLauncher.launch(arrayOf("*/*")) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = "导入剧本")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // 搜索栏
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("搜索剧本标题…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "清空")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            if (novels.isEmpty()) {
                // 空状态
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
                        Text(
                            "还没有剧本",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "点击右下角 + 开始创作",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                // 3:4 封面网格
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(novels, key = { it.id }) { novel ->
                        NovelCoverCard(
                            novel = novel,
                            onClick = { onOpenReader(novel.id) },
                            onLongClick = { menuNovel = novel }
                        )
                    }
                }
            }
        }
    }

    // 新建剧本对话框
    if (showNewDialog) {
        NovelEditDialog(
            title = "新建剧本",
            initialTitle = "",
            initialDescription = "",
            initialCoverColor = "#FFF8DC",
            onConfirm = { t, d, c -> viewModel.createNovel(t, d) },
            onDismiss = { viewModel.hideNewNovelDialog() }
        )
    }

    // 编辑剧本对话框
    editingNovel?.let { novel ->
        NovelEditDialog(
            title = "编辑剧本",
            initialTitle = novel.title,
            initialDescription = novel.description,
            initialCoverColor = novel.coverColor,
            onConfirm = { t, d, c ->
                viewModel.updateNovel(novel.id, t, d, c)
                viewModel.hideEditNovelDialog()
            },
            onDismiss = { viewModel.hideEditNovelDialog() }
        )
    }

    // 长按菜单
    menuNovel?.let { novel ->
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        AlertDialog(
            onDismissRequest = { menuNovel = null },
            title = { Text(novel.title) },
            text = {
                Column {
                    TextButton(onClick = {
                        menuNovel = null
                        onOpenCreation(novel.id)
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(Modifier.width(8.dp)); Text("编辑到创作台")
                    }
                    TextButton(onClick = {
                        exportLauncher.launch("${novel.title}.json")
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(Modifier.width(8.dp)); Text("导出")
                    }
                    TextButton(onClick = {
                        viewModel.toggleBookshelf(novel, false)
                        menuNovel = null
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Unpublished, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp)); Text("从书架移除", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { menuNovel = null }) {
                    Text("关闭")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NovelCoverCard(
    novel: Novel,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val coverColor = try {
        Color(android.graphics.Color.parseColor(novel.coverColor))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primaryContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = coverColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(12.dp)
            ) {
                Icon(
                    Icons.Default.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = novel.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (novel.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = novel.description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NovelEditDialog(
    title: String,
    initialTitle: String,
    initialDescription: String,
    initialCoverColor: String,
    onConfirm: (title: String, desc: String, coverColor: String) -> Unit,
    onDismiss: () -> Unit
) {
    var novelTitle by remember { mutableStateOf(initialTitle) }
    var novelDesc by remember { mutableStateOf(initialDescription) }
    var coverColor by remember { mutableStateOf(initialCoverColor) }

    // 预设颜色选项
    val colorOptions = listOf(
        "#FFF8DC", "#FFE4E1", "#E8F5E9", "#E3F2FD",
        "#FFF3E0", "#F3E5F5", "#E0F7FA", "#FBE9E7"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = novelTitle,
                    onValueChange = { novelTitle = it },
                    label = { Text("标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = novelDesc,
                    onValueChange = { novelDesc = it },
                    label = { Text("简介") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("封面颜色", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    colorOptions.forEach { colorHex ->
                        val isSelected = coverColor == colorHex
                        val color = Color(android.graphics.Color.parseColor(colorHex))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .combinedClickable(
                                    onClick = { coverColor = colorHex },
                                    onLongClick = {}
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                modifier = Modifier.size(32.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = color,
                                border = if (isSelected) {
                                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                } else null
                            ) {}
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (novelTitle.isNotBlank()) {
                        onConfirm(novelTitle.trim(), novelDesc.trim(), coverColor)
                    }
                },
                enabled = novelTitle.isNotBlank()
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
