package com.example.anew

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import androidx.core.view.isVisible

interface Bubble {
    val view: View
    val params: LayoutParams
    fun show()
    fun hide()
    fun update()
}
interface Position {
    val x: Float
    val y: Float
}

class FloatingBubbleService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var bubbleList: MutableList<Bubble>
    private lateinit var inflater: LayoutInflater

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        bubbleList = mutableListOf()
        inflater = LayoutInflater.from(this)

        fun createBubble(layout: Int, x: Int, y: Int, fullScreen: Boolean = false, focusable: Boolean = false): Bubble {
            val size = if (fullScreen) LayoutParams.MATCH_PARENT else LayoutParams.WRAP_CONTENT

            val params = LayoutParams(
                size,
                size,
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    LayoutParams.TYPE_PHONE,
                if(!focusable) LayoutParams.FLAG_NOT_FOCUSABLE else LayoutParams.FLAG_NOT_TOUCH_MODAL or LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
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
        fun getViewCenterPosition(view: View): Position {
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            val x = location[0] + view.width / 2f
            val y = location[1] + view.height / 2f

            return object: Position {
                override val x = x
                override val y = y
            }
        }
        fun targetedChildNotClicked(parent: View, event: MotionEvent): Boolean {
            if (parent !is ViewGroup) return true
            for (i in 0 until parent.childCount) {
                val child = parent.getChildAt(i)
                val childRect = Rect()
                child.getHitRect(childRect)
                if (child.isVisible && childRect.contains(event.x.toInt(), event.y.toInt())) {
                    return false
                }
            }
            return true
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
        val settingPopup = createBubble(R.layout.setting_popup_layout, 0, 0, fullScreen = true, focusable = true)
        val controllerBubble = createBubble(R.layout.bubble_layout, 0, 100)
        controllerBubble.show()
        val clickBubble = createBubble(R.layout.crosshair_layout, 300, 300)
        clickBubble.show()

        val logoBubble = controllerBubble.view.findViewById<View>(R.id.bubble)
        val playButton = controllerBubble.view.findViewById<ImageButton>(R.id.play)
        val settingButton = controllerBubble.view.findViewById<View>(R.id.setting)

        fun getClickDelay(): Long {
            val clickIntervalValue = settingPopup.view.findViewById<EditText>(R.id.interval_value).text.toString().toLong()
            val clickIntervalUnit = settingPopup.view.findViewById<Spinner>(R.id.interval_unit).selectedItem.toString()

            println("$clickIntervalValue $clickIntervalUnit")

            return when (clickIntervalUnit) {
                "ms" ->  clickIntervalValue
                "s" ->  clickIntervalValue * 1000
                "m" ->  clickIntervalValue * 1000 * 60
                "h" ->  clickIntervalValue * 1000 * 60 * 60
                else -> throw IllegalArgumentException("Unknown click interval unit: $clickIntervalUnit")
            }
        }

        logoBubble.setOnTouchListener(object : View.OnTouchListener {
            private var lastX = 0
            private var lastY = 0
            private var initialX = 0
            private var initialY = 0
            private val longPressThreshold = 400L
            private var isLongPress = false

            private val handler = Handler(Looper.getMainLooper())
            private val longPressRunnable = Runnable {
                isLongPress = true
                if(!bubbleDeleteBubble.view.isAttachedToWindow) bubbleDeleteBubble.show()
            }


            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val controllerView = controllerBubble.view.findViewById<View>(R.id.controller)
                val controllerViewVisibility: Int

                if (ClickService.isClicking) return false

                when(event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        lastX = event.rawX.toInt()
                        lastY = event.rawY.toInt()
                        initialX = controllerBubble.params.x
                        initialY = controllerBubble.params.y

                        isLongPress = false
                        handler.postDelayed(longPressRunnable, longPressThreshold)

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

                        handler.removeCallbacks(longPressRunnable)

                        if(controllerBubble.params.x <= bubbleDeleteBubble.params.x + 60 &&
                            controllerBubble.params.x + 60 >= bubbleDeleteBubble.params.x &&
                            controllerBubble.params.y <= bubbleDeleteBubble.params.y + 60 &&
                            controllerBubble.params.y + 60 >= bubbleDeleteBubble.params.y) {

                            stopSelf()
                        }

                        if(bubbleDeleteBubble.view.isAttachedToWindow) bubbleDeleteBubble.hide()

                        controllerViewVisibility = if (controllerView.isVisible) View.GONE else View.VISIBLE

                        if(!isLongPress) controllerView.visibility = controllerViewVisibility

                        return true
                    }
                }
                return false
            }
        })
        playButton.setOnClickListener {
            clickBubble.params.flags = if (ClickService.isClicking) LayoutParams.FLAG_NOT_FOCUSABLE else LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_NOT_TOUCHABLE
            windowManager.updateViewLayout(clickBubble.view, clickBubble.params)

            val clickBubblePosition = getViewCenterPosition(clickBubble.view)

            if (ClickService.isClicking) {
                ClickService.instance?.stop()
            } else {
                ClickService.instance?.start(clickBubblePosition.x, clickBubblePosition.y, getClickDelay())
            }

            bubbleList.forEach {
                val opacity = if (ClickService.isClicking) 0.5f else 1f

                it.view.alpha = opacity

                if (it.view.findViewById<View>(R.id.bubble) != null) {
                    it.view.alpha = 1f
                    it.view.findViewById<View>(R.id.bubble).alpha = opacity
                    it.view.findViewById<View>(R.id.setting).alpha = opacity
                }
            }
        }
        settingButton.setOnClickListener {
            if (ClickService.isClicking) return@setOnClickListener

            settingPopup.show()
        }
        settingPopup.view.setOnTouchListener { v, event ->
            v.performClick()
            if (event.action == MotionEvent.ACTION_DOWN && targetedChildNotClicked(v, event)) {
                settingPopup.hide()
                true
            } else {
                false
            }
        }

        clickBubble.view.setOnTouchListener(object : View.OnTouchListener {
            private var lastX = 0
            private var lastY = 0
            private var initialX = 0
            private var initialY = 0

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                if (ClickService.isClicking) return false
                when(event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.performClick()

                        lastX = event.rawX.toInt()
                        lastY = event.rawY.toInt()
                        initialX = clickBubble.params.x
                        initialY = clickBubble.params.y

                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX.toInt() - lastX
                        val dy = event.rawY.toInt() - lastY
                        clickBubble.params.x = initialX + dx
                        clickBubble.params.y = initialY + dy
                        clickBubble.update()

                        return true
                    }
                }
                return false
            }
        })
    }

    override fun onBind(intent: Intent): IBinder? = null
    override fun onDestroy() {
        super.onDestroy()
        bubbleList.forEach {
            if(it.view.isAttachedToWindow) it.hide()
        }
    }
}