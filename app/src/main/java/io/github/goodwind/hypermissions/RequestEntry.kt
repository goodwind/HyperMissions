package io.github.goodwind.hypermissions

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings

/**
 * Answers the settings screens that apps send people to (File Manager+ sends App Info) on firmware
 * that lacks them, as Xiaomi HyperOS on MiTV-MFTR0 does, through an alias that is off until then.
 *
 * It is on only while nothing else answers the App Info intent: neither the firmware nor another app
 * that stands in for it (Aerial 1.32+ does). Two answers would make Android ask which to use every
 * time. Whichever of two such apps looks second finds the other and stays off; this one looks when it
 * is opened and when the TV starts.
 */
object RequestEntry {
    fun update(context: Context) {
        val probe = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        @Suppress("DEPRECATION")
        val answered = context.packageManager.queryIntentActivities(probe, 0).any { it.activityInfo.packageName != context.packageName }
        val alias = ComponentName(context, "${context.packageName}.RequestEntry")
        val state = if (answered) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        if (context.packageManager.getComponentEnabledSetting(alias) != state) {
            context.packageManager.setComponentEnabledSetting(alias, state, PackageManager.DONT_KILL_APP)
        }
    }
}

/** Looks again when the TV starts, in case what answered the intent has been removed since. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = RequestEntry.update(context)
}
