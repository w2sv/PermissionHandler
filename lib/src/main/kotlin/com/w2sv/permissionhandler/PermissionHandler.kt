@file:Suppress("unused")

package com.w2sv.permissionhandler

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.w2sv.androidutils.ActivityCallContractAdministrator

abstract class PermissionHandler(
    protected val activity: ComponentActivity,
    protected val permission: String
) : ActivityCallContractAdministrator.Impl<String, Boolean>(
    activity,
    ActivityResultContracts.RequestPermission()
) {

    /**
     * Function wrapper either directly running [onPermissionGranted] if permission granted,
     * otherwise sets [onPermissionGranted] and launches [activityResultCallback]
     */
    fun requestPermission(
        onPermissionGranted: () -> Unit,
        onPermissionDenied: (() -> Unit)? = null,
        onDialogClosed: (() -> Unit)? = null
    ) {
        if (!requiresGranting)
            onPermissionGranted()
        else {
            this.onPermissionGranted = onPermissionGranted
            this.onPermissionDenied = onPermissionDenied
            this.onDialogClosed = onDialogClosed

            activityResultLauncher.launch(permission)
        }
    }

    private val requiresGranting: Boolean
        get() = requiredByAndroidSdk && activity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED

    private val requiredByAndroidSdk: Boolean = activity.run {
        packageManager.getPackageInfoCompat(packageName)
    }
        .requestedPermissions
        .toSet()
        .contains(permission)

    /**
     * Temporary callables set before, and cleared on exiting of [activityResultCallback]
     */
    private var onPermissionGranted: (() -> Unit)? = null
    private var onPermissionDenied: (() -> Unit)? = null
    private var onDialogClosed: (() -> Unit)? = null

    override val registryKey: String = "${this::class.java.name}.$permission"

    override val activityResultCallback: (Boolean) -> Unit = { permissionGranted ->
        when (permissionGranted) {
            true -> {
                if (!permissionRationalSuppressed)
                    onPermissionDenied()
                else
                    onPermissionRationalSuppressed()

                onPermissionDenied?.invoke()
            }

            false -> onPermissionGranted?.invoke()
        }

        onDialogClosed?.invoke()

        onDialogClosed = null
        onPermissionDenied = null
        onPermissionGranted = null
    }

    protected abstract fun onPermissionDenied()
    protected abstract fun onPermissionRationalSuppressed()

    val permissionRationalSuppressed: Boolean =
        !activity.shouldShowRequestPermissionRationale(permission)
}

private fun PackageManager.getPackageInfoCompat(packageName: String) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        getPackageInfo(
            packageName,
            PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
        )
    else
        @Suppress("DEPRECATION")
        getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)