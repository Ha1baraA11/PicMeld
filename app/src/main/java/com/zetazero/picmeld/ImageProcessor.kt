package com.zetazero.picmeld

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
import java.io.InputStream

object ImageProcessor {

    // 最大边长像素：防止内存爆栈的 OOM 问题限制，提高到 8000 以避免普通的截图发生任何压缩扭曲
    private const val MAX_SINGLE_IMAGE_DIMENS = 8000

    suspend fun generateGrids(
        context: Context,
        uris: List<Uri>,
        onProgress: (Int, Int) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val totalGrids = Math.ceil(uris.size / 4.0).toInt()
            
            for (i in 0 until totalGrids) {
                // 每4张1组
                val groupUris = uris.drop(i * 4).take(4)
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

                // 2x2 网格长宽各自乘以 2
                val outWidth = quadrantW * 2
                val outHeight = quadrantH * 2

                val outBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(outBitmap)
                canvas.drawColor(Color.WHITE) // 设定白板底色
                
                val paint = Paint(Paint.FILTER_BITMAP_FLAG)

                bitmaps.forEachIndexed { index, bitmap ->
                    val row = index / 2
                    val col = index % 2
                    
                    val destX = col * quadrantW
                    val destY = row * quadrantH

                    // 同比例缩小以放进方格（要求等比缩放、不裁剪被白边填补）
                    val scale = minOf(
                        quadrantW.toFloat() / bitmap.width,
                        quadrantH.toFloat() / bitmap.height
                    )
                    
                    val scaledW = (bitmap.width * scale).toInt()
                    val scaledH = (bitmap.height * scale).toInt()
                    
                    val matrix = Matrix()
                    matrix.postScale(scale, scale)
                    val transX = destX + (quadrantW - scaledW) / 2f
                    val transY = destY + (quadrantH - scaledH) / 2f
                    matrix.postTranslate(transX, transY)

                    canvas.drawBitmap(bitmap, matrix, paint)
                    bitmap.recycle() // 尽早销毁这部分的占用图块内存
                }

                // 放入相册并重置这张 2x2 拼版占用
                saveToGallery(context, outBitmap, "PicMeld_2X2_${System.currentTimeMillis()}.jpg")
                outBitmap.recycle()
                
                withContext(Dispatchers.Main) {
                    onProgress(i + 1, totalGrids)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun loadAndFixBitmap(context: Context, uri: Uri): Bitmap {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            // 第一遍解码获取原始高宽，计算下采样参数以防OOM
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(stream, null, options)
            options.inSampleSize = calculateInSampleSize(options, MAX_SINGLE_IMAGE_DIMENS, MAX_SINGLE_IMAGE_DIMENS)
            options.inJustDecodeBounds = false
            
            // 第二遍进行真正的解码
            val imgBitmap = context.contentResolver.openInputStream(uri)?.use { 
                BitmapFactory.decodeStream(it, null, options)
            } ?: throw IllegalStateException("无法解码图片: $uri")

            // 解析EXIF以回正倒翻的拍摄图片
            val rotationFlags = try {
                context.contentResolver.openInputStream(uri)?.use { exifStream ->
                    ExifInterface(exifStream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                } ?: ExifInterface.ORIENTATION_NORMAL
            } catch (e: Exception) {
                ExifInterface.ORIENTATION_NORMAL
            }
            
            return rotateBitmap(imgBitmap, rotationFlags)
        }
        throw IllegalStateException("无法打开图片流: $uri")
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
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

    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
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
