package com.example.ukonnect2.ui.screen

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.app.NotificationManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import com.example.ukonnect2.R
import com.example.ukonnect2.network.AktivitasDto
import com.example.ukonnect2.network.AktivitasUpsertRequest
import com.example.ukonnect2.network.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ==========================================================
// === DATA CLASS & ENUM ====================================
// ==========================================================

enum class AktivitasStatus { UPCOMING, COMPLETED, MISSED }

data class JadwalItem(
    val id: Int,
    val dayShort: String,
    val dateNumber: Int,
    val title: String,
    val timeString: String,
    val startTime: Date,
    val endTime: Date,
    val iconResId: Int,
    val status: AktivitasStatus = AktivitasStatus.UPCOMING
)

private const val CHANNEL_ID_AKTIVITAS = "aktivitas_reminder_channel"

// ==========================================================
// === FUNGSI PEMBANTU ======================================
// ==========================================================

fun formatDateForJadwal(timestamp: Long): Pair<String, Int> {
    val date = Date(timestamp)
    val localeId = Locale.forLanguageTag("in-ID") // ✅ pengganti yang tidak deprecated
    val dayFormat = SimpleDateFormat("EEE", localeId)
    val dayShort = dayFormat.format(date).uppercase(Locale.ROOT).replace(".", "")
    val dateFormat = SimpleDateFormat("d", Locale.ROOT)
    return Pair(dayShort, dateFormat.format(date).toIntOrNull() ?: 0)
}

fun isToday(timestamp: Long): Boolean {
    val now = Calendar.getInstance()
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    return now.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
}

private fun jadwalStatusToDb(status: AktivitasStatus): String = status.name

private fun dbStatusToJadwal(status: String?): AktivitasStatus {
    return try {
        if (status.isNullOrBlank()) AktivitasStatus.UPCOMING else AktivitasStatus.valueOf(status)
    } catch (_: IllegalArgumentException) {
        AktivitasStatus.UPCOMING
    }
}

private fun formatTime(date: Date): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
}

private fun dtoToJadwalItem(dto: AktivitasDto): JadwalItem {
    val (d, n) = formatDateForJadwal(dto.startTimeMillis)
    val start = Date(dto.startTimeMillis)
    val end = Date(dto.endTimeMillis)
    return JadwalItem(
        id = dto.id,
        dayShort = d,
        dateNumber = n,
        title = dto.title,
        timeString = "${formatTime(start)} - ${formatTime(end)} WIB",
        startTime = start,
        endTime = end,
        iconResId = R.drawable.ic_back,
        status = dbStatusToJadwal(dto.status)
    )
}

// ==========================================================
// === KOMPONEN UTAMA =======================================
// ==========================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AktivitasScreen() {
    val context = LocalContext.current
    val orange = Color(0xFFFFA726)
    val redWarning = Color(0xFFFF5722)
    val greenSuccess = Color(0xFF4CAF50)

    val scope = rememberCoroutineScope()
    val api = remember { RetrofitClient.instance }

    var showDialog by remember { mutableStateOf(false) }
    var selectedJadwalIndex by remember { mutableStateOf<Int?>(null) }
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    var startTimeStr by remember { mutableStateOf<String?>(null) }
    var endTimeStr by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val sampleJadwal = remember { mutableStateListOf<JadwalItem>() }

    suspend fun refreshAktivitas() {
        try {
            val items = api.getAktivitas()
            sampleJadwal.clear()
            sampleJadwal.addAll(items.map { dtoToJadwalItem(it) })
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal memuat aktivitas: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        refreshAktivitas()
    }

    // ✅ Loop pengecekan status otomatis
    LaunchedEffect(Unit) {
        while (true) {
            delay(10000)
            val now = Date()
            sampleJadwal.forEachIndexed { i, item ->
                if (item.status == AktivitasStatus.UPCOMING && now.after(item.endTime)) {
                    val updated = item.copy(status = AktivitasStatus.MISSED)
                    scope.launch {
                        try {
                            api.updateAktivitas(
                                updated.id,
                                AktivitasUpsertRequest(
                                    title = updated.title,
                                    startTimeMillis = updated.startTime.time,
                                    endTimeMillis = updated.endTime.time,
                                    status = jadwalStatusToDb(updated.status)
                                )
                            )
                        } catch (_: Exception) {
                        }
                    }

                    val notificationManager =
                        context.getSystemService(NotificationManager::class.java)

                    val notification = NotificationCompat.Builder(context, CHANNEL_ID_AKTIVITAS)
                        .setSmallIcon(R.drawable.ic_calendar)
                        .setContentTitle("Aktivitas terlewat")
                        .setContentText("${item.title} terlewatkan. Jangan lupa atur ulang jadwalmu.")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .build()

                    notificationManager.notify(item.id + 2000000, notification)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Aktivitas",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                FloatingActionButton(
                    onClick = {
                        selectedJadwalIndex = null
                        title = ""
                        selectedDate = null
                        startTimeStr = null
                        endTimeStr = null
                        showDialog = true
                    },
                    containerColor = orange,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah", tint = Color.White)
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            AktivitasListContent(
                sampleJadwal,
                selectedTabIndex,
                { selectedTabIndex = it },
                { item, idx ->
                    selectedJadwalIndex = idx
                    title = item.title
                    selectedDate = item.startTime.time
                    val times = item.timeString.replace(" WIB", "").split(" - ")
                    startTimeStr = times.getOrNull(0)?.trim()
                    endTimeStr = times.getOrNull(1)?.trim()
                    showDialog = true
                },
                {
                    val id = sampleJadwal[it].id
                    // FCM handles scheduling automatically
                    scope.launch {
                        try {
                            api.deleteAktivitas(id)
                            refreshAktivitas()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Gagal hapus aktivitas: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                {
                    val item = sampleJadwal[it]
                    val updated = item.copy(status = AktivitasStatus.COMPLETED)
                    // FCM handles scheduling automatically
                    scope.launch {
                        try {
                            api.updateAktivitas(
                                updated.id,
                                AktivitasUpsertRequest(
                                    title = updated.title,
                                    startTimeMillis = updated.startTime.time,
                                    endTimeMillis = updated.endTime.time,
                                    status = jadwalStatusToDb(updated.status)
                                )
                            )
                            refreshAktivitas()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Gagal update status: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                Triple(orange, greenSuccess, redWarning)
            )
        }
    }

    // ==========================================================
    // === DIALOG INPUT / EDIT JADWAL ===========================
    // ==========================================================
    if (showDialog) {
        val isEditing = selectedJadwalIndex != null
        val calendarNow = Calendar.getInstance()

        val datePicker = DatePickerDialog(
            context,
            { _, y, m, d ->
                calendarNow.set(y, m, d)
                selectedDate = calendarNow.timeInMillis
                startTimeStr = null; endTimeStr = null
            },
            calendarNow.get(Calendar.YEAR),
            calendarNow.get(Calendar.MONTH),
            calendarNow.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.datePicker.minDate = System.currentTimeMillis() - 1000

        val timePickerStart = TimePickerDialog(
            context,
            { _, h, m ->
                val now = Calendar.getInstance()
                val selectedDay = Calendar.getInstance().apply { timeInMillis = selectedDate!! }
                val isToday = now.get(Calendar.YEAR) == selectedDay.get(Calendar.YEAR) &&
                        now.get(Calendar.DAY_OF_YEAR) == selectedDay.get(Calendar.DAY_OF_YEAR)

                if (isToday) {
                    val currentHour = now.get(Calendar.HOUR_OF_DAY)
                    val currentMinute = now.get(Calendar.MINUTE)
                    if (h < currentHour || (h == currentHour && m < currentMinute)) {
                        Toast.makeText(context, "Waktu mulai tidak bisa lampau", Toast.LENGTH_LONG).show()
                        return@TimePickerDialog
                    }
                }
                startTimeStr = String.format("%02d:%02d", h, m)
                endTimeStr = null
            },
            calendarNow.get(Calendar.HOUR_OF_DAY),
            calendarNow.get(Calendar.MINUTE),
            true
        )

        val timePickerEnd = TimePickerDialog(
            context,
            { _, h, m ->
                if (startTimeStr != null) {
                    val startParts = startTimeStr!!.split(":")
                    val startH = startParts.getOrNull(0)?.toIntOrNull()
                    val startM = startParts.getOrNull(1)?.toIntOrNull()
                    if (startH != null && startM != null) {
                        if (h == startH && m == startM) {
                            Toast.makeText(context, "Waktu selesai harus setelah waktu mulai", Toast.LENGTH_LONG).show()
                            return@TimePickerDialog
                        }
                    }
                }

                endTimeStr = String.format("%02d:%02d", h, m)
            },
            calendarNow.get(Calendar.HOUR_OF_DAY) + 1,
            0,
            true
        )

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (isEditing) "Ubah Jadwal" else "Jadwal Baru") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Nama Aktivitas") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = { datePicker.show() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            selectedDate?.let { val (d, n) = formatDateForJadwal(it); "$d, $n" } ?: "Pilih Tanggal",
                            color = orange
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { timePickerStart.show() },
                            modifier = Modifier.weight(1f),
                            enabled = selectedDate != null
                        ) {
                            Text(startTimeStr ?: "Mulai", color = if (selectedDate != null) orange else Color.Gray)
                        }
                        OutlinedButton(
                            onClick = { timePickerEnd.show() },
                            modifier = Modifier.weight(1f),
                            enabled = startTimeStr != null
                        ) {
                            Text(endTimeStr ?: "Selesai", color = if (startTimeStr != null) orange else Color.Gray)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedDate != null && startTimeStr != null && endTimeStr != null && title.isNotBlank()) {
                            val baseCal = Calendar.getInstance().apply { timeInMillis = selectedDate!! }
                            val startParts = startTimeStr!!.split(":")
                            val startCal = (baseCal.clone() as Calendar).apply {
                                set(Calendar.HOUR_OF_DAY, startParts[0].toInt())
                                set(Calendar.MINUTE, startParts[1].toInt())
                            }
                            val endParts = endTimeStr!!.split(":")
                            val endCal = (baseCal.clone() as Calendar).apply {
                                set(Calendar.HOUR_OF_DAY, endParts[0].toInt())
                                set(Calendar.MINUTE, endParts[1].toInt())
                            }

                            if (endCal.before(startCal)) {
                                endCal.add(Calendar.DAY_OF_YEAR, 1)
                            }

                            val nowCal = Calendar.getInstance()
                            val selectedDay = Calendar.getInstance().apply { timeInMillis = selectedDate!! }
                            val isSelectedToday = nowCal.get(Calendar.YEAR) == selectedDay.get(Calendar.YEAR) &&
                                    nowCal.get(Calendar.DAY_OF_YEAR) == selectedDay.get(Calendar.DAY_OF_YEAR)

                            if (isSelectedToday && startCal.timeInMillis <= System.currentTimeMillis()) {
                                Toast.makeText(context, "Waktu mulai harus setelah waktu sekarang", Toast.LENGTH_LONG).show()
                                return@Button
                            }

                            if (!endCal.after(startCal)) {
                                Toast.makeText(context, "Waktu selesai harus setelah waktu mulai", Toast.LENGTH_LONG).show()
                                return@Button
                            }

                            val nowDate = Date()
                            val status = if (nowDate.after(endCal.time)) AktivitasStatus.MISSED else AktivitasStatus.UPCOMING
                            val (d, n) = formatDateForJadwal(selectedDate!!)

                            if (isEditing) {
                                // FCM handles scheduling automatically
                            }

                            scope.launch {
                                try {
                                    val saved = if (isEditing) {
                                        val id = sampleJadwal[selectedJadwalIndex!!].id
                                        api.updateAktivitas(
                                            id,
                                            AktivitasUpsertRequest(
                                                title = title,
                                                startTimeMillis = startCal.timeInMillis,
                                                endTimeMillis = endCal.timeInMillis,
                                                status = jadwalStatusToDb(status)
                                            )
                                        )
                                    } else {
                                        api.createAktivitas(
                                            AktivitasUpsertRequest(
                                                title = title,
                                                startTimeMillis = startCal.timeInMillis,
                                                endTimeMillis = endCal.timeInMillis,
                                                status = jadwalStatusToDb(status)
                                            )
                                        )
                                    }

                                    // FCM handles scheduling automatically
                                    refreshAktivitas()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Gagal simpan aktivitas: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }

                            selectedTabIndex = if (status == AktivitasStatus.UPCOMING) 0 else 1
                            showDialog = false

                            if (isToday(startCal.timeInMillis)) {
                                val notificationManager =
                                    context.getSystemService(NotificationManager::class.java)

                                val notification = NotificationCompat.Builder(context, CHANNEL_ID_AKTIVITAS)
                                    .setSmallIcon(R.drawable.ic_calendar)
                                    .setContentTitle("Aktivitas mulai hari ini")
                                    .setContentText("$title akan dimulai hari ini. Ayo lakukan aktivitas Anda.")
                                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                                    .setAutoCancel(true)
                                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                                    .build()

                                val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt().let { if (it == 0) 1 else it }
                                notificationManager.notify(notificationId, notification)
                            }
                        }
                    },
                    enabled = title.isNotBlank() && endTimeStr != null,
                    colors = ButtonDefaults.buttonColors(containerColor = orange)
                ) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Batal", color = Color.Gray)
                }
            }
        )
    }
}

// ==========================================================
// === LIST KONTEN ==========================================
// ==========================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AktivitasListContent(
    sampleJadwal: List<JadwalItem>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    onEdit: (JadwalItem, Int) -> Unit,
    onDelete: (Int) -> Unit,
    onComplete: (Int) -> Unit,
    colors: Triple<Color, Color, Color>
) {
    val (orange, green, red) = colors
    var searchText by remember { mutableStateOf("") }

    val filtered by remember(sampleJadwal, selectedTabIndex, searchText) {
        derivedStateOf {
            sampleJadwal.filter {
                (if (selectedTabIndex == 0) it.status == AktivitasStatus.UPCOMING else it.status != AktivitasStatus.UPCOMING) &&
                        it.title.contains(searchText, true)
            }.sortedBy { it.startTime }
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        TabRow(
            selectedTabIndex,
            indicator = {
                TabRowDefaults.SecondaryIndicator( // ✅ pengganti non-deprecated
                    Modifier.tabIndicatorOffset(it[selectedTabIndex]),
                    color = orange
                )
            }
        ) {
            listOf("Mendatang", "Riwayat").forEachIndexed { i, t ->
                Tab(
                    selected = selectedTabIndex == i,
                    onClick = { onTabSelected(i) },
                    text = { Text(t, color = if (selectedTabIndex == i) orange else Color.Gray) }
                )
            }
        }

        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = { Text("Cari...") },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = orange,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(filtered) { _, item ->
                AktivitasItemCard(
                    item,
                    selectedTabIndex == 1,
                    green,
                    red,
                    { onEdit(item, sampleJadwal.indexOf(item)) },
                    { onDelete(sampleJadwal.indexOf(item)) },
                    { onComplete(sampleJadwal.indexOf(item)) }
                )
            }
        }
    }
}

@Composable
fun AktivitasItemCard(
    item: JadwalItem,
    isHistory: Boolean,
    green: Color,
    red: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onComplete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(48.dp)) {
                Text(item.dayShort, color = Color.Gray, fontWeight = FontWeight.Bold)
                Text("${item.dateNumber}", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.timeString, fontSize = 12.sp, color = Color.Gray)
            }

            if (isHistory) {
                val (label, color) = if (item.status == AktivitasStatus.COMPLETED)
                    "Selesai" to green else "Terlewat" to red
                Surface(color = color.copy(0.1f), shape = RoundedCornerShape(50)) {
                    Text(
                        label,
                        color = color,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(6.dp, 2.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, null, tint = Color.Gray)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Selesai") },
                            onClick = { onComplete(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.CheckCircle, null, tint = green) }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = { onEdit(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Edit, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Hapus") },
                            onClick = { onDelete(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = red) }
                        )
                    }
                }
            }
        }
    }
}
