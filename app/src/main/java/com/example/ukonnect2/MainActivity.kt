package com.example.ukonnect2

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// Import ViewModel
import com.example.ukonnect2.ui.screen.PeminjamanViewModel
import com.example.ukonnect2.ui.viewmodel.GaleriViewModel

// Import Components & Screens
import com.example.ukonnect2.ui.component.BottomNavBar
import com.example.ukonnect2.ui.screen.*
import com.example.ukonnect2.ui.theme.UKOnnect2Theme
import com.example.ukonnect2.network.SessionManager
import com.example.ukonnect2.network.AbsensiUpsertRequest
import com.example.ukonnect2.network.RetrofitClient
import com.example.ukonnect2.services.FCMTokenManager

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SessionManager.init(applicationContext)

        // Initialize FCM Token
        val fcmTokenManager = FCMTokenManager(applicationContext)
        fcmTokenManager.initializeFCMToken()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            val granted = ContextCompat.checkSelfPermission(this, permission) ==
                    PackageManager.PERMISSION_GRANTED

            if (!granted) {
                requestNotificationPermissionLauncher.launch(permission)
            }
        }

        setContent {
            UKOnnect2Theme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "login"
                ) {

                    // --- LOGIN SCREEN ---
                    composable("login") {
                        LoginScreen(
                            onLoginSuccess = {
                                navController.navigate("main") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }

                    // --- MAIN APP FLOW (Bottom Navigation) ---
                    composable("main") {
                        val innerNav = rememberNavController()

                        // ViewModel untuk Peminjaman dideklarasikan di scope 'main'
                        // agar data tetap tersimpan saat pindah tab
                        val peminjamanVM: PeminjamanViewModel = viewModel()
                        val galeriVM: GaleriViewModel = viewModel()

                        Scaffold(
                            bottomBar = {
                                BottomNavBar(
                                    currentRoute = innerNav.currentBackStackEntryAsState().value?.destination?.route,
                                    onNavigate = { route ->
                                        innerNav.navigate(route) {
                                            popUpTo(innerNav.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onAbsenClick = {
                                        navController.navigate("qr_scanner")
                                    }
                                )
                            }
                        ) { innerPadding ->
                            NavHost(
                                navController = innerNav,
                                startDestination = "beranda",
                                modifier = Modifier.padding(innerPadding)
                            ) {
                                // 1. BERANDA
                                composable("beranda") {
                                    MainScreen(
                                        peminjamanVM = peminjamanVM,
                                        galeriVM = galeriVM,
                                        onGoGaleri = { innerNav.navigate("galeri") },
                                        onGoAbsensi = { innerNav.navigate("absensi") },
                                        onGoProfil = { innerNav.navigate("profil") },
                                        onGoAktivitas = { innerNav.navigate("aktivitas") },
                                        onGoBeranda = { innerNav.navigate("beranda") } // Biasanya tidak perlu navigate ke diri sendiri, tapi oke
                                    )
                                }

                                // 2. AKTIVITAS
                                composable("aktivitas") { AktivitasScreen() }

                                // 3. GALERI (UPDATE: Tambahkan onBack)
                                composable("galeri") {
                                    GaleriScreen(
                                        onBack = {
                                            // Aksi tombol back: kembali ke layar sebelumnya
                                            innerNav.popBackStack()
                                        }
                                    )
                                }

                                // 4. PEMINJAMAN
                                composable("pinjam") {
                                    PinjamScreen(viewModel = peminjamanVM)
                                }

                                // 5. PROFIL
                                composable("profil") {
                                    ProfilScreen(
                                        onLogout = {
                                            navController.navigate("login") {
                                                popUpTo("main") { inclusive = true }
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                }

                                // 6. ABSENSI LIST
                                composable("absensi") {
                                    AbsensiScreen(
                                        onBack = { innerNav.popBackStack() }
                                    )
                                }
                            }
                        }
                    }

                    // --- MAIN APP FLOW (Start at Absensi) ---
                    composable("main_absensi") {
                        val innerNav = rememberNavController()

                        // ViewModel untuk Peminjaman dideklarasikan di scope 'main'
                        // agar data tetap tersimpan saat pindah tab
                        val peminjamanVM: PeminjamanViewModel = viewModel()
                        val galeriVM: GaleriViewModel = viewModel()

                        Scaffold(
                            bottomBar = {
                                BottomNavBar(
                                    currentRoute = innerNav.currentBackStackEntryAsState().value?.destination?.route,
                                    onNavigate = { route ->
                                        innerNav.navigate(route) {
                                            popUpTo(innerNav.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onAbsenClick = {
                                        navController.navigate("qr_scanner")
                                    }
                                )
                            }
                        ) { innerPadding ->
                            NavHost(
                                navController = innerNav,
                                startDestination = "absensi",
                                modifier = Modifier.padding(innerPadding)
                            ) {
                                // 1. BERANDA
                                composable("beranda") {
                                    MainScreen(
                                        peminjamanVM = peminjamanVM,
                                        onGoGaleri = { innerNav.navigate("galeri") },
                                        onGoAbsensi = { innerNav.navigate("absensi") },
                                        onGoProfil = { innerNav.navigate("profil") },
                                        onGoAktivitas = { innerNav.navigate("aktivitas") },
                                        onGoBeranda = { innerNav.navigate("beranda") }
                                    )
                                }

                                // 2. AKTIVITAS
                                composable("aktivitas") { AktivitasScreen() }

                                // 3. GALERI
                                composable("galeri") {
                                    GaleriScreen(
                                        onBack = {
                                            innerNav.popBackStack()
                                        }
                                    )
                                }

                                // 4. PEMINJAMAN
                                composable("pinjam") {
                                    PinjamScreen(viewModel = peminjamanVM)
                                }

                                // 5. PROFIL
                                composable("profil") {
                                    ProfilScreen(
                                        onLogout = {
                                            navController.navigate("login") {
                                                popUpTo("main_absensi") { inclusive = true }
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                }

                                // 6. ABSENSI LIST
                                composable("absensi") {
                                    AbsensiScreen(
                                        onBack = { innerNav.popBackStack() }
                                    )
                                }
                            }
                        }
                    }

                    // --- QR SCANNER (Fullscreen, di luar BottomNav) ---
                    composable("qr_scanner") {
                        QrScannerScreen(
                            onBack = {
                                navController.popBackStack()
                            },
                            onQrCodeScanned = { qrValue ->
                                val nowMillis = System.currentTimeMillis()
                                val tanggal = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    .format(Date(nowMillis))
                                val jam = SimpleDateFormat("HH:mm", Locale.getDefault())
                                    .format(Date(nowMillis))

                                val statusMessage = "Absen berhasil diambil!"

                                val tipe = when {
                                    qrValue.contains("MASUK", ignoreCase = true) -> "Masuk"
                                    qrValue.contains("PULANG", ignoreCase = true) -> "Pulang"
                                    else -> "Masuk"
                                }

                                lifecycleScope.launch {
                                    try {
                                        RetrofitClient.instance.upsertAbsensi(
                                            AbsensiUpsertRequest(
                                                id = UUID.randomUUID().toString(),
                                                tanggal = tanggal,
                                                jam = jam,
                                                tipe = tipe,
                                                qrValue = qrValue,
                                                status = statusMessage
                                            )
                                        )
                                    } catch (_: Exception) {
                                    }
                                }

                                Toast.makeText(
                                    applicationContext,
                                    statusMessage,
                                    Toast.LENGTH_LONG
                                ).show()

                                navController.navigate("main_absensi") {
                                    popUpTo("main") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}