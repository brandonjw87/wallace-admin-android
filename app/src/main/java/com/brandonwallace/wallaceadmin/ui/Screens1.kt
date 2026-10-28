package com.brandonwallace.wallaceadmin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brandonwallace.wallaceadmin.Screen
import com.brandonwallace.wallaceadmin.data.*
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@Composable fun Page(content: @Composable ColumnScope.() -> Unit) =
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp), content = content)

// ---------------- Pairing ----------------
@Composable fun PairScreen(vm: AppVm) {
    val ctx = LocalContext.current; val scope = rememberCoroutineScope()
    var manual by remember { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }; var err by remember { mutableStateOf<String?>(null) }
    fun pair(text: String) { busy = true; err = null; vm.viewModelScopeLaunch { err = vm.pairFromUri(text); busy = false } }
    Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(30.dp))
        Ring(1f, W.Green, 150, 10) { Icon(Icons.Filled.Shield, null, tint = W.Green, modifier = Modifier.size(64.dp)) }
        Spacer(Modifier.height(18.dp))
        Text("WALLACE Admin", style = MaterialTheme.typography.headlineLarge)
        Text("Pair this phone with the WALLACE Agent on your PC", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(26.dp))
        Glass {
            Text("1. On the PC run  pair-phone.cmd  (WALLACE Agent folder).", fontSize = 14.sp)
            Text("2. Scan the QR it shows (here, or with the Camera app). It's single-use and expires in 10 minutes.", fontSize = 14.sp)
            Text("The phone only gets its own revocable token, never the AdGuard password.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(14.dp))
            ActionButton(if (busy) "Pairing…" else "Scan pairing QR", Icons.Filled.QrCodeScanner, W.Green, Modifier.fillMaxWidth()) {
                val opts = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
                GmsBarcodeScanning.getClient(ctx, opts).startScan().addOnSuccessListener { b -> b.rawValue?.let { pair(it) } }.addOnFailureListener { err = "Scanner unavailable: ${it.message}. Paste the pairing link below instead." }
            }
        }
        Spacer(Modifier.height(12.dp))
        Glass {
            Text("Or paste the pairing link", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(manual, { manual = it }, Modifier.fillMaxWidth().padding(top = 8.dp), placeholder = { Text("wallace://pair?host=…") }, singleLine = true)
            ActionButton("Pair", Icons.Filled.Link, W.Blue, Modifier.padding(top = 8.dp)) { pair(manual) }
        }
        err?.let { Spacer(Modifier.height(12.dp)); Text(it, color = W.Red) }
    }
}

// ---------------- Dashboard ----------------
@Composable fun DashboardScreen(vm: AppVm, go: (Screen) -> Unit) {
    val st = vm.status
    val health = rememberLoader(vm, autoRefreshSec = 60) { vm.api.get("/v1/health") }
    val stats = rememberLoader(vm, autoRefreshSec = 60) { vm.api.get("/v1/stats") }
    Page {
        if (st == null) { Glass { Text(vm.lastError?.let { "Can't reach the PC: $it" } ?: "Connecting to WALLACE Agent…", color = if (vm.lastError != null) W.Red else W.Sub) }; return@Page }
        val hs = st.optJSONObject("hotspot") ?: JSONObject(); val ag = st.optJSONObject("adguard") ?: JSONObject(); val h = st.optJSONObject("health") ?: JSONObject()
        val red = h.optInt("red"); val yellow = h.optInt("yellow"); val green = h.optInt("green"); val total = (red + yellow + green).coerceAtLeast(1)
        val overall = when { red > 0 -> W.Red; yellow > 0 -> W.Yellow; else -> W.Green }
        Glass(pad = 20) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Ring(green.toFloat() / total, overall, 116) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("$green/$total", fontSize = 22.sp, fontWeight = FontWeight.Bold); Text("healthy", style = MaterialTheme.typography.bodySmall) }
                }
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(when { red > 0 -> "Needs attention"; yellow > 0 -> "Mostly good"; else -> "All systems go" }, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { Pill("$red red", W.Red); Pill("$yellow yellow", W.Yellow) }
                    Spacer(Modifier.height(6.dp)); Text("Checked ${ago(h.s("time"))}" + if (h.optBoolean("running")) " · rechecking…" else "", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton("Recheck everything", Icons.Filled.Refresh, W.Blue) { vm.act("Recheck") { vm.api.post("/v1/health/recheck") } }
                ActionButton("Watchdog", Icons.Filled.Pets, W.Violet) { vm.act("Watchdog") { vm.api.post("/v1/watchdog/run") } }
            }
        }
        SectionTitle("Network")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Glass(Modifier.weight(1f), onClick = { go(Screen.Secure) }) {
                Row(verticalAlignment = Alignment.CenterVertically) { Dot(W.status(hs.s("state")), pulse = true); Spacer(Modifier.width(8.dp)); Text("WALLACE", fontWeight = FontWeight.SemiBold) }
                Spacer(Modifier.height(8.dp)); Stat("${hs.s("band", "?")} · ${hs.optInt("clients", 0)}/${hs.optInt("maxClients", 0)} clients", hs.s("state", "?"), W.status(hs.s("state")))
            }
            Glass(Modifier.weight(1f), onClick = { go(Screen.SafeGuard) }) {
                Row(verticalAlignment = Alignment.CenterVertically) { Dot(if (ag.optBoolean("reachable")) W.Green else W.Red, pulse = true); Spacer(Modifier.width(8.dp)); Text("AdGuard", fontWeight = FontWeight.SemiBold) }
                Spacer(Modifier.height(8.dp)); Stat("${ag.s("version")} · avg ${ag.optDouble("avgMs", 0.0)} ms", if (ag.optBoolean("protection", false)) "Protecting" else "Paused", if (ag.optBoolean("protection", false)) W.Green else W.Yellow)
            }
        }
        SectionTitle("DNS today")
        Glass {
            val q = ag.optLong("queries"); val b = ag.optLong("blocked")
            Row { Stat("queries", big(q), W.Blue, Modifier.weight(1f)); Stat("blocked", big(b), W.Red, Modifier.weight(1f)); Stat("blocked %", if (q > 0) String.format("%.1f%%", b * 100.0 / q) else "-", W.Violet, Modifier.weight(1f)) }
            stats.value?.let { s ->
                val dq = s.optJSONArray("dns_queries") ?: JSONArray(); val bf = s.optJSONArray("blocked_filtering") ?: JSONArray()
                Spacer(Modifier.height(10.dp)); Text("Queries · last 24 h", style = MaterialTheme.typography.bodySmall)
                Spark((0 until dq.length()).map { dq.optDouble(it).toFloat() }, W.Blue)
                Text("Blocked · last 24 h", style = MaterialTheme.typography.bodySmall); Spark((0 until bf.length()).map { bf.optDouble(it).toFloat() }, W.Red, bars = true)
            }
            Spacer(Modifier.height(8.dp))
            Row { Stat("lists on", "${ag.optInt("listsEnabled")}", modifier = Modifier.weight(1f)); Stat("list rules", big(ag.optLong("listRules")), modifier = Modifier.weight(1f)); Stat("user rules", "${ag.optInt("userRules")}", modifier = Modifier.weight(1f)) }
        }
        SectionTitle("Live status", trailing = { TextButton({ go(Screen.Live) }) { Text("All") } })
        Glass {
            val mons = st.optJSONArray("monitors")?.objs().orEmpty()
            if (mons.isEmpty()) Text("Waiting for the first check…", style = MaterialTheme.typography.bodySmall)
            mons.forEach { m -> Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Dot(if (m.optBoolean("up")) W.Green else W.Red); Spacer(Modifier.width(10.dp)); Text(m.s("name"), Modifier.weight(1f), fontSize = 14.sp)
                Text(if (m.optDouble("ms", -1.0) >= 0) "${m.optDouble("ms").toInt()} ms" else m.s("note").take(18), style = MaterialTheme.typography.bodySmall) } }
        }
        SectionTitle("Health tiles")
        LoadState(health)
        health.value?.optJSONArray("checks")?.objs()?.sortedBy { when (it.s("status")) { "Red" -> 0; "Yellow" -> 1; else -> 2 } }?.forEach { c ->
            Glass(Modifier.padding(bottom = 8.dp), pad = 12) {
                Row(verticalAlignment = Alignment.CenterVertically) { Dot(W.status(c.s("status"))); Spacer(Modifier.width(10.dp)); Text("${c.s("layer")} · ${c.s("name")}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
                Text(c.s("detail"), fontSize = 12.sp, color = W.Text, modifier = Modifier.padding(top = 4.dp)); Text(c.s("explain"), style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(12.dp)); MessagingBadge()
    }
}

// ---------------- WALLACE Secure (hotspot) ----------------
@Composable fun SecureScreen(vm: AppVm) {
    var confirm by remember { mutableStateOf(false) }
    val l = rememberLoader(vm, autoRefreshSec = 30) { vm.api.get("/v1/hotspot") }
    Page {
        LoadState(l); val h = l.value ?: vm.status?.optJSONObject("hotspot") ?: return@Page
        Glass(pad = 20) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Ring(if (h.s("state") == "On") 1f else .15f, W.status(h.s("state")), 100) { Icon(Icons.Filled.WifiTethering, null, tint = W.status(h.s("state")), modifier = Modifier.size(40.dp)) }
                Spacer(Modifier.width(16.dp))
                Column { Text(h.s("ssid", "WALLACE"), style = MaterialTheme.typography.headlineSmall); Pill(h.s("state"), W.status(h.s("state"))); Text("${h.optInt("clients")} of ${h.optInt("maxClients")} clients · ${h.s("band")}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)) }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton("Start / repair", Icons.Filled.Build, W.Yellow) { confirm = true }
                ActionButton("Refresh", Icons.Filled.Refresh) { vm.act("Refresh", after = { l.reload() }) { vm.api.post("/v1/hotspot/refresh"); null } }
            }
        }
        SectionTitle("Details")
        Glass { KV("Uplink", h.s("uplink")); KV("Host adapter", "${h.s("host")} · ${h.s("hostDesc")}"); KV("Virtual", h.s("virtual")); KV("Watchdog", h.s("watchdog")); if (h.s("error").isNotBlank()) KV("Error", h.s("error"), W.Red) }
        SectionTitle("Adapters")
        Glass { h.optJSONArray("adapters")?.objs()?.forEach { a -> KV(a.s("name"), "${a.s("status")} · ${a.s("link")}\n${a.s("desc")}") }; h.optJSONArray("radios")?.strings()?.forEach { Text(it, style = MaterialTheme.typography.bodySmall) } }
        SectionTitle("Watchdog log")
        Glass { h.optJSONArray("watchdogLog")?.strings()?.forEach { Text(it, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = W.Sub, modifier = Modifier.padding(vertical = 2.dp)) } }
    }
    if (confirm) Confirm("Start / repair hotspot?", "Runs start-hotspot.ps1 on the PC. Devices on WALLACE may drop for a few seconds.", "Start / repair", { confirm = false }) {
        vm.act("Hotspot") { vm.api.post("/v1/hotspot/restart", JSONObject().put("confirm", true)) }
    }
}

// ---------------- Live status (monitors) ----------------
@Composable fun LiveScreen(vm: AppVm) {
    val mons = vm.status?.optJSONArray("monitors")?.objs().orEmpty()
    Page {
        Row { ActionButton("Check now", Icons.Filled.PlayArrow) { vm.act("Check", after = { vm.refreshStatus() }) { vm.api.obj("POST", "/v1/monitors/check"); JSONObject().put("message", "Checked") } } }
        Spacer(Modifier.height(8.dp))
        mons.forEach { m ->
            Glass(Modifier.padding(bottom = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Dot(if (m.optBoolean("up")) W.Green else W.Red, 12, pulse = true); Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) { Text(m.s("name"), fontWeight = FontWeight.SemiBold); Text("${m.s("type")} · ${m.s("target")}", style = MaterialTheme.typography.bodySmall) }
                    Column(horizontalAlignment = Alignment.End) { Text(if (m.optDouble("ms", -1.0) >= 0) "${m.optDouble("ms").toInt()} ms" else "-", fontWeight = FontWeight.Bold); Text("${m.optDouble("uptimePct")}% up", style = MaterialTheme.typography.bodySmall) }
                }
                Spacer(Modifier.height(8.dp)); Uptime((m.optJSONArray("history") ?: JSONArray()).let { a -> (0 until a.length()).map { a.optInt(it) } })
                Text(m.s("note"), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

// ---------------- Devices ----------------
@Composable fun DevicesScreen(vm: AppVm) {
    val l = rememberLoader(vm, autoRefreshSec = 30) { vm.api.arr("/v1/devices") }
    var filter by remember { mutableStateOf("All") }; var q by remember { mutableStateOf("") }
    var sel by remember { mutableStateOf<JSONObject?>(null) }; var pauseAsk by remember { mutableStateOf<JSONObject?>(null) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Filled.Search, null) }, placeholder = { Text("Search name, IP, MAC, vendor") }, singleLine = true, shape = RoundedCornerShape(16.dp))
        Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("All", "Hotspot", "Home LAN", "Online", "Paused").forEach { f -> FilterChip(f == filter, { filter = f }, { Text(f, fontSize = 12.sp) }) }
        }
        LoadState(l)
        val list = l.value?.objs().orEmpty().filter { d ->
            (filter == "All" || (filter == "Online" && d.optBoolean("online")) || (filter == "Paused" && d.optBoolean("paused")) || d.s("network") == filter) &&
            (q.isBlank() || listOf("name", "ip", "mac", "vendor", "client").any { d.s(it).contains(q, true) })
        }
        Text("${list.size} devices · ${list.count { it.optBoolean("online") }} online", style = MaterialTheme.typography.bodySmall)
        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list, key = { it.s("mac") }) { d ->
                Glass(pad = 14, onClick = { sel = d }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(when { d.optBoolean("phone") -> Icons.Filled.Smartphone; d.optBoolean("roku") -> Icons.Filled.Tv; d.s("vendor").contains("LG", true) -> Icons.Filled.Tv; else -> Icons.Filled.DevicesOther }, null, tint = if (d.optBoolean("online")) W.Blue else W.Sub)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(d.s("name").ifBlank { d.s("mac") }, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text("${d.s("ip")} · ${d.s("network")} · ${d.s("vendor")}", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            if (d.optBoolean("paused")) Pill("Paused", W.Red) else Pill(d.s("mode"), when (d.s("mode")) { "exempt" -> W.Green; "roku" -> W.Violet; "filtered" -> W.Blue; else -> W.Sub })
                            if (d.optBoolean("owner")) Pill("Owner", W.Green, Icons.Filled.Lock)
                        }
                    }
                }
            }
        }
    }
    sel?.let { d ->
        AlertDialog(onDismissRequest = { sel = null }, containerColor = W.Bg2, title = { Text(d.s("name").ifBlank { d.s("mac") }) }, text = {
            Column {
                KV("IP", d.s("ip")); KV("MAC", d.s("mac")); KV("Network", d.s("network")); KV("Vendor", d.s("vendor")); KV("AdGuard client", d.s("client", "-")); KV("Mode", d.s("mode")); KV("Last seen", d.s("lastSeen").take(16).replace('T', ' '))
                d.s("lockReason").takeIf { it.isNotBlank() }?.let { Spacer(Modifier.height(6.dp)); Pill(it, W.Green, Icons.Filled.Lock) }
                Spacer(Modifier.height(12.dp)); Text("Filter mode", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("exempt", "roku", "filtered").forEach { m -> FilterChip(d.s("mode") == m, { vm.act("Mode", after = { l.reload() }) { vm.api.post("/v1/devices/${d.s("mac")}/mode", JSONObject().put("mode", m)); JSONObject().put("message", "Mode set to $m") }; sel = null }, { Text(m) }, enabled = d.optBoolean("canChangeMode")) }
                }
            }
        }, confirmButton = {
            if (d.optBoolean("paused")) TextButton({ vm.act("Unpause", after = { l.reload() }) { vm.api.post("/v1/devices/${d.s("mac")}/pause", JSONObject().put("on", false)) }; sel = null }) { Text("Unpause", color = W.Green) }
            else TextButton({ pauseAsk = d; sel = null }, enabled = d.optBoolean("canPause")) { Text(if (d.optBoolean("canPause")) "Pause internet" else "Can't pause", color = if (d.optBoolean("canPause")) W.Red else W.Sub) }
        }, dismissButton = { TextButton({ sel = null }) { Text("Close") } })
    }
    pauseAsk?.let { d -> Confirm("Pause ${d.s("name")}?", "Blocks this device's internet until you unpause it.", "Pause", { pauseAsk = null }) { vm.act("Pause", after = { l.reload() }) { vm.api.post("/v1/devices/${d.s("mac")}/pause", JSONObject().put("on", true)) } } }
}
