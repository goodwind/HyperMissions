package io.github.goodwind.hypermissions

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The sentences a change can end in, in the TV's language. */
class EditorTexts(val waiting: String, val unreachable: String, val failed: String, val stopped: String, val cleared: String)

/**
 * One app's details and the changes that can be made to it: what its panels share. Reading is the app's own
 * PackageManager; changing goes through [AdbShell], one change at a time, and reloads the details after.
 */
@Stable
class PermissionEditor(
    private val context: Context,
    private val scope: CoroutineScope,
    val pkg: String,
    private val texts: EditorTexts,
) {
    var details by mutableStateOf<AppDetails?>(null)
        private set
    /** What went wrong (shown orange) or what was done (grey) with the last change. */
    var message by mutableStateOf<String?>(null)
        private set
    var notice by mutableStateOf<String?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    /** Clearing data cannot be undone, so it takes two presses; the second must come soon after the first. */
    var confirmClear by mutableStateOf(false)
        private set

    suspend fun load(): AppDetails? {
        val loaded = withContext(Dispatchers.IO) { AppDetails.load(context, pkg) }
        details = loaded
        return loaded
    }

    /** Runs [change] (AdbShell calls, each returning what the shell printed), then reloads. [done] is shown when it worked. */
    private fun change(done: String? = null, change: (waiting: () -> Unit) -> List<String>) {
        if (busy) return
        busy = true
        message = null
        notice = null
        scope.launch {
            val problem = withContext(Dispatchers.IO) {
                try {
                    val printed = change { message = texts.waiting }.firstOrNull { it.isNotEmpty() }
                    printed?.lineSequence()?.first()?.let { texts.failed.format(it) }
                } catch (e: AdbShell.AdbException) {
                    texts.unreachable
                }
            }
            details = withContext(Dispatchers.IO) { AppDetails.load(context, pkg) }
            message = problem
            if (problem == null) notice = done
            busy = false
        }
    }

    /** OK on a group turns the whole group on, or off when it is all on. */
    fun toggle(group: PermissionGroup) = change { waiting ->
        group.permissions.map { AdbShell.setPermission(context, pkg, it, !group.allGranted, waiting) }
    }

    fun toggle(access: SpecialAccess) = change { waiting ->
        listOf(AdbShell.setAppOp(context, pkg, access.op, !access.allowed, waiting))
    }

    fun forceStop() = change(texts.stopped) { waiting ->
        listOf(AdbShell.act(context, pkg, AdbShell.AppAction.FORCE_STOP, waiting))
    }

    fun clearData() {
        if (!confirmClear) {
            confirmClear = true
            return
        }
        confirmClear = false
        change(texts.cleared) { waiting -> listOf(AdbShell.act(context, pkg, AdbShell.AppAction.CLEAR_DATA, waiting)) }
    }

    /** The first press of Clear data is forgotten after this long. */
    fun forgetConfirm() { confirmClear = false }
}

@Composable
fun rememberPermissionEditor(pkg: String): PermissionEditor {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val texts = EditorTexts(
        waiting = stringResource(R.string.info_waiting),
        unreachable = stringResource(R.string.info_unreachable),
        failed = stringResource(R.string.info_failed),
        stopped = stringResource(R.string.info_stopped),
        cleared = stringResource(R.string.info_data_cleared),
    )
    return remember(pkg) { PermissionEditor(context, scope, pkg, texts) }
}
