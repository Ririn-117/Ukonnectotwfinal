@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ukonnect2.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ukonnect2.R
import kotlinx.coroutines.launch

@Composable
fun ProfilScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("-") }
    var nama by remember { mutableStateOf("-") }
    var memberId by remember { mutableStateOf("-") }
    var divisi by remember { mutableStateOf("-") }
    var infoKegiatan by remember { mutableStateOf(true) }
    var pengingatJadwal by remember { mutableStateOf(false) }

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var editUsername by remember { mutableStateOf("") }
    var editNama by remember { mutableStateOf("") }
    var editMemberId by remember { mutableStateOf("") }
    var editDivisi by remember { mutableStateOf("") }

    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profil", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editNama,
                        onValueChange = { editNama = it },
                        label = { Text("Nama") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editUsername,
                        onValueChange = { editUsername = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editMemberId,
                        onValueChange = { editMemberId = it },
                        label = { Text("ID Anggota") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDivisi,
                        onValueChange = { editDivisi = it },
                        label = { Text("Divisi") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val nowMillis = System.currentTimeMillis()
                        val newUsername = editUsername.trim()
                        val newNama = editNama.trim()
                        val newMemberId = editMemberId.trim()
                        val newDivisi = editDivisi.trim()

                        scope.launch {
                            username = if (newUsername.isBlank()) "-" else newUsername
                            nama = if (newNama.isBlank()) "-" else newNama
                            memberId = if (newMemberId.isBlank()) "-" else newMemberId
                            divisi = if (newDivisi.isBlank()) "-" else newDivisi
                        }

                        showEditProfileDialog = false
                    }
                ) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) { Text("Batal") }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout", fontWeight = FontWeight.Bold) },
            text = { Text("Yakin ingin logout?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Ya, Logout", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Batal") }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        containerColor = Color(0xFFF8F8F8),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profil Saya", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = { /* aksi kembali */ }) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "Kembali",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Foto profil
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFCC99))
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF7A00))
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_edit),
                        contentDescription = "Edit Profil",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(nama, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Username: $username", color = Color.Gray, fontSize = 14.sp)
            Text("ID Anggota: $memberId", color = Color.Gray, fontSize = 14.sp)
            Text(divisi, color = Color.Gray, fontSize = 14.sp)

            Spacer(modifier = Modifier.height(24.dp))

            // Manajemen Akun
            Text("Manajemen Akun", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    MenuItem(R.drawable.ic_user, "Edit Profil") {
                        editUsername = if (username == "-") "" else username
                        editNama = if (nama == "-") "" else nama
                        editMemberId = if (memberId == "-") "" else memberId
                        editDivisi = if (divisi == "-") "" else divisi
                        showEditProfileDialog = true
                    }
                    HorizontalDivider(color = Color(0xFFFFE5CC))
                    MenuItem(R.drawable.ic_lock, "Ubah Kata Sandi")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Notifikasi
            Text("Notifikasi", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SwitchItem(R.drawable.ic_info, "Info Kegiatan", infoKegiatan) {
                        infoKegiatan = it
                    }
                    HorizontalDivider(color = Color(0xFFFFE5CC))
                    SwitchItem(R.drawable.ic_calendar, "Pengingat Jadwal", pengingatJadwal) {
                        pengingatJadwal = it
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Tombol Logout
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFE5E0),
                    contentColor = Color(0xFFFF3B30)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_logout),
                    contentDescription = "Logout",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Logout", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun MenuItem(iconRes: Int, text: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = text,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, color = Color.Black, modifier = Modifier.weight(1f))
        Image(
            painter = painterResource(id = R.drawable.ic_next),
            contentDescription = "Next",
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun SwitchItem(iconRes: Int, text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = text,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, color = Color.Black, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF7A00))
        )
    }
}
