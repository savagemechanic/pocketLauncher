package com.github.codeworkscreativehub.mlauncher.domain.launcher

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import com.github.codeworkscreativehub.common.AppLogger
import com.github.codeworkscreativehub.common.CrashHandler
import com.github.codeworkscreativehub.common.showShortToast
import com.github.codeworkscreativehub.mlauncher.data.AppListItem
import com.github.codeworkscreativehub.mlauncher.data.repository.AppManagementRepository
import com.github.codeworkscreativehub.mlauncher.helper.analytics.AppUsageMonitor
import com.github.codeworkscreativehub.mlauncher.helper.logActivitiesFromPackage

class AppLauncherImpl(
    private val context: Context,
    private val appMgmt: AppManagementRepository
) : AppLauncher {

    override fun launch(app: AppListItem, onAuthRequired: (AppListItem, AuthCallback) -> Unit) {
        val lockedApps = appMgmt.lockedApps.value
        logActivitiesFromPackage(context, app.activityPackage)

        if (app.activityPackage in lockedApps) {
            onAuthRequired(app, object : AuthCallback {
                override fun onSuccess(app: AppListItem) = launchDirect(app)
                override fun onFailed() {
                    AppLogger.e("AppLauncher", "Authentication failed")
                }
                override fun onError(errorCode: Int, message: CharSequence?) {
                    AppLogger.e("AppLauncher", "Auth error: $message ($errorCode)")
                }
            })
        } else {
            launchDirect(app)
        }
    }

    override fun launchDirect(app: AppListItem) {
        val launcher = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val activityInfo = launcher.getActivityList(app.activityPackage, app.user)
        if (activityInfo.isNotEmpty()) {
            val component = ComponentName(app.activityPackage, activityInfo.first().name)
            launchWithFallback(component, app.activityPackage, app.user, launcher)
        } else {
            context.showShortToast("App not found")
        }
    }

    private fun launchWithFallback(
        component: ComponentName,
        packageName: String,
        userHandle: UserHandle,
        launcher: LauncherApps
    ) {
        val tracker = AppUsageMonitor.createInstance(context)

        fun tryLaunch(user: UserHandle): Boolean = runCatching {
            tracker.updateLastUsedTimestamp(packageName)
            launcher.startMainActivity(component, user, null, null)
            CrashHandler.logUserAction("${component.packageName} App Launched")
            true
        }.getOrDefault(false)

        if (!tryLaunch(userHandle) && !tryLaunch(Process.myUserHandle())) {
            context.showShortToast("Unable to launch app")
        }
    }
}
