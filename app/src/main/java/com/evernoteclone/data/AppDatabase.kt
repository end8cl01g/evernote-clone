package com.evernoteclone.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import org.json.JSONArray

class Converters {
    @TypeConverter
    fun fromStringList(list: List<String>): String = JSONArray(list).toString()

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isEmpty()) emptyList()
        else {
            val arr = JSONArray(value)
            (0 until arr.length()).map { arr.getString(it) }
        }
}

@Database(
    entities = [
        Note::class, Notebook::class, Tag::class, RecentSearch::class,
        MessageThread::class, Message::class, ChangeLog::class, Settings::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun notebookDao(): NotebookDao
    abstract fun tagDao(): TagDao
    abstract fun recentSearchDao(): RecentSearchDao
    abstract fun messageThreadDao(): MessageThreadDao
    abstract fun messageDao(): MessageDao
    abstract fun changeLogDao(): ChangeLogDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "evernote.db",
                ).build().also { INSTANCE = it }
            }
    }
}
