package com.gamepilot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.KeyguardManager
import android.content.Intent
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
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
    private var missingFramesSince = 0L
    private var noCandidatesSince = 0L
    private var wrongAppChecks = 0
    private var launchStartedAt = 0L
    private var foregroundConfirmed = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var lastRecoveryTime = 0L
    @Volatile private var lastWindowPackage: String? = null

    override fun onServiceConnected() {
        instance = this
        brain = Brain(this)
        Profiles.migrateGame26NoFixedTimeout(this)
        Events.record(this, "Accessibility controller connected")
        if (AutomationControl.running(this)) ScheduledJobs.recover(this).forEach(::enqueue)
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString()
            if (!pkg.isNullOrBlank()) lastWindowPackage = pkg
            val now = System.currentTimeMillis()
            if (AutomationControl.running(this) && now - lastRecoveryTime > 20_000 &&
                getSystemService(KeyguardManager::class.java)?.isKeyguardLocked != true) {
                lastRecoveryTime = now
                ScheduledJobs.recover(this).forEach(::enqueue)
            }
        }
    }
    override fun onInterrupt() { stopAll() }
    override fun onDestroy() { stopAll(); instance = null; super.onDestroy() }

    fun enqueue(p: GameProfile) {
        if (!AutomationControl.running(this)) {
            if (AutomationControl.enabled(this)) ScheduledJobs.enqueue(this, p)
            return
        }
        main.post {
            if (!AutomationControl.running(this)) {
                if (AutomationControl.enabled(this)) ScheduledJobs.enqueue(this, p)
                return@post
            }
            if (queue.none { it.id == p.id } && active?.id != p.id) queue.addLast(p)
            if (active == null) startNext()
        }
    }
    /** Immediately invalidates queued callbacks; keeps active and queued jobs persistent. */
    fun pauseAndRetain() {
        active?.let { ScheduledJobs.enqueue(this, it) }
        queue.forEach { ScheduledJobs.enqueue(this, it) }
        stopAll(cancelPending = false)
    }
    /** No blind continuation from stale coordinates; restart from live screen state. */
    fun recoverPending() {
        if (!AutomationControl.running(this)) return
        ScheduledJobs.recover(this, ignoreCooldown = true).forEach(::enqueue)
    }
    fun stopAll(cancelPending: Boolean = false) {
        generation++
        main.removeCallbacksAndMessages(null)
        queue.clear()
        active = null
        lastAction = null
        lastBefore = null
        isDispatching = false
        if (::brain.isInitialized) brain.resetSession()
        releaseWakeLock()
        if (cancelPending) ScheduledJobs.cancelAll(this)
        Events.record(this, if (cancelPending) "User stopped all jobs" else "Controller stopped")
    }
    private fun startNext() {
        if (!AutomationControl.running(this)) { stopAll(); return }
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
        if (GameCodexBridge.accountFor(p.id) != null) {
            val sync = GameCodexBridge.refresh(this, p.id)
            Events.record(this, "${p.title}: codex " + sync.state.name + " (" +
                sync.ownedCount + " character entries)")
        }
        active = p
        actionCount = 0
        lastAction = null
        lastBefore = null
        lastFrameTime = 0
        isDispatching = false
        wrongAppChecks = 0
        foregroundConfirmed = false
        launchStartedAt = System.currentTimeMillis()
        missingFramesSince = 0L
        noCandidatesSince = 0L
        brain.resetSession()
        GoalLedger(this).reopenForNextSession(p.id)
        expiry = if (p.maxMinutes == 0) Long.MAX_VALUE
            else System.currentTimeMillis() + p.maxMinutes.toLong() * 60_000L
        generation++
        val id = generation
        try {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            startActivity(launch)
            ScheduledJobs.started(this, p)
            acquireWakeLock(if (p.maxMinutes == 0) 3_600_000L
                else p.maxMinutes.toLong() * 60_000L + 10_000L)
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
        active?.let { game ->
            Events.record(this, "Session ended ${game.title}. VERIFIED goals only: ${brain.summary(game)}")
            val hasObjectives = GoalSettings.get(this, game.id).any { it.enabled }
            ScheduledJobs.finished(this, game, hasObjectives && brain.complete(game))
        }
        active = null
        lastAction = null
        lastBefore = null
        releaseWakeLock()
        generation++
        main.post { startNext() }
    }
    private fun acquireWakeLock(maxDuration: Long) {
        releaseWakeLock()
        try {
            val manager = getSystemService(PowerManager::class.java)
            wakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AsterionPilot:game-session")
                .apply { setReferenceCounted(false); acquire(maxDuration.coerceIn(1000L, 7_300_000L)) }
        } catch (error: Exception) {
            Events.record(this, "CPU wakelock unavailable: ${error.javaClass.simpleName}")
        }
    }
    private fun releaseWakeLock() {
        try { if (wakeLock?.isHeld == true) wakeLock?.release() } catch (_: Exception) {}
        wakeLock = null
    }
    private fun currentPackageMatches(p: GameProfile): Boolean {
        val node = rootInActiveWindow
        if (node == null) return lastWindowPackage == p.packageName
        return try { node.packageName?.toString() == p.packageName } finally { node.recycle() }
    }
    private fun adapt(p: GameProfile, id: Int) {
        if (id != generation) return
        if (System.currentTimeMillis() >= expiry || (p.maxMinutes != 0 && actionCount >= 500)) {
            brain.unresolved(p, "Session limit; no verified completion")
            finish(id)
            return
        }
        if (p.maxMinutes == 0 && wakeLock?.isHeld != true) acquireWakeLock(3_600_000L)
        if (isDispatching) { main.postDelayed({ adapt(p, id) }, 400); return }
        if (!currentPackageMatches(p)) {
            wrongAppChecks++
            if ((!foregroundConfirmed && System.currentTimeMillis() - launchStartedAt < 90_000L) ||
                wrongAppChecks <= 3) {
                main.postDelayed({ adapt(p, id) }, 1500)
                return
            }
            Events.record(this, "${p.title}: foreground app mismatch; no input dispatched")
            brain.unresolved(p, "Wrong foreground app")
            finish(id); return
        }
        wrongAppChecks = 0
        foregroundConfirmed = true
        val live = VisionBus.latest
        val frame = if (live != null && System.currentTimeMillis() - live.time <= 3_000L) live
            else UiTreePerception.snapshot(rootInActiveWindow, resources.displayMetrics)
        if (frame == null || System.currentTimeMillis() - frame.time > 3_000) {
            if (missingFramesSince == 0L) {
                missingFramesSince = System.currentTimeMillis()
                Events.record(this, "${p.title}: no live pixels or accessible UI elements; cannot navigate without perception")
            }
            if (System.currentTimeMillis() - missingFramesSince >= 30_000) {
                brain.unresolved(p, "Live perception unavailable for 30 seconds")
                finish(id)
            } else main.postDelayed({ adapt(p, id) }, 1500)
            return
        }
        if (!AutomationControl.running(this)) { stopAll(cancelPending = !AutomationControl.enabled(this)); return }
        GameAccounts.observe(this, p, frame)
        missingFramesSince = 0L
        if (frame.time == lastFrameTime) { main.postDelayed({ adapt(p, id) }, 500); return }
        lastFrameTime = frame.time
        val previous = lastAction
        val before = lastBefore
        if (previous != null && before != null) brain.learn(p, previous, before, frame)
        lastAction = null
        lastBefore = null
        if (brain.confirmPending(p, frame)) {
            main.postDelayed({ adapt(p, id) }, 750)
            return
        }
        if (brain.complete(p)) { finish(id); return }
        val choice = brain.choose(p, frame)
        if (choice == null) {
            if (noCandidatesSince == 0L) noCandidatesSince = System.currentTimeMillis()
            val startupGrace = System.currentTimeMillis() - launchStartedAt < 120_000L
            if (!startupGrace && System.currentTimeMillis() - noCandidatesSince >= 45_000L) {
                brain.unresolved(p, "No relevant or safe action found after startup grace")
                finish(id)
            } else main.postDelayed({ adapt(p, id) }, 1_000)
            return
        }
        noCandidatesSince = 0L
        lastAction = choice
        lastBefore = frame
        actionCount++
        isDispatching = true
        dispatch(choice.action) { worked ->
            isDispatching = false
            if (id != generation) return@dispatch
            if (!worked) {
                Events.record(this, "${p.title}: gesture dispatch failed")
                brain.unresolved(p, "Gesture dispatch failed")
                finish(id)
            } else {
                brain.actionSent(p, choice)
                main.postDelayed({ adapt(p, id) }, 1_200)
            }
        }
    }
    private fun scripted(p: GameProfile, index: Int, id: Int) {
        if (id != generation) return
        if (System.currentTimeMillis() >= expiry || index >= p.actions.size) {
            // Script execution is not evidence of completion.
            Events.record(this, "${p.title}: scripted steps ended without goal verification")
            finish(id)
            return
        }
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
