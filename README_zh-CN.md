<p align="center">
  <img src="assets/logo.png" alt="PicMeld Logo" width="200">
</p>

<h1 align="center">PicMeld</h1>

<p align="center">
  <strong>批量照片拼图 — 最多 200 张照片，自动拼成 2x2 网格</strong><br>
  轻量、离线、除存储权限外零权限申请
</p>

<p align="center">
  <a href="README.md">English</a> · <a href="README_zh-TW.md">繁體中文</a>
</p>

---

## 功能特点

- **批量处理** — 选择最多 200 张照片，自动按每 4 张一组拼成 2x2 网格拼图
- **智能缩放** — 等比缩放不裁剪，空白区域自动填充白色
- **EXIF 修正** — 自动识别并修正拍摄照片的旋转方向
- **追加模式** — 分多次添加照片，自动去重
- **实时进度** — 进度条显示当前处理数量和百分比
- **完全离线** — 无网络请求、无数据统计、无追踪
- **直存相册** — 输出 JPG 直接保存到系统相册

## 系统要求

- Android 8.0 (API 26) 及以上
- Android 9 及以下需要存储权限（Android 10+ 自动授权）

## 项目架构

```
com.ha1baraa11.picmeld/
├── MainActivity.kt      # UI 层 — 图片选择器、缩略图网格、进度遮罩
├── MainViewModel.kt     # 状态管理 — 已选 URI、进度、错误信息
├── ImageProcessor.kt    # 核心引擎 — Bitmap 缩放、EXIF 修正、Canvas 合图
└── PhotoAdapter.kt      # RecyclerView 适配器 — 三列缩略图网格，支持单张删除
```

**技术栈：** Kotlin · MVVM · ViewBinding · Coroutines · Material 3

## 使用方法

1. 点击 **选择照片** 从相册多选图片
2. 在三列缩略图网格中预览，点击任意图片可删除
3. 点击 **生成拼图** — 应用自动将每 4 张照片拼成一张 2x2 拼图
4. 输出的 JPG 保存在系统相册的 `Pictures/PicMeld/` 目录

## 构建

```bash
git clone https://github.com/Ha1baraA11/PicMeld.git
cd PicMeld
./gradlew assembleDebug
```

APK 输出路径：`app/build/outputs/apk/debug/app-debug.apk`

## 许可证

MIT
