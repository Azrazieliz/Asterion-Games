package com.gamepilot

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.ByteArrayOutputStream

/**
 * Unified single-app home. Full existing Codex runs as an embedded activity,
 * while all Pilot settings and learning data continue in this same package.
 */
class MainActivity : Activity() {
    private enum class Tab { GAMES, AUTOMATION, CODEX }
    private val ink = Color.rgb(237, 242, 250)
    private val muted = Color.rgb(151, 164, 183)
    private val dark = Color.rgb(12, 17, 28)
    private val panel = Color.rgb(25, 34, 49)
    private val accent = Color.rgb(94, 164, 238)
    private val outline = Color.rgb(50, 64, 84)
    private val projectionRequest = 101
    private val exportRequest = 102
    private val importRequest = 103
    private var tab = Tab.GAMES
    private var selectedGameId: Int? = null
    private lateinit var root: LinearLayout
    private lateinit var area: LinearLayout

    private fun dp(n: Int): Int = (n * resources.displayMetrics.density).toInt()
    private fun bg(color: Int, radius: Int = 17, border: Boolean = false): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (border) setStroke(dp(1), outline)
        }
    private fun label(value: String, size: Float = 14f, color: Int = ink,
                      bold: Boolean = false): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        includeFontPadding = true
    }
    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = bg(panel, 16, true)
        setPadding(dp(16), dp(15), dp(16), dp(15))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(11) }
    }
    private fun action(title: String, primary: Boolean = false, click: () -> Unit): Button =
        Button(this).apply {
            text = title
            textSize = 13f
            isAllCaps = false
            setTextColor(if (primary) dark else ink)
            background = bg(if (primary) accent else Color.rgb(37, 50, 69), 12)
            setOnClickListener { click() }
            minHeight = dp(44)
            minimumHeight = dp(44)
        }
    private fun verticalSpace(px: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(px))
    }
    private fun headline(title: String, subtitle: String): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(label(title, 21f, ink, true))
            addView(label(subtitle, 12.5f, muted))
            addView(verticalSpace(14))
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = dark
        window.navigationBarColor = dark
        window.decorView.systemUiVisibility = 0
        Profiles.migrateGame26NoFixedTimeout(this)
        InstalledGames.autoLink(this)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 30)
        render()
    }
    override fun onResume() {
        super.onResume()
        if (::root.isInitialized) render()
    }
    private fun render() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(dark)
        }
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(19), dp(16), dp(19), dp(10))
            addView(label("ASTERION", 24f, ink, true))
            addView(label("Game automation and character intelligence", 12f, muted))
        })
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(12), dp(4), dp(12), dp(12))
        }
        listOf(Tab.GAMES to "Games", Tab.AUTOMATION to "Automation", Tab.CODEX to "Game Codex")
            .forEach { (option, text) ->
                nav.addView(action(text, tab == option) {
                    tab = option
                    selectedGameId = null
                    render()
                }, LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                    marginStart = dp(4); marginEnd = dp(4)
                })
            }
        root.addView(nav)
        val scroll = ScrollView(this).apply {
            fillViewport = true
            isVerticalScrollBarEnabled = false
        }
        area = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(5), dp(16), dp(26))
        }
        scroll.addView(area)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        when (tab) {
            Tab.GAMES -> showGames()
            Tab.AUTOMATION -> showAutomation()
            Tab.CODEX -> showCodex()
        }
    }
    private fun showGames() {
        val selected = selectedGameId?.let { id -> Profiles.all(this).firstOrNull { it.id == id } }
        if (selected != null) { gameDetail(selected); return }
        val profiles = Profiles.all(this)
        val linked = profiles.count { it.packageName.isNotBlank() }
        val active = profiles.count { it.enabled }
        area.addView(headline("Your games", "${profiles.size} profiles  •  $linked linked  •  $active scheduled"))
        val find = EditText(this).apply {
            singleLine = true
            hint = "Search games"
            textSize = 15f
            setTextColor(ink)
            setHintTextColor(muted)
            setPadding(dp(15), dp(8), dp(15), dp(8))
            background = bg(panel, 12, true)
        }
        area.addView(find, LinearLayout.LayoutParams(-1, dp(52)))
        area.addView(verticalSpace(14))
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        area.addView(list)
        fun fill(query: String) {
            list.removeAllViews()
            profiles.sortedWith(compareBy<GameProfile> {
                if (it.id == 26) 0 else if (it.id == 22) 1 else 2
            }.thenBy { it.id }).filter {
                it.title.contains(query, true) || it.packageName.contains(query, true)
            }.forEach { game ->
                val c = card()
                c.addView(label(game.title, 16f, ink, true))
                val state = when {
                    game.packageName.isBlank() -> "Not linked"
                    game.enabled -> "Scheduled  •  %02d:%02d".format(game.hour, game.minute)
                    else -> "Linked  •  Schedule off"
                }
                c.addView(label(state, 12.5f, if (game.enabled) accent else muted))
                c.setOnClickListener { selectedGameId = game.id; render() }
                list.addView(c)
            }
        }
        fill("")
        find.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                fill(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        area.addView(action("Find installed games") {
            val linkedNow = InstalledGames.autoLink(this)
            Toast.makeText(this, "$linkedNow newly linked games", Toast.LENGTH_SHORT).show()
            render()
        }, LinearLayout.LayoutParams(-1, dp(48)))
        area.addView(verticalSpace(9))
        area.addView(action("Add a game") {
            val id = (Profiles.all(this).maxOfOrNull { it.id } ?: 0) + 1
            Profiles.save(this, Profiles.all(this) +
                GameProfile(id, "New Game", "", 6, 0, false, emptyList()))
            openEditor(id, false)
        }, LinearLayout.LayoutParams(-1, dp(48)))
    }
    private fun gameDetail(p: GameProfile) {
        area.addView(action("Back to games") {
            selectedGameId = null
            render()
        }, LinearLayout.LayoutParams(-1, dp(46)))
        area.addView(verticalSpace(14))
        area.addView(headline(p.title,
            (if (p.packageName.isBlank()) "Not linked" else "Installed game linked") +
                "  •  " + (if (p.enabled) "Scheduled" else "Not scheduled")))
        val summary = card().apply {
            addView(label("Today's objectives", 16f, ink, true))
            addView(verticalSpace(8))
            addView(label(GoalLedger(this@MainActivity).report(p.id), 13f, muted))
        }
        area.addView(summary)
        area.addView(action("Run now", true) {
            val ctl = GameAccessibilityService.instance
            when {
                p.packageName.isBlank() -> Toast.makeText(this, "Link the installed game first", Toast.LENGTH_LONG).show()
                ctl == null -> Toast.makeText(this, "Enable game control in Automation first", Toast.LENGTH_LONG).show()
                else -> ctl.enqueue(p)
            }
        }, LinearLayout.LayoutParams(-1, dp(50)).also { })
        area.addView(verticalSpace(9))
        area.addView(action("Schedule and game settings") { openEditor(p.id, false) },
            LinearLayout.LayoutParams(-1, dp(49)))
        area.addView(verticalSpace(9))
        area.addView(action("Daily goals") { openEditor(p.id, true) },
            LinearLayout.LayoutParams(-1, dp(49)))
        area.addView(verticalSpace(9))
        area.addView(action("Gameplay history") {
            dialog("Learned gameplay", EpisodeJournal(this).recent(p.id))
        }, LinearLayout.LayoutParams(-1, dp(49)))
        area.addView(verticalSpace(9))
        area.addView(action("Game playbook") {
            dialog("Gameplay rules", GamePlaybooks.overview(p.id) +
                if (ShoujoKaisenResources.account(p.id) != null)
                    "\n\n" + ShoujoKaisenResources.overview(p.id) else "")
        }, LinearLayout.LayoutParams(-1, dp(49)))
        if (GameCodexBridge.accountFor(p.id) != null) {
            area.addView(verticalSpace(9))
            area.addView(action("Open character knowledge") {
                tab = Tab.CODEX; selectedGameId = null; render()
            }, LinearLayout.LayoutParams(-1, dp(49)))
        }
    }
    private fun showAutomation() {
        area.addView(headline("Automation", "Controls, permissions and current activity"))
        val service = GameAccessibilityService.instance != null
        val capture = LiveVisionService.running
        area.addView(card().apply {
            addView(label("System readiness", 17f, ink, true))
            addView(verticalSpace(9))
            addView(label("Game control: " + if (service) "Connected" else "Not connected",
                14f, if (service) accent else muted))
            addView(label("Live screen perception: " + if (capture) "Active" else "Not active",
                14f, if (capture) accent else muted))
            addView(label("Queued games: ${ScheduledJobs.pendingCount(this@MainActivity)}", 14f, muted))
            addView(verticalSpace(9))
            addView(label("Android may require capture permission again after a new session.",
                12f, muted))
        })
        val controls = card().apply { addView(label("Set up once, then run", 16f, ink, true)) }
        controls.addView(verticalSpace(8))
        controls.addView(action("Enable game control", true) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }, LinearLayout.LayoutParams(-1, dp(48)))
        controls.addView(verticalSpace(8))
        controls.addView(action("Start live perception") {
            val mgr = getSystemService(MediaProjectionManager::class.java)
            startActivityForResult(mgr.createScreenCaptureIntent(), projectionRequest)
        }, LinearLayout.LayoutParams(-1, dp(48)))
        controls.addView(verticalSpace(8))
        controls.addView(action("Scheduling permission") {
            if (Build.VERSION.SDK_INT >= 31)
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
        }, LinearLayout.LayoutParams(-1, dp(48)))
        area.addView(controls)
        area.addView(card().apply {
            addView(label("Session controls", 16f, ink, true))
            addView(verticalSpace(8))
            addView(action("Stop all game sessions") {
                GameAccessibilityService.instance?.stopAll(cancelPending = true)
                render()
            }, LinearLayout.LayoutParams(-1, dp(48)))
            addView(verticalSpace(8))
            addView(action("Stop live perception") {
                stopService(Intent(this@MainActivity, LiveVisionService::class.java))
                render()
            }, LinearLayout.LayoutParams(-1, dp(48)))
            addView(verticalSpace(8))
            addView(action("See activity log") {
                dialog("Activity log", Events.history(this@MainActivity).take(9000)
                    .ifBlank { "No activity recorded yet." })
            }, LinearLayout.LayoutParams(-1, dp(48)))
        })
        area.addView(card().apply {
            addView(label("Backups and advanced settings", 16f, ink, true))
            addView(verticalSpace(8))
            addView(action("Export learning data") { chooseBackup(true) },
                LinearLayout.LayoutParams(-1, dp(48)))
            addView(verticalSpace(8))
            addView(action("Restore learning data") { chooseBackup(false) },
                LinearLayout.LayoutParams(-1, dp(48)))
            addView(verticalSpace(8))
            addView(action("Advanced settings") {
                startActivity(Intent(this@MainActivity, AdvancedSettingsActivity::class.java))
            }, LinearLayout.LayoutParams(-1, dp(48)))
        })
    }
    private fun showCodex() {
        area.addView(headline("Game Codex", "Characters, equipment, teams and analysis in this app"))
        area.addView(card().apply {
            addView(label("Shoujo Kaisen knowledge", 18f, ink, true))
            addView(verticalSpace(6))
            addView(label("Full original character catalogue, artwork, rankings, team builder and equipment systems.",
                13f, muted))
            addView(verticalSpace(14))
            addView(action("Open Game Codex", true) {
                startActivity(Intent(this@MainActivity, net.gamecodex.app.MainActivity::class.java))
            }, LinearLayout.LayoutParams(-1, dp(53)))
        })
        listOf(26 to "Global account", 22 to "Japanese account").forEach { (gameId, name) ->
            area.addView(card().apply {
                addView(label(name, 16f, ink, true))
                addView(verticalSpace(6))
                addView(label(GameCodexBridge.status(this@MainActivity, gameId), 13f, muted))
                addView(verticalSpace(9))
                addView(action("Refresh character data") {
                    val result = GameCodexBridge.refresh(this@MainActivity, gameId)
                    dialog(name, "${result.state}: ${result.ownedCount} owned entries\n${result.note}")
                    render()
                }, LinearLayout.LayoutParams(-1, dp(46)))
                addView(verticalSpace(6))
                addView(action("Character strategy overview") {
                    dialog(name, CodexStrategyAdvisor.overview(this@MainActivity, gameId))
                }, LinearLayout.LayoutParams(-1, dp(46)))
            })
        }
        area.addView(label("Game Codex data is research evidence, not permission to spend resources or grade heroes.",
            12f, muted))
    }
    private fun dialog(title: String, message: String) {
        AlertDialog.Builder(this).setTitle(title).setMessage(message)
            .setPositiveButton("Close", null).show()
    }
    private fun openEditor(gameId: Int, goals: Boolean) {
        startActivity(Intent(this, AdvancedSettingsActivity::class.java)
            .putExtra("edit_game_id", gameId).putExtra("edit_goals", goals))
    }
    private fun chooseBackup(export: Boolean) {
        val intent = Intent(if (export) Intent.ACTION_CREATE_DOCUMENT else Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            if (export) putExtra(Intent.EXTRA_TITLE, "Asterion-learning-backup.json")
        }
        startActivityForResult(intent, if (export) exportRequest else importRequest)
    }
    @Deprecated("Activity results maintained for existing Android 29+ support.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == projectionRequest) {
            if (resultCode == RESULT_OK && data != null) {
                startForegroundService(Intent(this, LiveVisionService::class.java)
                    .putExtra(LiveVisionService.EXTRA_CODE, resultCode)
                    .putExtra(LiveVisionService.EXTRA_DATA, data))
            } else Toast.makeText(this, "Live perception permission not granted", Toast.LENGTH_LONG).show()
            return
        }
        val uri = data?.data ?: return
        if (resultCode != RESULT_OK) return
        if (requestCode == exportRequest) {
            try {
                val payload = GameDataVault.export(this)
                contentResolver.openOutputStream(uri, "w")?.use { it.write(payload) }
                    ?: error("Cannot write backup")
                Toast.makeText(this, "Learning backup exported", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else if (requestCode == importRequest) {
            AlertDialog.Builder(this).setTitle("Restore learning data?")
                .setMessage("This replaces the Pilot profiles, learning history and goal records.")
                .setNegativeButton("Cancel", null).setPositiveButton("Restore") { _, _ ->
                    try {
                        val buffer = ByteArrayOutputStream()
                        contentResolver.openInputStream(uri)?.use { input ->
                            val chunk = ByteArray(32768)
                            while (true) {
                                val n = input.read(chunk)
                                if (n < 0) break
                                require(buffer.size() + n <= 24 * 1024 * 1024) { "File too large" }
                                buffer.write(chunk, 0, n)
                            }
                        } ?: error("Cannot read backup")
                        GameDataVault.restore(this, buffer.toByteArray())
                        render()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Restore failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }.show()
        }
    }
    override fun onBackPressed() {
        when {
            tab == Tab.GAMES && selectedGameId != null -> { selectedGameId = null; render() }
            tab != Tab.GAMES -> { tab = Tab.GAMES; render() }
            else -> super.onBackPressed()
        }
    }
}
