package com.novelchat.ui.bookshelf

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novelchat.NovelChatApp
import com.novelchat.data.model.Novel
import com.novelchat.util.AppModule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BookshelfViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppModule.getRepository(application as NovelChatApp)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _novels = MutableStateFlow<List<Novel>>(emptyList())
    val novels: StateFlow<List<Novel>> = _novels.asStateFlow()

    // 对话框状态
    private val _showNewNovelDialog = MutableStateFlow(false)
    val showNewNovelDialog: StateFlow<Boolean> = _showNewNovelDialog.asStateFlow()

    private val _editingNovel = MutableStateFlow<Novel?>(null)
    val editingNovel: StateFlow<Novel?> = _editingNovel.asStateFlow()

    init {
        // 监听搜索
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .flatMapLatest { query ->
                    if (query.isBlank()) repository.getAllNovels()
                    else repository.searchNovels(query)
                }
                .collect { _novels.value = it }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun showNewNovelDialog() {
        _showNewNovelDialog.value = true
    }

    fun hideNewNovelDialog() {
        _showNewNovelDialog.value = false
    }

    fun showEditNovelDialog(novel: Novel) {
        _editingNovel.value = novel
    }

    fun hideEditNovelDialog() {
        _editingNovel.value = null
    }

    fun createNovel(title: String, description: String) {
        viewModelScope.launch {
            repository.insertNovel(
                Novel(
                    title = title,
                    description = description
                )
            )
        }
    }

    fun updateNovel(id: Long, title: String, description: String, coverColor: String) {
        viewModelScope.launch {
            val existing = repository.getNovelById(id) ?: return@launch
            repository.updateNovel(
                existing.copy(
                    title = title,
                    description = description,
                    coverColor = coverColor,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteNovel(novel: Novel) {
        viewModelScope.launch {
            repository.deleteNovel(novel)
        }
    }

    fun exportNovel(novel: Novel) {
        viewModelScope.launch {
            val app = getApplication<NovelChatApp>()
            val json = com.novelchat.util.JsonExporter.exportNovelToString(repository, novel.id)
            val fileName = "${novel.title}.json".replace(" ", "_")
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS)
            val file = java.io.File(downloadsDir, fileName)
            file.writeText(json)
        }
    }

    fun toggleNovelStatus(novel: Novel) {
        viewModelScope.launch {
            val newStatus = if (novel.status == Novel.STATUS_DRAFT) {
                Novel.STATUS_COMPLETED
            } else {
                Novel.STATUS_DRAFT
            }
            repository.updateNovel(
                novel.copy(
                    status = newStatus,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
}
