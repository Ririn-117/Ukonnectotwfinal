package com.example.ukonnect2.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ukonnect2.R

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: Int
)

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onAbsenClick: () -> Unit // 👈 1. TAMBAHKAN PARAMETER INI
) {
    val orange = Color(0xFFFF9800)
    val gray = Color(0xFF374151)

    val items = listOf(
        BottomNavItem("beranda", "Beranda", R.drawable.ic_home),
        BottomNavItem("aktivitas", "Aktivitas", R.drawable.ic_calendar),
        BottomNavItem("pinjam", "Pinjam", R.drawable.ic_borrow),
        BottomNavItem("profil", "Profil", R.drawable.ic_profile)
    )

    Box {
        // Navbar background
        NavigationBar(
            containerColor = Color.White,
            tonalElevation = 8.dp
        ) {
            items.forEachIndexed { index, item ->
                // Spacer di tengah agar tombol Absen muat di atas
                if (index == 2) Spacer(Modifier.weight(1f, true))

                val selected = currentRoute == item.route
                NavigationBarItem(
                    selected = selected,
                    onClick = { onNavigate(item.route) }, // Ini untuk navigasi tab (DALAM)
                    icon = {
                        Icon(
                            painter = painterResource(id = item.icon),
                            contentDescription = item.title,
                            modifier = Modifier.size(22.dp),
                            tint = if (selected) orange else gray
                        )
                    },
                    label = {
                        Text(
                            text = item.title,
                            fontSize = 11.sp,
                            color = if (selected) orange else gray
                        )
                    },
                    alwaysShowLabel = true
                )
            }
        }

        // Tombol Absen melayang di tengah
        FloatingActionButton(
            onClick = { onAbsenClick() }, // 👈 2. UBAH INI (untuk navigasi LUAR)
            containerColor = orange,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-28).dp)
                .size(64.dp),
            elevation = FloatingActionButtonDefaults.elevation(8.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_qr), // ganti dengan ikon QR kamu
                contentDescription = "Absen",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}