package com.evernoteclone.ui.screens

import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evernoteclone.data.Note
import com.evernoteclone.ui.components.Chip
import com.evernoteclone.ui.theme.Amber
import com.evernoteclone.ui.theme.AmberBg
import com.evernoteclone.ui.theme.Green
import com.evernoteclone.util.Fmt
import com.evernoteclone.util.NotificationHelper
import com.evernoteclone.ui.viewmodel.AppViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    vm: AppViewModel,
    noteId: String?,
    onBack: () -> Unit,
    onOpenCamera: (mode: String) -> Unit,
) {
    val context = LocalContext.current
    val notebooks by vm.notebooks.collectAsState()
    val tags by vm.tags.collectAsState()
    val existing by if (noteId != null) {
        vm.noteFor(noteId).collectAsState(initial = null)
    } else {
        remember { mutableStateOf<Note?>(null) }
    }

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var notebookId by remember { mutableStateOf<String?>(null) }
    var tagIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var resources by remember { mutableStateOf<List<String>>(emptyList()) }
    var reminderAt by remember { mutableStateOf<Long?>(null) }

    // 載入既有筆記（非同步到達）
    LaunchedEffect(existing?.id) {
        existing?.let {
            title = it.title
            content = it.content
            notebookId = it.notebookId
            tagIds = it.tagIds
            resources = it.resources
            reminderAt = it.reminderAt
        }
    }

    var showNb by remember { mutableStateOf(false) }
    var showTags by remember { mutableStateOf(false) }
    var showReminder by remember { mutableStateOf(false) }
    var showRecord by remember { mutableStateOf(false) }
    var newNbName by remember { mutableStateOf("") }
    var newNbStack by remember { mutableStateOf("") }
    var newTagName by remember { mutableStateOf("") }
    // 相機/相簿回傳
    val pending by vm.pendingAttachment.collectAsState()
    LaunchedEffect(pending) {
        pending?.let { uri ->
            resources = resources + "image|IMG_${System.currentTimeMillis()}.jpg|$uri"
            vm.setPendingAttachment(null)
        }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { resources = resources + "image|相簿照片|$it" }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { resources = resources + "file|附件|$it" }
    }

    val save = {
        val note = Note(
            id = noteId ?: "",
            title = title,
            content = content,
            notebookId = notebookId,
            tagIds = tagIds,
            resources = resources,
            reminderAt = reminderAt,
        )
        vm.repo.saveNote(note) { id ->
            // 真實系統通知排程
            val s = vm.settings.value
            if (s?.notificationsEnabled == true) {
                if (reminderAt != null) NotificationHelper.scheduleReminder(context, id, title, reminderAt!!)
                else if (noteId != null) NotificationHelper.cancelReminder(context, noteId)
            }
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteId == null) "新筆記" else "編輯筆記") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
                actions = {
                    Button(onClick = save, modifier = Modifier.padding(end = 8.dp)) { Text("儲存") }
                },
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState())) {
            // 標題
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("標題") },
                textStyle = LocalTextStyle.current.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            // 筆記本
            Row(Modifier.fillMaxWidth().clickable { showNb = true }.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("筆記本", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(52.dp))
                Text(notebooks.firstOrNull { it.id == notebookId }?.name ?: "未分類", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Text(" ▾", color = MaterialTheme.colorScheme.primary)
            }
            // 標籤
            Row(Modifier.fillMaxWidth().clickable { showTags = true }.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("標籤", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(52.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    tagIds.forEach { id -> tags.firstOrNull { it.id == id }?.let { Chip(it.name, active = true) } }
                    Text("＋ 標籤", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            // 格式工具列
            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("B" to "**", "I" to "*", "H1" to "\n## ", "≡" to "\n• ", "☑" to "\n☐ ", "❝" to "\n> ").forEach { (label, mark) ->
                        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.clickable {
                                content = content + mark
                            }.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                }
            }
            // 內容
            BasicTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, lineHeight = 22.sp),
                decorationBox = { inner ->
                    Box {
                        if (content.isEmpty()) Text("開始輸入…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        inner()
                    }
                },
            )
            // 附件預覽
            if (resources.isNotEmpty()) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    resources.forEachIndexed { i, r ->
                        val parts = r.split("|")
                        Chip(
                            "${if (parts[0] == "image") "🖼" else if (parts[0] == "audio") "🎙" else "📎"} ${parts.getOrElse(1) { "附件" }} ✕",
                            onClick = { resources = resources.filterIndexed { j, _ -> j != i } },
                        )
                    }
                }
            }
            // 提醒
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AmberBg,
                modifier = Modifier.padding(16.dp).fillMaxWidth().clickable { showReminder = true },
            ) {
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("⏰ 提醒", color = Amber, fontWeight = FontWeight.SemiBold)
                    Text(reminderAt?.let { Fmt.reminderLabel(it) } ?: "無提醒 ▾", color = Amber, fontWeight = FontWeight.SemiBold)
                }
            }
            // 附件列
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(
                    "📷" to "拍照" to { onOpenCamera("photo") },
                    "🖼" to "相簿" to { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    "🎙" to "錄音" to { showRecord = true },
                    "📎" to "檔案" to { filePicker.launch(arrayOf("*/*")) },
                ).forEach { (g, l) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .clickable(onClick = g.second)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Text(g.first.first, fontSize = 18.sp)
                        Text(g.first.second, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text(Fmt.stats(Note(title = title, content = content)), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
        }
    }

    // 筆記本選擇
    if (showNb) {
        AlertDialog(
            onDismissRequest = { showNb = false },
            title = { Text("選擇筆記本") },
            text = {
                Column {
                    notebooks.forEach {
                        Text((if (it.id == notebookId) "✓ " else "") + it.name, modifier = Modifier.fillMaxWidth().clickable { notebookId = it.id; showNb = false }.padding(vertical = 8.dp))
                    }
                    HorizontalDivider()
                    OutlinedTextField(newNbName, { newNbName = it }, label = { Text("新筆記本名稱") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(newNbStack, { newNbStack = it }, label = { Text("堆疊（選填）") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newNbName.isNotBlank()) { vm.repo.createNotebook(newNbName.trim(), 0xFF2FBE4F, newNbStack.trim().ifEmpty { null }); newNbName = ""; newNbStack = "" }
                    showNb = false
                }) { Text("新增") }
            },
            dismissButton = { TextButton(onClick = { showNb = false }) { Text("取消") } },
        )
    }
    // 標籤選擇
    if (showTags) {
        AlertDialog(
            onDismissRequest = { showTags = false },
            title = { Text("選擇標籤") },
            text = {
                Column {
                    tags.forEach {
                        Text((if (it.id in tagIds) "✓ " else "") + it.name, modifier = Modifier.fillMaxWidth().clickable {
                            tagIds = if (it.id in tagIds) tagIds - it.id else tagIds + it.id
                        }.padding(vertical = 6.dp))
                    }
                    OutlinedTextField(newTagName, { newTagName = it }, label = { Text("新標籤") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newTagName.isNotBlank()) vm.repo.createTag(newTagName.trim())
                    newTagName = ""
                    showTags = false
                }) { Text("新增") }
            },
            dismissButton = { TextButton(onClick = { showTags = false }) { Text("關閉") } },
        )
    }
    // 提醒
    if (showReminder) {
        AlertDialog(
            onDismissRequest = { showReminder = false },
            title = { Text("設定提醒") },
            text = {
                Column {
                    listOf(
                        "無提醒" to null,
                        "1 小時後" to (System.currentTimeMillis() + 3600_000L),
                        "明天 09:00" to (System.currentTimeMillis() + 26 * 3600_000L),
                        "明天 18:00" to (System.currentTimeMillis() + 30 * 3600_000L),
                    ).forEach { (l, t) ->
                        Text(l, modifier = Modifier.fillMaxWidth().clickable { reminderAt = t; showReminder = false }.padding(vertical = 8.dp))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showReminder = false }) { Text("關閉") } },
        )
    }
    // 錄音
    if (showRecord) {
        RecordDialog(
            onDismiss = { showRecord = false },
            onSaved = { uri, name -> resources = resources + "audio|$name|$uri"; showRecord = false },
        )
    }
}

@Composable
private fun RecordDialog(onDismiss: () -> Unit, onSaved: (String, String) -> Unit) {
    val context = LocalContext.current
    var recording by remember { mutableStateOf(false) }
    var filePath by remember { mutableStateOf<String?>(null) }
    var elapsed by remember { mutableStateOf(0) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    LaunchedEffect(recording) {
        while (recording) { kotlinx.coroutines.delay(1000); elapsed++ }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (recording) "錄音中… ${elapsed}s" else if (filePath != null) "錄音完成" else "錄音") },
        text = {
            Column {
                if (!recording && filePath == null) {
                    Button(onClick = {
                        val f = File(context.cacheDir, "rec_${System.currentTimeMillis()}.m4a")
                        val mr = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
                        mr.setAudioSource(MediaRecorder.AudioSource.MIC)
                        mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                        mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                        mr.setOutputFile(f.absolutePath)
                        mr.prepare()
                        mr.start()
                        recorder = mr
                        filePath = f.absolutePath
                        recording = true
                    }) { Text("● 開始錄音") }
                }
                if (recording) {
                    Button(onClick = {
                        recorder?.stop(); recorder?.release(); recorder = null
                        recording = false
                    }) { Text("■ 停止") }
                }
                if (filePath != null && !recording) {
                    Button(onClick = { onSaved(filePath!!, File(filePath!!).name) }, colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("✓ 儲存") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(if (filePath != null) "捨棄" else "取消") } },
    )
}
