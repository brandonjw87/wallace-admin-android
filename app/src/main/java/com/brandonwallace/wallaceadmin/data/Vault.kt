package com.brandonwallace.wallaceadmin.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Device token is encrypted with an AES-256-GCM key that never leaves the Android Keystore. */
const val DEFAULT_TAILSCALE = "192.0.2.26"

class Vault(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("wallace", Context.MODE_PRIVATE)
    private val alias = "wallace_agent_token_key"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build())
        return gen.generateKey()
    }

    var token: String?
        get() = try {
            val iv = prefs.getString("tiv", null) ?: return null; val ct = prefs.getString("tct", null) ?: return null
            val c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
            String(c.doFinal(Base64.decode(ct, Base64.NO_WRAP)), Charsets.UTF_8)
        } catch (e: Exception) { null }
        set(v) {
            if (v == null) { prefs.edit().remove("tiv").remove("tct").apply(); return }
            val c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE, key())
            prefs.edit().putString("tiv", Base64.encodeToString(c.iv, Base64.NO_WRAP)).putString("tct", Base64.encodeToString(c.doFinal(v.toByteArray()), Base64.NO_WRAP)).apply()
        }

    var host: String get() = prefs.getString("host", "192.0.2.13") ?: "192.0.2.13"; set(v) = prefs.edit().putString("host", v).apply()
    var altHost: String get() = (prefs.getString("altHost", "") ?: "").ifBlank { DEFAULT_TAILSCALE }; set(v) = prefs.edit().putString("altHost", v).apply()
    var port: Int get() = prefs.getInt("port", 8088); set(v) = prefs.edit().putInt("port", v).apply()
    var tokenId: String get() = prefs.getString("tokenId", "") ?: ""; set(v) = prefs.edit().putString("tokenId", v).apply()
    var pollSeconds: Int get() = prefs.getInt("poll", 10); set(v) = prefs.edit().putInt("poll", v).apply()
    var useLive: Boolean get() = prefs.getBoolean("live", true); set(v) = prefs.edit().putBoolean("live", v).apply()
}

object HostRules {
    /** Only private LAN, CGNAT/Tailscale (192.0.2.22/10) or MagicDNS names. Never a public address. */
    fun allowed(h: String): Boolean {
        val host = h.trim().lowercase()
        if (host.endsWith(".ts.net") || host.endsWith(".local") || host == "localhost") return true
        val p = host.split('.').mapNotNull { it.toIntOrNull() }
        if (p.size != 4 || p.any { it !in 0..255 }) return !host.contains('.') && host.isNotEmpty() // bare MagicDNS short name e.g. "msi"
        return p[0] == 10 || (p[0] == 192 && p[1] == 168) || (p[0] == 172 && p[1] in 16..31) || (p[0] == 100 && p[1] in 64..127)
    }
}
