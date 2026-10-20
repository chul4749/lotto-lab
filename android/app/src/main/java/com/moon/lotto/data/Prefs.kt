package com.moon.lotto.data

import android.content.Context
import com.moon.lotto.core.Pick
import com.moon.lotto.core.Strategy
import org.json.JSONArray
import org.json.JSONObject

/** 저장한 번호와 카페 설정. 이 기기에만 남는다. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("lotto", Context.MODE_PRIVATE)

    var cafeUrl: String
        get() = sp.getString("cafeUrl", "") ?: ""
        set(v) = sp.edit().putString("cafeUrl", v).apply()

    var nick: String
        get() = sp.getString("nick", "") ?: ""
        set(v) = sp.edit().putString("nick", v).apply()

    var seenGuide: Boolean
        get() = sp.getBoolean("seenGuide", false)
        set(v) = sp.edit().putBoolean("seenGuide", v).apply()

    fun loadSaved(): List<Pick> = runCatching {
        val arr = JSONArray(sp.getString("saved", "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val nums = o.getJSONArray("nums")
            Pick(
                nums = (0 until nums.length()).map { nums.getInt(it) },
                round = o.getInt("round"),
                strategy = runCatching { Strategy.valueOf(o.getString("strategy")) }.getOrDefault(Strategy.MANUAL),
                at = o.optLong("at"),
            )
        }
    }.getOrDefault(emptyList())

    fun saveSaved(list: List<Pick>) {
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(JSONObject().put("nums", JSONArray(p.nums)).put("round", p.round).put("strategy", p.strategy.name).put("at", p.at))
        }
        sp.edit().putString("saved", arr.toString()).apply()
    }
}
