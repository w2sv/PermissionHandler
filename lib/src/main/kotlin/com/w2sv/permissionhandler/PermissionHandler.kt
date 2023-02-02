@file:Suppress("unused")

package com.w2sv.permissionhandler

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.edit
import com.w2sv.androidutils.ActivityCallContractHandler
import com.w2sv.permissionhandler.extensions.getAppPreferences

abstract class PermissionHandler<I, O>(
    protected val activity: ComponentActivity,
    protected val permission: I,
    resultContract: ActivityResultContract<I, O>,
    override val registryKey: String
) : ActivityCallContractHandler.Impl<I, O>(activity, resultContract) {

    private val sharedPreferencesKey by ::registryKey

    protected var permissionPreviouslyRequested: Boolean =
        activity.getAppPreferences().getBoolean(sharedPreferencesKey, false)
            .also {
                println("Retrieved $sharedPreferencesKey=$it")
            }

    /**
     * Temporary callables set before, and cleared on exiting of [resultCallback]
     */
    protected var onGranted: (() -> Unit)? = null
    protected var onDenied: (() -> Unit)? = null
    protected var onRequestDismissed: (() -> Unit)? = null

    /**
     * Function wrapper either directly running [onGranted] if permission granted,
     * otherwise sets [onGranted] and launches [resultCallback]
     *
     * @return Boolean indicating whether request dialog has been invoked
     */
    fun requestPermissionIfRequired(
        onGranted: () -> Unit,
        onDenied: (() -> Unit)? = null,
        onRequestDismissed: (() -> Unit)? = null
    ): Boolean =
        when {
            permissionGranted -> {
                onGranted()
                false
            }
            permissionRationalSuppressed -> {
                onPermissionRationalSuppressed()
                false
            }
            else -> {
                this.onGranted = onGranted
                this.onDenied = onDenied
                this.onRequestDismissed = onRequestDismissed

                resultLauncher.launch(permission)
                true
            }
        }

    abstract val permissionGranted: Boolean
    abstract val permissionRationalSuppressed: Boolean
    abstract val requiredByAndroidSdk: Boolean

    override val resultCallback: (O) -> Unit = { activityResult ->
        println("Request result: $activityResult")

        when (permissionGranted(activityResult)) {
            false -> onDenied?.invoke()
            true -> onGranted?.invoke()
        }

        onRequestDismissed?.invoke()

        onRequestDismissed = null
        onDenied = null
        onGranted = null

        if (!permissionPreviouslyRequested) {
            permissionPreviouslyRequested = true
            activity.getAppPreferences().edit {
                putBoolean(sharedPreferencesKey, true)
                println("Wrote $sharedPreferencesKey=true to sharedPreferences")
            }
        }
    }

    protected abstract fun permissionGranted(activityResult: O): Boolean

    abstract fun onPermissionRationalSuppressed()

    protected fun packageWideRequestedPermissions(): Set<String> =
        activity.run {
            packageManager.getPackageInfoCompat(packageName)
        }
            .requestedPermissions
            .toSet()

    companion object {
        @JvmStatic
        protected fun PackageManager.getPackageInfoCompat(packageName: String): PackageInfo =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                )
            else
                @Suppress("DEPRECATION")
                getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
    }
}