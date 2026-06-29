package com.novelchat.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 剧本
 */
@Entity(tableName = "novels")
data class Novel(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val coverColor: String = "#FFF8DC",
    val coverImagePath: String? = null,
    val status: String = "draft",       // "draft" | "completed"
    val isInBookshelf: Boolean = false,  // 是否添加到书架
    val sourceNovelId: Long? = null,     // 书架副本指向的创作台原件 ID，原件为 null
    val type: String = "normal",         // "normal" | "interactive" (预留)
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_DRAFT = "draft"
        const val STATUS_COMPLETED = "completed"
        const val TYPE_NORMAL = "normal"
        const val TYPE_INTERACTIVE = "interactive"
    }
}
