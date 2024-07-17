package sp.kx.storages

internal fun Int.toHEX(): String {
    return String.format("%02x", this and 0xff)
}

internal fun ByteArray.toHEX(): String {
    return joinToString(separator = "") { it.toInt().toHEX() }
}
