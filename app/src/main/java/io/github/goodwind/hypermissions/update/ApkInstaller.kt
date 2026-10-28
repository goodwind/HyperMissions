package io.github.goodwind.hypermissions.update

import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

/**
 * Hands a downloaded APK of this very app to Android's package installer. The system asks the person to confirm,
 * and refuses unless the APK is signed with the key of the copy it replaces, so a wrong file cannot get in.
 *
 * Some TV firmware (Xiaomi, Android 11) leaves its installer without the screen a session asks for; there the
 * same file goes to the installer's ordinary "open an APK" screen instead.
 */
object ApkInstaller {
    /** [onResult] gets null once the installer has taken it (the app restarts after a replace), or a sentence about what went wrong. */
    fun install(context: Context, apk: File, onResult: (String?) -> Unit) {
        val app = context.applicationContext
        val installer = app.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply { setAppPackageName(app.packageName) }
        val id = installer.createSession(params)
        val action = "${app.packageName}.INSTALL_RESULT.$id"
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
                    PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                        @Suppress("DEPRECATION")
                        val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                        val shown = confirm != null && try {
                            app.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
                        } catch (_: ActivityNotFoundException) { false } catch (_: SecurityException) { false }
                        if (!shown) {
                            app.unregisterReceiver(this)
                            runCatching { installer.abandonSession(id) }
                            openInInstaller(app, apk, onResult)
                        }
                    }
                    PackageInstaller.STATUS_SUCCESS -> { app.unregisterReceiver(this); onResult(null) }
                    else -> {
                        app.unregisterReceiver(this)
                        onResult(intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "status $status")
                    }
                }
            }
        }
        if (Build.VERSION.SDK_INT >= 33) app.registerReceiver(receiver, IntentFilter(action), Context.RECEIVER_NOT_EXPORTED)
        else app.registerReceiver(receiver, IntentFilter(action))
        installer.openSession(id).use { session ->
            apk.inputStream().use { input -> session.openWrite("update.apk", 0, apk.length()).use { out -> input.copyTo(out); session.fsync(out) } }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
            val sent = PendingIntent.getBroadcast(app, id, Intent(action).setPackage(app.packageName), flags)
            session.commit(sent.intentSender)
        }
    }

    /** The installer's own screen for a file: a content URI the installer may read, since it is another app. */
    private fun openInInstaller(app: Context, apk: File, onResult: (String?) -> Unit) {
        try {
            val uri = FileProvider.getUriForFile(app, "${app.packageName}.updates", apk)
            app.startActivity(
                Intent(Intent.ACTION_INSTALL_PACKAGE)
                    .setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION),
            )
        } catch (e: Exception) {
            onResult(e.message ?: e.javaClass.simpleName)
        }
    }
}
