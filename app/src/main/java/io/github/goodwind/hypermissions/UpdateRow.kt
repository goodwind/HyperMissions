package io.github.goodwind.hypermissions

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    data class Busy(val text: String) : UpdateUi
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
        state = UpdateUi.Busy(context.getString(R.string.update_downloading, 0))
        scope.launch {
            val file = File(context.cacheDir, "update/${u.apkName}").also { it.parentFile?.mkdirs() }
            try {
                withContext(Dispatchers.IO) {
                    Downloads.fetch(u.apkUrl, file, u.sha256) { done, total ->
                        if (total > 0) state = UpdateUi.Busy(context.getString(R.string.update_downloading, (done * 100 / total).toInt()))
                    }
                }
                state = UpdateUi.Busy(context.getString(R.string.update_installing))
                ApkInstaller.install(context, file) { problem -> if (problem != null) state = UpdateUi.Offer(u, problem) }
            } catch (e: Exception) {
                state = UpdateUi.Offer(u, e.message.orEmpty())
            }
        }
    }

    when (val s = state) {
        null -> Unit
        is UpdateUi.Busy -> BasicText(s.text, style = Text.Body, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier.padding(horizontal = 14.dp, vertical = 12.dp))
        is UpdateUi.Offer -> FocusRow({ install(s.update) }, modifier = modifier) { focused ->
            val fg = if (focused) Ink.OnFocus else Ink.Label
            BasicText(
                s.failure?.let { stringResource(R.string.update_failed, it) } ?: stringResource(R.string.update_available, s.update.version),
                style = Text.Body.copy(color = fg), maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(vertical = 6.dp),
            )
            BasicText(stringResource(R.string.update_install), style = Text.Body.copy(color = if (focused) Ink.OnFocus else Ink.Accent))
        }
    }
}
