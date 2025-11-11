package ferit.student.matijazagar.smishhunter

import android.os.Build
import android.util.Log
import android.util.Patterns
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

object URLAnalyser {
    @RequiresApi(Build.VERSION_CODES.O)
    fun getLinkAnalysis(url: String, onResult: (AnalysisResult) -> Unit) {
        val encodedURL: String = Base64.getEncoder().encodeToString(url.toByteArray()).trimEnd('=')
        val request = ServiceBuilder.buildService(AnalysisAPIEndpoints::class.java)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = request.getAnalysis(encodedURL)
                onResult(response)
            } catch (e: Exception) {
                Log.d("API-response", "fail \n message: " + e.message)
            }
        }
    }


    fun extractURLs(message: String): ArrayList<String> {
        @Suppress("LocalVariableName") val URLs = ArrayList<String>()

        val urlMatcher = Patterns.WEB_URL.matcher(message)

        while (urlMatcher.find()) {
            val matchStart = urlMatcher.start(1)
            val matchEnd = urlMatcher.end()
            val url = message.substring(matchStart, matchEnd)
            if (matchStart < 0 || matchEnd > message.length) {
                Log.d(
                    "inspectURLs","Found URL outside of message body (from $matchStart to ${matchEnd}))")
            } else {
                Log.d("inspectURLs", "Found URL in message body: \"${url}\"")
                URLs.add(url)
            }
        }
        return URLs
    }

}