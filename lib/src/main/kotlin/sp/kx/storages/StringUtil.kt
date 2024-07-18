package sp.kx.storages

import java.util.Locale

internal fun Byte.toHEX(): String {
    return String.format(Locale.US, "%02x", toInt().and(0xff))
}

internal fun ByteArray.toHEX(): String {
    return joinToString(separator = "") { it.toHEX() }
}
