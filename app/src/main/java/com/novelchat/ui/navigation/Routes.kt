package com.novelchat.ui.navigation

/**
 * 导航路由常量
 */
object Routes {
    const val BOOKSHELF = "bookshelf"
    const val CREATION_LIST = "creation_list/{novelId}"
    const val CREATION_EDITOR = "creation_editor/{novelId}"
    const val READER = "reader/{novelId}/{startMessageIndex}"
    const val SETTINGS = "settings"

    fun creationList(novelId: Long) = "creation_list/$novelId"
    fun creationEditor(novelId: Long) = "creation_editor/$novelId"
    fun reader(novelId: Long, startMessageIndex: Int = 0) = "reader/$novelId/$startMessageIndex"
}
