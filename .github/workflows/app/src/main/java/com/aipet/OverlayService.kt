package com.aipet

import android.app.*
import android.content.*
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.webkit.WebView
import android.webkit.WebSettings

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: WebView? = null
    private var params: WindowManager.LayoutParams? = null

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    companion object {
        const val CHANNEL_ID = "pet_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.aipet.STOP_PET"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        setupOverlay()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
                description = getString(R.string.channel_desc)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, OverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("AI桌宠")
            .setContentText("小鲸鱼正在陪着你~")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "关闭", stopPendingIntent)
            .build()
    }

    private fun setupOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val density = resources.displayMetrics.density
        val width = (58 * density).toInt()
        val height = (45 * density).toInt()

        params = WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 300
        }

        overlayView = WebView(this).apply {
            setBackgroundColor(0x00000000)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            loadUrl("file:///android_asset/pet.html")

            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params!!.x
                        initialY = params!!.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params!!.x = initialX + (event.rawX - initialTouchX).toInt()
                        params!!.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(this, params)
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY
                        val distance = kotlin.math.sqrt((dx * dx + dy * dy).toDouble())
                        if (distance < 20) {
                            evaluateJavascript("onTap()")
                        }
                        true
                    }
                    else -> false
                }
            }
        }

            try {
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            stopSelf()
        }
    }

    private fun evaluateJavascript(script: String) {
        overlayView?.evaluateJavascript(script, null)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        overlayView?.let {
            windowManager?.removeView(it)
            it.destroy()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
