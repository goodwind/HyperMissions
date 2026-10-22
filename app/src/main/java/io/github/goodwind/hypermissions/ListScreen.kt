package io.github.goodwind.hypermissions

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private class AppItem(val pkg: String, val label: String)

/** The apps as a grid of cards, to pick one; its panel replaces the grid while it is open, and Back returns to the grid. */
@Composable
fun ListScreen(onLaunch: (String) -> Unit, onUninstall: (String) -> Unit) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<AppItem>?>(null) }
    var showAll by rememberSaveable { mutableStateOf(false) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    // The app whose panel was closed last, to put focus back on its card.
    var lastSelected by rememberSaveable { mutableStateOf<String?>(null) }
    val gridState = rememberLazyGridState()

    LaunchedEffect(showAll) {
        apps = withContext(Dispatchers.IO) { loadApps(context, showAll) }
    }

    Box(Modifier.fillMaxSize().background(Ink.Background), contentAlignment = Alignment.Center) {
        val pkg = selected
        if (pkg != null) {
            DetailPanel(
                pkg = pkg,
                requestOnly = false,
                onlyOp = null,
                translucent = false,
                onOpen = onLaunch,
                onUninstall = onUninstall,
                onClose = {
                    lastSelected = pkg
                    selected = null
                },
            )
        } else {
            Column(Modifier.fillMaxSize().padding(horizontal = 48.dp).padding(top = 28.dp)) {
                BasicText(stringResource(R.string.app_name), style = Text.LargeTitle)
                Row(Modifier.padding(top = 12.dp, bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Chip(stringResource(R.string.filter_installed), selected = !showAll) { showAll = false }
                    Chip(stringResource(R.string.filter_all), selected = showAll) { showAll = true }
                }
                val list = apps.orEmpty()
                val restore = remember { FocusRequester() }
                LaunchedEffect(list.size) {
                    if (list.isNotEmpty()) {
                        withFrameNanos { }
                        runCatching { restore.requestFocus() }
                    }
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(COLUMNS),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(list, key = { it.pkg }) { item ->
                        val focusHere = if (lastSelected != null) item.pkg == lastSelected else item === list.first()
                        AppCard(item, if (focusHere) restore else null) { selected = item.pkg }
                    }
                }
            }
        }
    }
}

private const val COLUMNS = 5

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    FocusRow(onClick, modifier = Modifier.width(190.dp)) { focused ->
        val fg = if (focused) Ink.OnFocus else Ink.Label
        BasicText(label, style = Text.Body.copy(color = fg), modifier = Modifier.weight(1f))
        if (selected) BasicText("●", style = Text.Body.copy(color = fg))
    }
}

/** An app as a card: its icon, its name under it, and how many of its permissions are on. */
@Composable
private fun AppCard(item: AppItem, requester: FocusRequester?, onClick: () -> Unit) {
    val context = LocalContext.current
    val icon by produceState<ImageBitmap?>(null, item.pkg) {
        value = withContext(Dispatchers.IO) {
            runCatching { context.packageManager.getApplicationIcon(item.pkg).toBitmap(160, 160).asImageBitmap() }.getOrNull()
        }
    }
    // How many of its permissions and accesses are fully on, out of those it has (null while loading).
    val counts by produceState<Pair<Int, Int>?>(null, item.pkg) {
        value = withContext(Dispatchers.IO) {
            AppDetails.load(context, item.pkg)?.let { d ->
                (d.groups.count { it.allGranted } + d.special.count { it.allowed }) to (d.groups.size + d.special.size)
            }
        }
    }
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val label = if (focused) Ink.OnFocus else Ink.Label
    val secondary = if (focused) Ink.OnFocus else Ink.Secondary
    Column(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .let { if (requester != null) it.focusRequester(requester) else it }
            .clip(RoundedCornerShape(16.dp))
            .background(if (focused) Ink.Focus else Ink.Fill)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val bitmap = icon
        if (bitmap != null) {
            Image(bitmap, null, Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)))
        } else {
            Spacer(Modifier.size(64.dp))
        }
        Spacer(Modifier.height(8.dp))
        BasicText(
            item.label,
            style = Text.Body.copy(color = label, textAlign = TextAlign.Center, fontSize = 14.sp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.weight(1f))
        BasicText(
            counts?.takeIf { it.second > 0 }?.let { stringResource(R.string.list_on, it.first, it.second) } ?: " ",
            style = Text.Callout.copy(color = secondary, fontSize = 12.sp),
            maxLines = 1,
        )
    }
}

/** User-installed apps (updated system apps included), or every package with the permissions it asks for. */
private fun loadApps(context: Context, all: Boolean): List<AppItem> {
    val pm = context.packageManager
    @Suppress("DEPRECATION")
    return pm.getInstalledApplications(0)
        .filter { info ->
            all || (info.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        }
        .map { AppItem(it.packageName, runCatching { it.loadLabel(pm).toString() }.getOrDefault(it.packageName)) }
        .sortedBy { it.label.lowercase() }
}
