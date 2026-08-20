# 綠筆記 — Kotlin (Jetpack Compose) + Room 原生 Android

對齊 Evernote 7.8.2 功能分析的完整原生筆記 App。GitHub Actions 自動編譯出 100% 可用 APK。

## 功能（真實實作，非模擬）

| 模組 | 實作 |
|---|---|
| 筆記 | CRUD、釘選、回收站（清空）、字體 A±、筆記資訊、分享 |
| 筆記本 | 堆疊分組、顏色、計數、新增 |
| 標籤 | 多選、新增、捷徑 |
| 搜索 | 全文（Room LIKE）、語法 `notebook:` / `tag:`、最近搜尋 |
| 提醒 | **AlarmManager 真實系統通知**（App 關閉也觸發） |
| 附件 | **CameraX 真實拍照**、相簿（PhotoPicker）、**MediaRecorder 真實錄音**、檔案（SAF） |
| 名片掃描 | **ML Kit OCR（中文）** → 自動建立聯絡人筆記 |
| 訊息 | 線程＋氣泡＋真實資料互通（訊息→筆記） |
| 同步 | 變更日誌 + 待同步計數 + 匯出/匯入完整 JSON 備份（SAF） |
| 其他 | 深色模式、PIN 密碼鎖、導航抽屜、排序、列表/網格視圖、統計 |

## 版本矩陣（CI 驗證相容）

- AGP 8.7.2 · Gradle 8.11.1（wrapper 內含）· JDK 17
- Kotlin 2.1.20 · Compose BOM 2025.06.01 · Navigation Compose 2.8.5
- Room 2.7.1（KSP 2.1.20-2.0.0）· CameraX 1.4.1 · ML Kit text-recognition 16.0.1
- compileSdk 35 / targetSdk 35 / minSdk 26

## GitHub Actions 出 APK

1. 解壓本 zip → `git init` → push 到 GitHub（main 分支）。
2. Actions 自動跑 `./gradlew :app:assembleRelease`（wrapper 內含，無需本機 Android SDK）。
3. 下載 artifact `evernote-clone-release-apk`（內含 `app-release.apk`）。

正式簽名（選配）：在 repo Settings → Secrets 設定
`ANDROID_KEYSTORE_BASE64`（`base64 -w0 my.keystore`）/ `ANDROID_KEYSTORE_PASSWORD` / `ANDROID_KEY_ALIAS` / `ANDROID_KEY_PASSWORD`；
未設定時自動用 debug 簽名，APK 一樣可安裝。

## 防 OOM（CI 峰值 < 3GB）

- `org.gradle.jvmargs=-Xmx2g`、`workers.max=2`、`daemon=false`（gradle.properties）
- 單模組專案、KSP 取代 kapt、無 Hilt 等重型依賴

## 本機開發

```bash
./gradlew :app:assembleDebug    # 需 JDK 17 + Android SDK
./gradlew :app:installDebug
```
