package com.gamepilot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Xml
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.concurrent.thread

/**
 * Actual in-app background news ingestion. X private list OAuth is NOT connected;
 * these are public indexed news reports, explicitly labelled by their origin.
 * Only time-sensitive high-impact headlines may trigger notifications.
 */
object ImportantGameNews {
    private const val JOB_ID=47120
    private const val PERIOD_MS=2L*60L*60L*1000L
    private const val MAX_AGE_MS=7L*24L*60L*60L*1000L
    private const val CHANNEL="asterion_critical_game_news"
    data class Story(val gameId:Int,val title:String,val link:String,val publisher:String,
                     val publishedAt:Long,val critical:Boolean)

    fun schedule(context:Context){
        val scheduler=context.getSystemService(JobScheduler::class.java)
        val job=JobInfo.Builder(JOB_ID,ComponentName(context,GameNewsJob::class.java))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPersisted(true)
            .setPeriodic(PERIOD_MS)
            .build()
        scheduler.schedule(job)
    }
    fun refreshNow(context:Context,callback:(Int)->Unit={}){
        thread(name="asterion-news-sync") {
            val total=try { sync(context) } catch (e:Exception) {
                Events.record(context,"News refresh failed: "+e.javaClass.simpleName);0
            }
            callback(total)
        }
    }
    private fun prefs(context:Context)=
        context.getSharedPreferences("game_news_v1",Context.MODE_PRIVATE)

    fun read(context:Context,gameId:Int):List<Story> {
        val value=prefs(context).getString("stories:$gameId","[]") ?: "[]"
        return try {
            val arr=JSONArray(value)
            (0 until arr.length()).mapNotNull { idx ->
                val j=arr.optJSONObject(idx) ?: return@mapNotNull null
                Story(gameId,j.optString("title"),j.optString("link"),
                    j.optString("publisher"),j.optLong("published"),
                    j.optBoolean("critical"))
            }
        } catch (_:Exception){emptyList()}
    }
    fun lastRefresh(context:Context):Long=prefs(context).getLong("last_refresh",0)
    fun sourceLabel():String="Public indexed publisher reports (X list not yet connected)"

    fun isCritical(title:String):Boolean{
        val t=title.lowercase(Locale.ROOT)
        return listOf("end of service","service termination","server shutdown",
            "shutdown notice","urgent maintenance","emergency maintenance",
            "security incident","account breach","game closing","ends service",
            "server closure").any { t.contains(it) }
    }
    private fun readFeed(game:GameProfile):List<Story>{
        val q=if(game.id==26) "\"Attack on Time\" game"
            else if(game.id==22) "\"少女廻戦\" game"
            else "\""+game.title.replace("\"","")+"\" game"
        val url="https://news.google.com/rss/search?q="+
            URLEncoder.encode(q+" when:7d","UTF-8")+"&hl=en-US&gl=US&ceid=US:en"
        val conn=URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout=4500
        conn.readTimeout=4500
        conn.setRequestProperty("User-Agent","AsterionGameNews/0.1")
        try {
            if(conn.responseCode!=200)return emptyList()
            val parser=Xml.newPullParser()
            parser.setInput(conn.inputStream,"UTF-8")
            var event=parser.eventType
            var inside=false
            var title=""
            var link=""
            var date=""
            var publisher=""
            val results=mutableListOf<Story>()
            val fmt=SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz",Locale.US).apply{
                timeZone=TimeZone.getTimeZone("GMT")
            }
            while(event!=XmlPullParser.END_DOCUMENT && results.size<20){
                when(event){
                    XmlPullParser.START_TAG->when(parser.name){
                        "item"->{inside=true;title="";link="";date="";publisher=""}
                        "title"->if(inside)title=parser.nextText().take(300)
                        "link"->if(inside)link=parser.nextText().take(1500)
                        "pubDate"->if(inside)date=parser.nextText()
                        "source"->if(inside)publisher=parser.nextText().take(90)
                    }
                    XmlPullParser.END_TAG->if(parser.name=="item" && inside){
                        inside=false
                        val published=try{fmt.parse(date)?.time ?: 0L}catch(_:Exception){0L}
                        val age=System.currentTimeMillis()-published
                        if(title.isNotBlank() && link.startsWith("https://news.google.com/") &&
                            published>0 && age>=0 && age<MAX_AGE_MS){
                            results.add(Story(game.id,title,link,
                                publisher.ifBlank{"Indexed publisher"},published,isCritical(title)))
                        }
                    }
                }
                event=parser.next()
            }
            return results
        } finally {conn.disconnect()}
    }
    fun sync(context:Context):Int{
        val games=Profiles.all(context).filter{it.packageName.isNotBlank()}
        if(games.isEmpty())return 0
        val p=prefs(context)
        val next=p.getInt("cursor",0).coerceAtLeast(0)
        val batch=(0 until minOf(5,games.size)).map{games[(next+it)%games.size]}
        var total=0
        for(game in batch){
            val old=read(context,game.id)
            val incoming=try{readFeed(game)}catch(e:Exception){
                Events.record(context,"News source unavailable for ${game.title}: "+e.javaClass.simpleName)
                continue
            }
            if(incoming.isEmpty())continue
            val seen=old.map{it.link}.toSet()
            val merged=(incoming+old).distinctBy{it.link}.sortedByDescending{it.publishedAt}.take(30)
            val json=JSONArray()
            merged.forEach{json.put(JSONObject().put("title",it.title)
                .put("link",it.link).put("publisher",it.publisher)
                .put("published",it.publishedAt).put("critical",it.critical))}
            p.edit().putString("stories:${game.id}",json.toString()).commit()
            if(p.getBoolean("initialized:${game.id}",false)){
                incoming.filter{it.critical && it.link !in seen}.take(2)
                    .forEach{notifyImportant(context,game,it)}
            }else p.edit().putBoolean("initialized:${game.id}",true).apply()
            total+=incoming.size
        }
        p.edit().putInt("cursor",(next+batch.size)%games.size)
            .putLong("last_refresh",System.currentTimeMillis()).apply()
        return total
    }
    private fun notifyImportant(ctx:Context,game:GameProfile,story:Story){
        val manager=ctx.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL,
            "Important game news",NotificationManager.IMPORTANCE_HIGH))
        val i=Intent(Intent.ACTION_VIEW,Uri.parse(story.link))
        val click=PendingIntent.getActivity(ctx,story.link.hashCode(),i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify((game.id*10000+story.link.hashCode()).and(0x7fffffff),
            Notification.Builder(ctx,CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Important: "+game.title)
                .setContentText(story.title)
                .setStyle(Notification.BigTextStyle().bigText(
                    story.title+" — "+story.publisher+" (indexed report; verify source)"))
                .setContentIntent(click).setAutoCancel(true).build())
    }
}
class GameNewsJob:JobService(){
    override fun onStartJob(params:JobParameters):Boolean{
        ImportantGameNews.refreshNow(this){jobFinished(params,false)}
        return true
    }
    override fun onStopJob(params:JobParameters):Boolean=true
}
