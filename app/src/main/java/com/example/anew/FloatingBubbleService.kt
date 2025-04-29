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
import android.view.WindowManager.LayoutParams

interface Bubble {
    val view: View
    val params: LayoutParams
    fun show()
    fun hide()
    fun update()
}

class FloatingBubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var bubbleList: MutableList<Bubble>

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        bubbleList = mutableListOf<Bubble>()
        val inflater = LayoutInflater.from(this)

        fun createBubble(layout: Int, x: Int, y: Int): Bubble {
            val params = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    LayoutParams.TYPE_PHONE,
                LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.TOP or Gravity.START
            params.x = x
            params.y = y

            val newBubble = object: Bubble {
                override val view = inflater.inflate(layout, null)
                override val params = params
                override fun show() {
                    windowManager.addView(view, params)
                }
                override fun hide() {
                    windowManager.removeView(view)
                }

                override fun update() {
                    windowManager.updateViewLayout(view, params)
                }
            }
            bubbleList += newBubble

            return newBubble
        }

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

        val bubbleDeleteBubble = createBubble(R.layout.delete_bubble_layout, resources.displayMetrics.widthPixels / 2 - 80, resources.displayMetrics.heightPixels - 260)

        val controllerBubble = createBubble(R.layout.bubble_layout, 0, 100)

        controllerBubble.show()

        controllerBubble.view.setOnTouchListener(object : View.OnTouchListener {
            private var lastX = 0
            private var lastY = 0
            private var initialX = 0
            private var initialY = 0

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when(event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        lastX = event.rawX.toInt()
                        lastY = event.rawY.toInt()
                        initialX = controllerBubble.params.x
                        initialY = controllerBubble.params.y

                        bubbleDeleteBubble.show()

                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX.toInt() - lastX
                        val dy = event.rawY.toInt() - lastY
                        controllerBubble.params.x = initialX + dx
                        controllerBubble.params.y = initialY + dy
                        controllerBubble.update()
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        v.performClick()
                        if(controllerBubble.params.x <= bubbleDeleteBubble.params.x + 60 &&
                            controllerBubble.params.x + 60 >= bubbleDeleteBubble.params.x &&
                            controllerBubble.params.y <= bubbleDeleteBubble.params.y + 60 &&
                            controllerBubble.params.y + 60 >= bubbleDeleteBubble.params.y) {

                            stopSelf()
                        }

                        bubbleDeleteBubble.hide()
                        return true
                    }
                }
                return false
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        bubbleList.forEach {
            if(it.view.isAttachedToWindow) it.hide()
        }
    }
}