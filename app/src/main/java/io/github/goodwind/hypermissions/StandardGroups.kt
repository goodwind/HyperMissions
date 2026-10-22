package io.github.goodwind.hypermissions

import androidx.annotation.StringRes

/**
 * Android's runtime permission groups, with names in the app's languages. The permissions are listed
 * here because firmware can report them as ungrouped (Xiaomi HyperOS on MiTV-MFTR0 does for the
 * storage and contacts ones), and because its labels for single permissions come only in English.
 * A permission that is in none of these keeps the system's own label.
 */
enum class StandardGroups(@StringRes val label: Int, vararg val permissions: String) {
    CALENDAR(R.string.grp_calendar, "READ_CALENDAR", "WRITE_CALENDAR"),
    CALL_LOG(R.string.grp_call_log, "READ_CALL_LOG", "WRITE_CALL_LOG", "PROCESS_OUTGOING_CALLS"),
    CAMERA(R.string.grp_camera, "CAMERA"),
    CONTACTS(R.string.grp_contacts, "READ_CONTACTS", "WRITE_CONTACTS", "GET_ACCOUNTS"),
    LOCATION(R.string.grp_location, "ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION", "ACCESS_BACKGROUND_LOCATION"),
    MICROPHONE(R.string.grp_microphone, "RECORD_AUDIO"),
    PHONE(
        R.string.grp_phone, "READ_PHONE_STATE", "READ_PHONE_NUMBERS", "CALL_PHONE", "ANSWER_PHONE_CALLS", "ADD_VOICEMAIL",
        "USE_SIP", "ACCEPT_HANDOVER",
    ),
    SENSORS(R.string.grp_sensors, "BODY_SENSORS"),
    SMS(R.string.grp_sms, "SEND_SMS", "RECEIVE_SMS", "READ_SMS", "RECEIVE_WAP_PUSH", "RECEIVE_MMS"),
    STORAGE(
        R.string.grp_storage, "READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE", "ACCESS_MEDIA_LOCATION",
        "READ_MEDIA_IMAGES", "READ_MEDIA_VIDEO", "READ_MEDIA_AUDIO",
    ),
    ACTIVITY(R.string.grp_activity, "ACTIVITY_RECOGNITION"),
    NEARBY(R.string.grp_nearby, "BLUETOOTH_CONNECT", "BLUETOOTH_SCAN", "BLUETOOTH_ADVERTISE", "NEARBY_WIFI_DEVICES", "UWB_RANGING"),
    NOTIFICATIONS(R.string.grp_notifications, "POST_NOTIFICATIONS");

    /** Marks a [PermissionGroup] as one of these; not an Android group name, so it cannot clash with one. */
    val key get() = "std:$name"

    companion object {
        private val byName = entries.flatMap { g -> g.permissions.map { "android.permission.$it" to g } }.toMap()

        fun of(permission: String): StandardGroups? = byName[permission]

        fun byKey(key: String): StandardGroups? = if (key.startsWith("std:")) entries.firstOrNull { it.key == key } else null
    }
}
