package com.example.ukonnect2.ui.screen

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ukonnect2.R
import java.text.SimpleDateFormat
import java.util.*
import android.os.Build // Diperlukan untuk pengecekan Build.VERSION.SDK_INT
import androidx.compose.runtime.saveable.rememberSaveable // Diperlukan untuk state dialog
import android.app.AlarmManager // Diperlukan untuk pengecekan canScheduleExactAlarms
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.ukonnect2.network.PeminjamanCreateRequest
import com.example.ukonnect2.network.PeminjamanDto
import com.example.ukonnect2.network.RetrofitClient

// --- DATA MODELS (Tetap Sama) ---
data class Equipment(
    val id: String,
    val nama: String,
    val stokTersedia: Int,
    val stokTotal: Int,
    val icon: Int
)

data class Loan(
    val id: String,
    val alatId: String,
    val namaEquipment: String,
    val waktuPinjam: Long, // Waktu pinjam (dibuat)
    val tanggalMulai: Long, // Tanggal Mulai Peminjaman yang dipilih (termasuk waktu)
    val tanggalSelesai: Long, // Tanggal Selesai Peminjaman yang dipilih (termasuk waktu)
    val jumlah: Int,
    val status: String // Dipinjam | Dikembalikan
)

// --- VIEW MODEL (Tetap Sama) ---
class PeminjamanViewModel(application: Application) : AndroidViewModel(application) {
    // ... (Kode ViewModel tetap sama) ...
    private val api = RetrofitClient.instance

    var alatList = mutableStateListOf(
        Equipment("1", "Bola Futsal Specs", 0, 0, R.drawable.ic_ball_pinjam),
        Equipment("2", "Raket Badminton Yonex", 0, 0, R.drawable.ic_racket),
        Equipment("3", "Bola Basket Molten GG7X", 0, 0, R.drawable.ic_basket)
    )
        private set

    var riwayat = mutableStateListOf<Loan>()
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            refreshAll()
        }
    }

    private fun iconForKeyOrName(iconKey: String?, name: String): Int {
        return when (iconKey?.lowercase(Locale.ROOT)) {
            "ball" -> R.drawable.ic_ball_pinjam
            "racket" -> R.drawable.ic_racket
            "basket" -> R.drawable.ic_basket
            else -> {
                val n = name.lowercase(Locale.ROOT)
                when {
                    n.contains("bola") && n.contains("futsal") -> R.drawable.ic_ball_pinjam
                    n.contains("raket") -> R.drawable.ic_racket
                    n.contains("basket") -> R.drawable.ic_basket
                    else -> R.drawable.ic_ball_pinjam
                }
            }
        }
    }

    private fun PeminjamanDto.toLoan(): Loan = Loan(
        id = id,
        alatId = alatId,
        namaEquipment = namaEquipment,
        waktuPinjam = waktuPinjam,
        tanggalMulai = tanggalMulai,
        tanggalSelesai = tanggalSelesai,
        jumlah = jumlah,
        status = status
    )

    fun refreshAll() {
        viewModelScope.launch {
            try {
                val alat = api.getAlat()
                alatList.clear()
                alatList.addAll(
                    alat.map {
                        Equipment(
                            id = it.id,
                            nama = it.nama,
                            stokTersedia = it.stokTersedia,
                            stokTotal = it.stokTotal,
                            icon = iconForKeyOrName(it.iconKey, it.nama)
                        )
                    }
                )

                val loans = api.getPeminjaman()
                riwayat.clear()
                riwayat.addAll(loans.map { it.toLoan() })
                errorMessage = null
            } catch (e: Exception) {
                errorMessage = e.message ?: "Terjadi kesalahan saat memuat data"
                Log.e("PeminjamanViewModel", "refreshAll failed", e)
            }
        }
    }

    fun pinjam(
        alat: Equipment,
        qty: Int,
        tglMulai: Long,
        tglSelesai: Long,
        onPinjamSuccess: (Loan) -> Unit // Callback untuk menjadwalkan notifikasi
    ) {
        viewModelScope.launch {
            try {
                val created = api.createPeminjaman(
                    PeminjamanCreateRequest(
                        alatId = alat.id,
                        qty = qty,
                        tanggalMulai = tglMulai,
                        tanggalSelesai = tglSelesai
                    )
                )
                val loan = created.toLoan()
                riwayat.add(0, loan)
                refreshAll()
                onPinjamSuccess(loan)
                errorMessage = null
            } catch (e: Exception) {
                errorMessage = e.message ?: "Terjadi kesalahan saat meminjam"
                Log.e("PeminjamanViewModel", "pinjam failed", e)
            }
        }
    }

    fun kembalikan(loan: Loan, qty: Int, onReturnSuccess: (String) -> Unit) {
        if (loan.status != "Dipinjam") return
        viewModelScope.launch {
            try {
                api.kembalikanPeminjaman(loan.id, com.example.ukonnect2.network.PeminjamanKembalikanRequest(qty))
                refreshAll()
                onReturnSuccess(loan.id)
            } catch (_: Exception) {
            }
        }
    }

    fun batalkanPinjaman(loan: Loan, onBatalkanSuccess: (String) -> Unit) {
        if (loan.status != "Dipinjam") return
        viewModelScope.launch {
            try {
                api.kembalikanPeminjaman(loan.id, com.example.ukonnect2.network.PeminjamanKembalikanRequest(loan.jumlah))
                refreshAll()
                onBatalkanSuccess(loan.id)
            } catch (_: Exception) {
            }
        }
    }

    fun hapusLoan(loan: Loan, onHapusSuccess: (String) -> Unit) {
        viewModelScope.launch {
            try {
                api.deletePeminjaman(loan.id)
                refreshAll()
                onHapusSuccess(loan.id)
            } catch (_: Exception) {
            }
        }
    }

    fun loansAktif(): List<Loan> = riwayat.filter { it.status == "Dipinjam" && it.jumlah > 0 }
    fun loansSelesai(): List<Loan> = riwayat.filter { it.status == "Dikembalikan" || it.jumlah == 0 }
}

// --- MAIN SCREEN (Tetap Sama) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinjamScreen(viewModel: PeminjamanViewModel) {
    // ... (Kode PinjamScreen tetap sama) ...
    val tabTitles = listOf("Daftar Alat", "Sedang Dipinjam", "Riwayat Peminjaman")
    var selectedTabIndex by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text("Peminjaman Alat", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            })
        },
        containerColor = Color(0xFFF8FAFB)
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                edgePadding = 8.dp,
                indicator = {},
                divider = {}
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) Color(0xFFFF6B00) else Color(0xFF5B5B5B),
                                maxLines = 1
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            when (selectedTabIndex) {
                0 -> DaftarAlat(viewModel)
                1 -> SedangDipinjamList(viewModel)
                2 -> RiwayatList(viewModel)
            }
        }
    }
}

// --- TAB CONTENT: DAFTAR ALAT (Integrasi Notifikasi & Cek Izin) ---
@Composable
fun DaftarAlat(viewModel: PeminjamanViewModel) {
    val context = LocalContext.current
    // FCM handles scheduling automatically

    // State untuk mengontrol tampilan dialog izin
    var showPermissionDialog by rememberSaveable { mutableStateOf(false) }

    // Pengecekan status izin Exact Alarm (API 31+)
    val hasExactAlarmPermission: Boolean by remember {
        derivedStateOf {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                true // FCM handles scheduling automatically
            } else {
                true // Dianggap diizinkan secara default pada API < 31
            }
        }
    }

    when {
        viewModel.errorMessage != null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(viewModel.errorMessage ?: "Terjadi kesalahan", color = Color.Red)
            }
        }
        viewModel.alatList.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Tidak ada alat yang terdaftar.", color = Color.Gray)
            }
        }
        else -> {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(viewModel.alatList, key = { it.id }) { alat ->
                    EquipmentItem(
                        alat = alat,
                        onConfirm = { qty, tglMulai, tglSelesai ->
                            // Lakukan Peminjaman HANYA jika izin ada (atau API < 31)
                            if (hasExactAlarmPermission) {
                                viewModel.pinjam(
                                    alat = alat,
                                    qty = qty,
                                    tglMulai = tglMulai,
                                    tglSelesai = tglSelesai,
                                    onPinjamSuccess = { newLoan ->
                                        // FCM handles scheduling automatically
                                    }
                                )
                            } else {
                                // Ini seharusnya tidak terpanggil jika kita menggunakan pengecekan di tombol
                                showPermissionDialog = true
                            }
                        },
                        // Callback baru yang dipicu saat tombol 'Pinjam' ditekan
                        onInitialPinjamClick = {
                            if (!hasExactAlarmPermission) {
                                showPermissionDialog = true
                            }
                        }
                    )
                }
            }
        }
    }

    // Dialog Permintaan Izin
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Izin Khusus Diperlukan") },
            text = { Text("Untuk menjadwalkan pengingat pengembalian tepat waktu (Alarm), Anda perlu memberikan izin 'Alarm dan Pengingat Tepat Waktu' di pengaturan sistem.") },
            confirmButton = {
                Button(onClick = {
                    showPermissionDialog = false
                    true // FCM handles scheduling automatically
                }) { Text("Buka Pengaturan") }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) { Text("Tutup") }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// --- TAB CONTENT: SEDANG DIPINJAM (Tidak Berubah) ---
@Composable
fun SedangDipinjamList(viewModel: PeminjamanViewModel) {
    // ... (Kode SedangDipinjamList tetap sama) ...
    val fmtWaktu = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val fmtDateTime = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val data = viewModel.loansAktif()

    val context = LocalContext.current
    // FCM handles scheduling automatically

    if (data.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Belum ada alat yang sedang dipinjam.", color = Color.Gray)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(data, key = { it.id }) { loan ->
                var showReturnDialog by remember { mutableStateOf(false) }
                var showCancelDialog by remember { mutableStateOf(false) }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(loan.namaEquipment, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Dicatat: ${fmtWaktu.format(Date(loan.waktuPinjam))}", color = Color(0xFF7C8795), fontSize = 13.sp)
                        Text(
                            "Periode: ${fmtDateTime.format(Date(loan.tanggalMulai))} s/d ${fmtDateTime.format(Date(loan.tanggalSelesai))}",
                            color = Color(0xFF005691),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text("Jumlah dipinjam: ${loan.jumlah}", fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showReturnDialog = true },
                                border = BorderStroke(1.dp, Color(0xFF00A85A)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00A85A)),
                                modifier = Modifier.weight(1f)
                            ) { Text("Kembalikan", fontWeight = FontWeight.SemiBold) }

                            OutlinedButton(
                                onClick = { showCancelDialog = true },
                                border = BorderStroke(1.dp, Color(0xFFFF7A00)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF7A00)),
                                modifier = Modifier.weight(1f)
                            ) { Text("Batalkan", fontWeight = FontWeight.SemiBold) }
                        }
                    }
                }

                // Dialog kembalikan
                if (showReturnDialog) {
                    var qty by remember { mutableStateOf(1) }
                    val maxQty = loan.jumlah
                    AlertDialog(
                        onDismissRequest = { showReturnDialog = false },
                        title = { Text("Kembalikan Alat", fontWeight = FontWeight.Bold) },
                        text = {
                            Column {
                                Text("Pilih jumlah yang ingin dikembalikan.", color = Color(0xFF4E4E4E))
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { if (qty > 1) qty-- },
                                        contentPadding = PaddingValues(horizontal = 10.dp),
                                        modifier = Modifier.defaultMinSize(minWidth = 36.dp, minHeight = 36.dp)
                                    ) { Text("-") }
                                    Text("$qty", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    OutlinedButton(
                                        onClick = { if (qty < maxQty) qty++ },
                                        contentPadding = PaddingValues(horizontal = 10.dp),
                                        modifier = Modifier.defaultMinSize(minWidth = 36.dp, minHeight = 36.dp)
                                    ) { Text("+") }

                                    Spacer(Modifier.weight(1f))
                                    Text("Maks: $maxQty", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showReturnDialog = false
                                    viewModel.kembalikan(loan, qty) { loanId ->
                                        // FCM handles scheduling automatically
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A85A))
                            ) { Text("Kembalikan $qty", color = Color.White, fontWeight = FontWeight.Bold) }
                        },
                        dismissButton = { TextButton(onClick = { showReturnDialog = false }) { Text("Batal") } },
                        containerColor = Color.White,
                        shape = RoundedCornerShape(20.dp)
                    )
                }

                // Dialog Batalkan Pinjaman (Pengganti Hapus)
                if (showCancelDialog) {
                    AlertDialog(
                        onDismissRequest = { showCancelDialog = false },
                        title = { Text("Batalkan", fontWeight = FontWeight.Bold) },
                        text = {
                            Text("Yakin ingin membatalkan pinjaman ${loan.namaEquipment}? Stok akan dikembalikan penuh.", color = Color(0xFF4E4E4E))
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showCancelDialog = false
                                    viewModel.batalkanPinjaman(loan) { loanId ->
                                        // FCM handles scheduling automatically
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) { Text("Ya, Batalkan", color = Color.White, fontWeight = FontWeight.Bold) }
                        },
                        dismissButton = { TextButton(onClick = { showCancelDialog = false }) { Text("Tutup") } },
                        containerColor = Color.White,
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }
    }
}

// --- TAB CONTENT: RIWAYAT (Hapus Riwayat) (Tidak Berubah) ---
@Composable
fun RiwayatList(viewModel: PeminjamanViewModel) {
    // ... (Kode RiwayatList tetap sama) ...
    val fmtWaktu = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val fmtDateTime = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val data = viewModel.loansSelesai()

    val context = LocalContext.current
    // FCM handles scheduling automatically

    if (data.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Belum ada riwayat peminjaman.", color = Color.Gray)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(data, key = { it.id }) { loan ->
                var showDeleteDialog by remember { mutableStateOf(false) }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(loan.namaEquipment, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(fmtWaktu.format(Date(loan.waktuPinjam)), color = Color(0xFF7C8795), fontSize = 13.sp)
                        if (loan.tanggalMulai > 0 && loan.tanggalSelesai > 0) {
                            Text(
                                "Periode: ${fmtDateTime.format(Date(loan.tanggalMulai))} s/d ${fmtDateTime.format(Date(loan.tanggalSelesai))}",
                                color = Color(0xFF005691),
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text("Periode: Tidak Tercatat", color = Color.Gray, fontSize = 13.sp)
                        }
                        Text("Status: ${loan.status}", color = Color(0xFF00A85A), fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { showDeleteDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) { Text("Hapus Riwayat", color = Color.White) }
                    }
                }

                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("Hapus Riwayat", fontWeight = FontWeight.Bold) },
                        text = { Text("Yakin ingin menghapus riwayat ${loan.namaEquipment}?") },
                        confirmButton = {
                            Button(onClick = {
                                showDeleteDialog = false
                                viewModel.hapusLoan(loan) { loanId -> /* FCM handles scheduling automatically */ }
                            }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                                Text("Hapus", color = Color.White)
                            }
                        },
                        dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Batal") } },
                        containerColor = Color.White,
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }
    }
}

// --- DATE & TIME PICKER HELPER (Tidak Berubah) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerButton(
    label: String,
    initialDateTime: Long,
    onDateTimeSelected: (Long) -> Unit
) {
    // ... (Kode DateTimePickerButton tetap sama) ...
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = dateFormatter.format(Date(initialDateTime)),
        onValueChange = { /* read-only */ },
        label = { Text(label) },
        readOnly = true,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDatePicker = true },
        trailingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_calendar),
                contentDescription = "Pilih Tanggal dan Waktu",
                modifier = Modifier
                    .clickable { showDatePicker = true }
                    .size(24.dp)
            )
        }
    )

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateTime,
        initialDisplayMode = DisplayMode.Picker
    )

    val cal = remember(initialDateTime) { Calendar.getInstance().apply { timeInMillis = initialDateTime } }
    val initialHour = cal.get(Calendar.HOUR_OF_DAY)
    val initialMinute = cal.get(Calendar.MINUTE)

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    // Dialog 1: Date Picker
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        showDatePicker = false
                        showTimePicker = true
                    }
                }) { Text("Lanjut Pilih Waktu") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Dialog 2: Time Picker
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Pilih Waktu", fontWeight = FontWeight.Bold) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val selectedDateMillis = datePickerState.selectedDateMillis ?: initialDateTime
                    val selectedCalendar = Calendar.getInstance().apply {
                        timeInMillis = selectedDateMillis
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onDateTimeSelected(selectedCalendar.timeInMillis)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Batal") }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}


// --- ITEM ALAT (TERMASUK DIALOG PINJAM) (Diperbaiki untuk Pengecekan Izin) ---
@Composable
fun EquipmentItem(
    alat: Equipment,
    onConfirm: (Int, Long, Long) -> Unit,
    minRowHeight: Dp = 72.dp,
    buttonWidth: Dp = 110.dp,
    // TAMBAHKAN CALLBACK BARU UNTUK KLIK AWAL
    onInitialPinjamClick: () -> Unit = {}
) {
    var showInitialConfirm by remember { mutableStateOf(false) }
    var showQtyDateDialog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .heightIn(min = minRowHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = alat.icon),
                contentDescription = alat.nama,
                tint = Color(0xFFFF7A00),
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    alat.nama,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Stok tersedia: ${alat.stokTersedia} / ${alat.stokTotal}",
                    color = Color(0xFF00A85A),
                    fontSize = 13.sp
                )
            }
            Button(
                // PERBAIKAN: Panggil callback pengecekan izin sebelum menampilkan dialog
                onClick = {
                    if (alat.stokTersedia > 0) {
                        onInitialPinjamClick()
                        showInitialConfirm = true
                    }
                },
                enabled = alat.stokTersedia > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (alat.stokTersedia > 0) Color(0xFFFF7A00) else Color(0xFFE0E6ED),
                    contentColor = if (alat.stokTersedia > 0) Color.White else Color.Gray
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .width(buttonWidth)
                    .height(44.dp)
            ) { Text("Pinjam", fontWeight = FontWeight.SemiBold) }
        }
    }

    // Dialog 1: Konfirmasi awal
    if (showInitialConfirm) {
        AlertDialog(
            onDismissRequest = { showInitialConfirm = false },
            title = { Text("Konfirmasi Peminjaman", fontWeight = FontWeight.Bold) },
            text = { Text("Apakah kamu yakin ingin meminjam ${alat.nama}?") },
            confirmButton = {
                Button(onClick = {
                    showInitialConfirm = false
                    showQtyDateDialog = true
                }) { Text("Ya") }
            },
            dismissButton = {
                TextButton(onClick = { showInitialConfirm = false }) { Text("Batal") }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Dialog Kuantitas dan Waktu
    if (showQtyDateDialog) {
        var qty by remember { mutableStateOf(1) }
        val maxQty = alat.stokTersedia
        val now = System.currentTimeMillis()
        var tanggalMulai by remember { mutableStateOf(now) }
        var tanggalSelesai by remember { mutableStateOf(now + 60 * 60 * 1000) }

        AlertDialog(
            onDismissRequest = { showQtyDateDialog = false },
            title = { Text("Pilih Jumlah & Periode", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Pilih jumlah dan periode pinjam untuk ${alat.nama}", color = Color(0xFF4E4E4E))
                    Spacer(Modifier.height(16.dp))

                    // Input Jumlah
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { if (qty > 1) qty-- },
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier.defaultMinSize(minWidth = 36.dp, minHeight = 36.dp)
                        ) { Text("-") }

                        Text("$qty", fontSize = 18.sp, fontWeight = FontWeight.Bold)

                        OutlinedButton(
                            onClick = { if (qty < maxQty) qty++ },
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier.defaultMinSize(minWidth = 36.dp, minHeight = 36.dp)
                        ) { Text("+") }

                        Spacer(Modifier.weight(1f))
                        Text("Maks: $maxQty", fontSize = 12.sp, color = Color.Gray)
                    }
                    Spacer(Modifier.height(16.dp))

                    DateTimePickerButton(
                        label = "Mulai Pinjam",
                        initialDateTime = tanggalMulai,
                        onDateTimeSelected = { newDateTime -> tanggalMulai = newDateTime }
                    )
                    Spacer(Modifier.height(8.dp))

                    DateTimePickerButton(
                        label = "Selesai Pinjam",
                        initialDateTime = tanggalSelesai,
                        onDateTimeSelected = { newDateTime -> tanggalSelesai = newDateTime }
                    )

                    if (tanggalMulai > tanggalSelesai) {
                        Text("Tanggal Selesai harus setelah Tanggal Mulai.", color = Color.Red, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tanggalMulai <= tanggalSelesai) {
                            showQtyDateDialog = false
                            onConfirm(qty, tanggalMulai, tanggalSelesai)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7A00)),
                    enabled = tanggalMulai <= tanggalSelesai
                ) {
                    Text("Pinjam $qty", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showQtyDateDialog = false }) { Text("Batal") } },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}