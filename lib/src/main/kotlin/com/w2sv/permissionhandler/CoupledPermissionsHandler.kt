@file:Suppress("unused")

package com.w2sv.permissionhandler

import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat

abstract class CoupledPermissionsHandler(
    activity: ComponentActivity,
    permissions: Array<String>,
    classKey: String
) : PermissionHandler<Array<String>, Map<String, Boolean>>(
    activity,
    permissions,
    resultContract = ActivityResultContracts.RequestMultiplePermissions(),
    registryKey = "$classKey.${permissions.toList()}"
) {

    override val permissionGranted: Boolean
        get() = !requiredByAndroidSdk || permission.all {
            ActivityCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }

    override val permissionRationalSuppressed: Boolean
        get() = permissionPreviouslyRequested && permission.all {
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
        }

    override val requiredByAndroidSdk: Boolean =
        packageWideRequestedPermissions().let { requestedPermissions ->
            permissions.any { requestedPermissions.contains(it) }
        }

    override fun permissionGranted(activityResult: Map<String, Boolean>): Boolean =
        activityResult.values.all { it }
}