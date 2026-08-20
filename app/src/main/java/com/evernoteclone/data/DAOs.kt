package com.evernoteclone.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE inTrash = 0 ORDER BY updatedAt DESC")
    fun observeActive(): Flow<List<Note>>
    @Query("SELECT * FROM notes WHERE inTrash = 1 ORDER BY trashedAt DESC")
    fun observeTrash(): Flow<List<Note>>
    @Query("SELECT * FROM notes WHERE id = :id")
    fun observeById(id: String): Flow<Note?>
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: String): Note?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note)
    @Update
    suspend fun update(note: Note)
    @Query("UPDATE notes SET inTrash = 1, trashedAt = :t, updatedAt = :t WHERE id = :id")
    suspend fun trash(id: String, t: Long)
    @Query("UPDATE notes SET inTrash = 0, trashedAt = NULL WHERE id = :id")
    suspend fun restore(id: String)
    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: String)
    @Query("DELETE FROM notes WHERE inTrash = 1")
    suspend fun emptyTrash()
    @Query("UPDATE notes SET pinned = CASE WHEN pinned = 1 THEN 0 ELSE 1 END WHERE id = :id")
    suspend fun togglePin(id: String)
    @Query("SELECT * FROM notes WHERE inTrash = 0 AND (title LIKE '%' || :q || '%' OR content LIKE '%' || :q || '%') ORDER BY updatedAt DESC")
    fun search(q: String): Flow<List<Note>>
    @Query("SELECT COUNT(*) FROM notes WHERE inTrash = 0")
    suspend fun countActive(): Int
    @Query("SELECT * FROM notes")
    suspend fun getAllCompat(): List<Note>
    @Query("DELETE FROM notes")
    suspend fun clearAll()
}

@Dao
interface NotebookDao {
    @Query("SELECT * FROM notebooks ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<Notebook>>
    @Query("SELECT * FROM notebooks")
    suspend fun getAll(): List<Notebook>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notebook: Notebook)
    @Query("DELETE FROM notebooks WHERE id = :id")
    suspend fun delete(id: String)
    @Query("DELETE FROM notebooks")
    suspend fun clearAll()
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<Tag>>
    @Query("SELECT * FROM tags")
    suspend fun getAll(): List<Tag>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: Tag)
    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun delete(id: String)
    @Query("DELETE FROM tags")
    suspend fun clearAll()
}

@Dao
interface RecentSearchDao {
    @Query("SELECT * FROM recent_searches ORDER BY updatedAt DESC LIMIT 10")
    fun observeRecent(): Flow<List<RecentSearch>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recent: RecentSearch)
    @Query("DELETE FROM recent_searches")
    suspend fun clear()
}

@Dao
interface MessageThreadDao {
    @Query("SELECT * FROM message_threads ORDER BY lastAt DESC")
    fun observeAll(): Flow<List<MessageThread>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(thread: MessageThread)
    @Query("UPDATE message_threads SET lastMessage = :last, lastAt = :t WHERE id = :id")
    suspend fun touch(id: String, last: String, t: Long)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE threadId = :threadId ORDER BY sentAt ASC")
    fun observeByThread(threadId: String): Flow<List<Message>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: Message)
    @Query("UPDATE messages SET savedToNote = 1 WHERE id = :id")
    suspend fun markSaved(id: String)
}

@Dao
interface ChangeLogDao {
    @Query("SELECT * FROM change_log WHERE synced = 0")
    suspend fun pending(): List<ChangeLog>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: ChangeLog)
    @Query("UPDATE change_log SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
    @Query("DELETE FROM change_log")
    suspend fun clear()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 'default'")
    fun observe(): Flow<Settings?>
    @Query("SELECT * FROM settings WHERE id = 'default'")
    suspend fun get(): Settings?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: Settings)
    @Query("UPDATE settings SET pendingSyncCount = pendingSyncCount + 1")
    suspend fun bumpPending()
    @Query("UPDATE settings SET pendingSyncCount = 0, lastSyncAt = :t")
    suspend fun markSynced(t: Long)
    @Query("UPDATE settings SET darkMode = :v")
    suspend fun setDarkMode(v: Boolean)
    @Query("UPDATE settings SET pinEnabled = :enabled, pinCode = :code")
    suspend fun setPin(enabled: Boolean, code: String)
    @Query("UPDATE settings SET notificationsEnabled = :v")
    suspend fun setNotifications(v: Boolean)
    @Query("UPDATE settings SET viewMode = :v")
    suspend fun setViewMode(v: String)
    @Query("UPDATE settings SET sortMode = :v")
    suspend fun setSortMode(v: String)
    @Query("UPDATE settings SET fontScale = :v")
    suspend fun setFontScale(v: Float)
}
