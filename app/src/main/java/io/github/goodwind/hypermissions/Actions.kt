package io.github.goodwind.hypermissions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/** Opens [pkg] the way a launcher does; false when it has nothing to open. */
fun launchApp(context: Context, pkg: String): Boolean {
    val pm = context.packageManager
    val intent = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)
    if (intent == null) {
        Toast.makeText(context, R.string.nothing_to_open, Toast.LENGTH_SHORT).show()
        return false
    }
    return runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
}

/** Hands uninstalling [pkg] to the package installer, which asks for the confirmation. */
fun uninstallApp(context: Context, pkg: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_DELETE, Uri.fromParts("package", pkg, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
