package io.github.goodwind.hypermissions

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/** [on] of [total] permissions and accesses are fully on; none to switch when [total] is 0. */
private class AppItem(val pkg: String, val label: String, val on: Int, val total: Int)

/**
 * The apps in a column on the left, with the filters over it; on the right, the app under the remote's focus
 * with all its permissions as sliders. OK (or Right) on the list moves into the right column, Left or Back returns.
 */
@Composable
fun ListScreen(onLaunch: (String) -> Unit, onUninstall: (String) -> Unit) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<AppItem>?>(null) }
    var showAll by rememberSaveable { mutableStateOf(false) }
    var onlyWithPermissions by rememberSaveable { mutableStateOf(true) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var rightFocused by remember { mutableStateOf(false) }
    val selectedRow = remember { FocusRequester() }
    val rightFirst = remember { FocusRequester() }

    LaunchedEffect(showAll) {
        apps = withContext(Dispatchers.IO) { loadApps(context, showAll) }
    }
    val visible = apps.orEmpty().filter { !onlyWithPermissions || it.total > 0 }
    val shown = visible.firstOrNull { it.pkg == selected } ?: visible.firstOrNull()
    LaunchedEffect(visible.isNotEmpty()) {
        if (visible.isNotEmpty()) {
            withFrameNanos { }
            runCatching { selectedRow.requestFocus() }
        }
    }
    BackHandler(enabled = rightFocused) { runCatching { selectedRow.requestFocus() } }

    Column(Modifier.fillMaxSize().background(Ink.Background)) {
        Row(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 48.dp).padding(top = 24.dp)) {
            Column(Modifier.width(330.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.Image(painterResource(R.drawable.ic_launcher), contentDescription = null, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(12.dp))
                    BasicText(stringResource(R.string.app_name), style = Text.LargeTitle)
                }
                UpdateRow(Modifier.padding(top = 6.dp))
                SwitchRow(
                    stringResource(R.string.filter_show_system),
                    if (showAll) Position.On else Position.Off,
                    Modifier.padding(top = 10.dp),
                ) { showAll = !showAll }
                SwitchRow(
                    stringResource(R.string.filter_only_with_permissions),
                    if (onlyWithPermissions) Position.On else Position.Off,
                    Modifier.padding(bottom = 8.dp),
                ) { onlyWithPermissions = !onlyWithPermissions }
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(visible, key = { it.pkg }) { item ->
                        AppRow(
                            item,
                            modifier = Modifier
                                .onFocusChanged { if (it.isFocused) selected = item.pkg }
                                .onPreviewKeyEvent {
                                    if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionRight) {
                                        runCatching { rightFirst.requestFocus() }
                                        true
                                    } else {
                                        false
                                    }
                                },
                            requester = if (item.pkg == shown?.pkg) selectedRow else null,
                        ) { runCatching { rightFirst.requestFocus() } }
                    }
                }
            }
            Spacer(Modifier.width(32.dp))
            shown?.let {
                AppPane(
                    pkg = it.pkg,
                    first = rightFirst,
                    back = selectedRow,
                    onOpen = onLaunch,
                    onUninstall = onUninstall,
                    modifier = Modifier.weight(1f).fillMaxSize().onFocusChanged { s -> rightFocused = s.hasFocus },
                )
            } ?: Spacer(Modifier.weight(1f))
        }
        Footer()
    }
}

/** Who made it, under what licence, and where the source is: all proper names, so it is not translated. */
@Composable
private fun Footer() {
    val context = LocalContext.current
    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty() }
    BasicText(
        "HyperMissions $version · © 2026 Alexandr Serdtsev · Apache License 2.0 · github.com/goodwind/HyperMissions",
        style = Text.Callout.copy(fontSize = 12.sp, textAlign = TextAlign.Center),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    )
}

/** An app in the column: its icon, its name, and how many of its permissions are on. */
@Composable
private fun AppRow(item: AppItem, modifier: Modifier, requester: FocusRequester?, onClick: () -> Unit) {
    val context = LocalContext.current
    val icon by produceState<ImageBitmap?>(null, item.pkg) {
        value = withContext(Dispatchers.IO) {
            runCatching { context.packageManager.getApplicationIcon(item.pkg).toBitmap(96, 96).asImageBitmap() }.getOrNull()
        }
    }
    FocusRow(onClick, modifier = modifier, focusRequester = requester) { focused ->
        val bitmap = icon
        if (bitmap != null) Image(bitmap, null, Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))) else Spacer(Modifier.size(32.dp))
        Spacer(Modifier.width(12.dp))
        BasicText(
            item.label,
            style = Text.Body.copy(color = if (focused) Ink.OnFocus else Ink.Label, fontSize = 15.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (item.total > 0) {
            BasicText("${item.on}/${item.total}", style = Text.Callout.copy(color = if (focused) Ink.OnFocus else Ink.Secondary, fontSize = 13.sp))
        }
    }
}

/** User-installed apps (updated system apps included), or every package; each with how many of its permissions are on. */
private suspend fun loadApps(context: Context, all: Boolean): List<AppItem> {
    val pm = context.packageManager
    @Suppress("DEPRECATION")
    val infos = pm.getInstalledApplications(0).filter { info ->
        all || (info.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    }
    return coroutineScope {
        infos.map { info ->
            async(Dispatchers.IO) {
                val label = runCatching { info.loadLabel(pm).toString() }.getOrDefault(info.packageName)
                val d = AppDetails.load(context, info.packageName)
                AppItem(info.packageName, label, d?.let { it.groups.count { g -> g.allGranted } + it.special.count { s -> s.allowed } } ?: 0, d?.let { it.groups.size + it.special.size } ?: 0)
            }
        }.awaitAll()
    }.sortedBy { it.label.lowercase() }
}
