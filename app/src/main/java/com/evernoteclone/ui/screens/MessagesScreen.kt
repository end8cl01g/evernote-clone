package com.evernoteclone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evernoteclone.ui.viewmodel.AppViewModel
import com.evernoteclone.util.Fmt
import com.evernoteclone.ui.theme.Blue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(vm: AppViewModel, threadId: String?, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val threads by vm.threads.collectAsState()
    var input by remember { mutableStateOf("") }
    var showNew by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    val messages by (threadId?.let { vm.messagesFor(it).collectAsState(initial = emptyList()) }
        ?: remember { mutableStateOf<List<com.evernoteclone.data.Message>>(emptyList()) })

    val active = threads.firstOrNull { it.id == threadId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(active?.title ?: "訊息", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (threadId != null) vm.setSelectedThread(null) else onBack()
                    }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
                actions = {
                    if (threadId == null) {
                        TextButton(onClick = { showNew = true }) { Text("＋ 新訊息") }
                    }
                },
            )
        },
    ) { pad ->
        if (threadId == null) {
            LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (threads.isEmpty()) item { Text("尚無訊息線程") }
                items(threads, key = { it.id }) { t ->
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.setSelectedThread(t.id) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(Color(t.color)), contentAlignment = Alignment.Center) {
                            Text(t.title.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(t.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(t.lastMessage, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                        Text(Fmt.relTime(t.lastAt), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(pad)) {
                LazyColumn(
                    Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(messages, key = { it.id }) { m ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = if (m.isMine) Arrangement.End else Arrangement.Start,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp, 14.dp, if (m.isMine) 4.dp else 14.dp, if (m.isMine) 14.dp else 4.dp),
                                color = if (m.isMine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .widthIn(max = 280.dp)
                                    .clickable {
                                        if (!m.savedToNote) {
                                            vm.repo.saveMessageToNote(m.threadId, m)
                                            scope.launch { }
                                        }
                                    },
                            ) {
                                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Text(m.text, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        if (m.savedToNote) "已存為筆記 · ${Fmt.relTime(m.sentAt)}" else "長按存為筆記 · ${Fmt.relTime(m.sentAt)}",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
                Surface(shadowElevation = 8.dp) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            input, { input = it },
                            placeholder = { Text("輸入訊息…") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            if (input.isNotBlank()) { vm.repo.sendMessage(threadId, input.trim()); input = "" }
                        }) { Text("➤") }
                    }
                }
            }
        }
    }

    if (showNew) {
        AlertDialog(
            onDismissRequest = { showNew = false },
            title = { Text("新訊息線程") },
            text = {
                OutlinedTextField(newTitle, { newTitle = it }, label = { Text("線程名稱") }, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newTitle.isNotBlank()) vm.repo.createThread(newTitle.trim(), Blue.value.toLong())
                    newTitle = ""; showNew = false
                }) { Text("建立") }
            },
            dismissButton = { TextButton(onClick = { showNew = false }) { Text("取消") } },
        )
    }
}
