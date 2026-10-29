package com.gamepilot

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private lateinit var container: LinearLayout
    private val projectionRequest = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 20, 24, 20)
        }
        setContentView(ScrollView(this).apply { addView(container) })
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 30)
        }
        render()
    }
    override fun onResume() { super.onResume(); if (::container.isInitialized) render() }

    private fun render() {
        container.removeAllViews()
        text("Asterion Pilot", 24)
        text("26 game profiles • live perception • direct gestures • on-device policy learning", 15)
        text("Accessibility: ${if (GameAccessibilityService.instance == null) "Not connected" else "Connected"} | Live frames: ${if (LiveVisionService.running) "Running" else "Off"}", 14)
        text("Android requires approving each new live display-capture session. Screen lock and background launches can block automation.", 13)
        button("1. Enable direct screen control") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        button("2. Start live screen perception") {
            val manager = getSystemService(MediaProjectionManager::class.java)
            startActivityForResult(manager.createScreenCaptureIntent(), projectionRequest)
        }
        button("3. Configure exact scheduling permission") {
            if (Build.VERSION.SDK_INT >= 31) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
        }
        button("Stop all gameplay") {
            GameAccessibilityService.instance?.stopAll()
            Toast.makeText(this, "Gameplay queue stopped", Toast.LENGTH_SHORT).show()
        }
        button("Stop live screen access") {
            stopService(Intent(this, LiveVisionService::class.java))
            render()
        }
        text("Status: ${Events.current}", 14)
        button("Show activity / learning log") {
            AlertDialog.Builder(this).setTitle("Automation history")
                .setMessage(Events.history(this).take(5000).ifBlank { "No activity yet." })
                .setPositiveButton("Close", null).show()
        }
        text("Games (tap Edit to set installed package, goals and schedule)", 17)
        Profiles.all(this).forEach { p ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            val status = if (p.enabled) "Scheduled" else "Off"
            row.addView(TextView(this).apply {
                text = "${p.id}. ${p.title} • $status • %02d:%02d • ${if (p.packageName.isBlank()) "Unlinked" else "Linked"}".format(p.hour, p.minute)
                textSize = 15f
            })
            val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            actions.addView(Button(this).apply {
                text = "Edit"
                setOnClickListener { edit(p) }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            actions.addView(Button(this).apply {
                text = "Run now"
                isEnabled = p.packageName.isNotBlank()
                setOnClickListener {
                    val service = GameAccessibilityService.instance
                    if (service == null) {
                        Toast.makeText(this@MainActivity, "Enable Accessibility first", Toast.LENGTH_LONG).show()
                    } else service.enqueue(p)
                }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(actions)
            row.setPadding(0, 15, 0, 15)
            container.addView(row)
        }
    }

    private fun edit(p: GameProfile) {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(14, 14, 14, 14) }
        val name = field(layout, "Name", p.title)
        val pkg = field(layout, "Game's Android package name", p.packageName)
        layout.addView(Button(this).apply {
            text = "Find installed games"
            setOnClickListener {
                val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                val apps = packageManager.queryIntentActivities(launcher, 0)
                    .map { it.loadLabel(packageManager).toString() to it.activityInfo.packageName }
                    .distinctBy { it.second }
                    .sortedBy { it.first.lowercase() }
                if (apps.isEmpty()) {
                    Toast.makeText(this@MainActivity, "No launchable apps found", Toast.LENGTH_LONG).show()
                } else AlertDialog.Builder(this@MainActivity)
                    .setTitle("Select installed game")
                    .setItems(apps.map { "${it.first} (${it.second})" }.toTypedArray()) { _, i ->
                        pkg.setText(apps[i].second)
                    }.show()
            }
        })
        val hour = field(layout, "Local hour (0–23)", p.hour.toString())
        val minute = field(layout, "Minute (0–59)", p.minute.toString())
        val duration = field(layout, "Maximum session minutes (1–120)", p.maxMinutes.toString())
        val enabled = CheckBox(this).apply {
            text = "Enable daily scheduled run"
            isChecked = p.enabled
            layout.addView(this)
        }
        val adaptive = CheckBox(this).apply {
            text = "Adaptive live vision and learning (unchecked: exact scripted sequence)"
            isChecked = p.adaptive
            layout.addView(this)
        }
        val steps = field(layout,
            "Optional action examples: tap 0.5 0.8 | swipe 0.8 0.5 0.2 0.5 | wait 1000 | text Claim",
            p.actions.joinToString("\n") { step ->
                when (step) {
                    is GameAction.Tap -> "tap ${step.x} ${step.y}"
                    is GameAction.Swipe -> "swipe ${step.x1} ${step.y1} ${step.x2} ${step.y2}"
                    is GameAction.Wait -> "wait ${step.milliseconds}"
                    is GameAction.ClickText -> "text ${step.text}"
                }
            }).apply { minLines = 4 }
        val dialog = AlertDialog.Builder(this).setTitle("Game ${p.id}")
            .setView(ScrollView(this).apply { addView(layout) })
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                try {
                    val h = hour.text.toString().toInt()
                    val m = minute.text.toString().toInt()
                    val cap = duration.text.toString().toInt()
                    require(h in 0..23 && m in 0..59 && cap in 1..120) { "Invalid time or duration" }
                    val script = steps.text.toString().lines().filter { it.isNotBlank() }.map(::parseStep)
                    val updated = p.copy(
                        title = name.text.toString().trim(),
                        packageName = pkg.text.toString().trim(),
                        hour = h, minute = m, enabled = enabled.isChecked,
                        adaptive = adaptive.isChecked, actions = script, maxMinutes = cap
                    )
                    require(!updated.enabled || updated.packageName.isNotBlank()) { "Choose an installed game before enabling its schedule" }
                    Profiles.save(this, Profiles.all(this).filter { it.id != updated.id } + updated)
                    Scheduler.schedule(this, updated)
                    dialog.dismiss()
                    render()
                } catch (error: Exception) {
                    Toast.makeText(this, error.message ?: "Invalid settings", Toast.LENGTH_LONG).show()
                }
            }
        }
        dialog.show()
    }

    private fun parseStep(line: String): GameAction {
        val values = line.trim().split(Regex("\\s+"))
        fun fraction(value: String): Float = value.toFloat().also { require(it in 0f..1f) }
        return when (values[0].lowercase()) {
            "tap" -> {
                require(values.size == 3)
                GameAction.Tap(fraction(values[1]), fraction(values[2]))
            }
            "swipe" -> {
                require(values.size == 5)
                GameAction.Swipe(fraction(values[1]), fraction(values[2]), fraction(values[3]), fraction(values[4]))
            }
            "wait" -> {
                require(values.size == 2)
                GameAction.Wait(values[1].toLong().also { require(it in 0..600000) })
            }
            "text" -> GameAction.ClickText(line.substringAfter(" ").trim().also { require(it.isNotBlank()) })
            else -> error("Invalid action: ${values[0]}")
        }
    }
    private fun field(layout: LinearLayout, hint: String, value: String): EditText =
        EditText(this).apply {
            this.hint = hint
            setText(value)
            layout.addView(this, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    private fun text(value: String, size: Int) {
        container.addView(TextView(this).apply {
            text = value; textSize = size.toFloat(); setPadding(0, 9, 0, 9)
        })
    }
    private fun button(label: String, action: () -> Unit) {
        container.addView(Button(this).apply { text = label; setOnClickListener { action() } })
    }

    @Deprecated("Uses Activity result to preserve the dependency-free Activity implementation.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == projectionRequest && resultCode == Activity.RESULT_OK && data != null) {
            val intent = Intent(this, LiveVisionService::class.java)
                .putExtra(LiveVisionService.EXTRA_CODE, resultCode)
                .putExtra(LiveVisionService.EXTRA_DATA, data)
            startForegroundService(intent)
        } else if (requestCode == projectionRequest) {
            Toast.makeText(this, "Live screen permission was not granted", Toast.LENGTH_LONG).show()
        }
    }
}
