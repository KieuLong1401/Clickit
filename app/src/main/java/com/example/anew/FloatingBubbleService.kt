package com.example.anew

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

class FloatingBubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var bubbleView: View
    private lateinit var bubbleDeleteView: View

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val inflater = LayoutInflater.from(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("bubble", "Floating Bubble", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)

            val notification = Notification.Builder(this, "bubble")
                .setContentTitle("Bubble running")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build()

            startForeground(1, notification)
        }

        bubbleDeleteView = inflater.inflate(R.layout.delete_bubble_layout, null)

        val bubbleDeleteParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        bubbleDeleteParams.gravity = Gravity.TOP or Gravity.START
        bubbleDeleteParams.x = resources.displayMetrics.widthPixels / 2 - 80
        bubbleDeleteParams.y = resources.displayMetrics.heightPixels - 260

        bubbleView = inflater.inflate(R.layout.bubble_layout, null)
        val bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        bubbleParams.gravity = Gravity.TOP or Gravity.START
        bubbleParams.x = 0
        bubbleParams.y = 100

        windowManager.addView(bubbleView, bubbleParams)

        bubbleView.setOnTouchListener(object : View.OnTouchListener {
            private var lastX = 0
            private var lastY = 0
            private var initialX = 0
            private var initialY = 0

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when(event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        lastX = event.rawX.toInt()
                        lastY = event.rawY.toInt()
                        initialX = bubbleParams.x
                        initialY = bubbleParams.y

                        windowManager.addView(bubbleDeleteView, bubbleDeleteParams)

                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX.toInt() - lastX
                        val dy = event.rawY.toInt() - lastY
                        bubbleParams.x = initialX + dx
                        bubbleParams.y = initialY + dy
                        windowManager.updateViewLayout(bubbleView, bubbleParams)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if(bubbleParams.x <= bubbleDeleteParams.x + 60 &&
                            bubbleParams.x + 60 >= bubbleDeleteParams.x &&
                            bubbleParams.y <= bubbleDeleteParams.y + 60 &&
                            bubbleParams.y + 60 >= bubbleDeleteParams.y) {
                            stopSelf()
                        }
                        windowManager.removeView(bubbleDeleteView)

                        return true
                    }
                }
                return false
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        if(::bubbleView.isInitialized) windowManager.removeView(bubbleView)
    }
}