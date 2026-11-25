package com.jarvis.android.system

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class JarvisAccessibilityService : AccessibilityService() {

    companion object {
        var instance: JarvisAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    fun triggerGlobal(action: Int) {
        performGlobalAction(action)
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }
}
