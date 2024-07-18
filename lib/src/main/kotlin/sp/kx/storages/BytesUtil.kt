package sp.kx.storages

import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

internal object BytesUtil {
    fun writeBytes(stream: OutputStream, value: Int) {
        stream.write(value.shr(8 * 3).toByte().toInt())
        stream.write(value.shr(8 * 2).toByte().toInt())
        stream.write(value.shr(8 * 1).toByte().toInt())
        stream.write(value.toByte().toInt())
    }

    fun readInt(stream: InputStream): Int {
        return stream.read().and(0xff).shl(8 * 3)
            .or(stream.read().and(0xff).shl(8 * 2))
            .or(stream.read().and(0xff).shl(8 * 1))
            .or(stream.read().and(0xff))
    }

    fun writeBytes(stream: OutputStream, value: Long) {
        stream.write(value.shr(8 * 7).toByte().toInt())
        stream.write(value.shr(8 * 6).toByte().toInt())
        stream.write(value.shr(8 * 5).toByte().toInt())
        stream.write(value.shr(8 * 4).toByte().toInt())
        stream.write(value.shr(8 * 3).toByte().toInt())
        stream.write(value.shr(8 * 2).toByte().toInt())
        stream.write(value.shr(8 * 1).toByte().toInt())
        stream.write(value.toByte().toInt())
    }

    fun readLong(stream: InputStream): Long {
        return stream.read().toLong().and(0xff).shl(8 * 7)
            .or(stream.read().toLong().and(0xff).shl(8 * 6))
            .or(stream.read().toLong().and(0xff).shl(8 * 5))
            .or(stream.read().toLong().and(0xff).shl(8 * 4))
            .or(stream.read().toLong().and(0xff).shl(8 * 3))
            .or(stream.read().toLong().and(0xff).shl(8 * 2))
            .or(stream.read().toLong().and(0xff).shl(8 * 1))
            .or(stream.read().toLong().and(0xff))
    }

    fun writeBytes(stream: OutputStream, value: UUID) {
        writeBytes(stream, value.mostSignificantBits)
        writeBytes(stream, value.leastSignificantBits)
    }

    fun readUUID(stream: InputStream): UUID {
        return UUID(readLong(stream), readLong(stream))
    }
}
