package com.novelchat.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 消息
 *
 * type: "dialogue" | "narrator"
 *   - dialogue: 角色对话消息，roleId 指向说话者
 *   - narrator: 旁白消息，居中显示，roleId 为 null
 *
 * richTextJson: 富文本格式数据 (JSON)，未来可选
 * hasHiddenNote: 是否包含隐藏附注（小圆点标记）
 * hiddenNote: 隐藏附注内容，长按可见
 */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = Segment::class,
            parentColumns = ["id"],
            childColumns = ["segmentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Role::class,
            parentColumns = ["id"],
            childColumns = ["roleId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("segmentId"), Index("roleId")]
)
data class Message(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val segmentId: Long,
    val type: String = "dialogue",        // "dialogue" | "narrator"
    val roleId: Long? = null,             // narrator 时为 null
    val text: String,
    val richTextJson: String? = null,     // 富文本格式数据
    val hasHiddenNote: Boolean = false,
    val hiddenNote: String? = null,
    val orderIndex: Int = 0
) {
    companion object {
        const val TYPE_DIALOGUE = "dialogue"
        const val TYPE_NARRATOR = "narrator"
    }
}
