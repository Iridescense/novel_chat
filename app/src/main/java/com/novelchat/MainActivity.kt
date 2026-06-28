package com.novelchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.novelchat.ui.navigation.NavGraph
import com.novelchat.ui.theme.NovelChatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 检查是否有导入的 JSON 文件
        val importPath = intent?.data?.path
        setContent {
            NovelChatTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NavGraph(importPath = importPath)
                }
            }
        }
    }
}
