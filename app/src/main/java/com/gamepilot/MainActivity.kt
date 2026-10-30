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
import android.view.WindowInsets
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
    private enum class Tab { GAMES, AUTOMATION, NEWS }
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
    private var newsLoading = false
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
        AutomationNotification.refresh(this)
        ImportantGameNews.schedule(this)
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
        if (Build.VERSION.SDK_INT >= 30) {
            root.setOnApplyWindowInsetsListener { view, insets ->
                val bars = insets.getInsets(
                    WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                view.setPadding(0, bars.top, 0, bars.bottom)
                insets
            }
        } else root.fitsSystemWindows = true
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(19), dp(16), dp(19), dp(10))
            addView(label("ASTERION", 24f, ink, true))
            addView(label(SessionStatus.description(this@MainActivity).lineSequence().firstOrNull() ?: "Game automation", 12f, muted))
            addView(verticalSpace(12))
            val state = AutomationControl.mode(this@MainActivity)
            val powerRow = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            fun controlButton(text: String, primary: Boolean, operation: () -> Unit) {
                powerRow.addView(action(text, primary, operation),
                    LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                        marginEnd = dp(5)
                    })
            }
            when (state) {
                AutomationControl.Mode.RUNNING -> {
                    controlButton("Pause", true) {
                        AutomationControl.setMode(this@MainActivity, AutomationControl.Mode.PAUSED)
                        render()
                    }
                    controlButton("Turn off", false) {
                        AutomationControl.setMode(this@MainActivity, AutomationControl.Mode.OFF)
                        render()
                    }
                }
                AutomationControl.Mode.PAUSED -> {
                    controlButton("Resume", true) {
                        AutomationControl.setMode(this@MainActivity, AutomationControl.Mode.RUNNING)
                        render()
                    }
                    controlButton("Turn off", false) {
                        AutomationControl.setMode(this@MainActivity, AutomationControl.Mode.OFF)
                        render()
                    }
                }
                AutomationControl.Mode.OFF -> {
                    controlButton("Turn on", true) {
                        AutomationControl.setMode(this@MainActivity, AutomationControl.Mode.RUNNING)
                        render()
                    }
                }
            }
            addView(powerRow)
            addView(label(when(state) {
                AutomationControl.Mode.RUNNING -> "ACTIVE • automation may run scheduled tasks"
                AutomationControl.Mode.PAUSED -> "PAUSED • sessions held, schedules preserved"
                AutomationControl.Mode.OFF -> "OFF • sessions and alarms cancelled"
            }, 12f, if (state == AutomationControl.Mode.RUNNING) accent else muted))        })
        // Bottom navigation leaves the main content in full view.
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(8), dp(6), dp(8), dp(8))
            background = bg(panel, 0)
        }
        listOf(
            Tab.GAMES to "Games",
            Tab.AUTOMATION to "Activity",
            Tab.NEWS to "News"
        ).forEach { (target, name) ->
            nav.addView(action(name, tab == target) {
                tab = target
                selectedGameId = null
                render()
            }, LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginStart=dp(3); marginEnd=dp(3)
            })
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
        }
        area = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(5), dp(16), dp(26))
        }
        scroll.addView(area)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(nav)
        setContentView(root)
        when (tab) {
            Tab.GAMES -> showGames()
            Tab.AUTOMATION -> showAutomation()
            Tab.NEWS -> showNews()
        }
    }
    private fun showGames() {
        val selected = selectedGameId?.let { id -> Profiles.all(this).firstOrNull { it.id == id } }
        if (selected != null) { gameDetail(selected); return }
        val profiles = Profiles.all(this)
        val linked = profiles.count { it.packageName.isNotBlank() }
        val active = profiles.count { it.enabled }
        area.addView(headline("Your games", "$linked installed • $active scheduled"))
        area.addView(label("Select a game to see its account, routine and characters.",
            12.5f, muted))
        area.addView(verticalSpace(8))
        val find = EditText(this).apply {
            isSingleLine = true
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
                val account = GameAccounts.current(this, game)
                val line = LinearLayout(this).apply {
                    orientation=LinearLayout.HORIZONTAL
                    gravity=Gravity.CENTER_VERTICAL
                }
                val icon = android.widget.ImageView(this).apply {
                    try { setImageDrawable(packageManager.getApplicationIcon(game.packageName)) }
                    catch (_:Exception) { setImageResource(android.R.drawable.sym_def_app_icon) }
                    scaleType=android.widget.ImageView.ScaleType.FIT_CENTER
                }
                line.addView(icon,LinearLayout.LayoutParams(dp(48),dp(48)))
                val info=LinearLayout(this).apply {
                    orientation=LinearLayout.VERTICAL
                    setPadding(dp(12),0,0,0)
                }
                info.addView(label(if(game.id==26) "Shoujo Kaisen · Global"
                    else if(game.id==22) "Shoujo Kaisen · JP" else game.title,16f,ink,true))
                info.addView(label(when {
                    game.packageName.isBlank() -> "Game not detected"
                    account.playerId != null -> "Player identified • " + account.playerId
                    else -> "Game detected • player ID not yet read"
                },12f,muted))
                if(game.enabled)
                    info.addView(label("Routine scheduled at %02d:%02d".format(game.hour,game.minute),
                        12f,accent))
                line.addView(info,LinearLayout.LayoutParams(0,-2,1f))
                line.addView(label("›",24f,muted))
                c.addView(line)
                c.setOnClickListener { selectedGameId=game.id; render() }
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
        val owner = GameAccounts.current(this,p)
        val title = if(p.id == 26) "Shoujo Kaisen · Global"
            else if(p.id == 22) "Shoujo Kaisen · JP" else p.title
        area.addView(action("‹ All games") { selectedGameId=null; render() })
        area.addView(verticalSpace(12))
        area.addView(headline(title,
            if(p.packageName.isBlank()) "Not installed"
            else "Installed game • "+(if(p.enabled) "routine scheduled" else "routine not scheduled")))
        val buttons=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        buttons.addView(action("Open game",true){
            val launch=packageManager.getLaunchIntentForPackage(p.packageName)
            if(launch!=null) startActivity(launch)
            else Toast.makeText(this,"Game not found on this device",Toast.LENGTH_SHORT).show()
        },LinearLayout.LayoutParams(0,dp(52),1f).apply{marginEnd=dp(5)})
        buttons.addView(action("Run routine"){
            when {
                !AutomationControl.running(this) -> dialog("Automation paused","Resume automation first.")
                p.packageName.isBlank() -> dialog("Game not installed","Asterion could not locate this game.")
                GameAccessibilityService.instance==null ->
                    dialog("Enable game control","Open Activity and enable Android accessibility control.")
                else -> GameAccessibilityService.instance?.enqueue(p)
            }
        },LinearLayout.LayoutParams(0,dp(52),1f))
        area.addView(buttons)
        area.addView(verticalSpace(12))
        area.addView(card().apply{
            addView(label("Your account",17f,ink,true))
            addView(verticalSpace(7))
            addView(label(if(owner.playerId!=null) "Player ID: "+owner.playerId
                else if(p.packageName.isNotBlank())
                    "Game detected. Asterion will identify the player ID when its profile appears in the live game."
                else "Install the game before connecting an account.",13f,muted))
            if(GameCodexBridge.accountFor(p.id)!=null){
                val synced=GameCodexBridge.refresh(this@MainActivity,p.id)
                addView(verticalSpace(6))
                addView(label("Characters in this account's Codex: "+synced.ownedCount+
                    if(synced.state==GameCodexBridge.SyncState.LINKED) " synced" else " not imported",
                    13f,muted))
                addView(verticalSpace(10))
                addView(action("Characters, equipment & teams",true){
                    openCodex(GameCodexBridge.accountFor(p.id))
                },LinearLayout.LayoutParams(-1,dp(49)))
            }
        })
        area.addView(card().apply{
            addView(label("Today's routine",17f,ink,true))
            addView(verticalSpace(8))
            val goals=GoalSettings.get(this@MainActivity,p.id).filter{it.enabled}
            if(goals.isEmpty()) addView(label("No routine configured",13f,muted))
            goals.forEach{goal->
                val state=GoalLedger(this@MainActivity).read(p.id,goal)
                addView(label(when(state.status){
                    GoalStatus.VERIFIED -> "✓  "+goal.title
                    GoalStatus.BLOCKED -> "!  "+goal.title+" • needs attention"
                    else -> "○  "+goal.title
                },13f,if(state.status==GoalStatus.VERIFIED)accent else muted))
            }
            addView(verticalSpace(10))
            addView(action("Routine schedule & objectives"){openEditor(p.id,false)})
        })
        area.addView(card().apply{
            addView(label("Learning & results",17f,ink,true))
            addView(verticalSpace(7))
            addView(label(FailureRecoveryLab.description(this@MainActivity,p.id),12f,muted))
            addView(verticalSpace(6))
            addView(action("Recent gameplay"){dialog("Gameplay history",
                EpisodeJournal(this@MainActivity).recent(p.id))})
            addView(verticalSpace(6))
            addView(action("Strategy evidence"){dialog("Measured performance",
                StrategyExperiment.report(this@MainActivity,p))})
            if(FailureRecoveryLab.held(this@MainActivity,p.id)){
                addView(verticalSpace(8))
                addView(action("Review and retry"){
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Allow another attempt?")
                        .setMessage("The same action failed repeatedly. Asterion must reassess the live screen before retrying.")
                        .setNegativeButton("Keep blocked",null)
                        .setPositiveButton("Retry"){_,_->
                            FailureRecoveryLab.clear(this@MainActivity,p.id);render()
                        }.show()
                })
            }
        })
    }
    private fun showAutomation() {
        area.addView(headline("Automation", "Live session, permissions and scheduling"))
        area.addView(card().apply {
            addView(label("Current session",17f,ink,true))
            addView(verticalSpace(9))
            addView(label(SessionStatus.description(this@MainActivity),14f,muted))
            addView(verticalSpace(6))
            addView(label("${ScheduledJobs.pendingCount(this@MainActivity)} pending game(s)",12f,muted))
        })
        area.addView(card().apply {
            addView(label("Status: " + AutomationControl.mode(this@MainActivity).name, 17f,
                if (AutomationControl.running(this@MainActivity)) accent else muted, true))
            addView(verticalSpace(7))
            addView(label("Pause suspends inputs and retains jobs for Resume. Turn off cancels alarms, pending jobs, gameplay and perception. Both controls remain visible above.",
                13f, muted))
        })
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
    private fun openCodex(account: String? = null) {
        startActivity(Intent().setClassName(this, "net.gamecodex.app.MainActivity")
            .apply { if (!account.isNullOrBlank()) putExtra("codex_account", account) })
    }
    private fun showNews() {
        if (!newsLoading && ImportantGameNews.lastRefresh(this) == 0L) {
            newsLoading = true
            ImportantGameNews.refreshNow(this) {
                runOnUiThread {
                    newsLoading = false
                    if (tab == Tab.NEWS) render()
                }
            }
        }
        area.addView(headline("Important game news",
            "Automatic updates from public publisher reports"))
        area.addView(card().apply {
            addView(label("News monitoring",17f,ink,true))
            addView(verticalSpace(6))
            val checked=ImportantGameNews.lastRefresh(this@MainActivity)
            addView(label(if(checked==0L) "No completed check yet"
                else "Last checked: "+java.text.SimpleDateFormat("dd MMM HH:mm",
                    java.util.Locale.getDefault()).format(java.util.Date(checked)),
                12.5f,muted))
            addView(label("Checks run automatically while the device allows background network access.",
                12.5f,muted))
            addView(verticalSpace(9))
            addView(action("Check for updates",true){
                ImportantGameNews.refreshNow(this@MainActivity) {
                    runOnUiThread { if(tab==Tab.NEWS)render() }
                }
                Toast.makeText(this@MainActivity,"Checking game news…",Toast.LENGTH_SHORT).show()
            })
        })
        val profiles=Profiles.all(this).filter{it.packageName.isNotBlank()}
        val all=profiles.flatMap{p->ImportantGameNews.read(this,p.id)
            .map{p to it}}.sortedByDescending{it.second.publishedAt}.take(35)
        if(all.isEmpty()){
            area.addView(card().apply{
                addView(label("No reports yet",17f,ink,true))
                addView(label("Asterion is gathering relevant announcements. Only high-impact updates trigger alerts.",
                    13f,muted))
            })
        }else{
            all.forEach{(game,story)->
                area.addView(card().apply{
                    addView(label(if(game.id==22) "Shoujo Kaisen • JP"
                        else if(game.id==26) "Shoujo Kaisen • Global"
                        else game.title,12f,accent,true))
                    addView(verticalSpace(6))
                    addView(label(story.title,15f,ink,true))
                    addView(verticalSpace(5))
                    addView(label(story.publisher+" • "+
                        java.text.SimpleDateFormat("dd MMM",java.util.Locale.getDefault())
                            .format(java.util.Date(story.publishedAt))+
                        (if(story.critical)" • Important" else ""),12f,muted))
                    addView(verticalSpace(8))
                    addView(action("Read report"){
                        startActivity(Intent(Intent.ACTION_VIEW,android.net.Uri.parse(story.link)))
                    })
                })
            }
        }
        area.addView(label("X private-list synchronization is not connected. News here comes from independently indexed public reports, not your private X feed.",
            12f,muted))
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
