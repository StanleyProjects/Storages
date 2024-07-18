package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class StringUtilTest {
    @Test
    fun toHEXTest() {
        mapOf(
            -2 to "fe",
            -1 to "ff",
            0 to "00",
            1 to "01",
            2 to "02",
            8 to "08",
            16 to "10",
            32 to "20",
            64 to "40",
            128 to "80",
            256 to "00",
            42 to "2a",
        ).forEach { (number, expected) ->
            val actual = number.toByte().toHEX()
            assertEquals(actual.length, 2)
            assertEquals(expected, actual, "number: $number")
        }
    }

    @Test
    fun toHEXByteArrayTest() {
        // 01111111 10100001 10110010 11000011
        // 7f       a1       b2       c3
        // 127      161      178      195
        // 127      -95      -78      -61
        val bytes = byteArrayOf(127, -95, -78, -61)
        val actual = bytes.toHEX()
        assertEquals(actual.length, bytes.size * 2)
        assertEquals("7fa1b2c3", actual, "bytes: ${bytes.map { it }}")
    }
}
