package io.github.goodwind.hypermissions

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RequestEntry.update(this)
        setContent {
            CompositionLocalProvider(LocalDensity provides canvasDensity(this)) {
                ListScreen(onLaunch = { launchApp(this, it) }, onUninstall = { uninstallApp(this, it) })
            }
        }
    }
}

/** TVs ship at different densities for the same 1080p panel; the layout is drawn on a 960×540 dp canvas. */
fun canvasDensity(activity: ComponentActivity): Density {
    val m = activity.resources.displayMetrics
    return Density(density = maxOf(m.widthPixels, m.heightPixels) / 960f, fontScale = 1f)
}
