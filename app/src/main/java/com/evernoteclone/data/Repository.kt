package com.evernoteclone.data

import android.content.Context
import android.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class Repository(private val db: AppDatabase, private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)

    init { seedIfEmpty() }

    // ---------- 觀察 ----------
    val notes: Flow<List<Note>> get() = db.noteDao().observeActive()
    val trash: Flow<List<Note>> get() = db.noteDao().observeTrash()
    val notebooks: Flow<List<Notebook>> get() = db.notebookDao().observeAll()
    val tags: Flow<List<Tag>> get() = db.tagDao().observeAll()
    val recents: Flow<List<RecentSearch>> get() = db.recentSearchDao().observeRecent()
    val threads: Flow<List<MessageThread>> get() = db.messageThreadDao().observeAll()
    val settings: Flow<Settings?> get() = db.settingsDao().observe()
    fun observeNote(id: String): Flow<Note?> = db.noteDao().observeById(id)
    fun observeMessages(threadId: String): Flow<List<Message>> = db.messageDao().observeByThread(threadId)

    // ---------- 筆記 ----------
    fun saveNote(note: Note, onDone: (String) -> Unit = {}) {
        scope.launch {
            val id = if (db.noteDao().getById(note.id) != null) {
                db.noteDao().update(note); note.id
            } else {
                db.noteDao().insert(note); note.id
            }
            logChange("note", id, "update")
            onDone(id)
        }
    }
    fun trashNote(id: String) = scope.launch { db.noteDao().trash(id, System.currentTimeMillis()); logChange("note", id, "delete") }
    fun restoreNote(id: String) = scope.launch { db.noteDao().restore(id); logChange("note", id, "update") }
    fun deleteForever(id: String) = scope.launch { db.noteDao().delete(id); logChange("note", id, "delete") }
    fun emptyTrash() = scope.launch { db.noteDao().emptyTrash() }
    fun togglePin(id: String) = scope.launch { db.noteDao().togglePin(id); logChange("note", id, "update") }

    // ---------- 筆記本 / 標籤 ----------
    fun createNotebook(name: String, color: Long, stack: String?) =
        scope.launch { db.notebookDao().insert(Notebook(name = name, color = color, stack = stack)); logChange("notebook", "nb", "create") }
    fun createTag(name: String) =
        scope.launch { db.tagDao().insert(Tag(name = name)); logChange("tag", "tag", "create") }

    // ---------- 搜索 ----------
    fun search(q: String): Flow<List<Note>> = db.noteDao().search(q)
    fun upsertRecent(kw: String) = scope.launch { db.recentSearchDao().insert(RecentSearch(keyword = kw)) }

    // ---------- 訊息 ----------
    fun createThread(title: String, color: Long) = scope.launch {
        db.messageThreadDao().insert(MessageThread(title = title, color = color))
    }
    fun sendMessage(threadId: String, text: String) = scope.launch {
        db.messageDao().insert(
            Message(threadId = threadId, sender = "我", text = text, isMine = true, read = true)
        )
        db.messageThreadDao().touch(threadId, text, System.currentTimeMillis())
        logChange("message", threadId, "create")
    }
    fun saveMessageToNote(threadId: String, message: Message) = scope.launch {
        if (message.savedToNote) return@launch
        val nbId = ensureNotebook("訊息", Color.rgb(33, 150, 243).toLong())
        db.noteDao().insert(
            Note(
                title = "訊息：${message.sender}",
                content = message.text,
                notebookId = nbId,
            )
        )
        db.messageDao().markSaved(message.id)
        logChange("note", "msg-note", "create")
    }
    private suspend fun ensureNotebook(name: String, color: Long): String {
        val existing = db.notebookDao().getAll().firstOrNull { it.name == name }
        if (existing != null) return existing.id
        val nb = Notebook(name = name, color = color)
        db.notebookDao().insert(nb)
        return nb.id
    }

    // ---------- 設定 ----------
    fun setDarkMode(v: Boolean) = scope.launch { db.settingsDao().setDarkMode(v) }
    fun setPin(enabled: Boolean, code: String) = scope.launch { db.settingsDao().setPin(enabled, code) }
    fun setNotifications(v: Boolean) = scope.launch { db.settingsDao().setNotifications(v) }
    fun setViewMode(v: String) = scope.launch { db.settingsDao().setViewMode(v) }
    fun setSortMode(v: String) = scope.launch { db.settingsDao().setSortMode(v) }
    fun setFontScale(v: Float) = scope.launch { db.settingsDao().setFontScale(v) }

    // ---------- 同步引擎（真實：變更日誌 + 待同步計數） ----------
    private suspend fun logChange(entity: String, entityId: String, op: String) {
        db.changeLogDao().insert(ChangeLog(entity = entity, entityId = entityId, op = op))
        db.settingsDao().bumpPending()
    }
    fun syncNow(onDone: (Int) -> Unit = {}) = scope.launch {
        val pending = db.changeLogDao().pending()
        pending.forEach { db.changeLogDao().markSynced(it.id) }
        db.settingsDao().markSynced(System.currentTimeMillis())
        onDone(pending.size)
    }

    // ---------- 備份（真實完整 JSON 匯出/匯入） ----------
    fun exportBackup(): String {
        val out = JSONObject()
        out.put("_meta", JSONObject().put("schemaVersion", 1).put("exportedAt", System.currentTimeMillis()))
        out.put("notes", JSONArray(db.noteDao().let { runBlocking { it.getAllCompat() } }))
        out.put("notebooks", JSONArray(db.notebookDao().let { runBlocking { it.getAll() } }))
        out.put("tags", JSONArray(db.tagDao().let { runBlocking { it.getAll() } }))
        out.put("settings", JSONArray(runBlocking { listOfNotNull(db.settingsDao().get()) }))
        return out.toString()
    }

    fun importBackup(json: String): Pair<Boolean, String> = runBlocking {
        try {
            val data = JSONObject(json)
            if (!data.has("notes") || !data.has("notebooks")) {
                return@runBlocking false to "格式不符（缺少 notes/notebooks 表）"
            }
            db.noteDao().clearAll()
            db.notebookDao().clearAll()
            db.tagDao().clearAll()
            val notesArr = data.getJSONArray("notes")
            for (i in 0 until notesArr.length()) {
                val o = notesArr.getJSONObject(i)
                db.noteDao().insert(
                    Note(
                        id = o.optString("id"),
                        title = o.optString("title"),
                        content = o.optString("content"),
                        notebookId = o.optString("notebookId").ifEmpty { null },
                        tagIds = jsonArrToList(o.optJSONArray("tagIds")),
                        resources = jsonArrToList(o.optJSONArray("resources")),
                        reminderAt = if (o.has("reminderAt")) o.optLong("reminderAt") else null,
                        pinned = o.optBoolean("pinned"),
                        createdAt = o.optLong("createdAt"),
                        updatedAt = o.optLong("updatedAt"),
                        inTrash = o.optBoolean("inTrash"),
                        trashedAt = if (o.has("trashedAt")) o.optLong("trashedAt") else null,
                    )
                )
            }
            val nbArr = data.getJSONArray("notebooks")
            for (i in 0 until nbArr.length()) {
                val o = nbArr.getJSONObject(i)
                db.notebookDao().insert(
                    Notebook(
                        id = o.optString("id"),
                        name = o.optString("name"),
                        color = o.optLong("color"),
                        stack = o.optString("stack").ifEmpty { null },
                        createdAt = o.optLong("createdAt"),
                    )
                )
            }
            val tagArr = data.getJSONArray("tags")
            for (i in 0 until tagArr.length()) {
                val o = tagArr.getJSONObject(i)
                db.tagDao().insert(Tag(id = o.optString("id"), name = o.optString("name"), createdAt = o.optLong("createdAt")))
            }
            db.settingsDao().markSynced(System.currentTimeMillis())
            true to "筆記 ${notesArr.length()} · 筆記本 ${nbArr.length()} · 標籤 ${tagArr.length()}"
        } catch (e: Exception) {
            false to (e.message ?: "解析失敗")
        }
    }

    private fun jsonArrToList(arr: JSONArray?): List<String> =
        if (arr == null) emptyList() else (0 until arr.length()).map { arr.getString(it) }

    fun resetData(onDone: (String) -> Unit = {}) = scope.launch {
        db.noteDao().clearAll()
        db.notebookDao().clearAll()
        db.tagDao().clearAll()
        db.changeLogDao().clear()
        seedIfEmpty()
        onDone("資料已重置為種子範例")
    }

    // ---------- 種子資料（對應藍圖） ----------
    private fun seedIfEmpty() {
        scope.launch {
            if (db.notebookDao().getAll().isNotEmpty()) return@launch
            val work = Notebook(name = "工作", color = Color.rgb(47, 190, 79).toLong(), stack = null)
            val meeting = Notebook(name = "會議", color = Color.rgb(251, 140, 0).toLong())
            val personal = Notebook(name = "個人", color = Color.rgb(142, 36, 170).toLong())
            val life = Notebook(name = "生活", color = Color.rgb(33, 150, 243).toLong())
            db.notebookDao().insert(work)
            db.notebookDao().insert(meeting)
            db.notebookDao().insert(personal)
            db.notebookDao().insert(life)
            val important = Tag(name = "重要")
            val shopping = Tag(name = "購物")
            db.tagDao().insert(important)
            db.tagDao().insert(shopping)
            db.tagDao().insert(Tag(name = "靈感"))
            db.tagDao().insert(Tag(name = "閱讀"))

            db.noteDao().insert(
                Note(
                    title = "Evernote 使用技巧",
                    content = "## 核心概念\n把靈感、任務與資料全部收進一個地方，隨時隨地**同步**取用。\n\n• 用標籤取代資料夾\n• 提醒配合行事曆\n• 搜尋支援全文檢索",
                    notebookId = work.id,
                    tagIds = listOf(important.id),
                )
            )
            db.noteDao().insert(
                Note(
                    title = "產品發佈會議記錄",
                    content = "## 決定事項\n1. 9 月發佈 v2.0：深色模式、離線筆記本\n2. 全新的同步引擎（增量 syncChunk）\n\n## 執行清單\n☐ 與設計團隊確認動畫細節\n☑ 發送會議邀請給所有參與者",
                    notebookId = meeting.id,
                    tagIds = listOf(important.id),
                    reminderAt = System.currentTimeMillis() + 26 * 3600_000L,
                )
            )
            db.noteDao().insert(
                Note(
                    title = "購物清單",
                    content = "• 牛奶\n• 雞蛋\n• 咖啡豆\n• 燕麥片\n• 堅果\n• 貓糧",
                    notebookId = life.id,
                    tagIds = listOf(shopping.id),
                    reminderAt = System.currentTimeMillis() + 5 * 3600_000L,
                )
            )

            val g = MessageThread(title = "工作群組", color = Color.rgb(47, 190, 79).toLong())
            db.messageThreadDao().insert(g)
            db.messageDao().insert(Message(threadId = g.id, sender = "小明", text = "明天 demo 準備好了嗎？", sentAt = System.currentTimeMillis() - 3600_000L))
            db.messageDao().insert(Message(threadId = g.id, sender = "我", text = "準備好了，已同步 ✅", sentAt = System.currentTimeMillis() - 1800_000L, isMine = true))
            val b = MessageThread(title = "專案 B 討論", color = Color.rgb(0, 137, 123).toLong())
            db.messageThreadDao().insert(b)
            db.messageDao().insert(Message(threadId = b.id, sender = "我", text = "會議紀錄已更新 ✅", sentAt = System.currentTimeMillis() - 7200_000L, isMine = true))

            db.recentSearchDao().insert(RecentSearch(keyword = "會議"))
            db.recentSearchDao().insert(RecentSearch(keyword = "發票"))
            db.recentSearchDao().insert(RecentSearch(keyword = "靈感"))

            db.settingsDao().insert(Settings())
        }
    }
}
