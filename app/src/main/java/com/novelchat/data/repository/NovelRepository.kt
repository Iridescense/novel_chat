package com.novelchat.data.repository

import com.novelchat.data.db.NovelDao
import com.novelchat.data.model.*
import kotlinx.coroutines.flow.Flow

class NovelRepository(private val dao: NovelDao) {

    // ========== Novel ==========

    fun getAllNovels(): Flow<List<Novel>> = dao.getAllNovels()

    fun searchNovels(query: String): Flow<List<Novel>> = dao.searchNovels(query)

    suspend fun getNovelById(id: Long): Novel? = dao.getNovelById(id)

    suspend fun insertNovel(novel: Novel): Long = dao.insertNovel(novel)

    suspend fun updateNovel(novel: Novel) = dao.updateNovel(novel)

    suspend fun deleteNovel(novel: Novel) = dao.deleteNovel(novel)

    suspend fun deleteNovelById(id: Long) = dao.deleteNovelById(id)

    // ========== Role ==========

    fun getRolesByNovelId(novelId: Long): Flow<List<Role>> = dao.getRolesByNovelId(novelId)

    suspend fun getRoleById(id: Long): Role? = dao.getRoleById(id)

    suspend fun insertRole(role: Role): Long = dao.insertRole(role)

    suspend fun updateRole(role: Role) = dao.updateRole(role)

    suspend fun deleteRole(role: Role) = dao.deleteRole(role)

    suspend fun deleteRoleById(id: Long) = dao.deleteRoleById(id)

    // ========== Chapter ==========

    fun getChaptersByNovelId(novelId: Long): Flow<List<Chapter>> =
        dao.getChaptersByNovelId(novelId)

    suspend fun getChapterById(id: Long): Chapter? = dao.getChapterById(id)

    suspend fun insertChapter(chapter: Chapter): Long = dao.insertChapter(chapter)

    suspend fun updateChapter(chapter: Chapter) = dao.updateChapter(chapter)

    suspend fun deleteChapter(chapter: Chapter) = dao.deleteChapter(chapter)

    // ========== Segment ==========

    fun getSegmentsByChapterId(chapterId: Long): Flow<List<Segment>> =
        dao.getSegmentsByChapterId(chapterId)

    suspend fun getSegmentById(id: Long): Segment? = dao.getSegmentById(id)

    suspend fun insertSegment(segment: Segment): Long = dao.insertSegment(segment)

    suspend fun updateSegment(segment: Segment) = dao.updateSegment(segment)

    suspend fun deleteSegment(segment: Segment) = dao.deleteSegment(segment)

    // ========== Message ==========

    fun getMessagesBySegmentId(segmentId: Long): Flow<List<Message>> =
        dao.getMessagesBySegmentId(segmentId)

    suspend fun getMessageById(id: Long): Message? = dao.getMessageById(id)

    suspend fun insertMessage(message: Message): Long = dao.insertMessage(message)

    suspend fun updateMessage(message: Message) = dao.updateMessage(message)

    suspend fun deleteMessage(message: Message) = dao.deleteMessage(message)

    // ========== 全量导出 ==========

    suspend fun getRolesByNovelIdSync(novelId: Long): List<Role> =
        dao.getRolesByNovelIdSync(novelId)

    suspend fun getChaptersByNovelIdSync(novelId: Long): List<Chapter> =
        dao.getChaptersByNovelIdSync(novelId)

    suspend fun getSegmentsByNovelIdSync(novelId: Long): List<Segment> =
        dao.getSegmentsByNovelIdSync(novelId)

    suspend fun getMessagesByNovelIdSync(novelId: Long): List<Message> =
        dao.getMessagesByNovelIdSync(novelId)

    suspend fun getSegmentsByChapterIdSync(chapterId: Long): List<Segment> =
        dao.getSegmentsByChapterIdSync(chapterId)

    suspend fun getMessagesBySegmentIdSync(segmentId: Long): List<Message> =
        dao.getMessagesBySegmentIdSync(segmentId)

    suspend fun getMessageCountByNovelId(novelId: Long): Int =
        dao.getMessageCountByNovelId(novelId)

    // ========== 全量导入 ==========

    suspend fun importNovel(
        novel: Novel,
        roles: List<Role>,
        chapters: List<Chapter>,
        segments: List<Segment>,
        messages: List<Message>
    ): Long {
        val novelId = dao.insertNovel(novel)
        val roleIdMap = mutableMapOf<Long, Long>()  // oldId -> newId

        val newRoles = roles.map { role ->
            val newId = dao.insertRole(role.copy(id = 0, novelId = novelId))
            roleIdMap[role.id] = newId
            newId
        }

        val chapterIdMap = mutableMapOf<Long, Long>()  // oldId -> newId

        val newChapters = chapters.map { chapter ->
            val newId = dao.insertChapter(chapter.copy(id = 0, novelId = novelId))
            chapterIdMap[chapter.id] = newId
            newId
        }

        val segmentIdMap = mutableMapOf<Long, Long>()

        val newSegments = segments.map { segment ->
            val newProtoId = roleIdMap[segment.protagonistId]
            val newId = dao.insertSegment(
                segment.copy(
                    id = 0,
                    chapterId = chapterIdMap[segment.chapterId] ?: 0,
                    protagonistId = newProtoId
                )
            )
            segmentIdMap[segment.id] = newId
            newId
        }

        messages.forEach { message ->
            dao.insertMessage(
                message.copy(
                    id = 0,
                    segmentId = segmentIdMap[message.segmentId] ?: 0,
                    roleId = roleIdMap[message.roleId]
                )
            )
        }

        return novelId
    }
}
