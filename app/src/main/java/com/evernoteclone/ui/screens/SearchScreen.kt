package com.evernoteclone.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evernoteclone.ui.components.Chip
import com.evernoteclone.ui.components.EmptyState
import com.evernoteclone.ui.components.NoteCard
import com.evernoteclone.ui.theme.Green
import com.evernoteclone.ui.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(vm: AppViewModel, onBack: () -> Unit, onOpenNote: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var q by remember { mutableStateOf("") }
    val results by vm.searchResults.collectAsState()
    val recents by vm.recents.collectAsState()
    val notebooks by vm.notebooks.collectAsState()
    val tags by vm.tags.collectAsState()
    val tagName = { id: String -> tags.firstOrNull { it.id == id }?.name ?: "" }

    // 防抖搜索（真實語法 notebook:/tag:）
    LaunchedEffect(q) {
        if (q.isBlank()) { vm.clearSearch(); return@LaunchedEffect }
        delay(300)
        vm.runSearch(q)
        if (q.trim().isNotEmpty()) vm.repo.upsertRecent(q.trim())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = q,
                        onValueChange = { q = it },
                        placeholder = { Text("搜尋筆記…（notebook:工作  tag:重要）", fontSize = 13.sp) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
                actions = {
                    if (q.isNotEmpty()) {
                        IconButton(onClick = { q = "" }) { Icon(Icons.Default.Clear, "清除") }
                    }
                },
            )
        },
    ) { pad ->
        if (q.isBlank()) {
            LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp)) {
                item { Text("最近搜尋", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                items(recents, key = { it.keyword }) { r ->
                    Text("🕘 ${r.keyword}", fontSize = 14.sp, modifier = Modifier.fillMaxWidth().clickable { q = r.keyword }.padding(vertical = 12.dp))
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text("找到 ${results.size} 則筆記", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (results.isEmpty()) {
                    item { EmptyState("沒有符合「$q」的筆記") }
                }
                items(results, key = { it.id }) { n ->
                    NoteCard(n, notebooks.firstOrNull { it.id == n.notebookId }, n.tagIds.map(tagName)) { onOpenNote(n.id) }
                }
            }
        }
    }
}
