package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
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
}
