package ferit.student.matijazagar.smishhunter

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

class NewsFragment : Fragment() {

    private lateinit var adapter: ArticleAdapter
    private val articles = mutableListOf<Article>()
    private val feedUrls = listOf(
        "https://www.darkreading.com/rss.xml",
        "https://krebsonsecurity.com/feed/",
        "https://feeds.feedburner.com/TheHackersNews"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_news, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = ArticleAdapter(articles)
        recyclerView.adapter = adapter

        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            fetchFeeds()
        }

        fetchFeeds()

        return view
    }

    private fun fetchFeeds() {
        lifecycleScope.launch(Dispatchers.IO) {
            val allArticles = mutableListOf<Article>()

            for (url in feedUrls) {
                try {
                    val urlConnection = URL(url).openConnection()
                    val inputStream = urlConnection.getInputStream()
                    val parserFactory = XmlPullParserFactory.newInstance()
                    val parser = parserFactory.newPullParser()
                    parser.setInput(inputStream, null)

                    allArticles.addAll(parseRss(parser))

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val sortedArticles = allArticles.sortedByDescending { getTimestamp(it.pubDate) }

            withContext(Dispatchers.Main) {
                articles.clear()
                articles.addAll(sortedArticles)
                adapter.notifyDataSetChanged()
                view?.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)?.isRefreshing = false
            }
        }
    }
    
    private fun parseRss(parser: XmlPullParser): List<Article> {
        val articles = mutableListOf<Article>()
        var eventType = parser.eventType
        var currentArticle: Article? = null
        var text: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (tagName.equals("item", ignoreCase = true)) {
                        currentArticle = Article(null, null, null, null)
                    }
                }
                XmlPullParser.TEXT -> {
                    text = parser.text
                }
                XmlPullParser.END_TAG -> {
                    when {
                        tagName.equals("item", ignoreCase = true) -> {
                            currentArticle?.let { articles.add(it) }
                        }
                        tagName.equals("title", ignoreCase = true) -> {
                            currentArticle = currentArticle?.copy(title = text)
                        }
                        tagName.equals("pubDate", ignoreCase = true) -> {
                            currentArticle = currentArticle?.copy(pubDate = text)
                        }
                        tagName.equals("description", ignoreCase = true) -> {
                            currentArticle = currentArticle?.copy(description = text)
                        }
                        tagName.equals("link", ignoreCase = true) -> {
                            currentArticle = currentArticle?.copy(link = text)
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return articles
    }

    private fun getTimestamp(pubDate: String?): Long {
        if (pubDate == null) return 0
        val formats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z", // Standard RSS format
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", // ISO 8601
            "yyyy-MM-dd'T'HH:mm:ssZ"
        )

        for (format in formats) {
            try {
                return SimpleDateFormat(format, Locale.US).parse(pubDate)?.time ?: 0
            } catch (e: Exception) {
                // Continue to next format
            }
        }
        return 0
    }
}