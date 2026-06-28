package com.novelchat.util

import com.novelchat.NovelChatApp
import com.novelchat.data.db.NovelDatabase
import com.novelchat.data.repository.NovelRepository

/**
 * 简易依赖容器（替代 DI 框架）
 */
object AppModule {
    private var _repository: NovelRepository? = null

    fun getRepository(app: NovelChatApp): NovelRepository {
        if (_repository == null) {
            _repository = NovelRepository(app.database.novelDao())
        }
        return _repository!!
    }
}
