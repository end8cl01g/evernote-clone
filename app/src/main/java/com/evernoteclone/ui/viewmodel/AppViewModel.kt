package com.evernoteclone.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.evernoteclone.data.AppDatabase
import com.evernoteclone.data.Message
import com.evernoteclone.data.Note
import com.evernoteclone.data.Notebook
import com.evernoteclone.data.RecentSearch
import com.evernoteclone.data.Repository
import com.evernoteclone.data.Settings
import com.evernoteclone.data.Tag
import com.evernoteclone.data.MessageThread
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class Filter(val kind: String, val id: String? = null) {
    companion object {
        val All = Filter("all")
        val Trash = Filter("trash")
        val Reminders = Filter("reminders")
        fun notebook(id: String) = Filter("notebook", id)
        fun tag(id: String) = Filter("tag", id)
    }
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    val repo = Repository(AppDatabase.get(app), app)

    val settings: StateFlow<Settings?> =
        repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val filter = MutableStateFlow(Filter.All)

    val notes: StateFlow<List<Note>> =
        combine(repo.notes, repo.trash, filter) { active, trashed, f ->
            when (f.kind) {
                "trash" -> trashed
                "reminders" -> active.filter { it.reminderAt != null }
                "notebook" -> active.filter { it.notebookId == f.id }
                "tag" -> active.filter { n -> f.id != null && n.tagIds.contains(f.id) }
                else -> active
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val notebooks: StateFlow<List<Notebook>> =
        repo.notebooks.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val tags: StateFlow<List<Tag>> =
        repo.tags.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val recents: StateFlow<List<RecentSearch>> =
        repo.recents.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val threads: StateFlow<List<MessageThread>> =
        repo.threads.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun messagesFor(threadId: String): Flow<List<Message>> = repo.observeMessages(threadId)
    fun noteFor(id: String): Flow<Note?> = repo.observeNote(id)

    fun setFilter(f: Filter) { filter.value = f }

    fun sort(notes: List<Note>, mode: String): List<Note> = when (mode) {
        "created" -> notes.sortedByDescending { it.createdAt }
        "title" -> notes.sortedBy { it.title.lowercase() }
        "reminder" -> notes.sortedByDescending { it.reminderAt ?: 0L }
        else -> notes.sortedByDescending { it.updatedAt }
    }

    // 搜索（支援語法 notebook: / tag:）
    private var searchJob: kotlinx.coroutines.Job? = null
    private val _searchResults = MutableStateFlow<List<Note>>(emptyList())
    val searchResults: StateFlow<List<Note>> get() = _searchResults

    fun runSearch(raw: String) {
        searchJob?.cancel()
        val q = raw.trim()
        if (q.isEmpty()) { _searchResults.value = emptyList(); return }
        searchJob = viewModelScope.launch {
            var keyword = q
            var base = repo.notes  // 全量
            if (q.startsWith("notebook:")) {
                val name = q.removePrefix("notebook:").trim()
                keyword = ""
                val nb = notebooks.value.firstOrNull { it.name.contains(name, ignoreCase = true) }
                if (nb != null) {
                    repo.search("%").collect { all ->
                        _searchResults.value = all.filter { it.notebookId == nb.id }
                    }
                    return@launch
                }
            } else if (q.startsWith("tag:")) {
                val name = q.removePrefix("tag:").trim()
                keyword = ""
                val tag = tags.value.firstOrNull { it.name.contains(name, ignoreCase = true) }
                if (tag != null) {
                    repo.search("%").collect { all ->
                        _searchResults.value = all.filter { it.tagIds.contains(tag.id) }
                    }
                    return@launch
                }
            }
            if (keyword.isNotEmpty()) {
                repo.search(keyword).collect { _searchResults.value = it }
            }
        }
    }

    fun clearSearch() { searchJob?.cancel(); _searchResults.value = emptyList() }

    private val _selectedThread = MutableStateFlow<String?>(null)
    val selectedThread: StateFlow<String?> get() = _selectedThread
    fun setSelectedThread(id: String?) { _selectedThread.value = id }

    // 相機/相簿/錄音回傳通道（畫面間傳遞真實 URI）
    private val _pendingAttachment = MutableStateFlow<String?>(null)
    val pendingAttachment: StateFlow<String?> get() = _pendingAttachment
    fun setPendingAttachment(uri: String?) { _pendingAttachment.value = uri }
}
