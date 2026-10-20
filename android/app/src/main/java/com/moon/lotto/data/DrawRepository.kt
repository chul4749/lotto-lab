package com.moon.lotto.data

import android.content.Context
import com.moon.lotto.core.Draw
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 당첨번호. 앱에 같이 넣은 draws.json → 받아 둔 파일 → 인터넷 순으로 새 것을 쓴다.
 * 인터넷 파일은 GitHub Actions가 매주 토요일 추첨 뒤 갱신한다.
 */
class DrawRepository(private val context: Context) {
    private val cache = File(context.filesDir, "draws.json")

    suspend fun loadLocal(): List<Draw> = withContext(Dispatchers.IO) {
        val bundled = runCatching { context.assets.open("draws.json").bufferedReader().use { parse(it.readText()) } }.getOrDefault(emptyList())
        val cached = runCatching { if (cache.exists()) parse(cache.readText()) else emptyList() }.getOrDefault(emptyList())
        if (cached.size > bundled.size) cached else bundled
    }

    /** 새 회차가 있으면 받아서 저장하고 돌려준다. 실패하면 null */
    suspend fun fetchRemote(): List<Draw>? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL(URL_DRAWS).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 20_000
            conn.setRequestProperty("Cache-Control", "no-cache")
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val draws = parse(text)
            if (draws.isNotEmpty()) cache.writeText(text)
            draws
        }.getOrNull()
    }

    companion object {
        const val URL_DRAWS = "https://chul4749.github.io/lotto-lab/draws.json"

        fun parse(text: String): List<Draw> {
            val arr = JSONObject(text).getJSONArray("draws")
            return (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val nums = o.getJSONArray("nums")
                val prizes = o.optJSONArray("prizes")
                Draw(
                    no = o.getInt("no"),
                    date = o.getString("date"),
                    nums = (0 until nums.length()).map { nums.getInt(it) }.sorted(),
                    bonus = o.getInt("bonus"),
                    prize = o.optLong("prize"),
                    winners = o.optInt("winners"),
                    prizes = if (prizes == null) emptyList() else (0 until prizes.length()).map { prizes.getLong(it) },
                )
            }.sortedBy { it.no }
        }
    }
}
