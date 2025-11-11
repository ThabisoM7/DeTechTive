package ferit.student.matijazagar.smishhunter

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.telephony.SmsMessage
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ferit.student.matijazagar.smishhunter.myPrefAdding
import ferit.student.matijazagar.smishhunter.myPrefMalicious
import ferit.student.matijazagar.smishhunter.myPrefSuspicious
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.ArrayList


object SMSChecker {

    private const val fileName  = "reports.json"
    private const val dirName  = "json"
    private const val CHANNEL_ID = "REPORT"
    private const val CHANNEL_NAME = "Smishing alerts"
    private const val NOTIF_ID = 0

    private val URGENCY_KEYWORDS = listOf("urgent", "action required", "account suspended", "verify now", "limited time", "expire")
    private val PRIZE_KEYWORDS = listOf("winner", "congratulations", "free", "claim your prize", "won", "reward")
    private val ACTION_KEYWORDS = listOf("click the link", "login here", "update your details", "confirm your account")

    private enum class SuspicionLevel { NONE, LOW, MEDIUM, HIGH }

    @RequiresApi(Build.VERSION_CODES.O)
    fun handleSMS(context: Context, sms : SmsMessage){
        val text = sms.displayMessageBody
        val sender = sms.originatingAddress
        var rating = context.resources.getString(R.string.level_harmless)
        var explanation = "Harmless message"
        var analysisResult = AnalysisResult(Data(
            Attributes(AnalysisStats(0,0,0,0,0),
                0,Votes(0, 0),"No link analysed.")))

        val links = URLAnalyser.extractURLs(sms.displayMessageBody)
        val textSuspicionLevel = analyseTextContent(text)

        if (links.isEmpty()) {
            if (textSuspicionLevel >= SuspicionLevel.MEDIUM) {
                rating = context.resources.getString(R.string.level_suspicious)
                explanation = "Message contains suspicious keywords."
            }
            val report = Report(sender, text, rating, explanation, analysisResult)
            processAndSaveReport(context, report)
        } else {
            val linkCount = AtomicInteger(links.size)
            var finalRating = rating
            var finalExplanation = explanation
            var finalAnalysisResult = analysisResult

            links.forEach { url ->
                URLAnalyser.getLinkAnalysis(url) { analysis ->
                    synchronized(this) {
                        if (analysis.data.attributes.last_analysis_stats.malicious >= 3) {
                            if (finalRating != context.resources.getString(R.string.level_malicious)) {
                                finalRating = context.resources.getString(R.string.level_malicious)
                                finalExplanation = "Message contains a malicious link."
                                finalAnalysisResult = analysis
                            }
                        } else if (analysis.data.attributes.last_analysis_stats.suspicious >= 3
                            || analysis.data.attributes.last_analysis_stats.malicious >= 1
                        ) {
                            if (finalRating == context.resources.getString(R.string.level_harmless)) {
                                finalRating = context.resources.getString(R.string.level_suspicious)
                                finalExplanation = "Message contains a suspicious link."
                                finalAnalysisResult = analysis
                            }
                        }
                    }

                    if (linkCount.decrementAndGet() == 0) {
                        if (finalRating == context.resources.getString(R.string.level_harmless) && textSuspicionLevel >= SuspicionLevel.MEDIUM) {
                            finalRating = context.resources.getString(R.string.level_suspicious)
                            finalExplanation = "Message contains suspicious keywords but link seems harmless."
                        }
                        val report = Report(sender, text, finalRating, finalExplanation, finalAnalysisResult)
                        processAndSaveReport(context, report)
                    }
                }
            }
        }
    }

    private fun analyseTextContent(text: String): SuspicionLevel {
        val lowerCaseText = text.lowercase()
        var score = 0

        if (URGENCY_KEYWORDS.any { lowerCaseText.contains(it) }) {
            score += 2
        }
        if (PRIZE_KEYWORDS.any { lowerCaseText.contains(it) }) {
            score += 2
        }
        if (ACTION_KEYWORDS.any { lowerCaseText.contains(it) }) {
            score += 1
        }

        return when {
            score >= 4 -> SuspicionLevel.HIGH
            score >= 2 -> SuspicionLevel.MEDIUM
            score > 0 -> SuspicionLevel.LOW
            else -> SuspicionLevel.NONE
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun processAndSaveReport(context: Context, report: Report) {
        val rating = report.rating
        if (getPreferenceValue(myPrefAdding, context) == "off"
            && rating == context.resources.getString(R.string.level_harmless)
        )
            return

        if ((getPreferenceValue(myPrefMalicious, context) == "on" && rating == context.resources.getString(R.string.level_malicious))
            || (getPreferenceValue(myPrefSuspicious, context) == "on" && rating == context.resources.getString(R.string.level_suspicious))
        ) {
            Log.d("Notif", "sending notif: " + report.explanation)
            createNotifChannel(context)
            generateNotification(report, context)
        }

        val jsonString = readFileData(context)

        val listReportType = object : TypeToken<ArrayList<Report>>() {}.type
        val reports = Gson().fromJson<ArrayList<Report>?>(jsonString, listReportType) ?: ArrayList()
        reports.add(0, report)

        writeFileData(context, reports)
    }

    private fun readFileData(context: Context) : String{
        val dir = context.getDir(dirName, Context.MODE_PRIVATE)
        val file = File(dir, fileName)
        if (!file.exists()) {
            return "[]"
        }
        return file.readText()
    }

    private fun writeFileData(context: Context, reports: ArrayList<Report>){
        val dir = context.getDir(dirName, Context.MODE_PRIVATE)
        val file = File(dir, fileName)

        val newJsonString = try {
            Gson().toJson(reports)
        }
        catch (e: IOException){
            e.printStackTrace()
            ""
        }

        file.writeText(newJsonString)
    }

    private fun getPreferenceValue(preference: String, context: Context ): String? {
        val sp: SharedPreferences = context.getSharedPreferences("myPreferences", Context.MODE_PRIVATE)
        return sp.getString(preference, "on")
    }

    private fun generateNotification(report: Report, context: Context){
        val notif = NotificationCompat.Builder(context,CHANNEL_ID)
            .setContentTitle(report.explanation)
            .setContentText(report.content)//recimo prvih 20 znakova
            .setSmallIcon(R.drawable.ic_placeholder_logo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()


        val notifManager = NotificationManagerCompat.from(context)

        notifManager.notify(NOTIF_ID,notif)

    }

    private fun createNotifChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                lightColor = Color.BLUE
                enableLights(true)
            }
            val manager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
