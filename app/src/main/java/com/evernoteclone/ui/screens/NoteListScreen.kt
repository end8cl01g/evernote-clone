package com.evernoteclone.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evernoteclone.data.Note
import com.evernoteclone.ui.components.Chip
import com.evernoteclone.ui.components.EmptyState
import com.evernoteclone.ui.components.NoteCard
import com.evernoteclone.ui.viewmodel.AppViewModel
import com.evernoteclone.ui.viewmodel.Filter
import com.evernoteclone.util.Fmt
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    vm: AppViewModel,
    onOpenNote: (String) -> Unit,
    onNewNote: () -> Unit,
    onSearch: () -> Unit,
    onOpenNotebooks: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenCamera: () -> Unit,
    onMenu: () -> Unit,
) {
    val notes by vm.notes.collectAsState()
    val notebooks by vm.notebooks.collectAsState()
    val tags by vm.tags.collectAsState()
    val settings by vm.settings.collectAsState()
    val filter by vm.filter.collectAsState()
    val scope = rememberCoroutineScope()
    var syncing by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }

    val sorted = vm.sort(notes, settings?.sortMode ?: "updated")
    val viewMode = settings?.viewMode ?: "list"
    val tagName = { id: String -> tags.firstOrNull { it.id == id }?.name ?: "" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("筆記", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onMenu) { Text("☰", fontSize = 20.sp) }
                },
                actions = {
                    IconButton(onClick = {
                        syncing = true
                        vm.repo.syncNow { scope.launch { kotlinx.coroutines.delay(700); syncing = false } }
                    }) { Icon(Icons.Default.Sync, contentDescription = "同步") }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewNote) { Icon(Icons.Default.Add, contentDescription = "新增") }
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            // 搜索列
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()
                    .clickable(onClick = onSearch),
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 8.dp))
                    Text("搜尋筆記…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
            // 過濾 chips
            Row(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Chip("全部", filter.kind == "all") { vm.setFilter(Filter.All) }
                Chip("提醒", filter.kind == "reminders") { vm.setFilter(Filter.Reminders) }
                Chip("筆記本", filter.kind == "notebook") { onOpenNotebooks() }
                Chip("標籤", filter.kind == "tag") { onOpenNotebooks() }
                Chip("回收站", filter.kind == "trash") { vm.setFilter(Filter.Trash) }
            }
            // 排序 / 視圖 / 捷徑
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("排序 ▾", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { showSort = true })
                Text(
                    "☷ ${if (viewMode == "grid") "網格" else "列表"}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable {
                        vm.repo.setViewMode(if (viewMode == "list") "grid" else "list")
                    },
                )
                Text("📌 捷徑：", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                tags.take(3).forEach {
                    Text(it.name, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { vm.setFilter(Filter.tag(it.id)) })
                }
            }
            // 列表 / 網格
            if (sorted.isEmpty()) {
                EmptyState("尚無筆記，點右下角 ＋ 開始建立")
            } else if (viewMode == "grid") {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(sorted, key = { it.id }) { n ->
                        NoteCard(n, notebooks.firstOrNull { it.id == n.notebookId }, n.tagIds.map(tagName)) {
                            onOpenNote(n.id)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(sorted, key = { it.id }) { n ->
                        NoteCard(
                            n, notebooks.firstOrNull { it.id == n.notebookId }, n.tagIds.map(tagName),
                            onClick = { onOpenNote(n.id) },
                            onLongClick = { vm.repo.togglePin(n.id) },
                        )
                    }
                    if (filter.kind == "trash") {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = { vm.repo.emptyTrash() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                                    Text("清空回收站")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSort) {
        AlertDialog(
            onDismissRequest = { showSort = false },
            title = { Text("排序方式") },
            text = {
                Column {
                    listOf("updated" to "更新時間", "created" to "建立時間", "title" to "標題", "reminder" to "提醒時間").forEach { (k, l) ->
                        Text(
                            (if (settings?.sortMode == k) "✓ " else "") + l,
                            fontSize = 15.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { vm.repo.setSortMode(k); showSort = false }
                                .padding(vertical = 10.dp),
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSort = false }) { Text("關閉") } },
        )
    }
}
