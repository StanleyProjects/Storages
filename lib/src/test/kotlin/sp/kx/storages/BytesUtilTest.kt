package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.UUID

internal class BytesUtilTest {
    @Test
    fun writeBytesIntTest() {
        val number = 110485624
        val stream = ByteArrayOutputStream()
        BytesUtil.writeBytes(stream, number)
        val bytes = stream.toByteArray()
        assertEquals(4, bytes.size)
        assertEquals(0x06.toByte(), bytes[0])
        assertEquals(0x95.toByte(), bytes[1])
        assertEquals(0xe0.toByte(), bytes[2])
        assertEquals(0x78.toByte(), bytes[3])
    }

    @Test
    fun writeToArrayIntTest() {
        val number: Int = 123456789
        val bytes = ByteArray(32)
        assertTrue(bytes.all { it == 0.toByte() })
        BytesUtil.writeBytes(bytes = bytes, index = 4, value = number)
        assertEquals(bytes.size, 32)
        assertTrue(bytes.slice(0 until 4).all { it == 0.toByte() })
        assertTrue(bytes.slice((4 + 16) until 32).all { it == 0.toByte() })
        val slice = bytes.sliceArray(4 until (4 + 16))
        assertEquals(slice.size, 16)
        val actual: Int = BytesUtil.readInt(slice.inputStream())
        assertEquals(number, actual)
    }

    @Test
    fun readIntTest() {
        val bytes = byteArrayOf(
            0x05.toByte(),
            0x18.toByte(),
            0x9e.toByte(),
            0xe5.toByte(),
        )
        assertEquals(85499621, BytesUtil.readInt(ByteArrayInputStream(bytes)))
    }

    @Test
    fun readIntFromArrayTest() {
        val bytes = byteArrayOf(
            0,
            0x05.toByte(),
            0x18.toByte(),
            0x9e.toByte(),
            0xe5.toByte(),
            0
        )
        check(bytes.size == 4 + 2)
        val actual: Int = BytesUtil.readInt(bytes = bytes, index = 1)
        assertEquals(85499621, actual)
    }

    @Test
    fun writeBytesLongTest() {
        val number = 48799863196986557
        val stream = ByteArrayOutputStream()
        BytesUtil.writeBytes(stream, number)
        val bytes = stream.toByteArray()
        assertEquals(8, bytes.size)
        assertEquals(0x00.toByte(), bytes[0])
        assertEquals(0xad.toByte(), bytes[1])
        assertEquals(0x5f.toByte(), bytes[2])
        assertEquals(0x37.toByte(), bytes[3])
        assertEquals(0x8e.toByte(), bytes[4])
        assertEquals(0xf1.toByte(), bytes[5])
        assertEquals(0xac.toByte(), bytes[6])
        assertEquals(0xbd.toByte(), bytes[7])
    }

    @Test
    fun readLongTest() {
        val bytes = byteArrayOf(
            0x00.toByte(),
            0xe7.toByte(),
            0x74.toByte(),
            0x25.toByte(),
            0x4e.toByte(),
            0xe4.toByte(),
            0xc2.toByte(),
            0x7b.toByte(),
        )
        assertEquals(65148423206388347, BytesUtil.readLong(ByteArrayInputStream(bytes)))
    }

    @Test
    fun readLongFromArrayTest() {
        val bytes = byteArrayOf(
            0,
            0x00.toByte(),
            0xe7.toByte(),
            0x74.toByte(),
            0x25.toByte(),
            0x4e.toByte(),
            0xe4.toByte(),
            0xc2.toByte(),
            0x7b.toByte(),
            0
        )
        check(bytes.size == 8 + 2)
        val actual: Long = BytesUtil.readLong(bytes = bytes, index = 1)
        assertEquals(65148423206388347, actual)
    }

    @Test
    fun writeBytesUUIDTest() {
        val id = UUID(201043908171802077, 56416449117839213)
        val stream = ByteArrayOutputStream()
        BytesUtil.writeBytes(stream, id)
        val bytes = stream.toByteArray()
        assertEquals(16, bytes.size)
        assertEquals(0x02.toByte(), bytes[0])
        assertEquals(0xca.toByte(), bytes[1])
        assertEquals(0x40.toByte(), bytes[2])
        assertEquals(0x5e.toByte(), bytes[3])
        assertEquals(0x8a.toByte(), bytes[4])
        assertEquals(0xd6.toByte(), bytes[5])
        assertEquals(0x25.toByte(), bytes[6])
        assertEquals(0xdd.toByte(), bytes[7])
        assertEquals(0x00.toByte(), bytes[8])
        assertEquals(0xc8.toByte(), bytes[9])
        assertEquals(0x6e.toByte(), bytes[10])
        assertEquals(0x76.toByte(), bytes[11])
        assertEquals(0x29.toByte(), bytes[12])
        assertEquals(0x28.toByte(), bytes[13])
        assertEquals(0x5f.toByte(), bytes[14])
        assertEquals(0x6d.toByte(), bytes[15])
    }

    @Test
    fun readUUIDTest() {
        val expected = UUID(3790679846977846, 148689600201769005)
        val bytes = byteArrayOf(
            0x00.toByte(),
            0x0d.toByte(),
            0x77.toByte(),
            0x9a.toByte(),
            0x6d.toByte(),
            0xbc.toByte(),
            0x81.toByte(),
            0x36.toByte(),
            0x02.toByte(),
            0x10.toByte(),
            0x40.toByte(),
            0x67.toByte(),
            0x51.toByte(),
            0xd2.toByte(),
            0x40.toByte(),
            0x2d.toByte(),
        )
        assertEquals(expected, BytesUtil.readUUID(ByteArrayInputStream(bytes)))
    }

    @Test
    fun readUUIDFromArrayTest() {
        val expected: UUID = UUID(3790679846977846, 148689600201769005)
        val bytes = byteArrayOf(
            0,
            0x00.toByte(),
            0x0d.toByte(),
            0x77.toByte(),
            0x9a.toByte(),
            0x6d.toByte(),
            0xbc.toByte(),
            0x81.toByte(),
            0x36.toByte(),
            0x02.toByte(),
            0x10.toByte(),
            0x40.toByte(),
            0x67.toByte(),
            0x51.toByte(),
            0xd2.toByte(),
            0x40.toByte(),
            0x2d.toByte(),
            0,
        )
        check(bytes.size == 8 + 8 + 2)
        val actual: UUID = BytesUtil.readUUID(bytes = bytes, index = 1)
        assertEquals(expected, actual)
    }

    @Test
    fun readBytesTest() {
        val bytes = byteArrayOf(
            0x0a.toByte(),
            0x06.toByte(),
            0x16.toByte(),
            0xff.toByte(),
            0xbe.toByte(),
            0xea.toByte(),
            0xdb.toByte(),
            0x8d.toByte(),
        )
        listOf(
            byteArrayOf(),
            byteArrayOf(0x0a),
            byteArrayOf(0x0a, 0x06),
            byteArrayOf(0x0a, 0x06, 0x16),
            bytes,
        ).forEach { expected ->
            val actual = BytesUtil.readBytes(ByteArrayInputStream(bytes), expected.size)
            assertEquals(expected.size, actual.size)
            assertTrue(expected.contentEquals(actual))
        }
    }

    @Test
    fun writeToArrayLongTest() {
        val number: Long = 110480001
        val bytes = ByteArray(32)
        assertTrue(bytes.all { it == 0.toByte() })
        BytesUtil.writeBytes(bytes = bytes, index = 4, value = number)
        assertEquals(bytes.size, 32)
        assertTrue(bytes.slice(0 until 4).all { it == 0.toByte() })
        assertTrue(bytes.slice((4 + 16) until 32).all { it == 0.toByte() })
        val slice = bytes.sliceArray(4 until (4 + 16))
        assertEquals(slice.size, 16)
        val actual: Long = BytesUtil.readLong(slice.inputStream())
        assertEquals(number, actual)
    }

    @Test
    fun writeToArrayUUIDTest() {
        val id = UUID(201043908171234567, 56416449111234567)
        val bytes = ByteArray(64)
        assertTrue(bytes.all { it == 0.toByte() })
        BytesUtil.writeBytes(bytes = bytes, index = 4, value = id)
        assertEquals(bytes.size, 64)
        assertTrue(bytes.slice(0 until 4).all { it == 0.toByte() })
        assertTrue(bytes.slice((4 + 32) until bytes.size).all { it == 0.toByte() })
        val slice = bytes.sliceArray(4 until (4 + 32))
        assertEquals(slice.size, 32)
        val actual = BytesUtil.readUUID(slice.inputStream())
        assertEquals(id, actual)
    }
}
