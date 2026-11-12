package com.monchito007.whereismycar.ui

import android.Manifest
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.monchito007.whereismycar.R
import com.monchito007.whereismycar.bluetooth.AutoParkPermissions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoParkSettingsScreen(
    onBack: () -> Unit,
    viewModel: AutoParkSettingsViewModel = viewModel(),
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val config by viewModel.config.collectAsStateWithLifecycle()
    val pairedDevices by viewModel.pairedDevices.collectAsStateWithLifecycle()

    // Se incrementa para volver a leer el estado de los permisos (p. ej. al volver de Ajustes).
    var permissionsVersion by remember { mutableIntStateOf(0) }
    val refresh = {
        permissionsVersion++
        viewModel.refreshPairedDevices()
    }
    LifecycleResumeEffect(Unit) {
        refresh()
        onPauseOrDispose { }
    }

    val hasBluetooth = remember(permissionsVersion) { AutoParkPermissions.hasBluetooth(context) }
    val hasFineLocation = remember(permissionsVersion) { AutoParkPermissions.hasFineLocation(context) }
    val hasBackgroundLocation = remember(permissionsVersion) { AutoParkPermissions.hasBackgroundLocation(context) }
    val allGranted = hasBluetooth && hasFineLocation && hasBackgroundLocation

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { refresh() }

    val openAppSettings = {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.auto_park_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.auto_park_intro), style = MaterialTheme.typography.bodyMedium)

            // --- Permisos ---
            SettingsCard(title = stringResource(R.string.auto_park_permissions)) {
                AutoParkPermissions.bluetoothRuntimePermission?.let { btPermission ->
                    PermissionRow(
                        label = stringResource(R.string.auto_park_perm_bluetooth),
                        granted = hasBluetooth,
                        onRequest = { permissionLauncher.launch(arrayOf(btPermission)) },
                    )
                }
                PermissionRow(
                    label = stringResource(R.string.auto_park_perm_location),
                    granted = hasFineLocation,
                    onRequest = {
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        )
                    },
                )
                if (AutoParkPermissions.needsSeparateBackgroundLocation) {
                    PermissionRow(
                        label = stringResource(R.string.auto_park_perm_background),
                        description = stringResource(R.string.auto_park_perm_background_hint),
                        granted = hasBackgroundLocation,
                        enabled = hasFineLocation,
                        onRequest = {
                            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
                        },
                    )
                }
                if (!allGranted) {
                    TextButton(onClick = openAppSettings) { Text(stringResource(R.string.auto_park_no_dialog)) }
                }
            }

            // --- Elegir coche ---
            SettingsCard(title = stringResource(R.string.auto_park_your_car)) {
                when (val state = pairedDevices) {
                    PairedDevicesState.NoPermission -> Text(stringResource(R.string.auto_park_devices_no_permission))
                    PairedDevicesState.Unsupported -> Text(stringResource(R.string.auto_park_devices_unsupported))
                    PairedDevicesState.BluetoothOff -> Text(stringResource(R.string.auto_park_devices_bt_off))
                    is PairedDevicesState.Loaded ->
                        if (state.devices.isEmpty()) {
                            Text(stringResource(R.string.auto_park_devices_empty))
                        } else {
                            state.devices.forEach { device ->
                                val selected = device.address.equals(config.deviceAddress, ignoreCase = true)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectDevice(device) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(selected = selected, onClick = { viewModel.selectDevice(device) })
                                    Column {
                                        Text(device.name, style = MaterialTheme.typography.bodyLarge)
                                        Text(device.address, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                }
                TextButton(onClick = { viewModel.refreshPairedDevices() }) {
                    Text(stringResource(R.string.auto_park_refresh))
                }
            }

            // --- Activar ---
            SettingsCard(title = stringResource(R.string.auto_park_switch_title)) {
                val canEnable = allGranted && config.deviceAddress != null
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = config.deviceName?.let { stringResource(R.string.auto_park_switch_label, it) }
                            ?: stringResource(R.string.auto_park_choose_car_first),
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = config.enabled && canEnable,
                        onCheckedChange = viewModel::setEnabled,
                        enabled = canEnable,
                    )
                }
                if (config.enabled && !allGranted) {
                    Text(
                        stringResource(R.string.auto_park_missing_permissions),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            // --- Batería (recomendado) ---
            val ignoringBatteryOptimizations = remember(permissionsVersion) {
                context.getSystemService(PowerManager::class.java)
                    ?.isIgnoringBatteryOptimizations(context.packageName) == true
            }
            SettingsCard(title = stringResource(R.string.auto_park_battery_title)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (ignoringBatteryOptimizations) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = if (ignoringBatteryOptimizations) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        },
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(
                            if (ignoringBatteryOptimizations) {
                                R.string.auto_park_battery_ok
                            } else {
                                R.string.auto_park_battery_restricted
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (!ignoringBatteryOptimizations) {
                    TextButton(onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }) { Text(stringResource(R.string.auto_park_battery_open)) }
                }
            }

            // --- Registro de actividad (diagnóstico) ---
            val logEntries by viewModel.logEntries.collectAsStateWithLifecycle()
            SettingsCard(title = stringResource(R.string.auto_park_log_title)) {
                Text(stringResource(R.string.auto_park_log_hint), style = MaterialTheme.typography.bodySmall)
                if (logEntries.isEmpty()) {
                    Text(stringResource(R.string.auto_park_log_empty), style = MaterialTheme.typography.bodyMedium)
                } else {
                    logEntries.forEach { entry ->
                        Text(entry, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    }
                    TextButton(onClick = viewModel::clearLog) { Text(stringResource(R.string.auto_park_log_clear)) }
                }
                // Solo en versiones de prueba (instaladas desde Android Studio)
                val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
                if (isDebuggable) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = viewModel::simulateDisconnect) {
                            Text(stringResource(R.string.auto_park_simulate_disconnect))
                        }
                        FilledTonalButton(onClick = viewModel::simulateReconnect) {
                            Text(stringResource(R.string.auto_park_simulate_reconnect))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun PermissionRow(
    label: String,
    granted: Boolean,
    onRequest: () -> Unit,
    description: String? = null,
    enabled: Boolean = true,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (granted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = stringResource(if (granted) R.string.auto_park_granted else R.string.auto_park_pending),
            tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (description != null && !granted) {
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (!granted) {
            FilledTonalButton(onClick = onRequest, enabled = enabled) {
                Text(stringResource(R.string.auto_park_grant))
            }
        }
    }
}
