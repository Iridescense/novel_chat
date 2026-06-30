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

    private val _allNovels = MutableStateFlow<List<Novel>>(emptyList())
    val allNovels: StateFlow<List<Novel>> = _allNovels.asStateFlow()

    // 对话框状态
    private val _showNewNovelDialog = MutableStateFlow(false)
    val showNewNovelDialog: StateFlow<Boolean> = _showNewNovelDialog.asStateFlow()

    private val _editingNovel = MutableStateFlow<Novel?>(null)
    val editingNovel: StateFlow<Novel?> = _editingNovel.asStateFlow()

    init {
        // 监听创作台原件（bookshelfViewModel 同时被创作台复用）
        viewModelScope.launch {
            repository.getOriginalNovels().collect { _allNovels.value = it }
        }

        // 监听书架副本
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .flatMapLatest { query ->
                    if (query.isBlank()) repository.getBookshelfNovels()
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

    fun removeNovelFromBookshelf(novel: Novel) {
        viewModelScope.launch {
            repository.deleteNovel(novel)
        }
    }

    /** 深拷贝创作台原件到书架 */
    fun addToBookshelf(novelId: Long) {
        viewModelScope.launch {
            repository.deepCopyNovelToBookshelf(novelId)
        }
    }

    /** 更新书架副本（删除旧副本 → 重新深拷贝） */
    fun updateBookshelfCopy(novelId: Long) {
        viewModelScope.launch {
            repository.updateBookshelfCopy(novelId)
        }
    }

    /** 检查创作台原件是否已在书架中有副本 */
    suspend fun hasBookshelfCopy(novelId: Long): Boolean {
        return repository.hasBookshelfCopy(novelId)
    }

    fun exportNovelToUri(novel: Novel, uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                // 书架副本则导出创作台原件（始终包含最新消息内容）
                val exportId = novel.sourceNovelId ?: novel.id
                val json = com.novelchat.util.JsonExporter.exportNovelToString(repository, exportId)
                val app = getApplication<NovelChatApp>()
                app.contentResolver.openOutputStream(uri)?.use {
                    it.write(json.toByteArray(Charsets.UTF_8))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun exportNovel(novel: Novel) {
        viewModelScope.launch {
            try {
                val app = getApplication<NovelChatApp>()
                // 书架副本则导出创作台原件（始终包含最新消息内容）
                val exportId = novel.sourceNovelId ?: novel.id
                val json = com.novelchat.util.JsonExporter.exportNovelToString(repository, exportId)
                val fileName = "${novel.title}.json".replace(" ", "_")
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS)
                val file = java.io.File(downloadsDir, fileName)
                file.writeText(json)
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
