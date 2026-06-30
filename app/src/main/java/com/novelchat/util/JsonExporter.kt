package com.novelchat.util

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.novelchat.data.model.*
import com.novelchat.data.repository.NovelRepository

/**
 * JSON 导出/导入结构
 */
data class NovelExport(
    val version: Int = 1,
    val novel: Novel,
    val roles: List<Role>,
    val chapters: List<Chapter>,
    val segments: List<Segment>,
    val messages: List<Message>
)

object JsonExporter {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * 导出单个剧本为 JSON 字符串
     */
    suspend fun exportNovelToString(
        repository: NovelRepository,
        novelId: Long
    ): String {
        val novel = repository.getNovelById(novelId)
            ?: throw IllegalArgumentException("剧本不存在: $novelId")

        val roles = repository.getRolesByNovelIdSync(novelId)
        val chapters = repository.getChaptersByNovelIdSync(novelId)
        val segments = repository.getSegmentsByNovelIdSync(novelId)
        val messages = repository.getMessagesByNovelIdSync(novelId)

        val export = NovelExport(
            novel = novel,
            roles = roles,
            chapters = chapters,
            segments = segments,
            messages = messages
        )

        return gson.toJson(export)
    }

    /**
     * 导出到文件
     */
    suspend fun exportNovelToFile(
        context: Context,
        repository: NovelRepository,
        novelId: Long,
        uri: Uri
    ) {
        val json = exportNovelToString(repository, novelId)
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(json.toByteArray(Charsets.UTF_8))
        }
    }

    /**
     * 从 JSON 字符串导入剧本
     */
    suspend fun importNovelFromString(
        repository: NovelRepository,
        json: String
    ): Long {
        val export = gson.fromJson(json, NovelExport::class.java)
            ?: throw IllegalArgumentException("JSON 格式错误")

        // 导入的剧本总是作为书架书籍（非创作台副本）
        val importedNovel = export.novel.copy(
            id = 0,
            isInBookshelf = true,
            sourceNovelId = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return repository.importNovel(
            novel = importedNovel,
            roles = export.roles,              // ID 重置由 importNovel 内部处理，
            chapters = export.chapters,        // 外部保留原始 ID 以便正确建立映射
            segments = export.segments,
            messages = export.messages
        )
    }

    /**
     * 从 URI 导入
     */
    suspend fun importNovelFromUri(
        context: Context,
        repository: NovelRepository,
        uri: Uri
    ): Long {
        val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader().readText()
        } ?: throw IllegalArgumentException("无法读取文件")

        return importNovelFromString(repository, json)
    }
}
