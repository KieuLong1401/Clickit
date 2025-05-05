package com.example.anew

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.view.accessibility.AccessibilityEvent
import android.graphics.Path
import android.os.Handler
import android.os.Looper

class ClickService : AccessibilityService() {
    companion object {
        var isClicking = false
        var x = 0f
        var y = 0f
    }

    private val handler = Handler(Looper.getMainLooper())
    private val clickRunnable = object : Runnable {
        override fun run() {
            if (isClicking) {
                simulateClick(x, y)
            }
            handler.postDelayed(this, 300)
        }
    }

    fun simulateClick(x: Float, y: Float) {
        handler.post {
            val path = Path().apply { moveTo(x, y) }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
                .build()
            dispatchGesture(gesture, null, null)
        }
    }

    override fun onServiceConnected() {
        handler.post(clickRunnable)
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

    }
    override fun onInterrupt() {

    }
}