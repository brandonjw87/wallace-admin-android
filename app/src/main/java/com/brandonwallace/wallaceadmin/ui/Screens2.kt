package com.brandonwallace.wallaceadmin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brandonwallace.wallaceadmin.data.*
import org.json.JSONObject

// ---------------- Safe Guard ----------------
@Composable fun SafeGuardScreen(vm: AppVm) {
    val l = rememberLoader(vm, autoRefreshSec = 60) { vm.api.get("/v1/safeguard") }
    var busy by remember { mutableStateOf(false) }; var test by remember { mutableStateOf("doubleclick.net") }; var testRes by remember { mutableStateOf<JSONObject?>(null) }
    var ask by remember { mutableStateOf<Pair<String, JSONObject>?>(null) }
    fun put(label: String, body: JSONObject) { busy = true; vm.act(label, after = { l.reload(); busy = false }) { vm.obj("PUT", "/v1/safeguard", body); JSONObject().put("message", "$label updated") } }
    Page {
        MessagingBadge(); LoadState(l)
        val s = l.value ?: return@Page
        Glass(pad = 18) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Ring(if (s.optBoolean("master")) 1f else .1f, if (s.optBoolean("master")) W.Green else W.Sub, 92, 9) { Icon(Icons.Filled.Shield, null, tint = if (s.optBoolean("master")) W.Green else W.Sub, modifier = Modifier.size(34.dp)) }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) { Text(big(s.optLong("blocked24h")), fontSize = 32.sp, fontWeight = FontWeight.Bold); Text("Safe Guard blocks · 24 h", style = MaterialTheme.typography.bodySmall) }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 10.dp), color = W.Green)
        }
        SectionTitle("Protection")
        Glass {
            ToggleRow("Safe Guard (master)", "Adult + Instagram + social + Safe search", s.optBoolean("master")) { ask = "master" to JSONObject().put("master", it) }
            ToggleRow("Adult content", "HaGeZi NSFW, OISD NSFW, StevenBlack + AdGuard parental", s.optBoolean("adult")) { put("Adult", JSONObject().put("adult", it)) }
            ToggleRow("Adult extra", "Block List Project (optional)", s.optBoolean("adultExtra")) { put("Adult extra", JSONObject().put("adultExtra", it)) }
            ToggleRow("Instagram", "AdGuard blocked service", s.optBoolean("instagram")) { put("Instagram", JSONObject().put("instagram", it)) }
            ToggleRow("Other social", "TikTok, Snapchat, Facebook, X, Reddit, Discord, Tinder, OnlyFans", s.optBoolean("social")) { put("Social", JSONObject().put("social", it)) }
            ToggleRow("Safe search", "Google, Bing, DDG, Ecosia… (YouTube stays unrestricted)", s.optBoolean("safeSearch")) { put("Safe search", JSONObject().put("safeSearch", it)) }
            ToggleRow("YouTube restricted mode", "Always off - the Rokus need full YouTube", false, locked = true) {}
            ToggleRow("Bypass blocking", "DoH/VPN lists, iCloud Private Relay & Firefox canary", s.optBoolean("bypass")) { put("Bypass", JSONObject().put("bypass", it)) }
            ToggleRow("Exempt Rokus", if (s.optBoolean("exemptRokus")) "Rokus get ad lists only" else "Rokus fully filtered (${s.optInt("rokuClients")} Roku clients)", s.optBoolean("exemptRokus")) { put("Exempt Rokus", JSONObject().put("exemptRokus", it)) }
        }
        SectionTitle("Categories")
        Glass {
            s.optJSONArray("categories")?.objs()?.forEach { c ->
                ToggleRow(c.s("title"), "${big(c.optLong("rules"))} rules · ${c.optInt("blocked24h")} blocked 24 h", c.optBoolean("on")) { put(c.s("key"), JSONObject().put("categories", JSONObject().put(c.s("key"), it))) }
            }
        }
        SectionTitle("Test an address")
        Glass {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(test, { test = it }, Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(14.dp))
                Spacer(Modifier.width(8.dp)); ActionButton("Test", Icons.Filled.Search) { vm.act("Test") { testRes = vm.api.post("/v1/check-host", JSONObject().put("host", test.trim())); JSONObject().put("message", "${testRes?.s("host")}: ${testRes?.s("reason")}") } }
            }
            testRes?.let { r -> Spacer(Modifier.height(8.dp)); Row { Pill(if (r.optBoolean("blocked")) "Blocked" else "Allowed", if (r.optBoolean("blocked")) W.Red else W.Green); Spacer(Modifier.width(8.dp)); if (r.optBoolean("messagingProtected")) Pill("Messaging protected", W.Green, Icons.Filled.Lock) }
                Text(r.s("reason") + (r.s("rule").takeIf { it.isNotBlank() }?.let { "\n$it" } ?: ""), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)) }
        }
        SectionTitle("Top blocked · 24 h")
        Glass { s.optJSONArray("top")?.objs()?.forEach { t -> Row(Modifier.padding(vertical = 4.dp)) { Text(t.s("domain"), Modifier.weight(1f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${t.optInt("count")}", color = W.Red, fontWeight = FontWeight.Bold) } } }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton("Re-apply Roku/YouTube allow", Icons.Filled.Tv, W.Violet) { vm.act("Allow rules") { vm.api.post("/v1/safeguard/reapply-allow") } }
        }
    }
    ask?.let { (k, b) -> Confirm("Turn Safe Guard ${if (b.optBoolean(k)) "on" else "off"}?", "Changes adult, Instagram, social and Safe search together for every filtered device.", "Apply", { ask = null }) { put("Safe Guard", b) } }
}
private suspend fun AppVm.obj(m: String, p: String, b: JSONObject) = api.obj(m, p, b)

// ---------------- Query log ----------------
@Composable fun QueryLogScreen(vm: AppVm) {
    var search by remember { mutableStateOf("") }; var blockedOnly by remember { mutableStateOf(false) }; var live by remember { mutableStateOf(true) }; var applied by remember { mutableStateOf("") }
    val l = rememberLoader(vm, key = applied to blockedOnly to live, autoRefreshSec = if (live) 5 else 0) { vm.api.get("/v1/querylog?limit=150&status=${if (blockedOnly) "blocked" else "all"}" + if (applied.isNotBlank()) "&search=${vm.api.enc(applied)}" else "") }
    var sel by remember { mutableStateOf<JSONObject?>(null) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp), leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = { IconButton({ applied = search.trim() }) { Icon(Icons.Filled.ArrowForward, null) } }, placeholder = { Text("Domain or client IP") })
        Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(!blockedOnly, { blockedOnly = false }, { Text("All") }); FilterChip(blockedOnly, { blockedOnly = true }, { Text("Blocked") })
            Spacer(Modifier.weight(1f)); Dot(if (live) W.Green else W.Sub, 8, pulse = live); Text(" Live", fontSize = 12.sp); Switch(live, { live = it }, Modifier.padding(start = 4.dp))
        }
        LoadState(l)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(l.value?.optJSONArray("entries")?.objs().orEmpty()) { e ->
                Glass(pad = 12, onClick = { sel = e }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Dot(if (e.optBoolean("blocked")) W.Red else W.Green, 8); Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(e.s("domain"), fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${e.s("clientName").ifBlank { e.s("client") }} · ${e.s("type")} · ${clock(e.s("time"))}", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        }
                        if (e.optBoolean("protectedMessaging")) Icon(Icons.Filled.Lock, null, tint = W.Green, modifier = Modifier.size(14.dp))
                        Text(e.s("reason").removePrefix("NotFiltered").removePrefix("Filtered").ifBlank { "ok" }, fontSize = 10.sp, color = if (e.optBoolean("blocked")) W.Red else W.Sub, modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
        }
    }
    sel?.let { e ->
        AlertDialog(onDismissRequest = { sel = null }, containerColor = W.Bg2, title = { Text(e.s("domain"), fontSize = 16.sp) }, text = { Column {
            KV("Client", "${e.s("clientName")} ${e.s("client")}"); KV("Time", clock(e.s("time"))); KV("Type", e.s("type")); KV("Result", e.s("reason"), if (e.optBoolean("blocked")) W.Red else W.Green)
            KV("Rule", e.s("rule", "-")); KV("Service", e.s("service", "-")); KV("Upstream", e.s("upstream", "-")); KV("Time taken", "${e.optDouble("elapsedMs", 0.0)} ms")
            if (e.optBoolean("protectedMessaging")) Pill("Messaging protected - can't be blocked", W.Green, Icons.Filled.Lock)
        } }, confirmButton = {
            val dom = e.s("domain")
            if (e.optBoolean("blocked")) TextButton({ vm.act("Allow") { vm.api.post("/v1/custom-rules", JSONObject().put("domain", dom).put("allow", true)); JSONObject().put("message", "Allowed $dom") }; sel = null }) { Text("Allow", color = W.Green) }
            else if (!e.optBoolean("protectedMessaging")) TextButton({ vm.act("Block") { vm.api.post("/v1/custom-rules", JSONObject().put("domain", dom)); JSONObject().put("message", "Blocked $dom") }; sel = null }) { Text("Block", color = W.Red) }
        }, dismissButton = { TextButton({ sel = null }) { Text("Close") } })
    }
}

// ---------------- Admin ----------------
@Composable fun AdminScreen(vm: AppVm) {
    val rules = rememberLoader(vm) { vm.api.get("/v1/custom-rules") }
    var dom by remember { mutableStateOf("") }; var confirm by remember { mutableStateOf<String?>(null) }
    Page {
        MessagingBadge()
        SectionTitle("Custom rules")
        Glass {
            OutlinedTextField(dom, { dom = it }, Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(14.dp), placeholder = { Text("example.com or AdGuard rule") })
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton("Block", Icons.Filled.Block, W.Red) { val d = dom; vm.act("Block", after = { rules.reload() }) { vm.api.post("/v1/custom-rules", JSONObject().put("domain", d)); dom = ""; JSONObject().put("message", "Blocked $d") } }
                ActionButton("Allow", Icons.Filled.CheckCircle, W.Green) { val d = dom; vm.act("Allow", after = { rules.reload() }) { vm.api.post("/v1/custom-rules", JSONObject().put("domain", d).put("allow", true)); dom = ""; JSONObject().put("message", "Allowed $d") } }
            }
            Text("Rules that would block messaging are refused by the PC.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
            LoadState(rules)
            rules.value?.optJSONArray("rules")?.strings()?.forEach { r ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (r.startsWith("@@")) Icons.Filled.CheckCircle else Icons.Filled.Block, null, tint = if (r.startsWith("@@")) W.Green else W.Red, modifier = Modifier.size(16.dp))
                    Text(r, Modifier.weight(1f).padding(start = 8.dp), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    IconButton({ vm.act("Remove", after = { rules.reload() }) { vm.api.obj("DELETE", "/v1/custom-rules?rule=${vm.api.enc(r)}"); JSONObject().put("message", "Removed") } }) { Icon(Icons.Filled.Delete, "Remove", tint = W.Sub) }
                }
            }
        }
        SectionTitle("Maintenance")
        Glass {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton("Update all lists", Icons.Filled.CloudSync, modifier = Modifier.weight(1f)) { vm.act("Lists") { vm.api.post("/v1/blocklists/refresh") } }
                ActionButton("Run watchdog", Icons.Filled.Pets, W.Violet, Modifier.weight(1f)) { vm.act("Watchdog") { vm.api.post("/v1/watchdog/run") } }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton("Restart hotspot", Icons.Filled.WifiTethering, W.Yellow, Modifier.weight(1f)) { confirm = "hotspot" }
                ActionButton("Recheck health", Icons.Filled.HealthAndSafety, W.Green, Modifier.weight(1f)) { vm.act("Recheck") { vm.api.post("/v1/health/recheck") } }
            }
            Text("Restart AdGuard, bedtime schedules, weekly reboot and backups stay on the desktop app (they need UAC at the PC).", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }
    }
    if (confirm == "hotspot") Confirm("Restart the WALLACE hotspot?", "Runs start / repair on the PC. TVs reconnect in ~30 s.", "Restart", { confirm = null }) { vm.act("Hotspot") { vm.api.post("/v1/hotspot/restart", JSONObject().put("confirm", true)) } }
}

// ---------------- Blocklists ----------------
@Composable fun BlocklistsScreen(vm: AppVm) {
    val l = rememberLoader(vm) { vm.api.get("/v1/blocklists") }
    Page {
        Row { ActionButton("Update all lists now", Icons.Filled.CloudSync) { vm.act("Lists", after = { l.reload() }) { vm.api.post("/v1/blocklists/refresh") } } }
        LoadState(l)
        l.value?.optJSONArray("lists")?.objs()?.groupBy { it.s("category") }?.toSortedMap()?.forEach { (cat, lists) ->
            SectionTitle("$cat · ${big(lists.filter { it.optBoolean("enabled") }.sumOf { it.optLong("rules") })} rules")
            Glass { lists.forEach { x -> Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Dot(if (x.optBoolean("enabled")) W.Green else W.Sub, 8); Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) { Text(x.s("name"), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${big(x.optLong("rules"))} rules · ${x.s("lastUpdated").take(16).replace('T', ' ')}", style = MaterialTheme.typography.bodySmall) }
            } } }
        }
    }
}

// ---------------- Reports ----------------
@Composable fun ReportsScreen(vm: AppVm) {
    var hours by remember { mutableIntStateOf(24) }
    val l = rememberLoader(vm, key = hours) { vm.api.get("/v1/reports/blocked?hours=$hours") }
    Page {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(24 to "24 h", 48 to "48 h", 168 to "7 days", 336 to "14 days").forEach { (h, t) -> FilterChip(hours == h, { hours = h }, { Text(t) }) } }
        LoadState(l); val r = l.value ?: return@Page
        Glass(Modifier.padding(top = 8.dp)) {
            Stat("blocked in ${if (hours <= 48) "$hours h" else "${hours / 24} days"}", big(r.optLong("total")), W.Red)
            Spacer(Modifier.height(8.dp)); Spark(r.optJSONArray("buckets")?.objs().orEmpty().map { it.optInt("count").toFloat() }, W.Violet, Modifier.fillMaxWidth().height(90.dp), bars = true)
            Text(if (hours <= 48) "hourly" else "daily", style = MaterialTheme.typography.bodySmall)
        }
        @Composable fun Top(title: String, key: String, c: androidx.compose.ui.graphics.Color) {
            SectionTitle(title); val items = r.optJSONArray(key)?.objs().orEmpty(); val mx = (items.maxOfOrNull { it.optInt("count") } ?: 1).coerceAtLeast(1)
            Glass { items.forEach { t -> Column(Modifier.padding(vertical = 4.dp)) {
                Row { Text(t.s("key"), Modifier.weight(1f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${t.optInt("count")}", fontWeight = FontWeight.Bold, color = c) }
                LinearProgressIndicator({ t.optInt("count").toFloat() / mx }, Modifier.fillMaxWidth().height(4.dp).padding(top = 2.dp), color = c, trackColor = W.Card)
            } } }
        }
        Top("By device", "byDevice", W.Blue); Top("By reason / app", "byReason", W.Violet); Top("Top domains", "byDomain", W.Red)
        Text("PDF reports are generated by the desktop app (Reports page).", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
    }
}

// ---------------- Alerts ----------------
@Composable fun AlertsScreen(vm: AppVm) {
    val l = rememberLoader(vm, autoRefreshSec = 15) { vm.api.arr("/v1/alerts") }
    Page {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton("Send test", Icons.Filled.NotificationAdd) { vm.act("Test", after = { l.reload() }) { vm.api.post("/v1/alerts/test") } }
            ActionButton("Clear list", Icons.Filled.ClearAll, W.Sub) { vm.act("Clear", after = { l.reload() }) { vm.api.post("/v1/alerts/clear") } }
        }
        LoadState(l)
        val list = l.value?.objs().orEmpty()
        if (list.isEmpty() && l.value != null) Glass(Modifier.padding(top = 10.dp)) { Text("No alerts. The agent raises alerts for hotspot off/on, monitors going down and red health tiles.", style = MaterialTheme.typography.bodySmall) }
        list.forEach { a -> Glass(Modifier.padding(top = 8.dp), pad = 12) {
            Row(verticalAlignment = Alignment.CenterVertically) { Dot(when (a.s("kind")) { "red" -> W.Red; "green" -> W.Green; else -> W.Blue }); Spacer(Modifier.width(10.dp)); Text(a.s("title"), Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 14.sp); Text(ago(a.s("time")), style = MaterialTheme.typography.bodySmall) }
            if (a.s("body").isNotBlank()) Text(a.s("body"), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        } }
    }
}

// ---------------- Uptime Kuma ----------------
@Composable fun KumaScreen(vm: AppVm) {
    val l = rememberLoader(vm, autoRefreshSec = 30) { vm.api.get("/v1/kuma") }
    Page {
        LoadState(l); val k = l.value ?: return@Page
        Glass { Row(verticalAlignment = Alignment.CenterVertically) { Dot(if (k.optBoolean("reachable")) W.Green else W.Red, 12, true); Spacer(Modifier.width(10.dp)); Text("Uptime Kuma ${k.s("version")}", style = MaterialTheme.typography.titleMedium) }
            KV("Dashboard", k.s("url") + " (on the PC)"); KV("Response", if (k.optBoolean("reachable")) "HTTP ${k.optInt("http")} in ${k.optInt("ms")} ms" else k.s("error"), if (k.optBoolean("reachable")) W.Green else W.Red)
            k.s("note").takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)) } }
        k.optJSONArray("monitors")?.objs()?.let { ms -> SectionTitle("Monitors"); ms.forEach { m -> Glass(Modifier.padding(bottom = 6.dp), pad = 12) { Row(verticalAlignment = Alignment.CenterVertically) { Dot(if (m.optBoolean("up")) W.Green else W.Red); Spacer(Modifier.width(10.dp)); Text(m.s("name"), Modifier.weight(1f)); Text("${(m.optDouble("uptime24", 0.0) * 100).toInt()}% · ${m.optDouble("ms", 0.0).toInt()} ms", style = MaterialTheme.typography.bodySmall) } } } }
    }
}

// ---------------- Updates ----------------
@Composable fun UpdatesScreen(vm: AppVm) {
    var wu by remember { mutableStateOf(false) }
    val l = rememberLoader(vm, key = wu) { vm.api.get("/v1/updates" + if (wu) "?windows=true" else "") }
    Page {
        Glass { Text("Read-only. Nothing installs from here.", style = MaterialTheme.typography.bodySmall); Row(Modifier.padding(top = 8.dp)) { ActionButton(if (wu) "Windows Update checked" else "Check Windows Update", Icons.Filled.Window) { wu = true } } }
        if (l.loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 8.dp), color = W.Blue)
        LoadState(l)
        l.value?.optJSONArray("items")?.objs()?.forEach { u -> Glass(Modifier.padding(top = 8.dp), pad = 14) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text(u.s("name"), Modifier.weight(1f), fontWeight = FontWeight.SemiBold); Pill(u.s("verdict").take(24), when { u.s("verdict").contains("up to date") || u.s("verdict") == "installed" -> W.Green; u.s("verdict").contains("available") -> W.Yellow; else -> W.Sub }) }
            KV("Installed", u.s("installed")); KV("Latest", u.s("latest"))
        } }
    }
}

// ---------------- Logs ----------------
@Composable fun LogsScreen(vm: AppVm) {
    var name by remember { mutableStateOf("watchdog") }
    val l = rememberLoader(vm, key = name, autoRefreshSec = 20) { vm.api.get("/v1/logs/$name?lines=200") }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("watchdog", "hotspot", "app", "agent", "audit").forEach { n -> FilterChip(name == n, { name = n }, { Text(n) }) } }
        LoadState(l)
        Glass(Modifier.padding(vertical = 8.dp), pad = 10) {
            LazyColumn { items(l.value?.optJSONArray("lines")?.strings().orEmpty().reversed()) { Text(it, fontFamily = FontFamily.Monospace, fontSize = 10.5.sp, color = W.Sub, modifier = Modifier.padding(vertical = 2.dp)) } }
        }
    }
}

// ---------------- Settings / About ----------------
@Composable fun SettingsScreen(vm: AppVm) {
    var host by remember { mutableStateOf(vm.vault.host) }; var alt by remember { mutableStateOf(vm.vault.altHost) }; var port by remember { mutableStateOf(vm.vault.port.toString()) }
    var poll by remember { mutableFloatStateOf(vm.vault.pollSeconds.toFloat()) }; var live by remember { mutableStateOf(vm.vault.useLive) }; var unpair by remember { mutableStateOf(false) }
    Page {
        SectionTitle("Connection")
        Glass {
            OutlinedTextField(host, { host = it }, Modifier.fillMaxWidth(), label = { Text("PC address (home LAN)") }, singleLine = true)
            OutlinedTextField(alt, { alt = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Fallback address (Tailscale 100.x or MagicDNS)") }, singleLine = true)
            OutlinedTextField(port, { port = it.filter(Char::isDigit) }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Port") }, singleLine = true)
            Text("Only private LAN, Tailscale (192.0.2.22/10) or *.ts.net addresses are accepted.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
            ActionButton("Save & reconnect", Icons.Filled.Save, W.Green, Modifier.padding(top = 10.dp)) {
                when {
                    !HostRules.allowed(host) -> vm.toast("$host isn't a private / Tailscale address")
                    alt.isNotBlank() && !HostRules.allowed(alt) -> vm.toast("$alt isn't a private / Tailscale address")
                    else -> { vm.vault.host = host.trim(); vm.vault.altHost = alt.trim(); vm.vault.port = port.toIntOrNull() ?: 8088; vm.start(); vm.toast("Saved") }
                }
            }
        }
        SectionTitle("Sync")
        Glass {
            ToggleRow("Live updates (WebSocket)", "Falls back to polling when off or unavailable", live) { live = it; vm.vault.useLive = it; vm.start() }
            Text("Poll every ${poll.toInt()} s", style = MaterialTheme.typography.bodySmall)
            Slider(poll, { poll = it }, valueRange = 5f..60f, onValueChangeFinished = { vm.vault.pollSeconds = poll.toInt(); vm.start() })
        }
        SectionTitle("Pairing")
        Glass {
            KV("Token id", vm.vault.tokenId.ifBlank { "-" }); KV("Stored in", "Android Keystore (AES-GCM)")
            Text("To revoke from the PC: WallaceAgent.exe --revoke ${vm.vault.tokenId}", style = MaterialTheme.typography.bodySmall)
            ActionButton("Unpair this phone", Icons.Filled.LinkOff, W.Red, Modifier.padding(top = 8.dp)) { unpair = true }
        }
    }
    if (unpair) Confirm("Unpair?", "Deletes the token from this phone. You'll need a new QR from the PC to pair again.", "Unpair", { unpair = false }) { vm.unpair() }
}

@Composable fun AboutScreen(vm: AppVm) {
    val s = rememberLoader(vm) { vm.api.get("/v1/session") }
    Page {
        Glass(pad = 20) {
            Text("WALLACE Admin for Android", style = MaterialTheme.typography.headlineSmall); Text("v1.0.1 · companion to WALLACE Admin 3.4.0 (desktop)", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))
            s.value?.let { KV("Agent", "WALLACE Agent ${it.s("version")}"); KV("Elevated", if (it.optBoolean("elevated")) "yes (service)" else "no - hotspot pause needs the service") }
            KV("PC", vm.status?.optJSONObject("agent")?.s("machine") ?: "-"); KV("Connected to", vm.api.base())
        }
        Spacer(Modifier.height(12.dp)); MessagingBadge()
        Spacer(Modifier.height(12.dp))
        Glass { Text("Safety rules are enforced on the PC, not in this app: messaging allow-list always first, writes that would block messaging are refused, the owner Pixel 10 Pro XL is permanently exempt, phones can't be paused, and YouTube restricted mode stays off for the Rokus.", style = MaterialTheme.typography.bodySmall) }
    }
}
