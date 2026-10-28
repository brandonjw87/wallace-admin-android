package com.brandonwallace.wallaceadmin.data

import android.app.Application
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.WebSocket
import org.json.JSONObject

enum class Conn { Offline, Connecting, Polling, Live }

class AppVm(app: Application) : AndroidViewModel(app) {
    val vault = Vault(app)
    val api = Api(vault)
    var paired by mutableStateOf(vault.token != null)
    var status by mutableStateOf<JSONObject?>(null)
    var conn by mutableStateOf(Conn.Offline)
    var lastError by mutableStateOf<String?>(null)
    var lastUpdate by mutableStateOf(0L)
    val toasts = mutableStateListOf<String>()
    var pendingPair by mutableStateOf<String?>(null)
    private var ws: WebSocket? = null
    private var loop: Job? = null

    fun toast(s: String) { toasts.add(s) }
    fun viewModelScopeLaunch(block: suspend () -> Unit) { viewModelScope.launch { block() } }

    fun start() {
        if (!paired) return
        loop?.cancel(); ws?.cancel(); ws = null
        loop = viewModelScope.launch {
            conn = Conn.Connecting
            pickHost()
            while (isActive) {
                if (vault.useLive && ws == null) ws = api.live({ m -> status = m; lastUpdate = System.currentTimeMillis(); conn = Conn.Live; lastError = null }) { open -> if (!open) { ws = null; if (conn == Conn.Live) conn = Conn.Polling } }
                if (conn != Conn.Live) refreshStatus()
                delay(vault.pollSeconds * 1000L)
            }
        }
    }
    fun stop() { loop?.cancel(); ws?.close(1000, "bye"); ws = null; conn = Conn.Offline }

    private suspend fun pickHost() {
        for (h in listOf(vault.host, vault.altHost).filter { it.isNotBlank() }) {
            try { api.ping(h); api.activeHost = h; return } catch (_: Exception) {}
        }
        api.activeHost = vault.host
    }

    suspend fun refreshStatus() {
        try { status = api.get("/v1/status"); lastUpdate = System.currentTimeMillis(); if (conn != Conn.Live) conn = Conn.Polling; lastError = null }
        catch (e: ApiException) { lastError = e.message; conn = Conn.Offline; if (e.code == 401) { /* revoked */ } ; pickHost() }
        catch (e: Exception) { lastError = e.message ?: "Can't reach the PC"; conn = Conn.Offline; pickHost() }
    }

    /** Runs an action with a toast for the result or the server's refusal reason (403/409 from the safety rules). */
    fun act(label: String, after: (suspend () -> Unit)? = null, block: suspend () -> JSONObject?) = viewModelScope.launch {
        try { val r = block(); toast(r?.optString("message")?.takeIf { it.isNotBlank() } ?: "$label: done"); after?.invoke() }
        catch (e: ApiException) { toast(e.message ?: "$label failed") }
        catch (e: Exception) { toast("$label failed: ${e.message}") }
    }

    /** Pair from a QR payload: wallace://pair?host=..&port=..&code=.. */
    suspend fun pairFromUri(text: String): String? {
        val u = try { Uri.parse(text.trim()) } catch (e: Exception) { return "Not a WALLACE pairing code" }
        if (u.scheme != "wallace" || u.host != "pair") return "Not a WALLACE pairing code"
        val host = u.getQueryParameter("host") ?: return "QR has no host"; val code = u.getQueryParameter("code") ?: return "QR has no code"
        val port = u.getQueryParameter("port")?.toIntOrNull() ?: 8088
        if (!HostRules.allowed(host)) return "Refusing non-private host $host"
        return try {
            vault.port = port
            val r = api.pair(host, code, "${Build.MANUFACTURER} ${Build.MODEL}".trim())
            vault.token = r.getString("token"); vault.tokenId = r.optString("tokenId"); vault.host = host; api.activeHost = host
            paired = true; start(); null
        } catch (e: Exception) { e.message ?: "Pairing failed" }
    }
    /** Deep link from the Camera app: runs in the ViewModel scope so recomposition can't cancel it mid-pair. */
    fun pairFromLink(link: String) {
        pendingPair = null
        if (paired) { toast("Already paired. Unpair in Settings first to pair with a new QR."); return }
        viewModelScope.launch { toast(pairFromUri(link) ?: "Paired with ${vault.host}") }
    }
    fun unpair() { stop(); vault.token = null; vault.tokenId = ""; paired = false; status = null }

    override fun onCleared() { stop() }
}
