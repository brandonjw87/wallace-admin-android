package com.brandonwallace.wallaceadmin.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class ApiException(val code: Int, msg: String) : Exception(msg)

/** Thin client for the WALLACE Agent intent API (/v1). The phone never talks to AdGuard directly. */
class Api(private val vault: Vault) {
    private val http = OkHttpClient.Builder().connectTimeout(6, TimeUnit.SECONDS).readTimeout(300, TimeUnit.SECONDS).pingInterval(20, TimeUnit.SECONDS).build()
    private val json = "application/json".toMediaType()
    var activeHost: String = vault.host

    fun base(host: String = activeHost) = "http://$host:${vault.port}"

    private suspend fun raw(method: String, path: String, body: JSONObject? = null, auth: Boolean = true, host: String = activeHost): Any = withContext(Dispatchers.IO) {
        if (!HostRules.allowed(host)) throw ApiException(0, "Host $host is not a private LAN / Tailscale address")
        val b = Request.Builder().url(base(host) + path)
        if (auth) vault.token?.let { b.header("Authorization", "Bearer $it") } ?: throw ApiException(401, "Not paired")
        val rb = body?.toString()?.toRequestBody(json) ?: if (method == "GET" || method == "DELETE") null else "{}".toRequestBody(json)
        b.method(method, rb)
        http.newCall(b.build()).execute().use { r ->
            val text = r.body?.string().orEmpty()
            val parsed: Any = if (text.isBlank()) JSONObject() else try { JSONTokener(text).nextValue() } catch (e: Exception) { JSONObject().put("text", text) }
            if (!r.isSuccessful) throw ApiException(r.code, (parsed as? JSONObject)?.optString("error")?.takeIf { it.isNotBlank() } ?: when (r.code) {
                401 -> "Not authorized - pair again"; 403 -> "Refused by the PC (protected)"; 429 -> "Too many requests"; else -> "HTTP ${r.code}" })
            parsed
        }
    }
    suspend fun obj(method: String, path: String, body: JSONObject? = null) = raw(method, path, body) as JSONObject
    suspend fun arr(path: String) = raw("GET", path) as JSONArray
    suspend fun get(path: String) = obj("GET", path)
    suspend fun post(path: String, body: JSONObject = JSONObject()) = obj("POST", path, body)

    suspend fun ping(host: String) = raw("GET", "/v1/ping", auth = false, host = host) as JSONObject
    suspend fun pair(host: String, code: String, device: String) = raw("POST", "/v1/pair", JSONObject().put("code", code).put("deviceName", device), auth = false, host = host) as JSONObject

    fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    fun live(onMsg: (JSONObject) -> Unit, onState: (Boolean) -> Unit): WebSocket? {
        val tok = vault.token ?: return null
        if (!HostRules.allowed(activeHost)) return null
        val req = Request.Builder().url(base().replace("http", "ws") + "/v1/live").header("Authorization", "Bearer $tok").build()
        return http.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) = onState(true)
            override fun onMessage(webSocket: WebSocket, text: String) { try { onMsg(JSONObject(text)) } catch (_: Exception) {} }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = onState(false)
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) = onState(false)
        })
    }
}

fun JSONArray.objs(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }
fun JSONArray.strings(): List<String> = (0 until length()).map { optString(it) }
fun JSONObject.s(k: String, d: String = ""): String = if (isNull(k)) d else optString(k, d)
