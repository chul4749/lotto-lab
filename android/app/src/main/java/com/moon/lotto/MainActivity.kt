package com.moon.lotto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moon.lotto.ui.CafeScreen
import com.moon.lotto.ui.GenScreen
import com.moon.lotto.ui.LottoTheme
import com.moon.lotto.ui.MineScreen
import com.moon.lotto.ui.SimScreen
import com.moon.lotto.ui.StatScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LottoTheme { LottoApp() } }
    }
}

@Composable
fun LottoApp(vm: LottoViewModel = viewModel()) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm.message) {
        vm.message?.let {
            vm.message = null
            snackbar.showSnackbar(it)
        }
    }
    BackHandler(enabled = vm.tab != Tab.GEN) { vm.tab = Tab.GEN }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = vm.tab == t,
                        onClick = {
                            vm.tab = t
                            if (t == Tab.CAFE && vm.postBody.isEmpty()) vm.fillPost()
                        },
                        icon = {
                            Icon(
                                when (t) {
                                    Tab.GEN -> Icons.Filled.Star
                                    Tab.STAT -> Icons.Filled.Info
                                    Tab.SIM -> Icons.Filled.PlayArrow
                                    Tab.MINE -> Icons.Filled.Favorite
                                    Tab.CAFE -> Icons.Filled.Share
                                },
                                contentDescription = null,
                            )
                        },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                Text("🎱 로또 1등 연구소", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(vm.dataInfo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(Modifier.weight(1f)) {
                when (vm.tab) {
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
