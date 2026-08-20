package com.evernoteclone.ui.screens

import android.graphics.BitmapFactory
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.ui.viewinterop.AndroidView
import com.evernoteclone.ui.theme.Green
import com.evernoteclone.ui.viewmodel.AppViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import java.io.File

/** 真實相機：CameraX 預覽 + 拍照 → 相簿/編輯器；名片掃描 → ML Kit OCR → 聯絡人筆記 */
@Composable
fun CameraScreen(
    vm: AppViewModel,
    mode: String, // photo / scan
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val controller = remember { LifecycleCameraController(context) }
    var busy by remember { mutableStateOf(false) }
    var ocrText by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) { android.widget.Toast.makeText(context, "需要相機權限", android.widget.Toast.LENGTH_SHORT).show(); onBack() }
    }
    LaunchedEffect(Unit) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.CAMERA
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    controller.bindToLifecycle(lifecycleOwner)
                    controller.cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
                    controller.setImageAnalysisMode(CameraController.IMAGE_ANALYSIS_MODE_NONE)
                    this.controller = controller
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        Text(
            if (mode == "scan") "名片 / 文件掃描" else "拍照",
            color = Color.White,
            fontSize = 15.sp,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 40.dp)
                .background(Color(0x66000000), MaterialTheme.shapes.medium)
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart)) {
            Text("✕", color = Color.White, fontSize = 22.sp)
        }
        // 快門
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .size(72.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(enabled = !busy) {
                    busy = true
                    val file = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
                    val output = ImageCapture.OutputFileOptions.Builder(file).build()
                    controller.takePicture(
                        output, ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(results: ImageCapture.OutputFileResults) {
                                val uri = file.toURI().toString()
                                if (mode == "scan") {
                                    val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
                                    val image = InputImage.fromFilePath(context, file)
                                    recognizer.process(image)
                                        .addOnSuccessListener { result ->
                                            val text = result.text
                                            ocrText = text
                                            if (text.isNotBlank()) saveContactNote(vm, text, uri)
                                            else {
                                                vm.setPendingAttachment(uri)
                                                android.widget.Toast.makeText(context, "未識別到文字，已存為附件", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                            busy = false
                                        }
                                        .addOnFailureListener { busy = false }
                                } else {
                                    vm.setPendingAttachment(uri)
                                    onBack()
                                }
                            }
                            override fun onError(exception: ImageCaptureException) {
                                busy = false
                                android.widget.Toast.makeText(context, "拍照失敗", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(54.dp).clip(CircleShape).background(Green))
        }
    }

    // OCR 結果顯示
    ocrText?.let { text ->
        AlertDialog(
            onDismissRequest = { ocrText = null },
            title = { Text("名片已掃描") },
            text = {
                Text(
                    text.take(300),
                    fontSize = 12.sp,
                    maxLines = 12,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            },
            confirmButton = {
                TextButton(onClick = { ocrText = null; onBack() }) { Text("完成") }
            },
        )
    }
}

private fun saveContactNote(vm: AppViewModel, text: String, uri: String) {
    val email = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}").find(text)?.value ?: ""
    val phone = Regex("\\+?[0-9][0-9\\s\\-()]{6,}[0-9]").find(text)?.value?.trim() ?: ""
    val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
    val name = lines.firstOrNull() ?: ""
    val company = lines.getOrNull(1) ?: ""
    val content = buildString {
        append("## 名片資料（OCR）\n")
        if (name.isNotEmpty()) append("姓名：$name\n")
        if (company.isNotEmpty()) append("公司：$company\n")
        if (email.isNotEmpty()) append("Email：$email\n")
        if (phone.isNotEmpty()) append("電話：$phone\n")
        append("---\n原始文字：\n$text")
    }
    val note = com.evernoteclone.data.Note(
        title = if (name.isNotEmpty()) "名片：$name" else "名片掃描",
        content = content,
        resources = listOf("image|名片照片|$uri"),
    )
    vm.repo.saveNote(note) { }
}
