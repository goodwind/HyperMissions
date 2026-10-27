package io.github.goodwind.hypermissions.update

/** A version such as `0.2.1` or a tag `v0.2.1`, as numbers that compare as numbers. */
data class Version(val parts: List<Int>) : Comparable<Version> {
    override fun compareTo(other: Version): Int {
        for (i in 0 until maxOf(parts.size, other.parts.size)) {
            val c = parts.getOrElse(i) { 0 }.compareTo(other.parts.getOrElse(i) { 0 })
            if (c != 0) return c
        }
        return 0
    }

    override fun toString() = parts.joinToString(".")

    companion object {
        private val leading = Regex("^[vV]?(\\d+(?:\\.\\d+)*)")

        /** The numbers at the start of [text] (a suffix such as `-rc1` is ignored), or null if there are none. */
        fun parse(text: String?): Version? =
            leading.find(text.orEmpty().trim())?.groupValues?.get(1)?.split('.')?.map { it.toInt() }?.let(::Version)
    }
}
