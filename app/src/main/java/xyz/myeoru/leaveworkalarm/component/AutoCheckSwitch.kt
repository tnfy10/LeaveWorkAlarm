package xyz.myeoru.leaveworkalarm.component

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import xyz.myeoru.leaveworkalarm.service.AutoWorkService
import xyz.myeoru.leaveworkalarm.ui.theme.LeaveWorkAlarmTheme
import xyz.myeoru.leaveworkalarm.utils.checkAndRequestPermission
import xyz.myeoru.leaveworkalarm.utils.findActivity

@Composable
fun AutoCheckSwitch() {
    val context = LocalContext.current
    var autoWorkCheck by remember { mutableStateOf(AutoWorkService.isServiceRunning) }

    val permissions = mutableListOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestMultiplePermissions()) { permissionGrantedMap ->
            if (permissionGrantedMap.values.all { it }) {
                val intent = Intent(context, AutoWorkService::class.java)
                ContextCompat.startForegroundService(context, intent)
                autoWorkCheck = true
            }
        }

    var showPermissionSettingDialog by remember { mutableStateOf(false) }
    if (showPermissionSettingDialog) {
        AlertDialog(
            onDismissRequest = {
                showPermissionSettingDialog = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionSettingDialog = false
                        val uri = Uri.fromParts("package", context.packageName, null)
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = uri
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text(text = "설정")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionSettingDialog = false }) {
                    Text(text = "취소")
                }
            },
            text = {
                Text(text = "자동 체크를 사용하기 위해서는 권한 허용이 필요합니다.\n설정에서 알림과 권한을 허용해주세요.")
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = "자동 체크"
        )
        Switch(
            checked = autoWorkCheck,
            onCheckedChange = { isChecked ->
                val intent = Intent(context, AutoWorkService::class.java)
                if (isChecked) {
                    checkAndRequestPermission(
                        context.findActivity(),
                        permissions.toTypedArray(),
                        permissionLauncher,
                        onDenied = {
                            showPermissionSettingDialog = true
                        },
                        onGranted = {
                            ContextCompat.startForegroundService(context, intent)
                            autoWorkCheck = true
                        }
                    )
                } else {
                    context.stopService(intent)
                    autoWorkCheck = false
                }
            })
    }
}

@Preview(showBackground = true)
@Composable
fun AutoCheckSwitchPreview() {
    LeaveWorkAlarmTheme {
        AutoCheckSwitch()
    }
}