package com.novelchat.ui.creation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novelchat.NovelChatApp
import com.novelchat.data.model.*
import com.novelchat.util.AppModule
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CreationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppModule.getRepository(application as NovelChatApp)

    // 本地缓冲 — 编辑中的消息不立即写入 Room
    private var nextLocalId = -1L
    private fun nextLocalMsgId(): Long = nextLocalId--
    // 记录当前所属的 segmentId（用于 save 时写回 Room）
    private var currentSegmentRoomId: Long = 0L
    // 每节的消息本地缓冲（segmentId → 消息列表），切换节时不丢失
    private val segmentMessageBuffer = mutableMapOf<Long, List<Message>>()
    // 跟踪当前章节 ID，判断是否是章节切换
    private var previousChapterId: Long = -1L

    // 剧本数据
    private val _novel = MutableStateFlow<Novel?>(null)
    val novel: StateFlow<Novel?> = _novel.asStateFlow()

    // 章节列表
    private val _chapters = MutableStateFlow<List<Chapter>>(emptyList())
    val chapters: StateFlow<List<Chapter>> = _chapters.asStateFlow()

    // 当前章节
    private val _currentChapterIndex = MutableStateFlow(0)
    val currentChapterIndex: StateFlow<Int> = _currentChapterIndex.asStateFlow()

    val currentChapter: StateFlow<Chapter?> = combine(
        _chapters, _currentChapterIndex
    ) { list, index ->
        list.getOrNull(index)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // 节列表（当前章节下）
    private val _segments = MutableStateFlow<List<Segment>>(emptyList())
    val segments: StateFlow<List<Segment>> = _segments.asStateFlow()

    // 当前节
    private val _currentSegmentIndex = MutableStateFlow(0)
    val currentSegmentIndex: StateFlow<Int> = _currentSegmentIndex.asStateFlow()

    val currentSegment: StateFlow<Segment?> = combine(
        _segments, _currentSegmentIndex
    ) { list, index ->
        list.getOrNull(index)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // 角色列表
    private val _roles = MutableStateFlow<List<Role>>(emptyList())
    val roles: StateFlow<List<Role>> = _roles.asStateFlow()

    // 消息列表（当前节下）
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    // 全部消息（跨所有节，含分段信息）
    private val _allChapterMessages = MutableStateFlow<List<Pair<Segment, Message>>>(emptyList())
    val allChapterMessages: StateFlow<List<Pair<Segment, Message>>> = _allChapterMessages.asStateFlow()

    // 插入消息到指定消息之后
    private val _insertAfterId = MutableStateFlow<Long?>(null)
    val insertAfterId: StateFlow<Long?> = _insertAfterId.asStateFlow()

    // 当前节的主角
    val currentProtagonist: StateFlow<Role?> = combine(
        currentSegment, _roles
    ) { seg, roleList ->
        seg?.protagonistId?.let { protoId ->
            roleList.find { it.id == protoId }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // 是否有未保存的修改
    private var savedVersion: Int = 0
    private var currentVersion: Int = 0
    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges: StateFlow<Boolean> = _hasUnsavedChanges.asStateFlow()

    // 左侧滑菜单是否打开
    private val _showSlideMenu = MutableStateFlow(false)
    val showSlideMenu: StateFlow<Boolean> = _showSlideMenu.asStateFlow()

    // 编辑隐藏附注的状态
    private val _editingHiddenNote = MutableStateFlow<Message?>(null)
    val editingHiddenNote: StateFlow<Message?> = _editingHiddenNote.asStateFlow()

    // 预览相关
    private val _previewMessageIndex = MutableStateFlow<Int?>(null)
    val previewMessageIndex: StateFlow<Int?> = _previewMessageIndex.asStateFlow()

    // 底部输入栏状态
    private val _inputSenderType = MutableStateFlow(SenderType.PROTAGONIST)
    val inputSenderType: StateFlow<SenderType> = _inputSenderType.asStateFlow()

    private val _selectedOtherRoleId = MutableStateFlow<Long?>(null)
    val selectedOtherRoleId: StateFlow<Long?> = _selectedOtherRoleId.asStateFlow()

    fun loadNovel(novelId: Long, chapterId: Long? = null) {
        // 先清空旧内容
        _messages.value = emptyList()
        _segments.value = emptyList()
        _chapters.value = emptyList()
        viewModelScope.launch {
            val n = repository.getNovelById(novelId) ?: return@launch
            _novel.value = n

            // 加载角色
            repository.getRolesByNovelId(novelId).collect { roleList ->
                _roles.value = roleList
            }
        }

        // 加载章节
        viewModelScope.launch {
            repository.getChaptersByNovelId(novelId).collect { chapterList ->
                _chapters.value = chapterList
                if (chapterList.isEmpty()) {
                    val newChapterId = repository.insertChapter(
                        Chapter(novelId = novelId, title = "第一章", orderIndex = 0)
                    )
                    repository.insertSegment(
                        Segment(chapterId = newChapterId, title = "", orderIndex = 0)
                    )
                } else if (chapterId != null) {
                    // 切换到指定章节
                    val idx = chapterList.indexOfFirst { it.id == chapterId }
                    if (idx >= 0) _currentChapterIndex.value = idx
                }
            }
        }

        // 监听当前章节变化 → 加载节（用 flatMapLatest 避免嵌套 collect 阻塞）
        viewModelScope.launch {
            currentChapter
                .flatMapLatest { chapter ->
                    if (chapter != null) {
                        repository.getSegmentsByChapterId(chapter.id)
                    } else {
                        flowOf(emptyList())
                    }
                }
                .collect { segList ->
                    _segments.value = segList
                    if (segList.isEmpty()) {
                        val ch = currentChapter.value
                        if (ch != null) {
                            repository.insertSegment(
                                Segment(chapterId = ch.id, title = "", orderIndex = 0)
                            )
                        }
                    }
                    // 仅在章节切换时跳到最后一节（退出再进来从最后编辑的位置开始）
                    val ch = currentChapter.value
                    if (ch != null && ch.id != previousChapterId) {
                        _currentSegmentIndex.value = (segList.size - 1).coerceAtLeast(0)
                        previousChapterId = ch.id
                    }
                }
        }

        // 监听当前章节变化 → 加载所有节的消息（用 flatMapLatest 避免旧章节的加载干扰）
        viewModelScope.launch {
            currentChapter
                .flatMapLatest { chapter ->
                    if (chapter != null) {
                        flow {
                            loadAllChapterMessages(chapter.id)
                            emit(Unit)
                        }
                    } else {
                        flowOf(Unit)
                    }
                }
                .collect { }
        }

        // 监听当前节变化 → 切换时保存/恢复本地缓冲
        viewModelScope.launch {
            currentSegment.collect { segment ->
                if (segment != null) {
                    val newSegId = segment.id
                    // 真正的段切换（ID 变了才需要保存旧段 + 加载新段）
                    if (newSegId != currentSegmentRoomId) {
                        // 先把当前节的缓冲存起来
                        if (currentSegmentRoomId > 0L && _messages.value.isNotEmpty()) {
                            segmentMessageBuffer[currentSegmentRoomId] = _messages.value
                        }
                        currentSegmentRoomId = newSegId
                        // 从缓冲区或 Room 加载
                        _messages.value = segmentMessageBuffer[newSegId]
                            ?: repository.getMessagesBySegmentIdSync(newSegId)
                        refreshAllMessages()
                    }
                }
            }
        }
    }

    /** 从 Room 加载章节所有节的消息到本地缓冲（suspend 以便 flatMapLatest 能取消它） */
    private suspend fun loadAllChapterMessages(chapterId: Long) {
        val segList = repository.getSegmentsByChapterIdSync(chapterId)
        for (seg in segList) {
            if (!segmentMessageBuffer.containsKey(seg.id)) {
                val msgs = repository.getMessagesBySegmentIdSync(seg.id)
                segmentMessageBuffer[seg.id] = msgs
            }
        }
        refreshAllMessages()
    }

    private fun refreshAllMessages() {
        // 从本地 _segments + 缓冲区构建全量显示列表
        val segList = _segments.value
        val all = mutableListOf<Pair<Segment, Message>>()
        for (seg in segList) {
            // 当前段始终用 _messages.value（最新），不走缓冲，避免 setSegmentProtagonist
            // 等操作把缓冲覆盖成旧数据后新消息显示不出来
            val msgs = if (seg.id == currentSegmentRoomId) {
                _messages.value
            } else {
                segmentMessageBuffer[seg.id] ?: emptyList()
            }
            for (msg in msgs) {
                all.add(seg to msg)
            }
        }
        _allChapterMessages.value = all
    }

    // ========== 章节操作 ==========

    fun switchChapter(index: Int) {
        if (index in _chapters.value.indices) {
            _currentChapterIndex.value = index
        }
    }

    fun addChapter(title: String) {
        val novelId = _novel.value?.id ?: return
        viewModelScope.launch {
            val order = _chapters.value.size
            val chapterId = repository.insertChapter(
                Chapter(novelId = novelId, title = title, orderIndex = order)
            )
            // 自动创建第一个节
            repository.insertSegment(
                Segment(chapterId = chapterId, title = "", orderIndex = 0)
            )
            markChanged()
        }
    }

    fun deleteChapter(chapter: Chapter) {
        viewModelScope.launch {
            repository.deleteChapter(chapter)
            markChanged()
        }
    }

    fun renameChapter(chapter: Chapter, newTitle: String) {
        viewModelScope.launch {
            repository.updateChapter(chapter.copy(title = newTitle))
            markChanged()
        }
    }

    // ========== 节操作 ==========

    fun switchSegment(index: Int) {
        if (index in _segments.value.indices) {
            _currentSegmentIndex.value = index
        }
    }

    fun addSegment() {
        val chapter = currentChapter.value ?: return
        viewModelScope.launch {
            val order = _segments.value.size
            repository.insertSegment(
                Segment(
                    chapterId = chapter.id,
                    protagonistId = null,
                    title = "",
                    orderIndex = order
                )
            )
            markChanged()
        }
    }

    fun deleteSegment(segment: Segment) {
        viewModelScope.launch {
            repository.deleteSegment(segment)
            // 删除后重新编号剩余节的 orderIndex
            val chapter = currentChapter.value
            if (chapter != null) {
                val remaining = repository.getSegmentsByChapterIdSync(chapter.id)
                remaining.forEachIndexed { index, seg ->
                    if (seg.orderIndex != index) {
                        repository.updateSegment(seg.copy(orderIndex = index))
                    }
                }
            }
            // 如果删除的是当前节，切回第 0 节
            if (_segments.value.getOrNull(_currentSegmentIndex.value)?.id == segment.id) {
                _currentSegmentIndex.value = 0
            }
            markChanged()
        }
    }

    fun setSegmentProtagonist(segment: Segment, roleId: Long?) {
        // 立即更新本地状态，不等 Room Flow 回来
        val updatedSeg = segment.copy(protagonistId = roleId)
        val currentSegs = _segments.value.toMutableList()
        val idx = currentSegs.indexOfFirst { it.id == segment.id }
        if (idx >= 0) {
            currentSegs[idx] = updatedSeg
            _segments.value = currentSegs
        }
        viewModelScope.launch {
            repository.updateSegment(updatedSeg)
            markChanged()
        }
    }

    fun updateSegmentTitle(segment: Segment, title: String) {
        viewModelScope.launch {
            repository.updateSegment(segment.copy(title = title))
            markChanged()
        }
    }

    // ========== 角色操作 ==========

    fun addRole(name: String, color: String, avatarType: String, avatarValue: String) {
        val novelId = _novel.value?.id
        if (novelId == null || novelId == 0L) return
        viewModelScope.launch {
            try {
                val order = _roles.value.size
                repository.insertRole(
                    Role(
                        novelId = novelId,
                        name = name,
                        color = color,
                        avatarType = avatarType,
                        avatarValue = avatarValue,
                        orderIndex = order
                    )
                )
                markChanged()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateRole(role: Role) {
        viewModelScope.launch {
            repository.updateRole(role)
            markChanged()
        }
    }

    fun deleteRole(role: Role) {
        viewModelScope.launch {
            repository.deleteRole(role)
            markChanged()
        }
    }

    // ========== 消息操作 ==========

    fun sendMessage(text: String, richTextJson: String? = null) {
        val segment = currentSegment.value ?: return
        val senderType = _inputSenderType.value

        val type: String
        val roleId: Long?

        when (senderType) {
            SenderType.PROTAGONIST -> {
                type = Message.TYPE_DIALOGUE
                roleId = currentProtagonist.value?.id
            }
            SenderType.OTHER -> {
                type = Message.TYPE_DIALOGUE
                roleId = _selectedOtherRoleId.value
            }
            SenderType.NARRATOR -> {
                type = Message.TYPE_NARRATOR
                roleId = null
            }
        }

        val currentMsgs = _messages.value.toMutableList()
        val insertAfter = _insertAfterId.value

        if (insertAfter != null) {
            // 优先在当前节查找目标消息
            val targetIdx = currentMsgs.indexOfFirst { it.id == insertAfter }
            if (targetIdx >= 0) {
                val targetMsg = currentMsgs[targetIdx]
                val newMsg = Message(
                    id = nextLocalMsgId(), segmentId = segment.id, type = type,
                    roleId = roleId, text = text, richTextJson = richTextJson,
                    orderIndex = targetMsg.orderIndex + 1
                )
                for (i in targetIdx + 1 until currentMsgs.size) {
                    currentMsgs[i] = currentMsgs[i].copy(orderIndex = currentMsgs[i].orderIndex + 1)
                }
                currentMsgs.add(targetIdx + 1, newMsg)
                _messages.value = currentMsgs
            } else {
                // 不在当前节 → 搜索所有节的缓冲区
                var inserted = false
                for ((bufSegId, bufMsgs) in segmentMessageBuffer) {
                    val bufIdx = bufMsgs.indexOfFirst { it.id == insertAfter }
                    if (bufIdx >= 0) {
                        val targetMsg = bufMsgs[bufIdx]
                        val newMsg = Message(
                            id = nextLocalMsgId(), segmentId = bufSegId, type = type,
                            roleId = roleId, text = text, richTextJson = richTextJson,
                            orderIndex = targetMsg.orderIndex + 1
                        )
                        val updatedBuf = bufMsgs.toMutableList()
                        for (i in bufIdx + 1 until updatedBuf.size) {
                            updatedBuf[i] = updatedBuf[i].copy(orderIndex = updatedBuf[i].orderIndex + 1)
                        }
                        updatedBuf.add(bufIdx + 1, newMsg)
                        segmentMessageBuffer[bufSegId] = updatedBuf
                        inserted = true
                        break
                    }
                }
                if (!inserted) {
                    // 都没找到，追加到当前节末尾
                    currentMsgs.add(
                        Message(
                            id = nextLocalMsgId(), segmentId = segment.id, type = type,
                            roleId = roleId, text = text, richTextJson = richTextJson,
                            orderIndex = currentMsgs.size
                        )
                    )
                    _messages.value = currentMsgs
                }
            }
        } else {
            currentMsgs.add(
                Message(
                    id = nextLocalMsgId(), segmentId = segment.id, type = type,
                    roleId = roleId, text = text, richTextJson = richTextJson,
                    orderIndex = currentMsgs.size
                )
            )
            _messages.value = currentMsgs
        }

        refreshAllMessages()
        _insertAfterId.value = null
        markChanged()
    }

    fun setInsertAfterId(messageId: Long?) {
        _insertAfterId.value = messageId
    }

    fun clearInsertAfter() {
        _insertAfterId.value = null
    }

    fun updateMessage(message: Message) {
        // 先找当前段
        val currentMsgs = _messages.value.toMutableList()
        val idx = currentMsgs.indexOfFirst { it.id == message.id }
        if (idx >= 0) {
            currentMsgs[idx] = message
            _messages.value = currentMsgs
            refreshAllMessages()
            markChanged()
            return
        }
        // 再找其他段的缓冲
        for ((segId, msgs) in segmentMessageBuffer) {
            val bufIdx = msgs.indexOfFirst { it.id == message.id }
            if (bufIdx >= 0) {
                val updatedBuf = msgs.toMutableList()
                updatedBuf[bufIdx] = message
                segmentMessageBuffer[segId] = updatedBuf
                refreshAllMessages()
                markChanged()
                return
            }
        }
    }

    fun deleteMessage(message: Message) {
        // 先找当前段
        val currentMsgs = _messages.value.toMutableList()
        val idx = currentMsgs.indexOfFirst { it.id == message.id }
        if (idx >= 0) {
            currentMsgs.removeAt(idx)
            currentMsgs.forEachIndexed { i, msg -> currentMsgs[i] = msg.copy(orderIndex = i) }
            _messages.value = currentMsgs
            refreshAllMessages()
            markChanged()
            return
        }
        // 再找其他段的缓冲
        for ((segId, msgs) in segmentMessageBuffer) {
            val bufIdx = msgs.indexOfFirst { it.id == message.id }
            if (bufIdx >= 0) {
                val updatedBuf = msgs.toMutableList()
                updatedBuf.removeAt(bufIdx)
                updatedBuf.forEachIndexed { i, msg -> updatedBuf[i] = msg.copy(orderIndex = i) }
                segmentMessageBuffer[segId] = updatedBuf
                refreshAllMessages()
                markChanged()
                return
            }
        }
    }

    fun moveMessage(fromIndex: Int, toIndex: Int) {
        val msgs = _messages.value.toMutableList()
        if (fromIndex !in msgs.indices || toIndex !in msgs.indices) return
        val item = msgs.removeAt(fromIndex)
        msgs.add(toIndex, item)
        msgs.forEachIndexed { index, msg ->
            msgs[index] = msg.copy(orderIndex = index)
        }
        _messages.value = msgs
        refreshAllMessages()
        markChanged()
    }

    // ========== 隐藏附注 ==========

    fun startEditingHiddenNote(message: Message) {
        _editingHiddenNote.value = message
    }

    fun saveHiddenNote(messageId: Long, noteText: String) {
        val updatedMsg: (Message) -> Message = { msg ->
            msg.copy(
                hasHiddenNote = noteText.isNotBlank(),
                hiddenNote = noteText.ifBlank { null }
            )
        }
        // 先找当前段
        val currentMsgs = _messages.value.toMutableList()
        val idx = currentMsgs.indexOfFirst { it.id == messageId }
        if (idx >= 0) {
            currentMsgs[idx] = updatedMsg(currentMsgs[idx])
            _messages.value = currentMsgs
            refreshAllMessages()
            markChanged()
            _editingHiddenNote.value = null
            return
        }
        // 再找其他段的缓冲
        for ((segId, msgs) in segmentMessageBuffer) {
            val bufIdx = msgs.indexOfFirst { it.id == messageId }
            if (bufIdx >= 0) {
                val updatedBuf = msgs.toMutableList()
                updatedBuf[bufIdx] = updatedMsg(updatedBuf[bufIdx])
                segmentMessageBuffer[segId] = updatedBuf
                refreshAllMessages()
                markChanged()
                break
            }
        }
        _editingHiddenNote.value = null
    }

    fun cancelEditingHiddenNote() {
        _editingHiddenNote.value = null
    }

    // ========== 输入栏状态 ==========

    fun setInputSenderType(type: SenderType) {
        _inputSenderType.value = type
    }

    fun setSelectedOtherRoleId(roleId: Long?) {
        _selectedOtherRoleId.value = roleId
    }

    // ========== 菜单 ==========

    fun toggleSlideMenu() {
        _showSlideMenu.value = !_showSlideMenu.value
    }

    fun closeSlideMenu() {
        _showSlideMenu.value = false
    }

    // ========== 预览 ==========

    fun startPreviewFrom(index: Int) {
        _previewMessageIndex.value = index
    }

    fun clearPreview() {
        _previewMessageIndex.value = null
    }

    // ========== 保存 / 丢弃 ==========

    /** 将本地缓冲的所有消息写入 Room，完成后调用 onComplete */
    fun save(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            // 先把当前段的消息存入缓冲
            if (currentSegmentRoomId > 0L && _messages.value.isNotEmpty()) {
                segmentMessageBuffer[currentSegmentRoomId] = _messages.value
            }
            // 遍历所有段缓冲，逐段写入 Room
            for ((segId, msgs) in segmentMessageBuffer) {
                val oldMsgs = repository.getMessagesBySegmentIdSync(segId)
                oldMsgs.forEach { repository.deleteMessage(it) }
                msgs.forEach { msg ->
                    repository.insertMessage(
                        msg.copy(id = 0, segmentId = segId)
                    )
                }
            }
            // 写完后清空缓冲
            segmentMessageBuffer.clear()
            // 从 Room 重新加载所有段的消息到缓冲，让 refreshAllMessages 能读到全部
            val curChapter = currentChapter.value
            if (curChapter != null) {
                val segList = repository.getSegmentsByChapterIdSync(curChapter.id)
                for (seg in segList) {
                    val roomMsgs = repository.getMessagesBySegmentIdSync(seg.id)
                    segmentMessageBuffer[seg.id] = roomMsgs
                    if (seg.id == currentSegmentRoomId) {
                        _messages.value = roomMsgs
                    }
                }
            }
            _novel.value?.let { novel ->
                repository.updateNovel(
                    novel.copy(updatedAt = System.currentTimeMillis())
                )
            }
            refreshAllMessages()
            savedVersion = currentVersion
            _hasUnsavedChanges.value = false
            onComplete()
        }
    }

    /** 丢弃本地缓冲，从 Room 重新加载 */
    fun discardChanges() {
        segmentMessageBuffer.clear()
        viewModelScope.launch {
            if (currentSegmentRoomId > 0L) {
                _messages.value = repository.getMessagesBySegmentIdSync(currentSegmentRoomId)
            }
            refreshAllMessages()
            savedVersion = currentVersion
            _hasUnsavedChanges.value = false
        }
    }

    fun toggleNovelStatus() {
        _novel.value?.let { n ->
            viewModelScope.launch {
                val newStatus = if (n.status == Novel.STATUS_DRAFT) {
                    Novel.STATUS_COMPLETED
                } else {
                    Novel.STATUS_DRAFT
                }
                repository.updateNovel(n.copy(status = newStatus, updatedAt = System.currentTimeMillis()))
                _novel.value = n.copy(status = newStatus)
            }
        }
    }

    fun toggleChapterStatus(chapterId: Long = currentChapter.value?.id ?: 0L) {
        if (chapterId == 0L) return
        val chapter = repository.getChapterById(chapterId) ?: return
        viewModelScope.launch {
            val newStatus = if (chapter.status == Chapter.STATUS_DRAFT) {
                Chapter.STATUS_COMPLETED
            } else {
                Chapter.STATUS_DRAFT
            }
            repository.updateChapterStatus(chapterId, newStatus)
            // 更新本地 _chapters
            val updated = _chapters.value.toMutableList()
            val idx = updated.indexOfFirst { it.id == chapterId }
            if (idx >= 0) {
                updated[idx] = chapter.copy(status = newStatus)
                _chapters.value = updated
            }
        }
    }

    private fun markChanged() {
        currentVersion++
        _hasUnsavedChanges.value = currentVersion != savedVersion
    }
}

enum class SenderType {
    PROTAGONIST,
    OTHER,
    NARRATOR
}
