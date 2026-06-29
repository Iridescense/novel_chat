package com.novelchat.ui.navigation

/**
 * 导航路由常量
 */
object Routes {
    const val BOOKSHELF = "bookshelf"
    const val CREATION_LIST = "creation_list"
    const val CHAPTER_LIST = "chapter_list/{novelId}"
    const val CREATION_EDITOR = "creation_editor/{novelId}/{chapterId}"
    const val READER = "reader/{novelId}/{startMessageIndex}"
    const val SETTINGS = "settings"

    fun chapterList(novelId: Long) = "chapter_list/$novelId"
    fun creationEditor(novelId: Long, chapterId: Long) = "creation_editor/$novelId/$chapterId"
    fun reader(novelId: Long, startMessageIndex: Int = 0) = "reader/$novelId/$startMessageIndex"
}
