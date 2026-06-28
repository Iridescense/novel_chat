package com.novelchat.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 节(Segment) — 章节内的片段，每节指定一位主角
 */
@Entity(
    tableName = "segments",
    foreignKeys = [
        ForeignKey(
            entity = Chapter::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Role::class,
            parentColumns = ["id"],
            childColumns = ["protagonistId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("chapterId"), Index("protagonistId")]
)
data class Segment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val chapterId: Long,
    val protagonistId: Long? = null,      // 当前节的主角 Role.id，null 表示纯旁白节
    val title: String = "",               // 分割线文字，默认空则自动生成
    val orderIndex: Int = 0
)
