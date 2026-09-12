package com.start.launcher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File

/**
 * 主页面背景图存储：把用户选择的图片复制进应用私有目录，
 * 不依赖外部 URI 权限，图库删除/换机后依然有效。
 *
 * 处理链：采样解码 → EXIF 旋转 → 长边压到 2048 → JPEG(90) 覆盖写。
 */
object BackgroundStore {

    private const val FILE_NAME = "bg_image.jpg"
    private const val MAX_DIM = 2048

    fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    fun exists(context: Context): Boolean = file(context).exists()

    /**
     * 从 Uri 保存背景图（IO 线程调用）。成功返回 true。
     */
    fun saveFromUri(context: Context, uri: Uri): Boolean {
        val resolver = context.contentResolver
        return try {
            // 1. 只读尺寸（inJustDecodeBounds 时 decodeStream 返回 null，仅回填 bounds）
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val boundsInput = resolver.openInputStream(uri) ?: return false
            boundsInput.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false

            // 2. 按目标尺寸计算采样率，避免整图解码 OOM
            var sample = 1
            var w = bounds.outWidth
            var h = bounds.outHeight
            while (w / 2 >= MAX_DIM && h / 2 >= MAX_DIM) {
                w /= 2; h /= 2; sample *= 2
            }
            val src = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return false

            // 3. EXIF 旋转
            val orientation = resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL,
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
            val rotated = rotateBitmap(src, orientation)

            // 4. 长边压到 MAX_DIM
            val final = if (maxOf(rotated.width, rotated.height) > MAX_DIM) {
                val scale = MAX_DIM.toFloat() / maxOf(rotated.width, rotated.height)
                Bitmap.createScaledBitmap(
                    rotated,
                    (rotated.width * scale).toInt().coerceAtLeast(1),
                    (rotated.height * scale).toInt().coerceAtLeast(1),
                    true,
                )
            } else rotated

            // 5. 覆盖写入
            file(context).outputStream().use { final.compress(Bitmap.CompressFormat.JPEG, 90, it) }

            // 回收中间位图（final 可能与 rotated/src 同引用）
            if (src !== final && rotated !== final) rotated.recycle()
            if (src !== final && src !== rotated) src.recycle()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return bitmap
        }
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /** 清除背景图（IO 线程调用） */
    fun clear(context: Context) {
        try {
            file(context).delete()
        } catch (_: Exception) {
            // 文件不存在等场景直接忽略
        }
    }
}
