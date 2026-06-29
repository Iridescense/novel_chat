package com.novelchat.ui.reader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novelchat.NovelChatApp
import com.novelchat.data.model.*
import com.novelchat.util.AppModule
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ReaderItem(
    val message: Message,
    val role: Role?,
    val isProtagonist: Boolean,
    val chapterTitle: String,
    val segmentTitle: String
)

class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppModule.getRepository(application as NovelChatApp)

    private val _novel = MutableStateFlow<Novel?>(null)
    val novel: StateFlow<Novel?> = _novel.asStateFlow()

    private val _items = MutableStateFlow<List<ReaderItem>>(emptyList())
    val items: StateFlow<List<ReaderItem>> = _items.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    val currentItem: StateFlow<ReaderItem?> = combine(_items, _currentIndex) { list, i ->
        list.getOrNull(i)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val totalCount: StateFlow<Int> = _items.map { it.size }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val _fontSize = MutableStateFlow(16f)
    val fontSize: StateFlow<Float> = _fontSize.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

    private var autoPlayJob: Job? = null
    private val _isAutoPlaying = MutableStateFlow(false)
    val isAutoPlaying: StateFlow<Boolean> = _isAutoPlaying.asStateFlow()

    private val _autoPlaySpeed = MutableStateFlow(3f)
    val autoPlaySpeed: StateFlow<Float> = _autoPlaySpeed.asStateFlow()

    private val _visibleHiddenNoteId = MutableStateFlow<Long?>(null)
    val visibleHiddenNoteId: StateFlow<Long?> = _visibleHiddenNoteId.asStateFlow()

    fun loadNovel(novelId: Long, startIndex: Int = 0) {
        viewModelScope.launch {
            val n = repository.getNovelById(novelId) ?: return@launch
            _novel.value = n

            val roles = repository.getRolesByNovelIdSync(novelId)
            val roleMap = roles.associateBy { it.id }

            val chapters = repository.getChaptersByNovelIdSync(novelId)

            val readerItems = mutableListOf<ReaderItem>()

            for (chapter in chapters) {
                val segments = repository.getSegmentsByChapterIdSync(chapter.id)
                for (segment in segments) {
                    val protagId = segment.protagonistId
                    val messages = repository.getMessagesBySegmentIdSync(segment.id)
                    for (msg in messages) {
                        val role = msg.roleId?.let { roleMap[it] }
                        val isPro = protagId != null && msg.roleId == protagId
                        readerItems.add(
                            ReaderItem(
                                message = msg,
                                role = role,
                                isProtagonist = isPro,
                                chapterTitle = chapter.title,
                                segmentTitle = segment.title.ifBlank { "" }
                            )
                        )
                    }
                }
            }

            _items.value = readerItems

            _currentIndex.value = if (readerItems.isEmpty()) 0
                else startIndex.coerceIn(0, readerItems.size - 1)
        }
    }

    fun advance() {
        if (_currentIndex.value < _items.value.size - 1) {
            _currentIndex.value++
            _visibleHiddenNoteId.value = null
        }
    }

    fun goBack() {
        if (_currentIndex.value > 0) {
            _currentIndex.value--
            _visibleHiddenNoteId.value = null
        }
    }

    fun jumpTo(index: Int) {
        val clamped = index.coerceIn(0, _items.value.size - 1)
        _currentIndex.value = clamped
        _visibleHiddenNoteId.value = null
    }

    fun toggleHiddenNote(messageId: Long) {
        _visibleHiddenNoteId.value = if (_visibleHiddenNoteId.value == messageId) null else messageId
    }

    fun toggleSettings() {
        _showSettings.value = !_showSettings.value
    }

    fun hideSettings() {
        _showSettings.value = false
    }

    fun setFontSize(size: Float) {
        _fontSize.value = size.coerceIn(12f, 28f)
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun toggleAutoPlay() {
        if (_isAutoPlaying.value) {
            stopAutoPlay()
        } else {
            startAutoPlay()
        }
    }

    fun setAutoPlaySpeed(seconds: Float) {
        _autoPlaySpeed.value = seconds.coerceIn(1f, 10f)
    }

    private fun startAutoPlay() {
        _isAutoPlaying.value = true
        autoPlayJob?.cancel()
        autoPlayJob = viewModelScope.launch {
            while (_isAutoPlaying.value && _currentIndex.value < _items.value.size - 1) {
                delay((_autoPlaySpeed.value * 1000).toLong())
                advance()
            }
            _isAutoPlaying.value = false
        }
    }

    private fun stopAutoPlay() {
        _isAutoPlaying.value = false
        autoPlayJob?.cancel()
        autoPlayJob = null
    }

    override fun onCleared() {
        super.onCleared()
        autoPlayJob?.cancel()
    }
}
