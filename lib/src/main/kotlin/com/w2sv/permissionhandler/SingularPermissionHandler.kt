@file:Suppress("unused")

package com.w2sv.permissionhandler

import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat

abstract class SingularPermissionHandler(
    activity: ComponentActivity,
    permission: String,
    classKey: String
) : PermissionHandler<String, Boolean>(
    activity,
    permission,
    resultContract = ActivityResultContracts.RequestPermission(),
    registryKey = "$classKey.$permission"
) {

    override val permissionGranted: Boolean
        get() = !requiredByAndroidSdk || ActivityCompat.checkSelfPermission(
            activity,
            permission
        ) == PackageManager.PERMISSION_GRANTED

    override val permissionRationalSuppressed: Boolean
        get() = permissionPreviouslyRequested && !ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            permission
        )

    override val requiredByAndroidSdk: Boolean = packageWideRequestedPermissions()
        .contains(permission)

    override fun permissionGranted(activityResult: Boolean): Boolean =
        activityResult
}