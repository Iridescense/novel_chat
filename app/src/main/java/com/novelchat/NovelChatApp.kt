package com.novelchat

import android.app.Application
import com.novelchat.data.db.NovelDatabase

class NovelChatApp : Application() {

    lateinit var database: NovelDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = NovelDatabase.getInstance(this)
    }

    companion object {
        lateinit var instance: NovelChatApp
            private set
    }
}
