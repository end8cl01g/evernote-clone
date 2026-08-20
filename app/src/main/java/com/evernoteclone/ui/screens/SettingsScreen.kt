package com.evernoteclone.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evernoteclone.ui.theme.Amber
import com.evernoteclone.ui.theme.Blue
import com.evernoteclone.ui.theme.Green
import com.evernoteclone.ui.theme.Red
import com.evernoteclone.ui.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsState()
    val s = settings
    var showPin by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinStep by remember { mutableStateOf(0) } // 0=輸入 1=確認
    var firstPin by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            try {
                val json = vm.repo.exportBackup()
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                Toast.makeText(context, "備份已匯出", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "匯出失敗: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                val (ok, msg) = vm.repo.importBackup(json)
                Toast.makeText(context, if (ok) "匯入成功：$msg" else "匯入失敗：$msg", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "匯入失敗: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val reset = {
        android.app.AlertDialog.Builder(context)
            .setTitle("清除資料")
            .setMessage("將刪除所有資料並恢復種子範例，確定？")
            .setPositiveButton("清除") { _, _ ->
                vm.repo.resetData { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
            )
        },
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // 深色模式
            item {
                SettingRow("深色模式") {
                    Switch(
                        checked = s?.darkMode == true,
                        onCheckedChange = { vm.repo.setDarkMode(it) },
                    )
                }
            }
            // 密碼鎖
            item {
                SettingRow("密碼鎖", value = if (s?.pinEnabled == true) "已啟用 ›" else "未啟用 ›", onClick = { showPin = true })
            }
            // 通知
            item {
                SettingRow("通知提醒") {
                    Switch(
                        checked = s?.notificationsEnabled != false,
                        onCheckedChange = { vm.repo.setNotifications(it) },
                    )
                }
            }
            // 視圖 / 排序
            item { SettingRow("預設視圖", value = if (s?.viewMode == "grid") "網格 ›" else "列表 ›", onClick = { vm.repo.setViewMode(if (s?.viewMode == "grid") "list" else "grid") }) }
            item { SettingRow("預設排序", value = "更新時間 ›", onClick = { }) }
            item { Text("同步引擎", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp)) }
            // 同步狀態 + 立即同步
            item {
                SettingRow(
                    "⟳ 立即同步",
                    value = if ((s?.pendingSyncCount ?: 0) > 0) "${s?.pendingSyncCount ?: 0} 待同步" else "已同步",
                    onClick = {
                        vm.repo.syncNow { n -> Toast.makeText(context, "同步完成（$n 筆變更）", Toast.LENGTH_SHORT).show() }
                    },
                )
            }
            item {
                Text(
                    "最後同步：${s?.lastSyncAt?.let { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(it)) } ?: "從未"}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            // 匯出 / 匯入
            item { SettingRow("⬆ 匯出完整備份", value = "JSON ›", onClick = { exportLauncher.launch("evernote-backup-${System.currentTimeMillis()}.json") }) }
            item { SettingRow("⬇ 匯入備份", value = "JSON ›", onClick = { importLauncher.launch(arrayOf("application/json")) }) }
            item { Text("其他", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp)) }
            item {
                SettingRow("關於", value = "綠筆記 v1.0（Kotlin + Compose + Room）", onClick = { })
            }
            item {
                SettingRow("清除資料（恢復種子）", tint = Red, onClick = { reset() })
            }
        }
    }

    if (showPin) {
        AlertDialog(
            onDismissRequest = { showPin = false; pinInput = ""; pinStep = 0 },
            title = { Text(if (s?.pinEnabled == true) "關閉密碼鎖" else if (pinStep == 0) "設定 4 位密碼" else "再次輸入確認") },
            text = {
                OutlinedTextField(
                    pinInput, { pinInput = it },
                    label = { Text("4 位數字密碼") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    when {
                        s?.pinEnabled == true -> {
                            if (pinInput == s.pinCode) { vm.repo.setPin(false, ""); showPin = false }
                            else Toast.makeText(context, "密碼不正確", Toast.LENGTH_SHORT).show()
                            pinInput = ""
                        }
                        pinStep == 0 -> { firstPin = pinInput; pinInput = ""; pinStep = 1 }
                        else -> {
                            if (pinInput == firstPin) { vm.repo.setPin(true, pinInput); showPin = false; pinStep = 0 }
                            else { Toast.makeText(context, "兩次輸入不一致", Toast.LENGTH_SHORT).show(); pinStep = 0; pinInput = "" }
                        }
                    }
                }) { Text(if (s?.pinEnabled == true) "關閉" else if (pinStep == 0) "下一步" else "啟用") }
            },
            dismissButton = { TextButton(onClick = { showPin = false; pinStep = 0; pinInput = "" }) { Text("取消") } },
        )
    }
}

@Composable
private fun SettingRow(
    label: String,
    value: String? = null,
    tint: androidx.compose.ui.graphics.Color? = null,
    onClick: () -> Unit = {},
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 14.sp, color = tint ?: MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        value?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        trailing?.invoke()
    }
}
