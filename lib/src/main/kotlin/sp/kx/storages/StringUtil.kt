package sp.kx.storages

internal fun Byte.toHEX(): String {
    return String.format("%02x", toInt().and(0xff))
}

internal fun ByteArray.toHEX(): String {
    return joinToString(separator = "") { it.toHEX() }
}
