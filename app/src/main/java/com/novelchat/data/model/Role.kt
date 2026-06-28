package com.novelchat.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 角色
 */
@Entity(
    tableName = "roles",
    foreignKeys = [
        ForeignKey(
            entity = Novel::class,
            parentColumns = ["id"],
            childColumns = ["novelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("novelId")]
)
data class Role(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val novelId: Long,
    val name: String,
    val color: String = "#CCCCCC",        // 头像/标签颜色
    val avatarType: String = "text",       // "text" | "builtin" | "gallery"
    val avatarValue: String = "",          // 文字首字母 / 预设头像名 / 图片路径
    val orderIndex: Int = 0
) {
    companion object {
        const val AVATAR_TEXT = "text"
        const val AVATAR_BUILTIN = "builtin"
        const val AVATAR_GALLERY = "gallery"
    }
}
