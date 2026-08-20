package com.evernoteclone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.evernoteclone.ui.screens.*
import com.evernoteclone.ui.theme.EvernoteCloneTheme
import com.evernoteclone.ui.viewmodel.AppViewModel
import com.evernoteclone.ui.viewmodel.Filter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { EvernoteCloneApp() }
    }
}

@Composable
fun EvernoteCloneApp() {
    val vm: AppViewModel = viewModel()
    val nav = rememberNavController()
    val settings by vm.settings.collectAsState()
    val dark = settings?.darkMode == true
    var unlocked by remember { mutableStateOf(!(settings?.pinEnabled == true)) }
    var drawerOpen by remember { mutableStateOf(false) }

    EvernoteCloneTheme(darkTheme = dark) {
        Surface(Modifier.fillMaxSize()) {
            Box {
                NavHost(nav, startDestination = "list") {
                    composable("list") {
                        NoteListScreen(
                            vm = vm,
                            onOpenNote = { nav.navigate("view/$it") },
                            onNewNote = { nav.navigate("editor") },
                            onSearch = { nav.navigate("search") },
                            onOpenNotebooks = { nav.navigate("notebooks") },
                            onOpenSettings = { nav.navigate("settings") },
                            onOpenMessages = { nav.navigate("messages") },
                            onOpenCamera = { nav.navigate("camera/photo") },
                            onMenu = { drawerOpen = true },
                        )
                    }
                    composable(
                        "editor?noteId={noteId}",
                        arguments = listOf(navArgument("noteId") { type = NavType.StringType; nullable = true; defaultValue = null }),
                    ) {
                        val noteId = it.arguments?.getString("noteId")
                        NoteEditorScreen(
                            vm = vm,
                            noteId = noteId,
                            onBack = { nav.popBackStack() },
                            onOpenCamera = { nav.navigate("camera/$it") },
                        )
                    }
                    composable(
                        "view/{noteId}",
                        arguments = listOf(navArgument("noteId") { type = NavType.StringType }),
                    ) {
                        NoteViewScreen(
                            vm = vm,
                            noteId = it.arguments?.getString("noteId") ?: "",
                            onBack = { nav.popBackStack() },
                            onEdit = { nav.navigate("editor?noteId=$it") },
                            onOpenCamera = {},
                        )
                    }
                    composable("search") {
                        SearchScreen(vm = vm, onBack = { nav.popBackStack() }, onOpenNote = { nav.navigate("view/$it") })
                    }
                    composable("notebooks") {
                        NotebooksScreen(
                            vm = vm,
                            onBack = { nav.popBackStack() },
                            onOpenFilter = { f ->
                                vm.setFilter(f)
                                nav.navigate("list") {
                                    popUpTo("list") { inclusive = true }
                                }
                            },
                        )
                    }
                    composable("settings") {
                        SettingsScreen(vm = vm, onBack = { nav.popBackStack() })
                    }
                    composable("messages") {
                        MessagesScreen(vm = vm, threadId = null, onBack = { nav.popBackStack() })
                    }
                    composable(
                        "messages/{threadId}",
                        arguments = listOf(navArgument("threadId") { type = NavType.StringType }),
                    ) {
                        MessagesScreen(vm = vm, threadId = it.arguments?.getString("threadId"), onBack = { nav.popBackStack() })
                    }
                    composable(
                        "camera/{mode}",
                        arguments = listOf(navArgument("mode") { type = NavType.StringType }),
                    ) {
                        CameraScreen(
                            vm = vm,
                            mode = it.arguments?.getString("mode") ?: "photo",
                            onBack = { nav.popBackStack() },
                        )
                    }
                }

                // PIN 鎖（真實門禁）
                if (settings?.pinEnabled == true && !unlocked) {
                    PinLockScreen(pinCode = settings?.pinCode ?: "", onUnlock = { unlocked = true })
                }

                // 自製導航抽屜（無依賴）
                if (drawerOpen) {
                    Box(
                        Modifier.fillMaxSize().background(Color(0x66000000))
                            .clickable { drawerOpen = false },
                    )
                    Surface(
                        Modifier.fillMaxHeight().width(280.dp).align(Alignment.CenterStart),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 8.dp,
                    ) {
                        val notebooks by vm.notebooks.collectAsState()
                        val tags by vm.tags.collectAsState()
                        Column(Modifier.padding(20.dp).statusBarsPadding()) {
                            Text("📌 捷徑", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
                            tags.take(5).forEach {
                                Text("🏷 ${it.name}", fontSize = 15.sp, modifier = Modifier.fillMaxWidth().clickable {
                                    vm.setFilter(Filter.tag(it.id)); drawerOpen = false
                                    nav.navigate("list") { popUpTo("list") { inclusive = true } }
                                }.padding(vertical = 10.dp))
                            }
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            listOf(
                                "🗒 全部筆記" to { vm.setFilter(Filter.All) },
                                "⏰ 提醒" to { vm.setFilter(Filter.Reminders) },
                                "🗑 回收站" to { vm.setFilter(Filter.Trash) },
                                "📁 筆記本" to {},
                                "💬 訊息" to {},
                                "⚙️ 設定" to {},
                            ).forEach { (label, act) ->
                                Text(label, fontSize = 15.sp, modifier = Modifier.fillMaxWidth().clickable {
                                    when (label) {
                                        "📁 筆記本" -> nav.navigate("notebooks")
                                        "💬 訊息" -> nav.navigate("messages")
                                        "⚙️ 設定" -> nav.navigate("settings")
                                        else -> act()
                                    }
                                    drawerOpen = false
                                    if (label in listOf("🗒 全部筆記", "⏰ 提醒", "🗑 回收站")) {
                                        nav.navigate("list") { popUpTo("list") { inclusive = true } }
                                    }
                                }.padding(vertical = 12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
