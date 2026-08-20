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
import com.evernoteclone.ui.components.Chip
import com.evernoteclone.ui.viewmodel.AppViewModel
import com.evernoteclone.ui.viewmodel.Filter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebooksScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    onOpenFilter: (Filter) -> Unit,
) {
    val notebooks by vm.notebooks.collectAsState()
    val tags by vm.tags.collectAsState()
    val notes by vm.notes.collectAsState()
    var showNew by remember { mutableStateOf(false) }
    var nbName by remember { mutableStateOf("") }
    var nbStack by remember { mutableStateOf("") }
    val trashCount by remember { mutableStateOf(0) }
    // 回收站計數
    val trash by vm.repo.trash.collectAsState(initial = emptyList())

    val counts = remember(notebooks, notes, tags) {
        val perNb = notebooks.associate { nb -> nb.id to notes.count { it.notebookId == nb.id } }
        val perTag = tags.associate { t -> t.id to notes.count { n -> t.id in n.tagIds } }
        Triple(perNb, perTag, notes.size)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("筆記本", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
            )
        },
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // 堆疊分組
            val stacks = notebooks.groupBy { it.stack ?: "__none__" }
            stacks.forEach { (stack, items) ->
                if (stack != "__none__") {
                    item { Text("▾ $stack", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) }
                }
                items(items, key = { it.id }) { nb ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onOpenFilter(Filter.notebook(nb.id)) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(Color(nb.color)))
                        Text(nb.name, fontSize = 14.sp, modifier = Modifier.padding(start = 10.dp).weight(1f))
                        Text("${counts.first[nb.id] ?: 0}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { HorizontalDivider(Modifier.padding(vertical = 10.dp)) }
            item { Text("標籤", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(bottom = 8.dp)) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tags.forEach { t ->
                        Chip("${t.name} ${counts.second[t.id] ?: 0}", onClick = { onOpenFilter(Filter.tag(t.id)) })
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { showNew = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) { Text("＋ 新增筆記本") }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().clickable { onOpenFilter(Filter.Trash) }.padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🗑 回收站", fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text("${trash.size}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("›", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                Text(
                    "統計：${counts.third} 筆記 · ${notebooks.size} 筆記本 · ${tags.size} 標籤",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }

    if (showNew) {
        AlertDialog(
            onDismissRequest = { showNew = false },
            title = { Text("新增筆記本") },
            text = {
                Column {
                    OutlinedTextField(nbName, { nbName = it }, label = { Text("名稱") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(nbStack, { nbStack = it }, label = { Text("堆疊（選填）") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (nbName.isNotBlank()) vm.repo.createNotebook(nbName.trim(), 0xFF2FBE4F, nbStack.trim().ifEmpty { null })
                    nbName = ""; nbStack = ""; showNew = false
                }) { Text("建立") }
            },
            dismissButton = { TextButton(onClick = { showNew = false }) { Text("取消") } },
        )
    }
}
