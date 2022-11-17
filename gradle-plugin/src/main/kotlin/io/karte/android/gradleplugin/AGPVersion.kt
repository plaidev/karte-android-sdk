package io.karte.android.gradleplugin

data class AGPVersion(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<AGPVersion> {

    override fun compareTo(other: AGPVersion): Int {
        if (this.major < other.major) {
            return -1
        } else if (this.major > other.major) {
            return 1
        }
        if (this.minor < other.minor) {
            return -1
        } else if (this.minor > other.minor) {
            return 1
        }
        if (this.patch < other.patch) {
            return -1
        } else if (this.patch > other.patch) {
            return 1
        }
        return 0
    }

    companion object {
        val VERSION_7_2_2 = AGPVersion(7, 2, 2)

        private val versionPattern = Regex(
            """(0|[1-9]\d*)?(?:\.)?(0|[1-9]\d*)?(?:\.)?(0|[1-9]\d*)?(?:-([\dA-z\-]+(?:\.[\dA-z\-]+)*))?(?:\+([\dA-z\-]+(?:\.[\dA-z\-]+)*))?"""
        )

        fun fromVersionString(version: String): AGPVersion {
            val result = versionPattern.matchEntire(version) ?: throw IllegalArgumentException("Invalid version string: $version")
            val values = result.groupValues
            return AGPVersion(
                values[1].toIntOrNull() ?: 0,
                values[2].toIntOrNull() ?: 0,
                values[3].toIntOrNull() ?: 0
            )
        }
    }
}
