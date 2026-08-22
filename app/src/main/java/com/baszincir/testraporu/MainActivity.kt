package com.baszincir.testraporu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.baszincir.testraporu.auth.AuthViewModel
import com.baszincir.testraporu.ui.CustomerManageScreen
import com.baszincir.testraporu.ui.HistoryScreen
import com.baszincir.testraporu.ui.LoginScreen
import com.baszincir.testraporu.ui.NewReportScreen
import com.baszincir.testraporu.ui.ProductManageScreen
import com.baszincir.testraporu.ui.ReportViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
fun AppRoot() {
    val authVm: AuthViewModel = viewModel()
    if (!authVm.girisYapildi) {
        LoginScreen(authVm)
    } else {
        AnaEkran(authVm)
    }
}

private data class SekmeItem(val route: String, val baslik: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnaEkran(authVm: AuthViewModel) {
    val navController = rememberNavController()
    val reportVm: ReportViewModel = viewModel()
    val context = LocalContext.current

    // Herhangi bir ekranda oluşan bir Firestore/veri hatası, uygulamayı kapatmak yerine
    // burada okunabilir bir uyarı olarak gösterilir.
    LaunchedEffect(reportVm.hataMesaji) {
        reportVm.hataMesaji?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    val sekmeler = listOf(
        SekmeItem("yeni_rapor", "Yeni Rapor", Icons.Default.Add),
        SekmeItem("gecmis", "Geçmiş", Icons.Default.History),
        SekmeItem("urunler", "Ürünler", Icons.Default.List),
        SekmeItem("musteriler", "Müşteriler", Icons.Default.Person),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "Baş Zincir Logo",
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .height(28.dp)
                        )
                        Text("Baş Zincir - Test Raporu")
                    }
                },
                actions = {
                    IconButton(onClick = { authVm.cikisYap() }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Çıkış")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination
                sekmeler.forEach { sekme ->
                    NavigationBarItem(
                        selected = currentRoute?.hierarchy?.any { it.route == sekme.route } == true,
                        onClick = {
                            navController.navigate(sekme.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(sekme.icon, contentDescription = sekme.baslik) },
                        label = { Text(sekme.baslik) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "yeni_rapor",
            modifier = Modifier.padding(padding)
        ) {
            composable("yeni_rapor") { NewReportScreen(reportVm) }
            composable("gecmis") { HistoryScreen(reportVm) }
            composable("urunler") { ProductManageScreen(reportVm) }
            composable("musteriler") { CustomerManageScreen(reportVm) }
        }
    }
}
