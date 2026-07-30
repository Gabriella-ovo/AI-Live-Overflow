package com.aipet

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var webView: WebView
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.allowFileAccess = true
            setBackgroundColor(0x00000000)
            settings.setBackgroundColor(0)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    SupabaseManager(this@OverlayService).startPolling { msg ->
                        view.post { view.evaluateJavascript("petEngine.onAiMessage('$msg')", null) }
                    }
                }
            }
            loadUrl("file:///android_asset/pet.html")
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0; y = 200
        }
        wm.addView(webView, params)
        webView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    wm.updateViewLayout(webView, params)
                    true
                }
                else -> false
            }
        }
        startForeground(1, notification())
    }

    private fun notification(): Notification {
        val channel = NotificationChannel("pet", "AI Pet", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        return Notification.Builder(this, "pet")
            .setContentTitle("AI Pet")
            .setContentText("我在这儿~")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { if (::webView.isInitialized) wm.removeView(webView) }
}
