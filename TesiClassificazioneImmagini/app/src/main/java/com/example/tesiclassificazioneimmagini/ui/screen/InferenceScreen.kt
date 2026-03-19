@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.tesiclassificazioneimmagini.ui.screen

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tesiclassificazioneimmagini.ui.component.BitmapImage
import com.example.tesiclassificazioneimmagini.ui.component.SliderConRange
import com.example.tesiclassificazioneimmagini.ui.component.MseSlider
import com.example.tesiclassificazioneimmagini.viewmodel.YoloViewModel
import com.example.tesiclassificazioneimmagini.viewmodel.AnomalyViewModel
import com.example.tesiclassificazioneimmagini.yolo_v5.Detection
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import com.example.tesiclassificazioneimmagini.yolo_v5.COCOLabels
import java.util.Locale

private const val TAG = "InferenceScreen"

@Composable
fun InferenceScreen() {
    val viewModel: YoloViewModel = viewModel()
    val anomalyViewModel: AnomalyViewModel = viewModel()
    val context = LocalContext.current
    val activity = context as? Activity

    var hasPermissions by remember { mutableStateOf(false) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var isAnomalyModelReady by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    var detectedObjects by remember { mutableStateOf<List<Detection>>(emptyList()) }

    // Flag per popup statistiche
    var showYoloStatsDialog by remember { mutableStateOf(false) }
    var showAnomalyStatsDialog by remember { mutableStateOf(false) }

    // Toggle heatmap
    var showHeatmap by remember { mutableStateOf(false) }
    var showReconstruction by remember { mutableStateOf(false) }

    val readPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        android.Manifest.permission.READ_MEDIA_IMAGES
    } else {
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.d(TAG, "Risultato dalla galleria ricevuto.")
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            uri?.let {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                viewModel.selectedBitmap = bitmap
                anomalyViewModel.selectedBitmap = bitmap
                anomalyViewModel.clearVisuals()
                showHeatmap = false
                showReconstruction = false
                inputStream?.close()

                detectedObjects = emptyList()

                Log.d(TAG, "Immagine caricata con successo dalla galleria.")
            } ?: Log.e(TAG, "URI dell'immagine è nullo.")
        } else {
            Log.d(TAG, "Selezione immagine annullata.")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d(TAG, "Risultato dei permessi ricevuto.")
        hasPermissions = permissions.values.all { it }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        Log.d(TAG, "Risultato dalla fotocamera ricevuto.")
        if (success && photoUri != null) {
            val inputStream = context.contentResolver.openInputStream(photoUri!!)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            viewModel.selectedBitmap = bitmap
            anomalyViewModel.selectedBitmap = bitmap
            anomalyViewModel.clearVisuals()
            showHeatmap = false
            showReconstruction = false
            inputStream?.close()

            detectedObjects = emptyList()

            Log.d(TAG, "Immagine catturata dalla fotocamera e caricata con successo.")
        } else {
            Log.e(TAG, "Cattura foto fallita o URI nullo.")
        }
    }

    LaunchedEffect(Unit) {
        Log.d(TAG, "Avvio del LaunchedEffect. Tentativo di caricare i modelli.")
        try {
            viewModel.loadModel(context.assets)
            Log.d(TAG, "Modello YOLO caricato con successo.")
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel caricamento del modello YOLO: ${e.message}")
            Toast.makeText(context, "Errore nel caricamento del modello YOLO", Toast.LENGTH_LONG).show()
        }
        try {
            anomalyViewModel.loadModel(context)
            isAnomalyModelReady = true
            Log.d(TAG, "Modello di anomalia caricato con successo.")
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel caricamento del modello di anomalia: ${e.message}")
            Toast.makeText(context, "Errore nel caricamento del modello di anomalia", Toast.LENGTH_LONG).show()
            isAnomalyModelReady = false
        }
        val cameraGranted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val readGranted = ContextCompat.checkSelfPermission(context, readPermission) == PackageManager.PERMISSION_GRANTED
        hasPermissions = cameraGranted && readGranted
        Log.d(TAG, "Stato permessi: Camera=$cameraGranted, ReadStorage=$readGranted. hasPermissions=$hasPermissions")
    }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Inferenza Modello",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                actions = {
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "Info",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            Spacer(modifier = Modifier.height(12.dp))

            // Azioni immagine
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        Log.d(TAG, "Pulsante 'Scatta foto' cliccato. Controllo permessi...")
                        if (!hasPermissions) {
                            viewModel.checkInference = false
                            permissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.CAMERA,
                                    readPermission
                                )
                            )
                            Log.d(TAG, "Permessi non concessi, lancio la richiesta di permessi.")
                        } else {
                            photoUri = createImageUriByCamera(context)
                            photoUri?.let { uri -> cameraLauncher.launch(uri) }
                            Log.d(TAG, "Permessi concessi, lancio la fotocamera.")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Scatta foto", color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = {
                        Log.d(TAG, "Pulsante 'Trova foto' cliccato.")
                        activity?.let {
                            selectImageFromGallery(launcher)
                            viewModel.checkInference = false
                        } ?: Log.e(TAG, "Attività non disponibile.")
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Trova foto", color = MaterialTheme.colorScheme.onSecondary,
                        style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sezione YOLO
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Classificazione Immagini", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    SliderConRange(threshold = viewModel.threshold, onValueChange = { viewModel.threshold = it })
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                Log.d(TAG, "Pulsante 'Inferenza' cliccato.")
                                if (viewModel.selectedBitmap == null || viewModel.checkInference) {
                                    Log.e(TAG, "Inferenza non eseguita. Bitmap nullo o inferenza già in corso.")
                                    return@Button
                                }
                                Toast.makeText(context, "Inferenza in corso...", Toast.LENGTH_SHORT).show()
                                viewModel.runInference {
                                    detectedObjects = viewModel.detectedObjects
                                    // Mostra popup statistiche YOLO al termine
                                    showYoloStatsDialog = true
                                }
                                Log.d(TAG, "Iniziata l'inferenza del modello YOLO.")
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Classifica", color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelSmall)
                        }

                        Button(
                            onClick = {
                                Log.d(TAG, "Pulsante 'Salva immagine' cliccato.")
                                viewModel.selectedBitmap?.let {
                                    viewModel.saveImage(context, it)
                                    Log.d(TAG, "Salvataggio dell'immagine avviato.")
                                } ?: run {
                                    Log.e(TAG, "Nessuna immagine da salvare.")
                                    Toast.makeText(context, "Nessuna immagine da salvare", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Salva immagine", color = MaterialTheme.colorScheme.onSecondary,
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sezione Anomaly Detection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Rilevamento di Anomalie", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    MseSlider(
                        mse = anomalyViewModel.anomalyThreshold,
                        onValueChange = { anomalyViewModel.anomalyThreshold = it }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                Log.d(TAG, "Pulsante 'Detection anomalie' cliccato.")
                                val bmp = viewModel.selectedBitmap
                                if (bmp == null) {
                                    Log.e(TAG, "Nessuna immagine selezionata per la detection anomalie.")
                                    Toast.makeText(context, "Seleziona un'immagine prima", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                anomalyViewModel.selectedBitmap = bmp
                                Toast.makeText(context, "Rilevamento anomalie...", Toast.LENGTH_SHORT).show()

                                Log.d(TAG, "Avviato il rilevamento delle anomalie.")
                                anomalyViewModel.runInference { isAnomaly ->
                                    val msg = when (isAnomaly) {
                                        null -> "Errore rilevamento: ${anomalyViewModel.lastError ?: "sconosciuto"}"
                                        true -> "Anomalia rilevata (MSE: ${"%.6f".format(anomalyViewModel.mseValue)})"
                                        false -> "Nessuna anomalia (MSE: ${"%.6f".format(anomalyViewModel.mseValue)})"
                                    }
                                    Log.i(TAG, "Risultato rilevamento anomalie: $msg")
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    // Mostra popup statistiche anomalia al termine
                                    showAnomalyStatsDialog = true
                                }

                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = isAnomalyModelReady,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Detection anomalie", color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // Opzioni visualizzazione heatmap/ricostruzione
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Mostra Heatmap", modifier = Modifier.weight(1f))
                            Switch(
                                checked = showHeatmap,
                                onCheckedChange = { showHeatmap = it }
                            )
                        }
                        if (anomalyViewModel.reconstructedBitmap != null) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text("Mostra Ricostruzione", modifier = Modifier.weight(1f))
                                Switch(
                                    checked = showReconstruction,
                                    onCheckedChange = { showReconstruction = it }
                                )
                            }
                        }
                        if (anomalyViewModel.heatmapBitmap != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { anomalyViewModel.saveHeatmap(context); Toast.makeText(context, "Heatmap salvata", Toast.LENGTH_SHORT).show() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Text("Salva Heatmap", color = MaterialTheme.colorScheme.onSecondary,
                                        style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selezione bitmap da mostrare
            val bitmapToShow = when {
                showHeatmap && anomalyViewModel.heatmapBitmap != null -> anomalyViewModel.heatmapBitmap
                showReconstruction && anomalyViewModel.reconstructedBitmap != null -> anomalyViewModel.reconstructedBitmap
                else -> viewModel.selectedBitmap
            }
            BitmapImage(bitmap = bitmapToShow)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("OK")
                }
            },
            title = { Text("Inferenza sul Modello") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Benvenuti nella schermata di inferenza sul modello .tflite",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Andrea Passini 0001071060",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        )
    }

    // Popup Statistiche YOLO
    if (showYoloStatsDialog) {
        AlertDialog(
            onDismissRequest = { showYoloStatsDialog = false },
            confirmButton = {
                TextButton(onClick = { showYoloStatsDialog = false }) {
                    Text("OK")
                }
            },
            title = { Text("Statistiche - Classifica") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (detectedObjects.isNotEmpty()) {
                        Text("Oggetti rilevati (ord. confidenza):", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        detectedObjects
                            .sortedByDescending { it.objectness }
                            .forEach { detection ->
                                val confidence = String.format(Locale.US, "%.2f", detection.objectness * 100)
                                val className = COCOLabels.getLabel(detection.classId)
                                Text(text = "- $className: $confidence%", style = MaterialTheme.typography.bodyLarge)
                            }
                    } else {
                        Text("Nessun oggetto rilevato.", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        )
    }

    // Popup Statistiche Anomalia
    if (showAnomalyStatsDialog) {
        val anomalyColor = when (anomalyViewModel.isAnomaly) {
            true -> ComposeColor.Red
            false -> ComposeColor.Green
            null -> ComposeColor.Gray
        }
        val anomalyText = when (anomalyViewModel.isAnomaly) {
            true -> "Anomalia rilevata"
            false -> "Nessuna anomalia"
            null -> "In attesa..."
        }

        AlertDialog(
            onDismissRequest = { showAnomalyStatsDialog = false },
            confirmButton = {
                TextButton(onClick = { showAnomalyStatsDialog = false }) { Text("OK") }
            },
            title = { Text("Statistiche - Stato Anomalia") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Card(colors = CardDefaults.cardColors(containerColor = anomalyColor), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = anomalyText, style = MaterialTheme.typography.titleMedium, color = ComposeColor.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Livello MSE: ${"%.6f".format(anomalyViewModel.mseValue)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ComposeColor.White
                            )
                        }
                    }
                }
            }
        )
    }
}

// Metodo per selezionare un'immagine dalla galleria
fun selectImageFromGallery(launcher: ActivityResultLauncher<Intent>) {
    val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
    launcher.launch(intent)
}

// Crea un URI per salvare l'immagine catturata dalla fotocamera
fun createImageUriByCamera(context: Context): Uri? {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.TITLE, "New Picture")
        put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
    }
    return context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
}
