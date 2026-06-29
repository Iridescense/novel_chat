package com.novelchat.data.repository

import com.novelchat.data.db.NovelDao
import com.novelchat.data.model.*
import kotlinx.coroutines.flow.Flow

class NovelRepository(private val dao: NovelDao) {

    // ========== Novel ==========

    fun getAllNovels(): Flow<List<Novel>> = dao.getAllNovels()

    fun searchNovels(query: String): Flow<List<Novel>> = dao.searchNovels(query)

    fun getBookshelfNovels(): Flow<List<Novel>> = dao.getBookshelfNovels()

    fun getOriginalNovels(): Flow<List<Novel>> = dao.getOriginalNovels()

    suspend fun getNovelById(id: Long): Novel? = dao.getNovelById(id)

    suspend fun insertNovel(novel: Novel): Long = dao.insertNovel(novel)

    suspend fun updateNovel(novel: Novel) = dao.updateNovel(novel)

    suspend fun deleteNovel(novel: Novel) = dao.deleteNovel(novel)

    suspend fun deleteNovelById(id: Long) = dao.deleteNovelById(id)

    suspend fun hasBookshelfCopy(sourceNovelId: Long): Boolean =
        dao.getBookshelfCopyBySourceId(sourceNovelId) != null

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

    /**
     * 将创作台原件深拷贝到书架（副本标记 isInBookshelf=true）
     */
    suspend fun deepCopyNovelToBookshelf(sourceNovelId: Long): Long {
        val original = dao.getNovelById(sourceNovelId) ?: return -1L
        val roles = dao.getRolesByNovelIdSync(sourceNovelId)
        val chapters = dao.getChaptersByNovelIdSync(sourceNovelId)
        val segments = dao.getSegmentsByNovelIdSync(sourceNovelId)
        val messages = dao.getMessagesByNovelIdSync(sourceNovelId)

        // 副本使用全新的 ID，标记为书架副本
        val copyNovel = original.copy(
            id = 0,
            isInBookshelf = true,
            sourceNovelId = sourceNovelId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        return importNovel(copyNovel, roles, chapters, segments, messages)
    }

    /**
     * 更新书架副本：删除旧副本，重新深拷贝
     */
    suspend fun updateBookshelfCopy(sourceNovelId: Long): Long {
        val existing = dao.getBookshelfCopyBySourceId(sourceNovelId)
        if (existing != null) {
            dao.deleteNovelById(existing.id)
        }
        return deepCopyNovelToBookshelf(sourceNovelId)
    }
}
