package com.github.codeworkscreativehub.mlauncher.domain.launcher

import com.github.codeworkscreativehub.mlauncher.data.AppListItem

/**
 * Abstracts app launching with biometric check.
 * Pure domain interface — no Android framework types.
 */
interface AppLauncher {
    fun launch(app: AppListItem, onAuthRequired: (AppListItem, AuthCallback) -> Unit)
    fun launchDirect(app: AppListItem)
}

interface AuthCallback {
    fun onSuccess(app: AppListItem)
    fun onFailed()
    fun onError(errorCode: Int, message: CharSequence?)
}
