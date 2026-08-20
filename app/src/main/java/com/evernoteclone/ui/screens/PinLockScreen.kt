package com.evernoteclone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PinLockScreen(pinCode: String, onUnlock: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    val submit = {
        if (pin == pinCode) onUnlock()
        else { error = true; pin = "" }
    }
    LaunchedEffect(pin) {
        if (pin.length == 4) {
            kotlinx.coroutines.delay(80)
            submit()
        }
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🔒", fontSize = 44.sp)
        Text("綠筆記已鎖定", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
        Text("輸入 4 位密碼解鎖", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.padding(vertical = 24.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            repeat(4) { i ->
                Box(
                    Modifier.size(16.dp).clip(CircleShape)
                        .background(
                            when {
                                error -> MaterialTheme.colorScheme.error
                                i < pin.length -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outline
                            }
                        )
                )
            }
        }
        // 數字鍵盤
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(listOf(1,2,3), listOf(4,5,6), listOf(7,8,9), listOf(0)).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    row.forEach { n ->
                        Box(
                            Modifier.size(72.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { if (pin.length < 4) pin += n },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("$n", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (row.size < 3) Box(Modifier.size(72.dp))
                }
            }
        }
        Text(
            "清除",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp).clickable { pin = ""; error = false },
        )
    }
}
