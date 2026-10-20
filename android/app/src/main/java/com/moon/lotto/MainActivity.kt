package com.moon.lotto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moon.lotto.ui.CafeScreen
import com.moon.lotto.ui.GenScreen
import com.moon.lotto.ui.HelpScreen
import com.moon.lotto.ui.LottoTheme
import com.moon.lotto.ui.MineScreen
import com.moon.lotto.ui.SimScreen
import com.moon.lotto.ui.StatScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // tab·demo 는 화면 캡처(tools/screens.sh)용
        val tab = intent.getStringExtra("tab")
        val demo = intent.getBooleanExtra("demo", false)
        setContent { LottoTheme { LottoApp(startTab = tab, demo = demo) } }
    }
}

@Composable
fun LottoApp(vm: LottoViewModel = viewModel(), startTab: String? = null, demo: Boolean = false) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        if (startTab == "HELP") vm.showHelp = true
        Tab.entries.firstOrNull { it.name == startTab }?.let { vm.tab = it }
    }
    LaunchedEffect(vm.stats) { if (demo) vm.demo() }
    LaunchedEffect(vm.message) {
        vm.message?.let {
            vm.message = null
            snackbar.showSnackbar(it)
        }
    }
    BackHandler(enabled = vm.showHelp || vm.tab != Tab.GEN) {
        if (vm.showHelp) vm.showHelp = false else vm.tab = Tab.GEN
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = vm.tab == t && !vm.showHelp,
                        onClick = {
                            vm.showHelp = false
                            vm.tab = t
                            if (t == Tab.CAFE && vm.postBody.isEmpty()) vm.fillPost()
                        },
                        icon = { Text(t.emoji, fontSize = 20.sp) },
                        label = { Text(t.label, fontWeight = if (vm.tab == t) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer),
                    )
                }
            }
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).imePadding()) {
            Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (vm.showHelp) "도움말" else "🎱 로또 1등 연구소",
                        fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                    )
                    Text(vm.dataInfo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { vm.showHelp = !vm.showHelp }) {
                    Text(if (vm.showHelp) "✕ 닫기" else "❓ 도움말")
                }
            }
            Box(Modifier.weight(1f)) {
                if (vm.showHelp) HelpScreen(vm)
                else when (vm.tab) {
                    Tab.GEN -> GenScreen(vm)
                    Tab.STAT -> StatScreen(vm)
                    Tab.SIM -> SimScreen(vm)
                    Tab.MINE -> MineScreen(vm)
                    Tab.CAFE -> CafeScreen(vm)
                }
            }
        }
    }
}
