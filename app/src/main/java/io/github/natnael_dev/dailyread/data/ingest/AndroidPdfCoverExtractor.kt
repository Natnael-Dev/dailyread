package io.github.natnael_dev.dailyread.data.ingest

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object AndroidPdfCoverExtractor {

    suspend fun renderFirstPage(contentResolver: ContentResolver, uriString: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(uriString)
            val pfd = contentResolver.openFileDescriptor(uri, "r") ?: return@withContext null
            pfd.use { fd ->
                val renderer = PdfRenderer(fd)
                if (renderer.pageCount == 0) {
                    renderer.close()
                    return@withContext null
                }
                val page = renderer.openPage(0)
                val width = page.width
                val height = page.height
                val scale = minOf(600f / maxOf(width, height), 1.0f)
                val targetWidth = (width * scale).toInt().coerceAtLeast(1)
                val targetHeight = (height * scale).toInt().coerceAtLeast(1)

                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()

                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
                stream.toByteArray()
            }
        } catch (e: Throwable) {
            null
        }
    }
}
