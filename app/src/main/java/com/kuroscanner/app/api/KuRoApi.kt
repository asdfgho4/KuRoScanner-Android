package com.kuroscanner.app.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException

object KuRoApi {
    private const val DEV_CODE = "3dee77f8-1cb9-4cf6-a585-8677a867dab6"
    private const val BASE_URL = "https://api.kurobbs.com"
    private const val VERSION = "2.5.0"
    private const val VERSION_CODE = "2500"

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val gson = Gson()

    private fun createDefaultHeaders(token: String? = null): Map<String, String> {
        val headers = mutableMapOf(
            "Content-Type" to "application/x-www-form-urlencoded",
            "devCode" to DEV_CODE,
            "source" to "android",
            "version" to VERSION,
            "versionCode" to VERSION_CODE
        )
        token?.let { headers["token"] = it }
        return headers
    }

    private fun executePost(url: String, headers: Map<String, String>, body: RequestBody): String? {
        val request = Request.Builder()
            .url(url)
            .apply { headers.forEach { addHeader(it.key, it.value) } }
            .post(body)
            .build()

        return try {
            val response: Response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string()
            } else {
                Log.e("KuRoApi", "Request failed: ${response.code}")
                null
            }
        } catch (e: IOException) {
            Log.e("KuRoApi", "Network error: ${e.message}")
            null
        }
    }

    fun getSmsCode(phoneNumber: String, geeTestData: String): String? {
        val url = "$BASE_URL/user/getSmsCode"
        val headers = createDefaultHeaders()
        val body = FormBody.Builder()
            .add("mobile", phoneNumber)
            .add("geeTestData", geeTestData)
            .build()
        return executePost(url, headers, body)
    }

    fun loginByMobileCaptcha(phoneNumber: String, code: String): String? {
        val url = "$BASE_URL/user/sdkLogin"
        val headers = createDefaultHeaders()
        val body = FormBody.Builder()
            .add("mobile", phoneNumber)
            .add("code", code)
            .build()
        return executePost(url, headers, body)
    }

    fun getUserInfo(uid: String, token: String): String? {
        val url = "$BASE_URL/user/mineV2"
        val headers = createDefaultHeaders(token)
        val body = FormBody.Builder()
            .add("otherUserId", uid)
            .build()
        return executePost(url, headers, body)
    }

    fun loginGameByQrCode(qrcode: String, token: String): String? {
        val url = "$BASE_URL/user/auth/roleInfos"
        val headers = createDefaultHeaders(token)
        val body = FormBody.Builder()
            .add("qrCode", qrcode)
            .build()
        return executePost(url, headers, body)
    }

    fun confirmLoginGameByQrCode(qrcode: String, token: String, autoLogin: Boolean, smsCode: String): String? {
        val url = "$BASE_URL/user/auth/scanLogin"
        val headers = createDefaultHeaders(token)
        val body = FormBody.Builder()
            .add("autoLogin", if (autoLogin) "true" else "false")
            .add("qrCode", qrcode)
            .add("id", "")
            .add("verifyCode", smsCode)
            .build()
        return executePost(url, headers, body)
    }

    fun sendScanSms(token: String): String? {
        val url = "$BASE_URL/user/sms/scanSms"
        val headers = createDefaultHeaders(token)
        val body = FormBody.create("geeTestData=", null)
        return executePost(url, headers, body)
    }
}