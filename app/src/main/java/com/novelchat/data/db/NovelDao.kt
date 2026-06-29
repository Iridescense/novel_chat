package com.novelchat.data.db

import androidx.room.*
import com.novelchat.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NovelDao {

    // ========== Novel ==========

    @Query("SELECT * FROM novels ORDER BY createdAt DESC")
    fun getAllNovels(): Flow<List<Novel>>

    @Query("SELECT * FROM novels WHERE isInBookshelf = 1 ORDER BY createdAt DESC")
    fun getBookshelfNovels(): Flow<List<Novel>>

    @Query("SELECT * FROM novels WHERE id = :id")
    suspend fun getNovelById(id: Long): Novel?

    @Query("SELECT * FROM novels WHERE isInBookshelf = 1 AND title LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchNovels(query: String): Flow<List<Novel>>

    @Query("SELECT * FROM novels WHERE sourceNovelId IS NULL ORDER BY createdAt DESC")
    fun getOriginalNovels(): Flow<List<Novel>>

    @Query("SELECT * FROM novels WHERE sourceNovelId = :sourceNovelId LIMIT 1")
    suspend fun getBookshelfCopyBySourceId(sourceNovelId: Long): Novel?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNovel(novel: Novel): Long

    @Update
    suspend fun updateNovel(novel: Novel)

    @Delete
    suspend fun deleteNovel(novel: Novel)

    @Query("DELETE FROM novels WHERE id = :id")
    suspend fun deleteNovelById(id: Long)

    // ========== Role ==========

    @Query("SELECT * FROM roles WHERE novelId = :novelId ORDER BY orderIndex ASC")
    fun getRolesByNovelId(novelId: Long): Flow<List<Role>>

    @Query("SELECT * FROM roles WHERE id = :id")
    suspend fun getRoleById(id: Long): Role?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRole(role: Role): Long

    @Update
    suspend fun updateRole(role: Role)

    @Delete
    suspend fun deleteRole(role: Role)

    @Query("DELETE FROM roles WHERE id = :id")
    suspend fun deleteRoleById(id: Long)

    // ========== Chapter ==========

    @Query("SELECT * FROM chapters WHERE novelId = :novelId ORDER BY orderIndex ASC")
    fun getChaptersByNovelId(novelId: Long): Flow<List<Chapter>>

    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getChapterById(id: Long): Chapter?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: Chapter): Long

    @Update
    suspend fun updateChapter(chapter: Chapter)

    @Delete
    suspend fun deleteChapter(chapter: Chapter)

    @Query("DELETE FROM chapters WHERE id = :id")
    suspend fun deleteChapterById(id: Long)

    // ========== Segment ==========

    @Query("SELECT * FROM segments WHERE chapterId = :chapterId ORDER BY orderIndex ASC")
    fun getSegmentsByChapterId(chapterId: Long): Flow<List<Segment>>

    @Query("SELECT * FROM segments WHERE id = :id")
    suspend fun getSegmentById(id: Long): Segment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegment(segment: Segment): Long

    @Update
    suspend fun updateSegment(segment: Segment)

    @Delete
    suspend fun deleteSegment(segment: Segment)

    @Query("DELETE FROM segments WHERE id = :id")
    suspend fun deleteSegmentById(id: Long)

    // ========== Message ==========

    @Query("SELECT * FROM messages WHERE segmentId = :segmentId ORDER BY orderIndex ASC")
    fun getMessagesBySegmentId(segmentId: Long): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessageById(id: Long): Message?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message): Long

    @Update
    suspend fun updateMessage(message: Message)

    @Delete
    suspend fun deleteMessage(message: Message)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessageById(id: Long)

    // ========== 同步查询（阅读器、导出用） ==========

    @Query("SELECT * FROM segments WHERE chapterId = :chapterId ORDER BY orderIndex ASC")
    suspend fun getSegmentsByChapterIdSync(chapterId: Long): List<Segment>

    @Query("SELECT * FROM messages WHERE segmentId = :segmentId ORDER BY orderIndex ASC")
    suspend fun getMessagesBySegmentIdSync(segmentId: Long): List<Message>

    // ========== 全量数据（用于导出） ==========

    @Query("SELECT * FROM roles WHERE novelId = :novelId ORDER BY orderIndex ASC")
    suspend fun getRolesByNovelIdSync(novelId: Long): List<Role>

    @Query("SELECT * FROM chapters WHERE novelId = :novelId ORDER BY orderIndex ASC")
    suspend fun getChaptersByNovelIdSync(novelId: Long): List<Chapter>

    @Query("SELECT * FROM segments WHERE chapterId IN (SELECT id FROM chapters WHERE novelId = :novelId) ORDER BY orderIndex ASC")
    suspend fun getSegmentsByNovelIdSync(novelId: Long): List<Segment>

    @Query("SELECT * FROM messages WHERE segmentId IN (SELECT id FROM segments WHERE chapterId IN (SELECT id FROM chapters WHERE novelId = :novelId)) ORDER BY orderIndex ASC")
    suspend fun getMessagesByNovelIdSync(novelId: Long): List<Message>

    // ========== 全量插入（用于导入） ==========

    @Insert
    suspend fun insertRoles(roles: List<Role>)

    @Insert
    suspend fun insertChapters(chapters: List<Chapter>)

    @Insert
    suspend fun insertSegments(segments: List<Segment>)

    @Insert
    suspend fun insertMessages(messages: List<Message>)

    // ========== 计算消息数量 ==========

    @Query("SELECT COUNT(*) FROM messages WHERE segmentId IN (SELECT id FROM segments WHERE chapterId IN (SELECT id FROM chapters WHERE novelId = :novelId))")
    suspend fun getMessageCountByNovelId(novelId: Long): Int
}
