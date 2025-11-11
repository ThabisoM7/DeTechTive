package ferit.student.matijazagar.smishhunter

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

interface AnalysisAPIEndpoints {
    @Headers(
        "x-apikey: " + BuildConfig.API_KEY,
        "content-type: application/x-www-form-urlencoded"
    )
    @GET("/api/v3/urls/{url}")
    suspend fun getAnalysis(@Path("url") url: String): AnalysisResult
}