package com.evernoteclone.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evernoteclone.data.Note
import com.evernoteclone.ui.theme.Amber
import com.evernoteclone.ui.theme.Green
import com.evernoteclone.util.Fmt
import com.evernoteclone.util.NotificationHelper
import com.evernoteclone.ui.viewmodel.AppViewModel
import android.media.MediaPlayer

/** 輕量標記渲染：**粗體**、## 標題、• 列表、☐/☑ 待辦 */
@Composable
private fun RichContent(content: String, fontScale: Float) {
    val base = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp * fontScale, lineHeight = 24.sp * fontScale)
    Column {
        content.lines().forEach { line ->
            val parts = line.split("**")
            when {
                line.startsWith("## ") -> Text(line.removePrefix("## "), fontWeight = FontWeight.Bold, fontSize = 16.sp * fontScale, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                line.startsWith("• ") || line.startsWith("- ") ->
                    Row(Modifier.fillMaxWidth()) {
                        Text("•  ", color = Green, style = base)
                        parts.forEachIndexed { i, p -> if (i % 2 == 1) Text(p, style = base, fontWeight = FontWeight.Bold) else Text(p, style = base) }
                    }
                line.startsWith("☑ ") -> Text(line, style = base, color = Green)
                line.startsWith("> ") -> Text(line.removePrefix("> "), style = base, color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                line.isBlank() -> Spacer(Modifier.height(6.dp))
                else -> parts.forEachIndexed { i, p -> if (i % 2 == 1) Text(p, style = base, fontWeight = FontWeight.Bold) else Text(p, style = base) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteViewScreen(vm: AppViewModel, noteId: String, onBack: () -> Unit, onEdit: (String) -> Unit, onOpenCamera: (String) -> Unit) {
    val context = LocalContext.current
    val note by vm.noteFor(noteId).collectAsState(initial = null)
    val notebooks by vm.notebooks.collectAsState()
    val tags by vm.tags.collectAsState()
    val settings by vm.settings.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }

    if (note == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("筆記不存在或已刪除") }
        return
    }
    val n = note!!
    val nb = notebooks.firstOrNull { it.id == n.notebookId }
    val scale = settings?.fontScale ?: 1f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("筆記") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = { IconButton(onClick = { showMenu = true }) { Text("⋯", fontSize = 20.sp) } },
            )
        },
        bottomBar = {
            Surface(shadowElevation = 4.dp) {
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                    listOf(
                        "✎" to "編輯" to { onEdit(n.id) },
                        "↗" to "分享" to {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "${n.title}\n\n${n.content}")
                            }
                            context.startActivity(Intent.createChooser(send, "分享筆記"))
                        },
                        "🗑" to (if (n.inTrash) "還原" else "刪除") to {
                            if (n.inTrash) vm.repo.restoreNote(n.id) else vm.repo.trashNote(n.id)
                            onBack()
                        },
                    ).forEach { (g, l) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f).clickable(onClick = g.second),
                        ) {
                            Text(g.first.first, fontSize = 16.sp, color = if (g.first.second == "刪除") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                            Text(g.first.second, fontSize = 11.sp, color = if (g.first.second == "刪除") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(16.dp)) {
            // 字體 A±
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("字體 A−  A＋", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row {
                    Text("A−", modifier = Modifier.clickable { vm.repo.setFontScale((scale - 0.1f).coerceAtLeast(0.8f)) }.padding(horizontal = 8.dp))
                    Text("A＋", modifier = Modifier.clickable { vm.repo.setFontScale((scale + 0.1f).coerceAtMost(1.5f)) }.padding(horizontal = 8.dp))
                }
            }
            Text(n.title.ifEmpty { "（無標題）" }, fontSize = 20.sp * scale, fontWeight = FontWeight.Bold)
            // 元資料
            Row(Modifier.padding(top = 6.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(nb?.name ?: "未分類", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("· ${Fmt.relTime(n.updatedAt)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (n.reminderAt != null) Text("· ⏰ ${Fmt.reminderLabel(n.reminderAt!!)}", fontSize = 11.sp, color = Amber)
            }
            HorizontalDivider(Modifier.padding(bottom = 12.dp))
            RichContent(n.content, scale)
            // 附件（真實圖片 URI）
            n.resources.forEach { r ->
                val parts = r.split("|")
                val uri = parts.getOrNull(2)
                when (parts[0]) {
                    "image" -> {
                        val bmp = rememberBitmap(uri)
                        if (bmp != null) {
                            Image(
                                bitmap = bmp,
                                contentDescription = parts.getOrElse(1) { "圖片" },
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).heightIn(max = 240.dp),
                            )
                        }
                    }
                    "audio" -> Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clickable {
                            uri?.let { playAudio(it, context) }
                        },
                    ) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Text("🎙 ${parts.getOrElse(1) { "錄音" }}  ▶", fontSize = 13.sp) } }
                }
            }
        }
    }

    if (showMenu) {
        AlertDialog(
            onDismissRequest = { showMenu = false },
            title = { Text("筆記操作") },
            text = {
                Column {
                    listOf(
                        "📌 ${if (n.pinned) "取消釘選" else "釘選到頂部"}" to { vm.repo.togglePin(n.id) },
                        "ℹ️ 筆記資訊" to { showMenu = false; showInfo = true },
                        "⏰ 提醒（立即通知測試）" to {
                            NotificationHelper.scheduleReminder(context, n.id, n.title, System.currentTimeMillis() + 5_000L)
                            showMenu = false
                        },
                    ).forEach { (l, a) ->
                        Text(l, modifier = Modifier.fillMaxWidth().clickable(a).padding(vertical = 8.dp))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showMenu = false }) { Text("關閉") } },
        )
    }
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("筆記資訊") },
            text = {
                Column {
                    Text("建立：${java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(n.createdAt))}")
                    Text("更新：${java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(n.updatedAt))}")
                    Text("內容：${Fmt.stats(n)}")
                    Text("附件：${n.resources.size} 個")
                    Text("標籤：${n.tagIds.map { id -> tags.firstOrNull { it.id == id }?.name ?: "" }.joinToString("、")}")
                }
            },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text("關閉") } },
        )
    }
}

@Composable
private fun rememberBitmap(uri: String?): ImageBitmap? {
    if (uri == null) return null
    val context = LocalContext.current
    return remember(uri) {
        try {
            val input = context.contentResolver.openInputStream(android.net.Uri.parse(uri)) ?: return@remember null
            val bmp = android.graphics.BitmapFactory.decodeStream(input)
            input.close()
            bmp.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
}

private fun playAudio(uri: String, context: android.content.Context) {
    try {
        val mp = MediaPlayer()
        mp.setDataSource(context, android.net.Uri.parse(uri))
        mp.setOnCompletionListener { it.release() }
        mp.prepare()
        mp.start()
    } catch (e: Exception) { android.widget.Toast.makeText(context, "播放失敗: ${e.message}", android.widget.Toast.LENGTH_SHORT).show() }
}
