package com.gamepilot

import android.Manifest
import java.io.ByteArrayOutputStream
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.widget.*

class AdvancedSettingsActivity : Activity() {
    private lateinit var container: LinearLayout
    private val projectionRequest = 101
    private val exportRequest = 102
    private val importRequest = 103

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
        Profiles.migrateGame26NoFixedTimeout(this)
        InstalledGames.autoLink(this)
        render()
        val id = intent.getIntExtra("edit_game_id", -1)
        if (id > 0) {
            val profile = Profiles.all(this).firstOrNull { it.id == id }
            if (profile == null) finish()
            else if (intent.getBooleanExtra("edit_goals", false)) editGoals(profile)
            else edit(profile)
        }
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
            GameAccessibilityService.instance?.stopAll(cancelPending = true)
            Toast.makeText(this, "Gameplay queue stopped", Toast.LENGTH_SHORT).show()
        }
        button("Stop live screen access") {
            stopService(Intent(this, LiveVisionService::class.java))
            render()
        }
        text("Status: ${Events.current}", 14)
        text("Pending scheduled jobs: ${ScheduledJobs.pendingCount(this)}", 13)
        text("Today's task completion is counted only after verified game evidence, never from a timer.", 13)
        button("Show activity / learning log") {
            AlertDialog.Builder(this).setTitle("Automation history")
                .setMessage(Events.history(this).take(5000).ifBlank { "No activity yet." })
                .setPositiveButton("Close", null).show()
        }
        button("Export profiles + learned game mastery") {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
                putExtra(Intent.EXTRA_TITLE, "AsterionPilot-learning-backup.json")
            }
            startActivityForResult(intent, exportRequest)
        }
        button("Restore saved game mastery") {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
            }
            startActivityForResult(intent, importRequest)
        }
        button("Auto-link installed games") {
            val linked = InstalledGames.autoLink(this)
            Toast.makeText(this, "$linked newly matched installed games", Toast.LENGTH_LONG).show()
            render()
        }
        button("Add another Android game") {
            val id = (Profiles.all(this).maxOfOrNull { it.id } ?: 0) + 1
            edit(GameProfile(id, "New Game", "", 6, 0, false, emptyList()))
        }
        text("Games (matched automatically where possible; tap Edit for schedules or unmatched titles)", 17)
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
                        Toast.makeText(this@AdvancedSettingsActivity, "Enable Accessibility first", Toast.LENGTH_LONG).show()
                    } else service.enqueue(p)
                }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(actions)
            val taskControls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            taskControls.addView(Button(this).apply {
                text = "Tasks / targets"
                setOnClickListener { editGoals(p) }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            taskControls.addView(Button(this).apply {
                text = "Results"
                setOnClickListener {
                    AlertDialog.Builder(this@AdvancedSettingsActivity)
                        .setTitle(p.title + " — today's verified results")
                        .setMessage(GoalLedger(this@AdvancedSettingsActivity).report(p.id))
                        .setPositiveButton("Close", null).show()
                }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(Button(this).apply {
                text = "Game-specific farming / daily playbook"
                setOnClickListener {
                    AlertDialog.Builder(this@AdvancedSettingsActivity)
                        .setTitle(p.title + " — gameplay plan")
                        .setMessage(GamePlaybooks.overview(p.id))
                        .setPositiveButton("Close", null).show()
                }
            })
            if (p.id == AttackOnTimeVideoProfile.GAME_ID) {
                row.addView(Button(this).apply {
                    text = "Training: 15-minute Attack on Time recording"
                    setOnClickListener {
                        val timeline = AttackOnTimeVideoProfile.observations.joinToString("\n\n") { item ->
                            val from = "%02d:%02d".format(item.startSecond / 60, item.startSecond % 60)
                            val until = "%02d:%02d".format(item.endSecond / 60, item.endSecond % 60)
                            "$from–$until  ${item.scene.name.replace('_', ' ')}\n${item.evidence}"
                        }
                        AlertDialog.Builder(this@AdvancedSettingsActivity)
                            .setTitle("Attack on Time — video demonstration")
                            .setMessage("Observed screens, not automatically completed tasks.\n\n" + timeline)
                            .setPositiveButton("Close", null).show()
                    }
                })
            }
            if (GameCodexBridge.accountFor(p.id) != null) {
                row.addView(Button(this).apply {
                    text = "Codex experimental strategy shortlist"
                    setOnClickListener {
                        AlertDialog.Builder(this@AdvancedSettingsActivity)
                            .setTitle(p.title + " — Codex character research")
                            .setMessage(CodexStrategyAdvisor.overview(this@AdvancedSettingsActivity, p.id))
                            .setPositiveButton("Close", null).show()
                    }
                })
                row.addView(Button(this).apply {
                    text = "Sync Game Codex • " + GameCodexBridge.accountFor(p.id)
                    setOnClickListener {
                        val result = GameCodexBridge.refresh(this@AdvancedSettingsActivity, p.id)
                        AlertDialog.Builder(this@AdvancedSettingsActivity)
                            .setTitle("Codex integration — " + result.state.name)
                            .setMessage(
                                "Account: " + result.account + "\\n" +
                                "Known owned characters: " + result.ownedCount + "\\n" +
                                "Details: " + result.note + "\\n\\n" +
                                GameCodexBridge.status(this@AdvancedSettingsActivity, p.id)
                            )
                            .setPositiveButton("Close", null).show()
                    }
                })
            }
            if (ShoujoKaisenResources.account(p.id) != null) {
                row.addView(Button(this).apply {
                    text = "Shoujo Kaisen — resource allocation rules"
                    setOnClickListener {
                        AlertDialog.Builder(this@AdvancedSettingsActivity)
                            .setTitle(p.title + " — resource policy")
                            .setMessage(ShoujoKaisenResources.overview(p.id))
                            .setPositiveButton("Close", null).show()
                    }
                })
            }
            row.addView(taskControls)
            row.addView(Button(this).apply {
                text = "Inspect learned gameplay history"
                setOnClickListener {
                    AlertDialog.Builder(this@AdvancedSettingsActivity)
                        .setTitle(p.title + " — learning trace")
                        .setMessage(EpisodeJournal(this@AdvancedSettingsActivity).recent(p.id, 30))
                        .setPositiveButton("Close", null).show()
                }
            })
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
                    Toast.makeText(this@AdvancedSettingsActivity, "No launchable apps found", Toast.LENGTH_LONG).show()
                } else AlertDialog.Builder(this@AdvancedSettingsActivity)
                    .setTitle("Select installed game")
                    .setItems(apps.map { "${it.first} (${it.second})" }.toTypedArray()) { _, i ->
                        pkg.setText(apps[i].second)
                    }.show()
            }
        })
        val hour = field(layout, "Local hour (0–23)", p.hour.toString())
        val minute = field(layout, "Minute (0–59)", p.minute.toString())
        val duration = field(layout, "Maximum session minutes (0 = until objectives complete)", p.maxMinutes.toString())
        val reset = field(layout, "Daily reset hour in phone local time (0–23)", p.resetHour.toString())
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
                    val resetAt = reset.text.toString().toInt()
                    require(h in 0..23 && m in 0..59 && cap >= 0 && resetAt in 0..23) { "Invalid time or duration" }
                    val script = steps.text.toString().lines().filter { it.isNotBlank() }.map(::parseStep)
                    val updated = p.copy(
                        title = name.text.toString().trim(),
                        packageName = pkg.text.toString().trim(),
                        hour = h, minute = m, enabled = enabled.isChecked,
                        adaptive = adaptive.isChecked, actions = script, maxMinutes = cap,
                        resetHour = resetAt
                    )
                    require(updated.title.isNotBlank()) { "Game name is required" }
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
        dialog.setOnDismissListener {
            if (intent.hasExtra("edit_game_id")) finish()
        }
        dialog.show()
    }

    private fun editGoals(p: GameProfile) {
        val goals = GoalSettings.get(this, p.id)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 12, 18, 12)
        }
        val controls = goals.map { goal ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            val check = CheckBox(this).apply {
                text = goal.title
                isChecked = goal.enabled
                row.addView(this)
            }
            val count = EditText(this).apply {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                hint = "Verified runs required"
                setText(goal.targetRuns.toString())
                row.addView(this)
            }
            val keywords = EditText(this).apply {
                hint = "Additional game UI words (comma separated)"
                setText(goal.customCues.joinToString(", "))
                row.addView(this)
            }
            layout.addView(row)
            Triple(check, count, keywords)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Objectives: ${p.title}")
            .setMessage("Every enabled goal must have verified in-game evidence. Add UI cues discovered from actual gameplay; unverified goals remain incomplete.")
            .setView(ScrollView(this).apply { addView(layout) })
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                try {
                    val updated = goals.mapIndexed { i, goal ->
                        val (check, count, keywords) = controls[i]
                        val target = count.text.toString().toInt()
                        require(target in 1..999) { "Runs must be from 1 to 999" }
                        goal.copy(
                            enabled = check.isChecked,
                            targetRuns = target,
                            customCues = keywords.text.toString().split(",")
                                .map { it.trim() }.filter { it.isNotBlank() }.take(30)
                        )
                    }
                    GoalSettings.save(this, p.id, updated)
                    dialog.dismiss()
                    render()
                } catch (error: Exception) {
                    Toast.makeText(this, error.message ?: "Invalid goals", Toast.LENGTH_LONG).show()
                }
            }
        }
        dialog.setOnDismissListener {
            if (intent.hasExtra("edit_game_id")) finish()
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
        } else if (resultCode == Activity.RESULT_OK && data?.data != null && requestCode == exportRequest) {
            try {
                val bytes = GameDataVault.export(this)
                contentResolver.openOutputStream(data.data!!, "w")?.use { it.write(bytes) }
                    ?: error("Cannot write selected file")
                Toast.makeText(this, "Exported ${bytes.size} bytes", Toast.LENGTH_LONG).show()
            } catch (error: Exception) {
                Toast.makeText(this, "Export failed: ${error.message}", Toast.LENGTH_LONG).show()
            }
        } else if (resultCode == Activity.RESULT_OK && data?.data != null && requestCode == importRequest) {
            val uri = data.data!!
            AlertDialog.Builder(this)
                .setTitle("Restore profiles and learned game data?")
                .setMessage("This replaces existing profiles, objective history and learned trajectories.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Restore") { _, _ ->
                    try {
                        val bytes = contentResolver.openInputStream(uri)?.use { input ->
                            val buffer = ByteArrayOutputStream()
                            val chunk = ByteArray(32768)
                            var total = 0
                            while (true) {
                                val length = input.read(chunk)
                                if (length < 0) break
                                total += length
                                require(total <= 24 * 1024 * 1024) { "Backup exceeds 24 MB" }
                                buffer.write(chunk, 0, length)
                            }
                            buffer.toByteArray()
                        } ?: error("Cannot read backup")
                        GameDataVault.restore(this, bytes)
                        render()
                        Toast.makeText(this, "Game learning restored", Toast.LENGTH_LONG).show()
                    } catch (error: Exception) {
                        Toast.makeText(this, "Restore failed: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                }.show()
        }
    }
}
