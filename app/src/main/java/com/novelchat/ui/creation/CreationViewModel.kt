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

        // 监听当前章节变化 → 加载节
        viewModelScope.launch {
            currentChapter.collect { chapter ->
                if (chapter != null) {
                    repository.getSegmentsByChapterId(chapter.id).collect { segList ->
                        _segments.value = segList
                        if (segList.isEmpty()) {
                            val newSegId = repository.insertSegment(
                                Segment(chapterId = chapter.id, title = "", orderIndex = 0)
                            )
                        }
                        _currentSegmentIndex.value = 0
                    }
                }
            }
        }

        // 监听当前节变化 → 加载消息
        viewModelScope.launch {
            currentSegment.collect { segment ->
                if (segment != null) {
                    repository.getMessagesBySegmentId(segment.id).collect { msgList ->
                        _messages.value = msgList
                    }
                }
            }
        }
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
            val protagonistId = currentProtagonist.value?.id
            repository.insertSegment(
                Segment(
                    chapterId = chapter.id,
                    protagonistId = protagonistId,
                    title = "",
                    orderIndex = order
                )
            )
            markChanged()
        }
    }

    fun setSegmentProtagonist(segment: Segment, roleId: Long?) {
        viewModelScope.launch {
            repository.updateSegment(segment.copy(protagonistId = roleId))
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
        viewModelScope.launch {
            val order = _messages.value.size

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

            repository.insertMessage(
                Message(
                    segmentId = segment.id,
                    type = type,
                    roleId = roleId,
                    text = text,
                    richTextJson = richTextJson,
                    orderIndex = order
                )
            )
            markChanged()
        }
    }

    fun updateMessage(message: Message) {
        viewModelScope.launch {
            repository.updateMessage(message)
            markChanged()
        }
    }

    fun deleteMessage(message: Message) {
        viewModelScope.launch {
            repository.deleteMessage(message)
            // 重新排序
            val currentMsgs = _messages.value.filter { it.id != message.id }
            currentMsgs.forEachIndexed { index, msg ->
                if (msg.orderIndex != index) {
                    repository.updateMessage(msg.copy(orderIndex = index))
                }
            }
            markChanged()
        }
    }

    fun moveMessage(fromIndex: Int, toIndex: Int) {
        val msgs = _messages.value.toMutableList()
        if (fromIndex !in msgs.indices || toIndex !in msgs.indices) return
        val item = msgs.removeAt(fromIndex)
        msgs.add(toIndex, item)
        viewModelScope.launch {
            msgs.forEachIndexed { index, msg ->
                repository.updateMessage(msg.copy(orderIndex = index))
            }
            markChanged()
        }
    }

    // ========== 隐藏附注 ==========

    fun startEditingHiddenNote(message: Message) {
        _editingHiddenNote.value = message
    }

    fun saveHiddenNote(messageId: Long, noteText: String) {
        viewModelScope.launch {
            val msg = repository.getMessageById(messageId) ?: return@launch
            repository.updateMessage(
                msg.copy(
                    hasHiddenNote = noteText.isNotBlank(),
                    hiddenNote = noteText.ifBlank { null }
                )
            )
            _editingHiddenNote.value = null
            markChanged()
        }
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

    // ========== 保存 ==========

    fun save() {
        viewModelScope.launch {
            _novel.value?.let { novel ->
                repository.updateNovel(
                    novel.copy(updatedAt = System.currentTimeMillis())
                )
            }
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
