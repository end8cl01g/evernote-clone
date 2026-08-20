package com.evernoteclone.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Fmt {
    fun relTime(t: Long): String {
        val d = System.currentTimeMillis() - t
        return when {
            d < 60_000L -> "剛剛"
            d < 3_600_000L -> "${d / 60_000L} 分鐘前"
            d < 86_400_000L -> "${d / 3_600_000L} 小時前"
            d < 172_800_000L -> "昨天"
            else -> SimpleDateFormat("M/d", Locale.getDefault()).format(Date(t))
        }
    }

    fun reminderLabel(at: Long): String {
        val d = Date(at)
        return SimpleDateFormat("M/d HH:mm", Locale.getDefault()).format(d)
    }

    fun snippet(note: com.evernoteclone.data.Note): String {
        val first = note.content.lineSequence().firstOrNull { it.isNotBlank() } ?: ""
        return first
            .replaceFirst(Regex("^[#•\\-☐☑]\\s*"), "")
            .replace("**", "")
            .take(36)
            .ifEmpty { "（無內容）" }
    }

    fun stats(note: com.evernoteclone.data.Note): String {
        val words = note.content.trim().split(Regex("\\s+")).size
        return "$words 字"
    }
}
