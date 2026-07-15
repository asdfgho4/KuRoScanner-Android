package com.kuroscanner.app.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

enum class LiveStreamStatus {
    NORMAL, ABSENT, NOT_LIVE, ERROR
}

object LiveStreamApi {
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val gson = Gson()

    fun getBiliBiliLiveStatus(roomID: String): LiveStreamStatus {
        val url = "https://api.live.bilibili.com/room/v1/Room/room_init?id=$roomID"
        val request = Request.Builder().url(url).build()

        return try {
            val response: Response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = gson.fromJson(response.body?.string(), JsonObject::class.java)
                val code = json.getAsJsonPrimitive("code").asInt
                when (code) {
                    60004 -> LiveStreamStatus.ABSENT
                    0 -> {
                        val liveStatus = json.getAsJsonObject("data").getAsJsonPrimitive("live_status").asInt
                        if (liveStatus == 1) LiveStreamStatus.NORMAL else LiveStreamStatus.NOT_LIVE
                    }
                    else -> LiveStreamStatus.ERROR
                }
            } else {
                LiveStreamStatus.ERROR
            }
        } catch (e: IOException) {
            Log.e("LiveStreamApi", "Bilibili status error: ${e.message}")
            LiveStreamStatus.ERROR
        }
    }

    fun getBiliBiliStreamUrl(roomID: String): String? {
        val url = "https://api.live.bilibili.com/xlive/web-room/v2/index/getRoomPlayInfo?room_id=$roomID&qn=10000&platform=h5&protocol=0,1&format=0,2&codec=0&only_audio=0&only_video=0"
        val request = Request.Builder().url(url).build()

        return try {
            val response: Response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = gson.fromJson(response.body?.string(), JsonObject::class.java)
                val data = json.getAsJsonObject("data")
                val playurlInfo = data.getAsJsonObject("playurl_info")
                val playurl = playurlInfo.getAsJsonObject("playurl")
                val stream = playurl.getAsJsonArray("stream")[0].asJsonObject
                val format = stream.getAsJsonArray("format")[0].asJsonObject
                val codec = format.getAsJsonArray("codec")[0].asJsonObject
                val baseUrl = codec.getAsJsonPrimitive("base_url").asString
                val urlInfo = codec.getAsJsonArray("url_info")[0].asJsonObject
                val host = urlInfo.getAsJsonPrimitive("host").asString
                val extra = urlInfo.getAsJsonPrimitive("extra").asString
                "$host$baseUrl$extra"
            } else {
                null
            }
        } catch (e: IOException) {
            Log.e("LiveStreamApi", "Bilibili stream url error: ${e.message}")
            null
        }
    }

    fun getDouyinLiveStatus(roomID: String): LiveStreamStatus {
        val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Safari/537.36"
        val params = "aid=6383&app_name=douyin_web&live_id=1&device_platform=web&browser_language=zh-CN&browser_platform=Win32&browser_name=Edge&browser_version=139.0.0.0&is_need_double_stream=false&web_rid=$roomID"

        val abogus = generateAbogus(userAgent, params)
        val url = "https://live.douyin.com/webcast/room/web/enter/?$params&a_bogus=$abogus"

        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", userAgent)
            .addHeader("referer", "https://live.douyin.com/")
            .build()

        return try {
            val response: Response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = gson.fromJson(response.body?.string(), JsonObject::class.java)
                if (!json.has("status_code") || json.getAsJsonPrimitive("status_code").asInt != 0) {
                    return LiveStreamStatus.ABSENT
                }
                val data = json.getAsJsonObject("data")
                if (!data.has("data") || !data.get("data").isJsonArray || data.getAsJsonArray("data").size() == 0) {
                    return LiveStreamStatus.ABSENT
                }
                val roomData = data.getAsJsonArray("data")[0].asJsonObject
                val status = roomData.getAsJsonPrimitive("status").asInt
                when (status) {
                    2 -> LiveStreamStatus.NORMAL
                    4 -> LiveStreamStatus.NOT_LIVE
                    else -> LiveStreamStatus.ERROR
                }
            } else {
                LiveStreamStatus.ERROR
            }
        } catch (e: IOException) {
            Log.e("LiveStreamApi", "Douyin status error: ${e.message}")
            LiveStreamStatus.ERROR
        }
    }

    private fun generateAbogus(userAgent: String, params: String): String {
        return ""
    }
}