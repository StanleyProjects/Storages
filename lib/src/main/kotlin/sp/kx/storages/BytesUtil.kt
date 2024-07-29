package sp.kx.storages

import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

@Suppress("MagicNumber")
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

    fun writeBytes(bytes: ByteArray, index: Int, value: Long) {
        bytes[index] = value.shr(8 * 7).toByte()
        bytes[index + 1] = value.shr(8 * 6).toByte()
        bytes[index + 2] = value.shr(8 * 5).toByte()
        bytes[index + 3] = value.shr(8 * 4).toByte()
        bytes[index + 4] = value.shr(8 * 3).toByte()
        bytes[index + 5] = value.shr(8 * 2).toByte()
        bytes[index + 6] = value.shr(8).toByte()
        bytes[index + 7] = value.toByte()
    }

    fun writeBytes(bytes: ByteArray, index: Int, value: UUID) {
        writeBytes(bytes, index = index, value.mostSignificantBits)
        writeBytes(bytes, index = index + 8, value.leastSignificantBits)
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

    fun readBytes(stream: InputStream, size: Int): ByteArray {
        val bytes = ByteArray(size)
        stream.read(bytes)
        return bytes
    }
}
