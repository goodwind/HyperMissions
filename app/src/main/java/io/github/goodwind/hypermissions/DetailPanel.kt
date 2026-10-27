package io.github.goodwind.hypermissions

import android.content.pm.ApplicationInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

/**
 * One app's actions and permissions. OK on a permission turns the whole group on, or off when it is
 * all on. Reading is the app's own PackageManager; changing goes through [AdbShell].
 *
 * [requestOnly]: opened because another app asked for something, so only what is off is shown (or
 * only [onlyOp], when the request names one), with a way on to the whole panel.
 */
@Composable
fun DetailPanel(
    pkg: String,
    requestOnly: Boolean,
    onlyOp: AdbShell.AppOp?,
    translucent: Boolean,
    onOpen: (String) -> Unit,
    onUninstall: (String) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val first = remember { FocusRequester() }
    val editor = rememberPermissionEditor(pkg)
    val details = editor.details
    // What was off when the panel opened, so that a row stays on screen after it is turned on.
    var asked by remember(pkg) { mutableStateOf<Set<String>?>(null) }
    var expanded by remember(pkg) { mutableStateOf(false) }
    LaunchedEffect(editor.confirmClear) {
        if (editor.confirmClear) {
            delay(4_000)
            editor.forgetConfirm()
        }
    }

    LaunchedEffect(pkg) {
        val loaded = editor.load()
        asked = loaded?.let { d -> d.groups.filter { !it.allGranted }.map { it.key }.toSet() + d.special.filter { !it.allowed }.map { it.op.name } }
    }
    val compact = requestOnly && !expanded && (onlyOp != null || !asked.isNullOrEmpty())
    LaunchedEffect(details == null, compact) {
        if (details != null) {
            withFrameNanos { }
            runCatching { first.requestFocus() }
        }
    }
    BackHandler(onBack = onClose)

    val removable = remember(pkg) {
        runCatching { (context.packageManager.getApplicationInfo(pkg, 0).flags and ApplicationInfo.FLAG_SYSTEM) == 0 }.getOrDefault(false)
    }
    val message = editor.message
    val notice = editor.notice
    val confirmClear = editor.confirmClear
    fun toggle(group: PermissionGroup) = editor.toggle(group)
    fun toggle(access: SpecialAccess) = editor.toggle(access)
    fun forceStop() = editor.forceStop()
    fun clearData() = editor.clearData()

    val d = details ?: return
    Column(
        Modifier
            .width(640.dp)
            .heightIn(max = 480.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(if (translucent) Ink.Panel.copy(alpha = 0.9f) else Ink.Panel)
            .padding(28.dp),
    ) {
        BasicText(d.label, style = Text.LargeTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (compact) {
            BasicText(stringResource(R.string.request_subtitle), style = Text.Callout, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
            val groups = if (onlyOp == null) d.groups.filter { it.key in asked.orEmpty() } else emptyList()
            val specials = d.special.filter { if (onlyOp != null) it.op == onlyOp else it.op.name in asked.orEmpty() }
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                groups.forEachIndexed { i, group ->
                    AccessRow(group.label, stateText(group), if (i == 0) first else null) { toggle(group) }
                }
                specials.forEachIndexed { i, access ->
                    AccessRow(stringResource(access.op.labelRes()), stringResource(if (access.allowed) R.string.perm_on else R.string.perm_off), if (groups.isEmpty() && i == 0) first else null) { toggle(access) }
                }
                AccessRow(stringResource(R.string.request_all_settings), null) { expanded = true }
            }
            StatusLines(message, notice)
        } else {
            val date = DateFormat.getDateInstance(DateFormat.MEDIUM)
            BasicText(d.packageName, style = Text.Callout, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            // Version and date on lines of their own: a version string can be as long as the package name.
            BasicText(
                stringResource(R.string.info_version, d.versionName.ifEmpty { "?" }, d.versionCode),
                style = Text.Callout, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                stringResource(R.string.info_installed, date.format(Date(d.installedAt))),
                style = Text.Callout, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            ActionRow {
                ActionButton(GlyphPaths.OPEN_IN_NEW, stringResource(R.string.action_open), focusRequester = first) { onOpen(pkg) }
                ActionButton(GlyphPaths.STOP_CIRCLE, stringResource(R.string.action_force_stop)) { forceStop() }
                ActionButton(
                    GlyphPaths.DELETE_SWEEP,
                    stringResource(if (confirmClear) R.string.action_clear_data_confirm else R.string.action_clear_data),
                    destructive = true,
                ) { clearData() }
                if (removable) ActionButton(GlyphPaths.DELETE, stringResource(R.string.action_uninstall), destructive = true) { onUninstall(pkg) }
            }
            val nothing = d.groups.isEmpty() && d.special.isEmpty()
            BasicText(
                stringResource(if (nothing) R.string.info_no_permissions else R.string.info_permissions),
                style = Text.Headline, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
            )
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                d.groups.forEach { group -> AccessRow(group.label, stateText(group)) { toggle(group) } }
                if (d.special.isNotEmpty()) {
                    BasicText(stringResource(R.string.info_special_access), style = Text.Headline, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                    d.special.forEach { access ->
                        AccessRow(stringResource(access.op.labelRes()), stringResource(if (access.allowed) R.string.perm_on else R.string.perm_off)) { toggle(access) }
                    }
                }
            }
            StatusLines(message, notice)
        }
    }
}

@Composable
private fun stateText(group: PermissionGroup) = stringResource(
    when {
        group.allGranted -> R.string.perm_on
        group.noneGranted -> R.string.perm_off
        else -> R.string.perm_partial
    },
)

fun AdbShell.AppOp.labelRes() = when (this) {
    AdbShell.AppOp.MANAGE_EXTERNAL_STORAGE -> R.string.access_all_files
    AdbShell.AppOp.SYSTEM_ALERT_WINDOW -> R.string.access_overlay
    AdbShell.AppOp.WRITE_SETTINGS -> R.string.access_write_settings
    AdbShell.AppOp.REQUEST_INSTALL_PACKAGES -> R.string.access_install
}
