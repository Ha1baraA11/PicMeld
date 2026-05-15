package com.ha1baraa11.picmeld

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.ByteArrayInputStream

object ImageProcessor {

    // 最大边长像素：防止内存爆栈的 OOM 问题限制，提高到 8000 以避免普通的截图发生任何压缩扭曲
    private const val MAX_SINGLE_IMAGE_DIMENS = 8000

    suspend fun generateGrids(
        context: Context,
        uris: List<Uri>,
        layout: LayoutConfig = LayoutConfig.LAYOUT_2X2,
        bgColor: Int = Color.WHITE,
        gapPx: Int = 0,
        onProgress: (Int, Int) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val totalGrids = Math.ceil(uris.size / layout.groupSize.toDouble()).toInt()

            for (i in 0 until totalGrids) {
                val groupUris = uris.drop(i * layout.groupSize).take(layout.groupSize)
                val bitmaps = groupUris.map { loadAndFixBitmap(context, it) }

                // 计算这一组中，图片的最大宽高（有上限限制）来作为2x2的单个格子规格
                var maxW = 0
                var maxH = 0
                for (b in bitmaps) {
                    if (b.width > maxW) maxW = b.width
                    if (b.height > maxH) maxH = b.height
                }
                
                var quadW = maxW.toFloat()
                var quadH = maxH.toFloat()

                // 同步等比例缩小四格画板区域尺寸，而不是分别生硬砍断高宽，从而彻底消灭白边
                if (quadW > MAX_SINGLE_IMAGE_DIMENS || quadH > MAX_SINGLE_IMAGE_DIMENS) {
                    val limitFactor = minOf(
                        MAX_SINGLE_IMAGE_DIMENS / quadW,
                        MAX_SINGLE_IMAGE_DIMENS / quadH
                    )
                    quadW *= limitFactor
                    quadH *= limitFactor
                }

                val quadrantW = quadW.toInt()
                val quadrantH = quadH.toInt()

                // 按布局配置计算画布尺寸
                val outWidth = quadrantW * layout.columns
                val outHeight = quadrantH * layout.rows

                val outBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(outBitmap)
                canvas.drawColor(bgColor)

                val paint = Paint(Paint.FILTER_BITMAP_FLAG)
                val effectiveW = quadrantW - gapPx
                val effectiveH = quadrantH - gapPx

                bitmaps.forEachIndexed { index, bitmap ->
                    val row = index / layout.columns
                    val col = index % layout.columns

                    val destX = col * quadrantW + gapPx / 2
                    val destY = row * quadrantH + gapPx / 2

                    // 同比例缩小以放进方格（要求等比缩放、不裁剪被白边填补）
                    val scale = minOf(
                        effectiveW.toFloat() / bitmap.width,
                        effectiveH.toFloat() / bitmap.height
                    )

                    val scaledW = (bitmap.width * scale).toInt()
                    val scaledH = (bitmap.height * scale).toInt()

                    val matrix = Matrix()
                    matrix.postScale(scale, scale)
                    val transX = destX + (effectiveW - scaledW) / 2f
                    val transY = destY + (effectiveH - scaledH) / 2f
                    matrix.postTranslate(transX, transY)

                    canvas.drawBitmap(bitmap, matrix, paint)
                    bitmap.recycle() // 尽早销毁这部分的占用图块内存
                }

                // 放入相册并重置这张 2x2 拼版占用
                saveToGallery(context, outBitmap, "PicMeld_${layout.filenameTag}_${System.currentTimeMillis()}.jpg")
                outBitmap.recycle()
                yield() // 让出 CPU 给 GC 回收已释放的 Bitmap 内存

                withContext(Dispatchers.Main) {
                    onProgress(i + 1, totalGrids)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    internal fun loadAndFixBitmap(context: Context, uri: Uri): Bitmap {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("无法打开图片流: $uri")

        // 第一遍解码获取原始高宽，计算下采样参数以防OOM
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)
        boundsOptions.inSampleSize = calculateInSampleSize(boundsOptions, MAX_SINGLE_IMAGE_DIMENS, MAX_SINGLE_IMAGE_DIMENS)
        boundsOptions.inJustDecodeBounds = false

        // 第二遍进行真正的解码
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)
            ?: throw IllegalStateException("无法解码图片: $uri")

        // 解析EXIF以回正倒翻的拍摄图片
        val rotationFlags = try {
            ByteArrayInputStream(bytes).use { bs ->
                ExifInterface(bs).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
        } catch (_: Exception) { ExifInterface.ORIENTATION_NORMAL }

        return rotateBitmap(bitmap, rotationFlags)
    }

    internal fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    internal fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) {
            bitmap.recycle()
        }
        return rotated
    }

    private fun saveToGallery(context: Context, bitmap: Bitmap, title: String) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, title)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PicMeld")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IllegalStateException("建立相册占位失败,权限受限或磁盘已满")
            
        context.contentResolver.openOutputStream(uri)?.use { outStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outStream)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, contentValues, null, null)
        }
    }
}
