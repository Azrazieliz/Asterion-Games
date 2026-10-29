package com.gamepilot

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Continuous MediaProjection stream is processed directly in memory, not saved as screenshots.
 * System consent is required for every new capture session on Android 14+.
 */
class LiveVisionService : Service() {
    companion object {
        const val ACTION_STOP = "com.gamepilot.STOP_VISION"
        const val EXTRA_CODE = "projectionCode"
        const val EXTRA_DATA = "projectionData"
        @Volatile var running = false
            private set
    }
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val processing = AtomicBoolean(false)
    private var worker: HandlerThread? = null
    private var handler: Handler? = null
    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var display: VirtualDisplay? = null
    private var lastTime = 0L
    private var closing = false
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        if (running) return START_NOT_STICKY
        val data = if (Build.VERSION.SDK_INT >= 33)
            intent?.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        else intent?.getParcelableExtra<Intent>(EXTRA_DATA)
        val code = intent?.getIntExtra(EXTRA_CODE, Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        if (code != Activity.RESULT_OK || data == null) {
            Events.record(this, "Screen capture not authorized")
            stopSelf()
            return START_NOT_STICKY
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("capture", "Live game perception", NotificationManager.IMPORTANCE_LOW)
        )
        val notification = Notification.Builder(this, "capture")
            .setContentTitle("Asterion Pilot")
            .setContentText("Reading live game frames locally")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true).build()
        startForeground(52, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        try {
            projection = getSystemService(MediaProjectionManager::class.java).getMediaProjection(code, data)
            worker = HandlerThread("pilot-live-vision").apply { start() }
            handler = Handler(worker!!.looper)
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            require(width > 0 && height > 0)
            projection!!.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Events.record(this@LiveVisionService, "Android ended the live perception session")
                    stopSelf()
                }
            }, handler)
            reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3)
            reader!!.setOnImageAvailableListener({ source -> processLatest(source) }, handler)
            display = projection!!.createVirtualDisplay(
                "AsterionPilot-Live", width, height, metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader!!.surface, null, handler
            )
            running = true
            closing = false
            Events.record(this, "Live perception running")
        } catch (error: Exception) {
            Events.record(this, "Projection initialization failed: ${error.message}")
            stopSelf()
        }
        return START_NOT_STICKY
    }
    private fun processLatest(source: ImageReader) {
        val image = try { source.acquireLatestImage() } catch (_: Exception) { null } ?: return
        try {
            val now = System.currentTimeMillis()
            if (closing || processing.get() || now - lastTime < 700) return
            processing.set(true)
            lastTime = now
            val plane = image.planes.firstOrNull()
            if (plane == null || plane.pixelStride != 4) {
                processing.set(false); return
            }
            val rowStride = plane.rowStride
            val paddedWidth = rowStride / plane.pixelStride
            val bytes = ByteBuffer.allocate(rowStride * image.height)
            bytes.put(plane.buffer)
            bytes.rewind()
            val padded = Bitmap.createBitmap(paddedWidth, image.height, Bitmap.Config.ARGB_8888)
            padded.copyPixelsFromBuffer(bytes)
            val bitmap = Bitmap.createBitmap(padded, 0, 0, image.width, image.height)
            padded.recycle()
            val tiles = sampleTiles(bitmap)
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { result ->
                    if (!closing) {
                        val texts = result.textBlocks.flatMap { it.lines }
                            .mapNotNull { line -> line.boundingBox?.let { SeenText(line.text, it) } }
                        VisionBus.latest = LiveFrame(now, bitmap.width, bitmap.height, tiles, texts)
                    }
                }
                .addOnFailureListener { error ->
                    if (!closing) {
                        VisionBus.latest = LiveFrame(now, bitmap.width, bitmap.height, tiles, emptyList())
                        Events.record(this, "OCR backend error: ${error.javaClass.simpleName}")
                    }
                }
                .addOnCompleteListener {
                    bitmap.recycle()
                    processing.set(false)
                }
        } catch (error: Exception) {
            processing.set(false)
            Events.record(this, "Frame processing failed: ${error.javaClass.simpleName}")
        } finally { image.close() }
    }
    private fun sampleTiles(bitmap: Bitmap): IntArray {
        val cols = 12
        val rows = 8
        return IntArray(cols * rows) { index ->
            val col = index % cols
            val row = index / cols
            val px = ((col + 0.5) * bitmap.width / cols).toInt().coerceIn(0, bitmap.width - 1)
            val py = ((row + 0.5) * bitmap.height / rows).toInt().coerceIn(0, bitmap.height - 1)
            val pixel = bitmap.getPixel(px, py)
            (((pixel shr 16) and 255) * 299 + ((pixel shr 8) and 255) * 587 +
                (pixel and 255) * 114) / 1000
        }
    }
    override fun onDestroy() {
        closing = true
        running = false
        VisionBus.reset()
        reader?.setOnImageAvailableListener(null, null)
        display?.release()
        reader?.close()
        projection?.stop()
        handler?.removeCallbacksAndMessages(null)
        worker?.quitSafely()
        recognizer.close()
        super.onDestroy()
    }
}
