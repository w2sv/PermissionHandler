@file:Suppress("unused")

package com.w2sv.permissionhandler

import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.w2sv.androidutils.getPackagePermissions
import kotlinx.coroutines.flow.StateFlow

abstract class CoupledPermissionsHandler(
    activity: ComponentActivity,
    permissions: Array<String>,
    classKey: String,
    permissionPreviouslyRequested: StateFlow<Boolean>,
    savePermissionPreviouslyRequested: () -> Unit
) : AbstractPermissionHandler<Array<String>, Map<String, Boolean>>(
    activity = activity,
    permission = permissions,
    resultContract = ActivityResultContracts.RequestMultiplePermissions(),
    registryKey = "$classKey.${permissions.toList()}",
    permissionPreviouslyRequested = permissionPreviouslyRequested,
    savePermissionPreviouslyRequested = savePermissionPreviouslyRequested
) {
    override fun permissionGranted(): Boolean =
        !requiredByAndroidSdk ||
            permission.all {
                ActivityCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
            }

    override fun permissionRationalSuppressed(): Boolean =
        permissionPreviouslyRequested.value &&
            permission.all {
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
            }

    override val requiredByAndroidSdk: Boolean =
        activity.getPackagePermissions().let { requestedPermissions ->
            permissions.any { requestedPermissions.contains(it) }
        }

    override fun permissionNewlyGranted(activityResult: Map<String, Boolean>): Boolean =
        activityResult.values.all { it }
}
