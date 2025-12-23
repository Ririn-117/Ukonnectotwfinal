package com.example.ukonnect2.ui.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.DatePicker
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack // Import Icon Back
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.ukonnect2.ui.viewmodel.FotoItem
import com.example.ukonnect2.ui.viewmodel.GaleriViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GaleriScreen(
    viewModel: GaleriViewModel = viewModel(),
    onBack: () -> Unit = {} // Parameter tambahan untuk aksi Back
) {
    var showDialog by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<FotoItem?>(null) }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    // 1. State untuk menyimpan URI sementara saat menggunakan Kamera
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // 2. Launcher untuk Galeri
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) viewModel.selectedImageUri.value = uri
    }

    // 3. Launcher untuk Kamera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoUri != null) {
            viewModel.selectedImageUri.value = tempPhotoUri
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .background(Color(0xFFFF9800))
                    .statusBarsPadding(), // Tambahan agar tidak tertutup status bar (opsional)
            ) {
                // Tombol Back (Kiri)
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White
                    )
                }

                // Judul (Tengah)
                Text(
                    text = "Galeri Kegiatan",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editItem = null
                    showDialog = true
                },
                containerColor = Color(0xFFFF9800)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Foto", tint = Color.White)
            }
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (viewModel.galeriList.isEmpty()) {
                Text(
                    text = "Belum ada foto ditambahkan",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(viewModel.galeriList) { foto ->
                        FotoCard(
                            foto = foto,
                            onEdit = { editItem = foto; showDialog = true },
                            onDelete = { viewModel.deleteFoto(foto.id) }
                        )

                    }
                }
            }
        }
    }

    if (showDialog) {
        DialogTambahFoto(
            onDismiss = { 
                showDialog = false
                viewModel.clearSelectedImage()
            },
            onSave = { uri, ket, tgl, hari ->
                if (editItem == null) {
                    viewModel.addFoto(context, uri, ket, tgl, hari)
                } else {
                    viewModel.updateFoto(context, editItem!!, uri, ket, tgl, hari)
                }
                showDialog = false
            },
            onGalleryClick = { galleryLauncher.launch("image/*") },
            onCameraClick = {
                if (!hasCameraPermission) {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    return@DialogTambahFoto
                }

                try {
                    // Buat file temp dan URI sebelum launch kamera
                    val uri = createTempPictureUri(context)
                    tempPhotoUri = uri
                    cameraLauncher.launch(uri)
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal membuka kamera", Toast.LENGTH_LONG).show()
                }
            },
            selectedImageUri = viewModel.selectedImageUri.value,
            editData = editItem
        )
    }
}

@Composable
fun FotoCard(foto: FotoItem, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showPreview by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clickable { showPreview = true },
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .background(Color.White)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = rememberAsyncImagePainter(foto.uri),
                contentDescription = "Foto",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(6.dp))

            Text(
                text = foto.keterangan,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "${foto.hari}, ${foto.tanggal}",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showPreview) {
        AlertDialog(
            onDismissRequest = { showPreview = false },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = {
                        onEdit()
                        showPreview = false
                    }) {
                        Text("Edit", color = Color(0xFFFF9800))
                    }
                    TextButton(onClick = {
                        onDelete()
                        showPreview = false
                    }) {
                        Text("Hapus", color = Color.Red)
                    }
                    TextButton(onClick = { showPreview = false }) {
                        Text("Tutup")
                    }
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = rememberAsyncImagePainter(foto.uri),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(foto.keterangan, fontWeight = FontWeight.SemiBold)
                    Text("${foto.hari}, ${foto.tanggal}", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun DialogTambahFoto(
    onDismiss: () -> Unit,
    onSave: (Uri, String, String, String) -> Unit,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
    selectedImageUri: Uri?,
    editData: FotoItem? = null
) {
    var keterangan by remember { mutableStateOf(editData?.keterangan ?: "") }
    var tanggal by remember { mutableStateOf(editData?.tanggal ?: "") }
    var hari by remember { mutableStateOf(editData?.hari ?: "") }
    var showPreview by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val dayFormat = SimpleDateFormat("EEEE", Locale("id", "ID"))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editData == null) "Tambah Foto" else "Edit Foto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // PILIH SUMBER FOTO: GALERI ATAU KAMERA
                Text("Pilih Sumber Foto:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tombol Galeri (Menggunakan Icon List standar)
                    OutlinedButton(
                        onClick = onGalleryClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Galeri", fontSize = 12.sp)
                    }

                    // Tombol Kamera (Menggunakan Icon Add standar)
                    OutlinedButton(
                        onClick = onCameraClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Kamera", fontSize = 12.sp)
                    }
                }

                // Tampilkan gambar: prioritaskan selectedImageUri (baru), fallback ke editData (lama)
                val displayUri = selectedImageUri ?: editData?.uri
                displayUri?.let {
                    Image(
                        painter = rememberAsyncImagePainter(it),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showPreview = true },
                        contentScale = ContentScale.Crop
                    )
                }

                OutlinedTextField(
                    value = keterangan,
                    onValueChange = { keterangan = it },
                    label = { Text("Keterangan") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(onClick = {
                    val picker = android.app.DatePickerDialog(
                        context,
                        { _: DatePicker, year, month, dayOfMonth ->
                            val cal = Calendar.getInstance()
                            cal.set(year, month, dayOfMonth)
                            val chosen = dateFormat.format(cal.time)
                            tanggal = chosen
                            hari = dayFormat.format(cal.time)
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    )
                    picker.datePicker.minDate = System.currentTimeMillis() - 1000
                    picker.show()
                }) {
                    Text(if (tanggal.isEmpty()) "Pilih Tanggal" else "$hari, $tanggal")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val uriToSave = selectedImageUri ?: editData?.uri
                if (uriToSave != null && keterangan.isNotEmpty() && tanggal.isNotEmpty()) {
                    onSave(uriToSave, keterangan, tanggal, hari)
                }
            }) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

// Helper Function: Membuat file temporary dan URI untuk Kamera
fun createTempPictureUri(context: Context): Uri {
    val imagesDir = File(context.filesDir, "images").apply {
        if (!exists()) mkdirs()
    }

    val tempFile = File.createTempFile(
        "picture_${System.currentTimeMillis()}",
        ".jpg",
        imagesDir
    ).apply {
        createNewFile()
    }

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider", // Menggunakan packageName agar dinamis
        tempFile
    )
}