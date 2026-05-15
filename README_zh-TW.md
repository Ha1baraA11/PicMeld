<p align="center">
  <img src="assets/logo.png" alt="PicMeld Logo" width="200">
</p>

<h1 align="center">PicMeld</h1>

<p align="center">
  <strong>批次照片拼圖 — 最多 200 張照片，支援多種拼圖版面配置</strong><br>
  輕量、離線、除儲存權限外零權限申請
</p>

<p align="center">
  <a href="README.md">English</a> · <a href="README_zh-CN.md">简体中文</a>
</p>

---

## 功能特色

- **多種版面配置** — 支援 2x2 方格、3x3 方格、1x3 豎條三種拼圖範本
- **拖曳排序** — 長按拖動縮圖自由調整照片順序
- **自訂樣式** — 可選背景色（6 種預設）和圖片間距（0-40px）
- **批次處理** — 最多選擇 200 張照片，依所選版面配置自動分組拼接
- **智慧縮放** — 等比縮放不裁切，空白區域使用背景色填滿
- **EXIF 修正** — 自動辨識並修正拍攝照片的旋轉方向
- **追加模式** — 分多次新增照片，自動去重
- **即時進度** — 進度條顯示目前處理數量和百分比
- **完全離線** — 無網路請求、無資料統計、無追蹤
- **直存相簿** — 輸出 JPG 直接儲存到系統相簿

## 系統需求

- Android 8.0 (API 26) 及以上
- Android 9 及以下需要儲存權限（Android 10+ 自動授權）

## 專案架構

```
com.ha1baraa11.picmeld/
├── MainActivity.kt      # UI 層 — 圖片選擇器、版面配置/顏色/間距控制、進度遮罩
├── MainViewModel.kt     # 狀態管理 — 已選 URI、版面配置、進度、錯誤訊息
├── ImageProcessor.kt    # 核心引擎 — Bitmap 縮放、EXIF 修正、Canvas 合圖
├── PhotoAdapter.kt      # RecyclerView 配接器 — 縮圖網格，支援拖曳排序
├── LayoutConfig.kt      # 版面配置資料類別 — 2x2、3x3、1x3 預設
└── DragSwipeCallback.kt # ItemTouchHelper 拖曳回呼
```

**技術堆疊：** Kotlin · MVVM · ViewBinding · Coroutines · Material 3

## 使用方法

1. 點擊 **選擇照片** 從相簿多選圖片
2. 選擇拼圖版面配置：2x2 方格、3x3 方格或 1x3 豎條
3. 長按拖動縮圖調整順序，點擊右上角按鈕刪除
4. 可選調整背景色和圖片間距
5. 點擊 **生成拼圖** — 應用程式依版面配置自動分組拼接
6. 輸出的 JPG 儲存在系統相簿的 `Pictures/PicMeld/` 目錄

## 建置

```bash
git clone https://github.com/Ha1baraA11/PicMeld.git
cd PicMeld
./gradlew assembleDebug
```

APK 輸出路徑：`app/build/outputs/apk/debug/app-debug.apk`

## 授權條款

MIT
