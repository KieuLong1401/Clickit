package com.example.anew

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.view.accessibility.AccessibilityEvent
import android.graphics.Path
import android.os.Handler
import android.os.Looper

class ClickService : AccessibilityService() {
    companion object {
        var instance: ClickService? = null
        var isClicking: Boolean = false
    }

    private lateinit var clickRunnable: Runnable

    private val handler = Handler(Looper.getMainLooper())

    private fun simulateClick(x: Float, y: Float) {
        handler.post {
            val path = Path().apply { moveTo(x, y) }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
                .build()
            dispatchGesture(gesture, null, null)
        }
    }

    fun start(x: Float, y: Float, clickDelay: Long) {

        clickRunnable = object : Runnable {
            override fun run() {
                simulateClick(x, y)

                handler.postDelayed(this, clickDelay)
            }
        }

        handler.post(clickRunnable)
        isClicking = true
    }
    fun stop() {
        handler.removeCallbacks(clickRunnable)
        isClicking = false
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }
    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

    }
    override fun onInterrupt() {

    }
}