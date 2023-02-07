@file:Suppress("unused")

package com.w2sv.permissionhandler

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.edit
import com.w2sv.androidutils.ActivityCallContractHandler
import com.w2sv.androidutils.extensions.getDefaultPreferences
import slimber.log.i

abstract class PermissionHandler<I, O>(
    protected val activity: ComponentActivity,
    protected val permission: I,
    resultContract: ActivityResultContract<I, O>,
    override val registryKey: String
) : ActivityCallContractHandler.Impl<I, O>(activity, resultContract) {

    private val sharedPreferencesKey by ::registryKey

    protected var permissionPreviouslyRequested: Boolean =
        activity.getDefaultPreferences().getBoolean(sharedPreferencesKey, false)
            .also {
                i { "Retrieved $sharedPreferencesKey.permissionPreviouslyRequested=$it" }
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
    open fun requestPermissionIfRequired(
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
        i { "Request result: $activityResult" }

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
            activity.getDefaultPreferences().edit {
                putBoolean(sharedPreferencesKey, true)
                i { "Wrote $sharedPreferencesKey=true to sharedPreferences" }
            }
        }
    }

    protected abstract fun permissionGranted(activityResult: O): Boolean

    abstract fun onPermissionRationalSuppressed()
}

fun Iterable<PermissionHandler<*, *>>.requestPermissions(
    onGranted: () -> Unit,
    onDenied: (() -> Unit)? = null,
    onRequestDismissed: (() -> Unit)? = null
) {
    iterator().requestPermissions(onGranted, onDenied, onRequestDismissed)
}

private fun Iterator<PermissionHandler<*, *>>.requestPermissions(
    onGranted: () -> Unit,
    onDenied: (() -> Unit)? = null,
    onRequestDismissed: (() -> Unit)? = null
) {
    if (!hasNext()) {
        onGranted()
        onRequestDismissed?.invoke()
    } else {
        next().requestPermissionIfRequired(
            onGranted = { requestPermissions(onGranted, onDenied, onRequestDismissed) },
            onDenied = onDenied
        )
    }
}