# 技术栈锁定 (Tech Stack)

该应用将基于以下技术方向进行开发：

1. **平台与兼容性**:
   - 纯 Android 原生应用。
   - `minSdkVersion`: 26 (Android 8.0 兼容)。
   - 不依赖、不集成云端库。
2. **核心开发栈**:
   - 语言：**Kotlin**。
   - IDE：Android Studio。
   - UI 框架：**原生 XML** + **ViewModel** 数据绑定管理。
   - 布局控件：倾向于基于 `RecyclerView`（每行3张展示）进行选定图管理。
3. **关键技术实现**:
   - **图片选择**: Android SAF (Storage Access Framework) 或适用 8.0+ 的系统相册图片选择 API。
   - **图片操作**: Android `Bitmap` 核心类配合 `Matrix` 等完成高宽缩放与坐标定位整合。
   - **元数据分析**: `ExifInterface` 用于识别翻转、旋转等元信息。
