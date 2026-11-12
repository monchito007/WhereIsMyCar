package com.monchito007.whereismycar.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.monchito007.whereismycar.R
import com.monchito007.whereismycar.data.ParkingHistoryEntry
import com.monchito007.whereismycar.location.Geo
import com.monchito007.whereismycar.location.NavigationIntents
import java.io.File
import java.util.Date
import kotlin.math.roundToInt

private val locationPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

@Composable
fun ParkingScreen(
    onOpenAutoParkSettings: () -> Unit,
    viewModel: ParkingViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var cameraCommand by remember { mutableStateOf<CameraCommand?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showReplaceDialog by remember { mutableStateOf(false) }
    var pendingCameraFileName by rememberSaveable { mutableStateOf<String?>(null) }
    var fullScreenPhotoFile by remember { mutableStateOf<File?>(null) }
    var photoIdToDelete by remember { mutableStateOf<Long?>(null) }
    var showNoteDialog by rememberSaveable { mutableStateOf(false) }
    var showAddPhotoDialog by rememberSaveable { mutableStateOf(false) }
    var showMapStyleDialog by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showRestoreDialog by rememberSaveable { mutableStateOf(false) }
    var showClearHistoryDialog by rememberSaveable { mutableStateOf(false) }
    var historyEntryIdToDelete by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedHistoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selectedHistory = state.history.firstOrNull { it.id == selectedHistoryId }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success) {
            pendingCameraFileName?.let { viewModel.addPhoto(it) }
        }
        pendingCameraFileName = null
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let { sourceUri ->
            try {
                val photosDir = File(context.filesDir, "parking_photos")
                if (!photosDir.exists()) photosDir.mkdirs()
                val fileName = "photo_${System.currentTimeMillis()}.jpg"
                val dest = File(photosDir, fileName)
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    dest.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                viewModel.addPhoto(fileName)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            try {
                val photosDir = File(context.filesDir, "parking_photos")
                if (!photosDir.exists()) photosDir.mkdirs()
                val file = File(photosDir, "photo_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )
                pendingCameraFileName = file.name
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onPermissionChanged(granted)
    }

    fun hasPermission(): Boolean {
        return locationPermissions.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    LifecycleStartEffect(viewModel) {
        viewModel.onPermissionChanged(hasPermission())
        viewModel.onForegroundChanged(true)
        onStopOrDispose { viewModel.onForegroundChanged(false) }
    }

    LaunchedEffect(Unit) {
        val granted = hasPermission()
        viewModel.onPermissionChanged(granted)
        if (!granted) {
            permissionLauncher.launch(locationPermissions)
        }
    }

    val messageText = when (state.message) {
        ParkingViewModel.MESSAGE_NO_GPS -> stringResource(R.string.cannot_park)
        ParkingViewModel.MESSAGE_PARKED -> stringResource(R.string.parked_ok)
        ParkingViewModel.MESSAGE_DELETED -> stringResource(R.string.deleted_ok)
        ParkingViewModel.MESSAGE_NO_NAV -> stringResource(R.string.no_nav_app)
        else -> null
    }
    LaunchedEffect(state.message) {
        val text = messageText ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(text)
        viewModel.consumeMessage()
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.clearParking()
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showReplaceDialog) {
        AlertDialog(
            onDismissRequest = { showReplaceDialog = false },
            title = { Text(stringResource(R.string.replace_confirm_title)) },
            text = { Text(stringResource(R.string.replace_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showReplaceDialog = false
                    viewModel.saveCurrentParking()
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showReplaceDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showHistory) {
        HistorySheet(
            history = state.history,
            onDismiss = { showHistory = false },
            onSelect = { id ->
                showHistory = false
                selectedHistoryId = id
            },
            onDelete = { id -> historyEntryIdToDelete = id },
            onClearAll = { showClearHistoryDialog = true },
        )
    }

    historyEntryIdToDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { historyEntryIdToDelete = null },
            title = { Text(stringResource(R.string.delete_history_entry_title)) },
            text = { Text(stringResource(R.string.delete_history_entry_message)) },
            confirmButton = {
                TextButton(onClick = {
                    historyEntryIdToDelete = null
                    viewModel.deleteHistoryEntry(id)
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { historyEntryIdToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text(stringResource(R.string.clear_history_title)) },
            text = { Text(stringResource(R.string.clear_history_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearHistoryDialog = false
                    viewModel.clearHistory()
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    fun restoreFromHistory(entry: ParkingHistoryEntry) {
        selectedHistoryId = null
        viewModel.restoreFromHistory(entry)
    }

    if (showRestoreDialog && selectedHistory != null) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text(stringResource(R.string.restore_confirm_title)) },
            text = { Text(stringResource(R.string.restore_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreDialog = false
                    restoreFromHistory(selectedHistory)
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showMapStyleDialog) {
        MapStyleDialog(
            selected = state.mapStyle,
            onDismiss = { showMapStyleDialog = false },
            onSelect = { style ->
                showMapStyleDialog = false
                viewModel.setMapStyle(style)
            },
        )
    }

    fun addPhotoFromCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            try {
                val photosDir = File(context.filesDir, "parking_photos")
                if (!photosDir.exists()) photosDir.mkdirs()
                val file = File(photosDir, "photo_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )
                pendingCameraFileName = file.name
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (showAddPhotoDialog) {
        AlertDialog(
            onDismissRequest = { showAddPhotoDialog = false },
            title = { Text(stringResource(R.string.add_photo)) },
            text = { Text(stringResource(R.string.add_photo_source_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showAddPhotoDialog = false
                    addPhotoFromCamera()
                }) { Text(stringResource(R.string.camera)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddPhotoDialog = false
                    galleryLauncher.launch("image/*")
                }) { Text(stringResource(R.string.pick_gallery)) }
            },
        )
    }

    if (showNoteDialog) {
        NoteDialog(
            initialNote = state.parkedLocation?.note.orEmpty(),
            onDismiss = { showNoteDialog = false },
            onSave = { note ->
                showNoteDialog = false
                viewModel.updateNote(note)
            },
        )
    }

    photoIdToDelete?.let { photoId ->
        AlertDialog(
            onDismissRequest = { photoIdToDelete = null },
            title = { Text(stringResource(R.string.delete_photo_confirm_title)) },
            text = { Text(stringResource(R.string.delete_photo_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    photoIdToDelete = null
                    viewModel.deletePhoto(photoId)
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { photoIdToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (fullScreenPhotoFile != null) {
        AlertDialog(
            onDismissRequest = { fullScreenPhotoFile = null },
            confirmButton = {
                TextButton(onClick = { fullScreenPhotoFile = null }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                ) {
                    AsyncImage(
                        model = fullScreenPhotoFile,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { _ ->
        // La brújula del mapa se coloca justo debajo del último botón del lateral derecho.
        val density = LocalDensity.current
        var mapBounds by remember { mutableStateOf<Rect?>(null) }
        var sideButtonsBounds by remember { mutableStateOf<Rect?>(null) }
        val compassPosition = remember(mapBounds, sideButtonsBounds, density) {
            val map = mapBounds ?: return@remember null
            val buttons = sideButtonsBounds ?: return@remember null
            with(density) {
                CompassPosition(
                    topPx = (buttons.bottom - map.top + 8.dp.toPx()).roundToInt(),
                    // Todos los botones tienen el mismo ancho, así que el centro de la columna
                    // es el eje de los botones (incluye su zona táctil de 48 dp).
                    centerFromEndPx = (map.right - buttons.center.x).roundToInt(),
                )
            }
        }

        // Zona del mapa realmente visible: sin la barra de estado, la columna de botones de la
        // derecha ni las tarjetas de abajo. Los centrados de cámara se hacen dentro de ella.
        var bottomPanelBounds by remember { mutableStateOf<Rect?>(null) }
        val viewport = remember(mapBounds, sideButtonsBounds, bottomPanelBounds, density) {
            val map = mapBounds ?: return@remember MapViewport()
            val buttons = sideButtonsBounds
            val bottomPanel = bottomPanelBounds?.takeIf { it.height > 0f }
            with(density) {
                MapViewport(
                    leftPx = 0,
                    topPx = ((buttons?.top ?: map.top) - map.top).roundToInt().coerceAtLeast(0),
                    rightPx = (buttons?.let { map.right - it.left } ?: 0f).roundToInt().coerceAtLeast(0),
                    bottomPx = (bottomPanel?.let { map.bottom - it.top } ?: 0f).roundToInt().coerceAtLeast(0),
                    marginPx = 24.dp.roundToPx(),
                    markerHeightPx = CAR_ICON_SIZE_DP.dp.roundToPx(),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { mapBounds = it.boundsInRoot() },
        ) {
            ParkingMap(
                mapStyle = state.mapStyle,
                userLocation = state.userLocation,
                parkedLocation = state.parkedLocation,
                historyLocation = selectedHistory,
                cameraCommand = cameraCommand,
                onCameraCommandConsumed = { cameraCommand = null },
                modifier = Modifier.fillMaxSize(),
                compassPosition = compassPosition,
                viewport = viewport,
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = 16.dp, top = 8.dp)
                    .onGloballyPositioned { sideButtonsBounds = it.boundsInRoot() },
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box {
                    SmallFloatingActionButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.menu))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.history)) },
                            leadingIcon = { Icon(Icons.Filled.History, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showHistory = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.auto_park_title)) },
                            leadingIcon = { Icon(Icons.Filled.Bluetooth, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onOpenAutoParkSettings()
                            },
                        )
                    }
                }
                if (state.hasLocationPermission) {
                    SmallFloatingActionButton(onClick = { showMapStyleDialog = true }) {
                        Icon(Icons.Filled.Layers, contentDescription = stringResource(R.string.map_style))
                    }
                    if (state.parkedLocation != null) {
                        SmallFloatingActionButton(onClick = { cameraCommand = CameraCommand.FitBoth }) {
                            Icon(Icons.Filled.ZoomOutMap, contentDescription = stringResource(R.string.fit_both))
                        }
                    }
                    SmallFloatingActionButton(onClick = { cameraCommand = CameraCommand.CenterOnUser }) {
                        Icon(Icons.Filled.MyLocation, contentDescription = stringResource(R.string.center_me))
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .onGloballyPositioned { bottomPanelBounds = it.boundsInRoot() },
            ) {
                if (!state.hasLocationPermission) {
                    PermissionCard(
                        onGrant = { permissionLauncher.launch(locationPermissions) },
                        onSettings = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null),
                                ),
                            )
                        },
                    )
                } else if (selectedHistory != null) {
                    HistoryEntryCard(
                        entry = selectedHistory,
                        onNavigate = {
                            val opened = NavigationIntents.openWalkingNavigation(
                                context,
                                selectedHistory.latitude,
                                selectedHistory.longitude,
                            )
                            if (!opened) {
                                viewModel.reportNoNavigationApp()
                            }
                        },
                        onRestore = {
                            if (state.parkedLocation != null) {
                                showRestoreDialog = true
                            } else {
                                restoreFromHistory(selectedHistory)
                            }
                        },
                        onClose = { selectedHistoryId = null },
                    )
                } else {
                    ParkingInfoCard(
                        state = state,
                        onNavigate = {
                            val parked = state.parkedLocation ?: return@ParkingInfoCard
                            val opened = NavigationIntents.openWalkingNavigation(
                                context,
                                parked.latitude,
                                parked.longitude,
                            )
                            if (!opened) {
                                viewModel.reportNoNavigationApp()
                            }
                        },
                        onDelete = { showDeleteDialog = true },
                        onPark = {
                            if (state.parkedLocation != null) showReplaceDialog = true
                            else viewModel.saveCurrentParking()
                        },
                        onEditNote = { showNoteDialog = true },
                        onAddPhoto = { showAddPhotoDialog = true },
                        onDeletePhoto = { photoId ->
                            photoIdToDelete = photoId
                        },
                        onPhotoClick = { file ->
                            fullScreenPhotoFile = file
                        },
                    )
                }
            }
        }
    }
}

private fun formatDateTime(context: Context, millis: Long): String {
    val date = Date(millis)
    return DateFormat.getDateFormat(context).format(date) + " " + DateFormat.getTimeFormat(context).format(date)
}

private fun historySummary(context: Context, entry: ParkingHistoryEntry): String {
    val minutes = ((entry.endedAtMillis - entry.savedAtMillis) / 60_000L).toInt()
    return formatDateTime(context, entry.savedAtMillis) + " · " + Geo.formatDuration(minutes)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistorySheet(
    history: List<ParkingHistoryEntry>,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onClearAll: () -> Unit,
) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.history),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                if (history.isNotEmpty()) {
                    TextButton(onClick = onClearAll) { Text(stringResource(R.string.clear_history)) }
                }
            }
            if (history.isEmpty()) {
                Text(
                    text = stringResource(R.string.history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                LazyColumn {
                    items(history, key = { it.id }) { entry ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(entry.id) }
                                .padding(vertical = 8.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = historySummary(context, entry),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                entry.note?.let { note ->
                                    Text(
                                        text = note,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            IconButton(onClick = { onDelete(entry.id) }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.delete_history_entry_title),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryEntryCard(
    entry: ParkingHistoryEntry,
    onNavigate: () -> Unit,
    onRestore: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    Card(elevation = CardDefaults.cardElevation(6.dp)) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.history),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.history_close))
                }
            }
            Text(
                text = historySummary(context, entry),
                style = MaterialTheme.typography.bodyMedium,
            )
            entry.note?.let { note ->
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onNavigate) {
                    Icon(Icons.Filled.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.go_here))
                }
                FilledTonalButton(onClick = onRestore) {
                    Icon(Icons.Filled.LocalParking, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.restore_parking))
                }
            }
        }
    }
}

@Composable
private fun ParkButton(onClick: () -> Unit) {
    FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(Icons.Filled.LocalParking, contentDescription = stringResource(R.string.park_here))
    }
}

@Composable
private fun MapStyleDialog(
    selected: MapStyle,
    onDismiss: () -> Unit,
    onSelect: (MapStyle) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.map_style)) },
        text = {
            Column {
                MapStyle.entries.forEach { style ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(style) }
                            .padding(vertical = 12.dp),
                    ) {
                        RadioButton(selected = style == selected, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(style.labelRes))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun NoteDialog(
    initialNote: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf(initialNote) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.note)) },
        text = {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text(stringResource(R.string.note_hint)) },
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft) }) { Text(stringResource(R.string.confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun PermissionCard(
    onGrant: () -> Unit,
    onSettings: () -> Unit,
) {
    Card(elevation = CardDefaults.cardElevation(6.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.location_needed),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onGrant) {
                    Text(stringResource(R.string.grant_permission))
                }
                FilledTonalButton(onClick = onSettings) {
                    Text(stringResource(R.string.open_settings))
                }
            }
        }
    }
}

@Composable
private fun ParkingInfoCard(
    state: ParkingUiState,
    onNavigate: () -> Unit,
    onDelete: () -> Unit,
    onPark: () -> Unit,
    onEditNote: () -> Unit,
    onAddPhoto: () -> Unit,
    onDeletePhoto: (Long) -> Unit,
    onPhotoClick: (File) -> Unit,
) {
    val context = LocalContext.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    Card(elevation = CardDefaults.cardElevation(6.dp)) {
        Column(Modifier.animateContentSize().padding(16.dp).fillMaxWidth()) {
            val parked = state.parkedLocation
            if (parked == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (state.userLocation == null) {
                                stringResource(R.string.waiting_gps)
                            } else {
                                stringResource(R.string.no_parking)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(R.string.park_here),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    ParkButton(onClick = onPark)
                }
            } else {
                val note = parked.note
                val distance = state.distanceMeters?.let { Geo.formatDistance(it) } ?: "—"
                val walkingTime = state.walkingMinutes?.let { Geo.formatWalkingTime(it) } ?: "—"
                val walkingLabel = stringResource(R.string.walking_time).lowercase()
                val distanceLabel = stringResource(R.string.distance)
                val timeLabel = stringResource(R.string.time)

                Column(Modifier.fillMaxWidth().clickable { expanded = !expanded }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.my_car),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = stringResource(
                                if (expanded) R.string.hide_details else R.string.show_details,
                            ),
                        )
                    }
                    Text(
                        text = "$distanceLabel: $distance",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "$timeLabel: $walkingTime $walkingLabel",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (!expanded && (note != null || state.photos.isNotEmpty())) {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = note.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (state.photos.isNotEmpty()) {
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    Icons.Filled.CameraAlt,
                                    contentDescription = stringResource(R.string.photos),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = state.photos.size.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                if (expanded) {
                    Text(
                        text = stringResource(
                            R.string.saved_at,
                        ) + ": " + DateFormat.getDateFormat(context).format(Date(parked.savedAtMillis)) +
                            " " + DateFormat.getTimeFormat(context).format(Date(parked.savedAtMillis)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(4.dp))
                    if (note == null) {
                        TextButton(onClick = onEditNote) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.add_note))
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = onEditNote) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = stringResource(R.string.edit_note),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

                    TextButton(onClick = onAddPhoto) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.add_photo))
                    }

                    if (state.photos.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(state.photos, key = { it.id }) { photo ->
                                val photoFile = File(File(context.filesDir, "parking_photos"), photo.fileName)
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onPhotoClick(photoFile) },
                                ) {
                                    AsyncImage(
                                        model = photoFile,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                    IconButton(
                                        onClick = { onDeletePhoto(photo.id) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(24.dp)
                                            .padding(2.dp),
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = onNavigate) {
                        Icon(Icons.Filled.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.navigate_to_car))
                    }
                    Spacer(Modifier.weight(1f))
                    if (expanded) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.delete_location),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    ParkButton(onClick = onPark)
                }
            }
        }
    }
}