package com.brandonwallace.wallaceadmin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.brandonwallace.wallaceadmin.data.AppVm
import com.brandonwallace.wallaceadmin.data.Conn
import com.brandonwallace.wallaceadmin.ui.*
import kotlinx.coroutines.launch

enum class Screen(val title: String, val icon: ImageVector) {
    Dashboard("Dashboard", Icons.Filled.SpaceDashboard), Secure("WALLACE Secure", Icons.Filled.WifiTethering), Live("Live Status", Icons.Filled.MonitorHeart),
    Devices("Devices", Icons.Filled.Devices), SafeGuard("Safe Guard", Icons.Filled.Shield), QueryLog("Query Log", Icons.Filled.ManageSearch),
    Admin("Admin", Icons.Filled.AdminPanelSettings), Blocklists("Blocklists", Icons.Filled.FilterList), Reports("Reports", Icons.Filled.Insights),
    Alerts("Alerts", Icons.Filled.NotificationsActive), Kuma("Uptime Kuma", Icons.Filled.Speed), Updates("Updates", Icons.Filled.SystemUpdate),
    Logs("Logs", Icons.Filled.Article), Settings("Settings", Icons.Filled.Settings), About("About", Icons.Filled.Info)
}
val bottom = listOf(Screen.Dashboard, Screen.Devices, Screen.SafeGuard, Screen.QueryLog)

class MainActivity : ComponentActivity() {
    private val vm: AppVm by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handle(intent)
        setContent { WallaceTheme { Root(vm) } }
    }
    override fun onNewIntent(intent: android.content.Intent) { super.onNewIntent(intent); handle(intent) }
    private fun handle(i: android.content.Intent?) { val d = i?.dataString ?: return; if (d.startsWith("wallace://pair")) { vm.pendingPair = d; i.data = null } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Root(vm: AppVm) {
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, vm.paired) {
        val o = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_START) vm.start(); if (e == Lifecycle.Event.ON_STOP) vm.stop() }
        owner.lifecycle.addObserver(o); onDispose { owner.lifecycle.removeObserver(o) }
    }
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(vm.pendingPair) { vm.pendingPair?.let { vm.pairFromLink(it) } }
    LaunchedEffect(vm.toasts.size) { while (vm.toasts.isNotEmpty()) { val t = vm.toasts.removeAt(0); snack.showSnackbar(t, withDismissAction = true) } }

    Box(Modifier.fillMaxSize().background(W.bgBrush)) {
        if (!vm.paired) { PairScreen(vm); SnackbarHost(snack, Modifier.align(Alignment.BottomCenter).navigationBarsPadding()); return@Box }
        var screen by rememberSaveable { mutableStateOf(Screen.Dashboard) }
        val drawer = rememberDrawerState(DrawerValue.Closed); val scope = rememberCoroutineScope()
        BackHandler(screen != Screen.Dashboard) { screen = Screen.Dashboard }
        ModalNavigationDrawer(drawerState = drawer, drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Color(0xFF0A1128)) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(12.dp)) {
                    Text("WALLACE Admin", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp))
                    Text("Companion for DESKTOP-9QNBM0H", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 12.dp, bottom = 12.dp))
                    Screen.entries.forEach { s ->
                        NavigationDrawerItem(label = { Text(s.title) }, icon = { Icon(s.icon, null) }, selected = s == screen, onClick = { screen = s; scope.launch { drawer.close() } },
                            colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = W.Blue.copy(alpha = .18f), unselectedContainerColor = Color.Transparent))
                    }
                }
            }
        }) {
            Scaffold(containerColor = Color.Transparent, snackbarHost = { SnackbarHost(snack) },
                topBar = {
                    TopAppBar(colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                        navigationIcon = { IconButton({ scope.launch { drawer.open() } }) { Icon(Icons.Filled.Menu, "Menu") } },
                        title = { Column { Text(screen.title, fontWeight = FontWeight.Bold); ConnLine(vm) } },
                        actions = { IconButton({ scope.launch { vm.refreshStatus() } }) { Icon(Icons.Filled.Refresh, "Refresh") } })
                },
                bottomBar = {
                    NavigationBar(containerColor = Color(0xE60A1128)) {
                        bottom.forEach { s -> NavigationBarItem(selected = s == screen, onClick = { screen = s }, icon = { Icon(s.icon, null) }, label = { Text(s.title, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = W.Blue.copy(alpha = .22f))) }
                        NavigationBarItem(selected = screen !in bottom, onClick = { scope.launch { drawer.open() } }, icon = { Icon(Icons.Filled.Apps, null) }, label = { Text("More", fontSize = 11.sp) })
                    }
                }) { pad ->
                AnimatedContent(screen, Modifier.padding(pad), transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "nav") { s ->
                    when (s) {
                        Screen.Dashboard -> DashboardScreen(vm) { screen = it }
                        Screen.Secure -> SecureScreen(vm); Screen.Live -> LiveScreen(vm); Screen.Devices -> DevicesScreen(vm)
                        Screen.SafeGuard -> SafeGuardScreen(vm); Screen.QueryLog -> QueryLogScreen(vm); Screen.Admin -> AdminScreen(vm)
                        Screen.Blocklists -> BlocklistsScreen(vm); Screen.Reports -> ReportsScreen(vm); Screen.Alerts -> AlertsScreen(vm)
                        Screen.Kuma -> KumaScreen(vm); Screen.Updates -> UpdatesScreen(vm); Screen.Logs -> LogsScreen(vm)
                        Screen.Settings -> SettingsScreen(vm); Screen.About -> AboutScreen(vm)
                    }
                }
            }
        }
    }
}

@Composable fun ConnLine(vm: AppVm) {
    val (c, t) = when (vm.conn) { Conn.Live -> W.Green to "Live"; Conn.Polling -> W.Blue to "Polling"; Conn.Connecting -> W.Yellow to "Connecting"; Conn.Offline -> W.Red to "Offline" }
    Row(verticalAlignment = Alignment.CenterVertically) { Dot(c, 7, pulse = vm.conn == Conn.Live); Spacer(Modifier.width(6.dp)); Text("$t · ${vm.api.activeHost}", fontSize = 11.sp, color = W.Sub) }
}
