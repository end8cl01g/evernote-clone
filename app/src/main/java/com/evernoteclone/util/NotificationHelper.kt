package com.evernoteclone.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.evernoteclone.MainActivity

/** 真實提醒：AlarmManager 排程 → 系統通知（App 關閉也觸發） */
object NotificationHelper {
    const val CHANNEL_ID = "reminders"
    const val ACTION_REMINDER = "com.evernoteclone.action.REMINDER"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "筆記提醒", NotificationManager.IMPORTANCE_HIGH)
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    fun scheduleReminder(context: Context, noteId: String, title: String, at: Long) {
        val am = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra("note_id", noteId)
            putExtra("note_title", title)
        }
        val pi = PendingIntent.getBroadcast(
            context, noteId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // setWindow（不精確）避免 API 31+ 精確鬧鐘權限問題
        am.setWindow(AlarmManager.RTC_WAKEUP, at, 60_000L, pi)
    }

    fun cancelReminder(context: Context, noteId: String) {
        val am = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra("note_id", noteId)
        }
        val pi = PendingIntent.getBroadcast(
            context, noteId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        am.cancel(pi)
    }

    fun show(context: Context, id: String, title: String, body: String) {
        ensureChannel(context)
        val nm = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        val intent = Intent(context, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            context, id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        nm.notify(id.hashCode(), notif)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("note_id") ?: return
        val title = intent.getStringExtra("note_title") ?: "（無標題）"
        NotificationHelper.show(context, id, "⏰ 筆記提醒", title)
    }
}
