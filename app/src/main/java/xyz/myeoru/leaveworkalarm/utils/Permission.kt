package xyz.myeoru.leaveworkalarm.utils

import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.core.content.ContextCompat

fun checkAndRequestPermission(
    activity: Activity,
    permissions: Array<String>,
    launcher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
    onDenied: () -> Unit = {},
    onGranted: () -> Unit = {}
) {
    when {
        permissions.all {
            ContextCompat.checkSelfPermission(
                activity,
                it
            ) == PackageManager.PERMISSION_GRANTED
        } -> {
            onGranted()
        }

        permissions.all { activity.shouldShowRequestPermissionRationale(it) } -> {
            onDenied()
        }

        else -> {
            launcher.launch(permissions)
        }
    }
}