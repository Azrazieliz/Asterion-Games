package com.gamepilot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.KeyguardManager
import android.content.Intent
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque

/** Direct input controller, sequential queue and adaptive live-scene agent. */
class GameAccessibilityService : AccessibilityService() {
    companion object { @Volatile var instance: GameAccessibilityService? = null; private set }
    private val main = Handler(Looper.getMainLooper())
    private val queue = ArrayDeque<GameProfile>()
    private lateinit var brain: Brain
    private var generation = 0
    private var active: GameProfile? = null
    private var expiry = 0L
    private var actionCount = 0
    private var lastAction: Brain.Choice? = null
    private var lastBefore: LiveFrame? = null
    private var lastFrameTime = 0L
    private var isDispatching = false
    @Volatile private var lastWindowPackage: String? = null

    override fun onServiceConnected() {
        instance = this
        brain = Brain(this)
        Events.record(this, "Accessibility controller connected")
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString()
            if (!pkg.isNullOrBlank()) lastWindowPackage = pkg
        }
    }
    override fun onInterrupt() { stopAll() }
    override fun onDestroy() { stopAll(); instance = null; super.onDestroy() }

    fun enqueue(p: GameProfile) {
        main.post {
            if (queue.none { it.id == p.id } && active?.id != p.id) queue.addLast(p)
            if (active == null) startNext()
        }
    }
    fun stopAll() {
        generation++
        main.removeCallbacksAndMessages(null)
        queue.clear()
        active = null
        lastAction = null
        lastBefore = null
        isDispatching = false
        Events.record(this, "Stopped")
    }
    private fun startNext() {
        if (active != null || queue.isEmpty()) return
        val p = queue.removeFirst()
        if (getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true) {
            Events.record(this, "${p.title}: device locked; cannot interact")
            startNext()
            return
        }
        if (p.packageName.isBlank()) {
            Events.record(this, "${p.title}: no Android package configured")
            startNext()
            return
        }
        val launch = packageManager.getLaunchIntentForPackage(p.packageName)
        if (launch == null) {
            Events.record(this, "${p.title}: package unavailable or not launchable")
            startNext()
            return
        }
        active = p
        actionCount = 0
        lastAction = null
        lastBefore = null
        lastFrameTime = 0
        isDispatching = false
        expiry = System.currentTimeMillis() + p.maxMinutes * 60_000L
        generation++
        val id = generation
        try {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            startActivity(launch)
            Events.record(this, "Launching ${p.title}")
            main.postDelayed({ if (generation == id) {
                if (p.adaptive) adapt(p, id) else scripted(p, 0, id)
            } }, 5_000)
        } catch (error: Exception) {
            Events.record(this, "${p.title}: Android blocked app launch: ${error.javaClass.simpleName}")
            finish(id)
        }
    }
    private fun finish(id: Int) {
        if (id != generation) return
        Events.record(this, "Finished ${active?.title ?: "game"}")
        active = null
        lastAction = null
        lastBefore = null
        generation++
        main.post { startNext() }
    }
    private fun currentPackageMatches(p: GameProfile): Boolean {
        val node = rootInActiveWindow
        if (node == null) return lastWindowPackage == p.packageName
        return try { node.packageName?.toString() == p.packageName } finally { node.recycle() }
    }
    private fun adapt(p: GameProfile, id: Int) {
        if (id != generation) return
        if (System.currentTimeMillis() >= expiry || actionCount >= 500) { finish(id); return }
        if (isDispatching) { main.postDelayed({ adapt(p, id) }, 400); return }
        if (!currentPackageMatches(p)) {
            Events.record(this, "${p.title}: wrong foreground app; stopping")
            finish(id); return
        }
        val frame = VisionBus.latest
        if (frame == null || System.currentTimeMillis() - frame.time > 3_000) {
            Events.record(this, "${p.title}: no live frames; paused (projection authorization required)")
            main.postDelayed({ adapt(p, id) }, 3_000)
            return
        }
        if (frame.time == lastFrameTime) { main.postDelayed({ adapt(p, id) }, 500); return }
        lastFrameTime = frame.time
        val previous = lastAction
        val before = lastBefore
        if (previous != null && before != null) brain.learn(p, previous, before, frame)
        lastAction = null
        lastBefore = null
        val choice = brain.choose(p, frame)
        if (choice == null) {
            main.postDelayed({ adapt(p, id) }, 1_000)
            return
        }
        lastAction = choice
        lastBefore = frame
        actionCount++
        isDispatching = true
        dispatch(choice.action) { worked ->
            isDispatching = false
            if (id != generation) return@dispatch
            if (!worked) { Events.record(this, "${p.title}: gesture dispatch failed"); finish(id) }
            else main.postDelayed({ adapt(p, id) }, 1_200)
        }
    }
    private fun scripted(p: GameProfile, index: Int, id: Int) {
        if (id != generation) return
        if (System.currentTimeMillis() >= expiry || index >= p.actions.size) { finish(id); return }
        val step = p.actions[index]
        if (step is GameAction.Wait) {
            main.postDelayed({ scripted(p, index + 1, id) }, step.milliseconds.coerceIn(0, 600000))
        } else if (step is GameAction.ClickText) {
            if (clickText(rootInActiveWindow, step.text)) main.post { scripted(p, index + 1, id) }
            else { Events.record(this, "${p.title}: text missing: ${step.text}"); finish(id) }
        } else dispatch(step) { worked ->
            if (id == generation) {
                if (worked) main.post { scripted(p, index + 1, id) }
                else finish(id)
            }
        }
    }
    private fun clickText(node: AccessibilityNodeInfo?, text: String): Boolean {
        if (node == null) return false
        try {
            for (match in node.findAccessibilityNodeInfosByText(text)) {
                var candidate: AccessibilityNodeInfo? = match
                while (candidate != null) {
                    if (candidate.isClickable && candidate.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
                    candidate = candidate.parent
                }
            }
            return false
        } finally { node.recycle() }
    }
    private fun dispatch(action: GameAction, done: (Boolean) -> Unit) {
        val a = when (action) {
            is GameAction.Tap -> floatArrayOf(action.x, action.y, action.x, action.y)
            is GameAction.Swipe -> floatArrayOf(action.x1, action.y1, action.x2, action.y2)
            else -> { done(false); return }
        }
        val duration = when (action) {
            is GameAction.Tap -> action.duration
            is GameAction.Swipe -> action.duration
            else -> 80L
        }
        val metrics = resources.displayMetrics
        val path = Path().apply {
            moveTo(a[0].coerceIn(0f, 1f) * metrics.widthPixels, a[1].coerceIn(0f, 1f) * metrics.heightPixels)
            lineTo(a[2].coerceIn(0f, 1f) * metrics.widthPixels, a[3].coerceIn(0f, 1f) * metrics.heightPixels)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, duration.coerceIn(1, 10_000))
        val accepted = dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(),
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) { done(true) }
                override fun onCancelled(gestureDescription: GestureDescription?) { done(false) }
            }, main)
        if (!accepted) done(false)
    }
}
