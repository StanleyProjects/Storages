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

    fun writeBytes(bytes: ByteArray, index: Int, value: Int) {
        bytes[index] = value.shr(8 * 3).toByte()
        bytes[index + 1] = value.shr(8 * 2).toByte()
        bytes[index + 2] = value.shr(8).toByte()
        bytes[index + 3] = value.toByte()
    }

    fun readInt(bytes: ByteArray, index: Int): Int {
        return bytes[index].toInt().and(0xff).shl(8 * 3)
            .or(bytes[index + 1].toInt().and(0xff).shl(8 * 2))
            .or(bytes[index + 2].toInt().and(0xff).shl(8 * 1))
            .or(bytes[index + 3].toInt().and(0xff))
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

    fun readLong(bytes: ByteArray, index: Int): Long {
        return bytes[index].toLong().and(0xff).shl(8 * 7)
            .or(bytes[index + 1].toLong().and(0xff).shl(8 * 6))
            .or(bytes[index + 2].toLong().and(0xff).shl(8 * 5))
            .or(bytes[index + 3].toLong().and(0xff).shl(8 * 4))
            .or(bytes[index + 4].toLong().and(0xff).shl(8 * 3))
            .or(bytes[index + 5].toLong().and(0xff).shl(8 * 2))
            .or(bytes[index + 6].toLong().and(0xff).shl(8 * 1))
            .or(bytes[index + 7].toLong().and(0xff))
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

    fun readUUID(bytes: ByteArray, index: Int): UUID {
        return UUID(readLong(bytes = bytes, index = index), readLong(bytes = bytes, index = index + 8))
    }

    fun readBytes(stream: InputStream, size: Int): ByteArray {
        val bytes = ByteArray(size)
        stream.read(bytes)
        return bytes
    }
}
