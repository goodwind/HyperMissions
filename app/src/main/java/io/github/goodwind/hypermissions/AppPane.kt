package io.github.goodwind.hypermissions

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.content.pm.ApplicationInfo
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

/**
 * The right column: what the app on the left is, what can be done to it, and all its permissions and special
 * accesses as sliders. [first] is where the remote's focus goes when OK is pressed on the list; [back] is the
 * row of the list that Left returns to.
 */
@Composable
fun AppPane(
    pkg: String,
    first: FocusRequester,
    back: FocusRequester,
    onOpen: (String) -> Unit,
    onUninstall: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val editor = rememberPermissionEditor(pkg)
    // A short wait, so that running down the list does not read every app it passes.
    LaunchedEffect(pkg) {
        delay(120)
        editor.load()
    }
    LaunchedEffect(editor.confirmClear) {
        if (editor.confirmClear) {
            delay(4_000)
            editor.forgetConfirm()
        }
    }
    val icon by produceState<ImageBitmap?>(null, pkg) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { context.packageManager.getApplicationIcon(pkg).toBitmap(160, 160).asImageBitmap() }.getOrNull()
        }
    }
    val removable = remember(pkg) {
        runCatching { (context.packageManager.getApplicationInfo(pkg, 0).flags and ApplicationInfo.FLAG_SYSTEM) == 0 }.getOrDefault(false)
    }
    val d = editor.details
    // The panel keeps still while the next app loads; a different app's details are never shown under this one.
    if (d == null || d.packageName != pkg) {
        Column(modifier) { Spacer(Modifier.fillMaxSize()) }
        return
    }
    val toLeft = Modifier.focusProperties { left = back }
    Column(modifier) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            icon?.let { Image(it, null, Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))) } ?: Spacer(Modifier.size(56.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                BasicText(d.label, style = Text.LargeTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                BasicText(d.packageName, style = Text.Callout, maxLines = 1, overflow = TextOverflow.Ellipsis)
                BasicText(
                    stringResource(R.string.info_version, d.versionName.ifEmpty { "?" }, d.versionCode) + " · " +
                        stringResource(R.string.info_installed, DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(d.installedAt))),
                    style = Text.Callout, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButton(GlyphPaths.OPEN_IN_NEW, stringResource(R.string.action_open), focusRequester = first, width = 118.dp, modifier = toLeft) { onOpen(pkg) }
            ActionButton(GlyphPaths.STOP_CIRCLE, stringResource(R.string.action_force_stop), width = 118.dp) { editor.forceStop() }
            ActionButton(
                GlyphPaths.DELETE_SWEEP,
                stringResource(if (editor.confirmClear) R.string.action_clear_data_confirm else R.string.action_clear_data),
                destructive = true, width = 118.dp,
            ) { editor.clearData() }
            if (removable) ActionButton(GlyphPaths.DELETE, stringResource(R.string.action_uninstall), destructive = true, width = 118.dp) { onUninstall(pkg) }
        }
        val nothing = d.groups.isEmpty() && d.special.isEmpty()
        BasicText(
            stringResource(if (nothing) R.string.info_no_permissions else R.string.info_permissions),
            style = Text.Headline, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
        )
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
            d.groups.forEach { group -> SwitchRow(group.label, positionOf(group), toLeft) { editor.toggle(group) } }
            if (d.special.isNotEmpty()) {
                BasicText(stringResource(R.string.info_special_access), style = Text.Headline, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                d.special.forEach { access ->
                    SwitchRow(stringResource(access.op.labelRes()), if (access.allowed) Position.On else Position.Off, toLeft) { editor.toggle(access) }
                }
            }
        }
        StatusLines(editor.message, editor.notice)
    }
}

private fun positionOf(group: PermissionGroup) = when {
    group.allGranted -> Position.On
    group.noneGranted -> Position.Off
    else -> Position.Partial
}
