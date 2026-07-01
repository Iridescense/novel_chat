package com.novelchat.ui.settings

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.novelchat.util.UpdateChecker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 当前版本号
    val currentVersion = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (_: Exception) { "1.0.0" }
    }

    // 更新相关状态
    var isChecking by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<com.novelchat.util.UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // 检查更新
    fun checkForUpdates() {
        isChecking = true
        scope.launch {
            val info = UpdateChecker.checkUpdate(currentVersion)
            isChecking = false
            updateInfo = info
            showUpdateDialog = info.hasUpdate
            if (!info.hasUpdate) {
                snackbarHostState.showSnackbar("已是最新版本 v$currentVersion")
            }
        }
    }

    // 下载并安装
    fun downloadAndInstall() {
        val url = updateInfo?.downloadUrl ?: return
        isDownloading = true
        scope.launch {
            val file = UpdateChecker.downloadApk(context, url)
            isDownloading = false
            if (file != null) {
                UpdateChecker.installApk(context, file)
            } else {
                snackbarHostState.showSnackbar("下载失败，请稍后重试")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // 主题（预留入口）
            SettingsItem(
                icon = Icons.Default.Palette,
                title = "主题",
                subtitle = "当前主题：默认 #FAFAD2 色系",
                enabled = false
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // 版本信息与更新
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "当前版本",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "v$currentVersion",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                if (isChecking) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    TextButton(onClick = { checkForUpdates() }) {
                        Text("检查更新")
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "对话式小说创作工具",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                text = "Made with ❤️",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }

    // 更新对话框
    if (showUpdateDialog && updateInfo != null) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("发现新版本 v${updateInfo!!.latestVersion}") },
            text = {
                Column {
                    Text(
                        text = updateInfo!!.releaseNotes.ifBlank { "暂无更新说明" },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (isDownloading) {
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("正在下载…", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { downloadAndInstall() },
                    enabled = !isDownloading
                ) { Text("下载并安装") }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) { Text("稍后再说") }
            }
        )
    }
}
