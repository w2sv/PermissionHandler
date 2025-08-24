@file:Suppress("unused")

package com.w2sv.permissionhandler

import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.w2sv.androidutils.getPackagePermissions
import kotlinx.coroutines.flow.StateFlow

abstract class PermissionHandler(
    activity: ComponentActivity,
    permission: String,
    classKey: String,
    permissionPreviouslyRequested: StateFlow<Boolean>,
    savePermissionPreviouslyRequested: () -> Unit
) : AbstractPermissionHandler<String, Boolean>(
    activity = activity,
    permission = permission,
    resultContract = ActivityResultContracts.RequestPermission(),
    registryKey = "$classKey.$permission",
    permissionPreviouslyRequested = permissionPreviouslyRequested,
    savePermissionPreviouslyRequested = savePermissionPreviouslyRequested
) {
    override fun permissionGranted(): Boolean =
        !requiredByAndroidSdk ||
            ActivityCompat.checkSelfPermission(
                activity,
                permission
            ) == PackageManager.PERMISSION_GRANTED

    override fun permissionRationalSuppressed(): Boolean =
        permissionPreviouslyRequested.value &&
            !ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                permission
            )

    override val requiredByAndroidSdk: Boolean =
        activity
            .getPackagePermissions()
            .contains(permission)

    override fun permissionNewlyGranted(activityResult: Boolean): Boolean =
        activityResult
}
