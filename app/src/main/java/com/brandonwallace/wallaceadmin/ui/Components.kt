package com.brandonwallace.wallaceadmin.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brandonwallace.wallaceadmin.data.AppVm
import kotlinx.coroutines.launch

@Composable
fun Glass(modifier: Modifier = Modifier, pad: Int = 16, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(Brush.verticalGradient(listOf(W.CardHi, W.Card))).border(1.dp, W.Stroke, shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier).padding(pad.dp), content = content
    )
}

@Composable fun SectionTitle(t: String, trailing: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(t.uppercase(), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.4.sp, modifier = Modifier.weight(1f)); trailing?.invoke()
    }
}

@Composable fun Dot(c: Color, size: Int = 10, pulse: Boolean = false) {
    val t = rememberInfiniteTransition(label = "p"); val a by t.animateFloat(0.35f, 1f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "a")
    Box(Modifier.size(size.dp).clip(CircleShape).background(c.copy(alpha = if (pulse) a else 1f)))
}

@Composable fun Pill(text: String, c: Color, icon: ImageVector? = null) {
    Row(Modifier.clip(RoundedCornerShape(50)).background(c.copy(alpha = .16f)).border(1.dp, c.copy(alpha = .5f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) { Icon(icon, null, tint = c, modifier = Modifier.size(13.dp)); Spacer(Modifier.width(4.dp)) }
        Text(text, color = c, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable fun MessagingBadge() {
    Glass(pad = 14) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(W.Green.copy(alpha = .18f)), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Lock, null, tint = W.Green, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Messaging protected (locked)", color = W.Green, fontWeight = FontWeight.SemiBold)
                Text("Texts, one-time codes, RCS, iMessage/FaceTime, Wi-Fi Calling and chat apps are never blocked. No setting here can override it.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable fun Ring(value: Float, color: Color, size: Int = 120, stroke: Int = 12, content: @Composable BoxScope.() -> Unit) {
    val v by animateFloatAsState(value.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing), label = "ring")
    Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = Stroke(stroke.dp.toPx(), cap = StrokeCap.Round)
            drawArc(Color.White.copy(alpha = .08f), 135f, 270f, false, style = s)
            drawArc(Brush.sweepGradient(listOf(color.copy(alpha = .6f), color, color.copy(alpha = .6f))), 135f, 270f * v, false, style = s)
        }
        content()
    }
}

@Composable fun Spark(values: List<Float>, color: Color, modifier: Modifier = Modifier.fillMaxWidth().height(46.dp), bars: Boolean = false) {
    Canvas(modifier) {
        if (values.isEmpty()) return@Canvas
        val mx = (values.maxOrNull() ?: 1f).coerceAtLeast(1e-3f); val w = size.width; val h = size.height
        if (bars) {
            val bw = w / values.size
            values.forEachIndexed { i, v -> val bh = h * (v / mx); drawRoundRect(Brush.verticalGradient(listOf(color, color.copy(alpha = .35f))), Offset(i * bw + bw * .15f, h - bh), androidx.compose.ui.geometry.Size(bw * .7f, bh), androidx.compose.ui.geometry.CornerRadius(4f, 4f)) }
        } else {
            val step = if (values.size > 1) w / (values.size - 1) else w
            val p = Path(); values.forEachIndexed { i, v -> val x = i * step; val y = h - h * (v / mx) * .9f; if (i == 0) p.moveTo(x, y) else p.lineTo(x, y) }
            val fill = Path().apply { addPath(p); lineTo(w, h); lineTo(0f, h); close() }
            drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = .35f), Color.Transparent)))
            drawPath(p, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable fun Uptime(history: List<Int>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(16.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        history.takeLast(40).forEach { Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(if (it == 1) W.Green else W.Red)) }
    }
}

@Composable fun Stat(label: String, value: String, color: Color = W.Text, modifier: Modifier = Modifier) {
    Column(modifier) { Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(label, style = MaterialTheme.typography.bodySmall, maxLines = 1) }
}

@Composable fun KV(k: String, v: String, vColor: Color = W.Text) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text(k, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(120.dp)); Text(v, color = vColor, fontSize = 13.sp, modifier = Modifier.weight(1f)) }
}

@Composable fun ToggleRow(title: String, sub: String? = null, checked: Boolean, enabled: Boolean = true, locked: Boolean = false, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium, fontSize = 15.sp); if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall) }
        if (locked) Icon(Icons.Filled.Lock, null, tint = W.Sub, modifier = Modifier.padding(end = 8.dp).size(16.dp))
        Switch(checked, onChange, enabled = enabled && !locked, colors = SwitchDefaults.colors(checkedTrackColor = W.Green, checkedThumbColor = Color.White))
    }
}

@Composable fun ActionButton(text: String, icon: ImageVector? = null, color: Color = W.Blue, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(onClick, modifier, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = .18f), contentColor = color), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
        if (icon != null) { Icon(icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }; Text(text, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable fun Confirm(title: String, body: String, confirm: String = "Confirm", onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, containerColor = W.Bg2, title = { Text(title) }, text = { Text(body, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = { TextButton({ onDismiss(); onConfirm() }) { Text(confirm, color = W.Yellow) } }, dismissButton = { TextButton(onDismiss) { Text("Cancel") } })
}

/** Loads data for a screen, keeps last good value, exposes reload. */
class Loader<T>(val value: T?, val loading: Boolean, val error: String?, val reload: () -> Unit)
@Composable fun <T> rememberLoader(vm: AppVm, key: Any = Unit, autoRefreshSec: Int = 0, fetch: suspend () -> T): Loader<T> {
    var v by remember(key) { mutableStateOf<T?>(null) }; var loading by remember(key) { mutableStateOf(true) }; var err by remember(key) { mutableStateOf<String?>(null) }
    var tick by remember(key) { mutableIntStateOf(0) }
    LaunchedEffect(key, tick) {
        loading = true
        try { v = fetch(); err = null } catch (e: Exception) { err = e.message ?: "error" }
        loading = false
        if (autoRefreshSec > 0) { kotlinx.coroutines.delay(autoRefreshSec * 1000L); tick++ }
    }
    return Loader(v, loading, err) { tick++ }
}

@Composable fun LoadState(l: Loader<*>) {
    if (l.loading && l.value == null) Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = W.Blue) }
    if (l.error != null) Glass(pad = 12) { Text(l.error, color = W.Red, fontSize = 13.sp) }
}

fun ago(iso: String?): String {
    if (iso.isNullOrBlank()) return "-"
    return try { val t = java.time.OffsetDateTime.parse(iso).toInstant(); val s = java.time.Duration.between(t, java.time.Instant.now()).seconds
        when { s < 60 -> "${s}s ago"; s < 3600 -> "${s / 60}m ago"; s < 86400 -> "${s / 3600}h ago"; else -> "${s / 86400}d ago" } } catch (e: Exception) { iso.take(16).replace('T', ' ') }
}
fun clock(iso: String?): String = try { java.time.OffsetDateTime.parse(iso).atZoneSameInstant(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("h:mm:ss a")) } catch (e: Exception) { iso ?: "" }
fun big(n: Long): String = when { n >= 1_000_000 -> String.format("%.2fM", n / 1e6); n >= 10_000 -> String.format("%.1fK", n / 1e3); else -> n.toString() }
