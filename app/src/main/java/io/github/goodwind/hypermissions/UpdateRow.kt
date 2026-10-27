package io.github.goodwind.hypermissions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.goodwind.hypermissions.update.ApkInstaller
import io.github.goodwind.hypermissions.update.Downloads
import io.github.goodwind.hypermissions.update.HttpTransport
import io.github.goodwind.hypermissions.update.Update
import io.github.goodwind.hypermissions.update.UpdateChecker
import io.github.goodwind.hypermissions.update.UpdateResult
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val REPO = "goodwind/HyperMissions"

private sealed interface UpdateUi {
    /** A newer release; [failure] is why the last try at installing it did not work. */
    data class Offer(val update: Update, val failure: String? = null) : UpdateUi
    /** Being got: [text] says what is going on and the bar fills to [fraction]. */
    data class Busy(val update: Update, val text: String, val fraction: Float) : UpdateUi
}

/**
 * Asks GitHub once, when the screen opens, whether there is a newer release. Nothing shows unless there is;
 * then a row offers it: OK downloads it (checked against GitHub's digest) and hands it to the installer,
 * which asks for the confirmation.
 */
@Composable
fun UpdateRow(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<UpdateUi?>(null) }
    LaunchedEffect(Unit) {
        val installed = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
        val result = withContext(Dispatchers.IO) { UpdateChecker(HttpTransport(), REPO).check(installed) { it.startsWith("HyperMissions-") } }
        if (result is UpdateResult.Available) state = UpdateUi.Offer(result.update)
    }

    fun install(u: Update) {
        state = UpdateUi.Busy(u, context.getString(R.string.update_downloading), 0f)
        scope.launch {
            val file = File(context.cacheDir, "update/${u.apkName}").also { it.parentFile?.mkdirs() }
            try {
                withContext(Dispatchers.IO) {
                    Downloads.fetch(u.apkUrl, file, u.sha256) { done, total ->
                        if (total > 0) state = UpdateUi.Busy(u, context.getString(R.string.update_downloading), done.toFloat() / total)
                    }
                }
                state = UpdateUi.Busy(u, context.getString(R.string.update_installing), 1f)
                ApkInstaller.install(context, file) { problem -> if (problem != null) state = UpdateUi.Offer(u, problem) }
            } catch (e: Exception) {
                state = UpdateUi.Offer(u, e.message.orEmpty())
            }
        }
    }

    val s = state ?: return
    val update = when (s) { is UpdateUi.Offer -> s.update; is UpdateUi.Busy -> s.update }
    val failure = (s as? UpdateUi.Offer)?.failure
    Column(modifier.fillMaxWidth()) {
        // The words first, then under them one button the width of the column: it turns into the bar while the file comes.
        BasicText(
            failure?.let { stringResource(R.string.update_failed, it) } ?: stringResource(R.string.update_available, update.version),
            style = Text.Body.copy(color = if (failure != null) Ink.Orange else Ink.Label, fontWeight = FontWeight.Bold),
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(8.dp))
        when (s) {
            is UpdateUi.Busy -> BasicText(
                s.text,
                style = Text.Body.copy(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center),
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Ink.Fill)
                    .drawBehind { drawRect(Ink.Accent, size = Size(size.width * s.fraction.coerceIn(0f, 1f), size.height)) }
                    .padding(vertical = 10.dp),
            )
            is UpdateUi.Offer -> {
                val source = remember { MutableInteractionSource() }
                val focused by source.collectIsFocusedAsState()
                BasicText(
                    stringResource(R.string.update_install),
                    style = Text.Body.copy(color = if (focused) Ink.OnFocus else Ink.Label, fontSize = 14.sp, textAlign = TextAlign.Center),
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (focused) Ink.Focus else Ink.Fill)
                        .clickable(interactionSource = source, indication = null) { install(s.update) }
                        .padding(vertical = 10.dp),
                )
            }
        }
    }
}
