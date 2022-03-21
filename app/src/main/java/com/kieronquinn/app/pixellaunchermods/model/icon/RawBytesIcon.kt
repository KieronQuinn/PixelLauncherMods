package com.kieronquinn.app.pixellaunchermods.model.icon

data class RawBytesIcon(val bytes: ByteArray, val size: Int) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RawBytesIcon

        if (!bytes.contentEquals(other.bytes)) return false
        if (size != other.size) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + size
        return result
    }

}
