package io.github.goodwind.hypermissions

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity

/**
 * The answer to a settings intent another app sent (App Info, All files access, Display over other
 * apps...), reached through the RequestEntry alias: a translucent panel over that app showing what
 * is off, in a task of its own. Back closes the task and lands in the app that asked.
 */
class RequestActivity : ComponentActivity() {

    private var target by mutableStateOf<String?>(null)
    private var named by mutableStateOf<AdbShell.AppOp?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        read(intent)
        setContent {
            CompositionLocalProvider(LocalDensity provides canvasDensity(this)) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
                    target?.let { pkg ->
                        DetailPanel(
                            pkg = pkg,
                            requestOnly = true,
                            onlyOp = named,
                            translucent = true,
                            onOpen = {
                                finishAndRemoveTask()
                                launchApp(this@RequestActivity, it)
                            },
                            onUninstall = {
                                finishAndRemoveTask()
                                uninstallApp(this@RequestActivity, it)
                            },
                            onClose = { finishAndRemoveTask() },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        read(intent)
    }

    private fun read(intent: Intent) {
        val pkg = intent.data?.takeIf { it.scheme == "package" }?.schemeSpecificPart
        if (pkg == null) {
            finishAndRemoveTask()
            return
        }
        // Some of these name the one access the app wants; App Info itself does not.
        named = when (intent.action) {
            "android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION" -> AdbShell.AppOp.MANAGE_EXTERNAL_STORAGE
            "android.settings.action.MANAGE_OVERLAY_PERMISSION" -> AdbShell.AppOp.SYSTEM_ALERT_WINDOW
            "android.settings.action.MANAGE_WRITE_SETTINGS" -> AdbShell.AppOp.WRITE_SETTINGS
            "android.settings.MANAGE_UNKNOWN_APP_SOURCES" -> AdbShell.AppOp.REQUEST_INSTALL_PACKAGES
            else -> null
        }
        target = pkg
    }
}
