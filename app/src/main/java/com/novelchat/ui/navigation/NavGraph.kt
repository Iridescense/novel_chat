package com.novelchat.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import com.novelchat.ui.bookshelf.BookshelfScreen
import com.novelchat.ui.creation.CreationEditorScreen
import com.novelchat.ui.creation.CreationListScreen
import com.novelchat.ui.reader.ReaderScreen
import com.novelchat.ui.settings.SettingsScreen

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

val bottomNavItems = listOf(
    BottomNavItem("书架", Icons.Default.Book, Routes.BOOKSHELF),
    BottomNavItem("创作台", Icons.Default.Edit, Routes.CREATION_LIST),
    BottomNavItem("设置", Icons.Default.Settings, Routes.SETTINGS)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph(importPath: String? = null) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // 判断是否显示底部导航（书架/创作台/设置 显示，阅读器/编辑器 隐藏）
    val showBottomBar = currentDestination?.route in listOf(
        Routes.BOOKSHELF,
        Routes.CREATION_LIST,
        Routes.SETTINGS
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.BOOKSHELF,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 书架
            composable(Routes.BOOKSHELF) {
                BookshelfScreen(
                    onOpenReader = { novelId ->
                        navController.navigate(Routes.reader(novelId))
                    },
                    onOpenCreation = { novelId ->
                        navController.navigate(Routes.creationEditor(novelId))
                    }
                )
            }

            // 创作台 — 列表
            composable(Routes.CREATION_LIST) {
                CreationListScreen(
                    onOpenEditor = { novelId ->
                        navController.navigate(Routes.creationEditor(novelId))
                    }
                )
            }

            // 创作台 — 编辑器
            composable(
                route = "creation_editor/{novelId}",
                arguments = listOf(navArgument("novelId") { type = NavType.LongType })
            ) { backStackEntry ->
                val novelId = backStackEntry.arguments?.getLong("novelId") ?: 0L
                CreationEditorScreen(
                    novelId = novelId,
                    onBack = { navController.popBackStack() },
                    onPreview = { id, startIndex ->
                        navController.navigate(Routes.reader(id, startIndex))
                    }
                )
            }

            // 沉浸式阅读器
            composable(
                route = "reader/{novelId}/{startMessageIndex}",
                arguments = listOf(
                    navArgument("novelId") { type = NavType.LongType },
                    navArgument("startMessageIndex") {
                        type = NavType.IntType
                        defaultValue = 0
                    }
                )
            ) { backStackEntry ->
                val novelId = backStackEntry.arguments?.getLong("novelId") ?: 0L
                val startIndex = backStackEntry.arguments?.getInt("startMessageIndex") ?: 0
                ReaderScreen(
                    novelId = novelId,
                    startMessageIndex = startIndex,
                    onBack = { navController.popBackStack() }
                )
            }

            // 设置
            composable(Routes.SETTINGS) {
                SettingsScreen()
            }
        }
    }
}

@Composable
fun PlaceholderScreen(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.headlineMedium)
    }
}
