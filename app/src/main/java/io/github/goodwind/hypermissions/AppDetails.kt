package io.github.goodwind.hypermissions

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build

/** The runtime permissions of one kind (camera, location...), which Android grants together. */
data class PermissionGroup(val key: String, val label: String, val permissions: List<String>, val granted: Int) {
    val allGranted get() = granted == permissions.size
    val noneGranted get() = granted == 0
}

data class SpecialAccess(val op: AdbShell.AppOp, val allowed: Boolean)

/**
 * What the system's App Info screen shows, for TVs whose firmware has none (Xiaomi HyperOS on
 * MiTV-MFTR0, Android 11, has no handler for ACTION_APPLICATION_DETAILS_SETTINGS).
 */
data class AppDetails(
    val label: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val installedAt: Long,
    val updatedAt: Long,
    val groups: List<PermissionGroup>,
    /** Accesses the app asks for that are not runtime permissions (All files access...). */
    val special: List<SpecialAccess>,
) {
    companion object {
        /** Null when the package is gone. Reads only; changing a permission is [AdbShell]. */
        fun load(context: Context, pkg: String): AppDetails? {
            val pm = context.packageManager
            val info = try {
                pm.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS)
            } catch (e: PackageManager.NameNotFoundException) {
                return null
            }
            val label = runCatching { info.applicationInfo?.loadLabel(pm)?.toString() }.getOrNull() ?: pkg
            return AppDetails(
                label = label,
                packageName = pkg,
                versionName = info.versionName.orEmpty(),
                versionCode = versionCode(info),
                installedAt = info.firstInstallTime,
                updatedAt = info.lastUpdateTime,
                groups = groups(context, pm, info),
                special = special(context, info),
            )
        }

        /**
         * Runtime permissions are not the only way to reach files or draw over other apps: All files
         * access (MANAGE_EXTERNAL_STORAGE) is an app op, and File Manager+ kept seeing storage with
         * its storage permissions revoked while that was on.
         */
        private fun special(context: Context, info: PackageInfo): List<SpecialAccess> {
            // The app op constants are Android 10+, and All files access is Android 11+.
            if (Build.VERSION.SDK_INT < 29) return emptyList()
            val requested = info.requestedPermissions?.toSet() ?: return emptyList()
            val uid = info.applicationInfo?.uid ?: return emptyList()
            val ops = context.getSystemService(AppOpsManager::class.java) ?: return emptyList()
            return AdbShell.AppOp.entries.filter { it.permission in requested }.map { op ->
                val mode = runCatching { ops.unsafeCheckOpNoThrow(op.opString, uid, info.packageName) }.getOrNull()
                SpecialAccess(op, allowed = mode == AppOpsManager.MODE_ALLOWED)
            }
        }

        @Suppress("DEPRECATION")
        private fun versionCode(info: PackageInfo): Long =
            if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()

        @Suppress("DEPRECATION")
        private fun isRuntime(info: PermissionInfo) =
            info.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE == PermissionInfo.PROTECTION_DANGEROUS

        private fun groups(context: Context, pm: PackageManager, info: PackageInfo): List<PermissionGroup> {
            val requested = info.requestedPermissions ?: return emptyList()
            val flags = info.requestedPermissionsFlags ?: IntArray(requested.size)
            // Permission name -> group key (or the permission's own name when it has no group) -> granted.
            val byGroup = LinkedHashMap<String, MutableList<Pair<String, Boolean>>>()
            requested.forEachIndexed { i, name ->
                val perm = runCatching { pm.getPermissionInfo(name, 0) }.getOrNull() ?: return@forEachIndexed
                if (!isRuntime(perm)) return@forEachIndexed
                val granted = flags.getOrElse(i) { 0 } and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0
                // Permissions Android keeps in no real group (group null or UNDEFINED) get a row each.
                // Android's own groups, which some firmware reports as "undefined" or leaves out, and whose
                // names the firmware may not have in the TV's language: ours are.
                val group = StandardGroups.of(name)?.key ?: perm.group?.takeUnless { it.endsWith(".UNDEFINED") } ?: name
                byGroup.getOrPut(group) { mutableListOf() }.add(name to granted)
            }
            return byGroup.map { (key, perms) ->
                val label = StandardGroups.byKey(key)?.let { context.getString(it.label) } ?: groupLabel(pm, key)
                // A background grant needs the foreground one first.
                val names = perms.map { it.first }.sortedBy { it.endsWith("BACKGROUND_LOCATION") }
                PermissionGroup(key, label, names, perms.count { it.second })
            }.sortedBy { it.label.lowercase() }
        }

        /**
         * The system's own label, which comes in the TV's language: the group's, or for a permission
         * that is in no group, the permission's. The label can come back as the raw key when the
         * firmware lacks the string; then the key's last part, as words, in English.
         */
        private fun groupLabel(pm: PackageManager, key: String): String {
            val group = runCatching { pm.getPermissionGroupInfo(key, 0).loadLabel(pm).toString() }.getOrNull()
            if (!group.isNullOrBlank() && group != key) return group
            val own = runCatching { pm.getPermissionInfo(key, 0).loadLabel(pm).toString() }.getOrNull()
            if (!own.isNullOrBlank() && own != key && !own.startsWith("android.permission")) return own.replaceFirstChar { it.uppercase() }
            return key.substringAfterLast('.').replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
        }
    }
}
