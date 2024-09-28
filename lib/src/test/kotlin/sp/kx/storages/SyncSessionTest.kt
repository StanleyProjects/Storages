package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Objects

internal class SyncSessionTest {
    @Test
    fun toStringTest() {
        val it = mockSyncSession(
            src = byteArrayOf(0x0a, 0x0b, 0x0c),
            dst = byteArrayOf(0x1a, 0x2b, 0x3c),
        )
        val expected = "SyncSession(src: 0a0b0c, dst: 1a2b3c)"
        val actual = it.toString()
        assertEquals(expected, actual)
    }

    @Test
    fun equalsTest() {
        val src = byteArrayOf(0x0a, 0x0b, 0x0c)
        val dst = byteArrayOf(0x1a, 0x2b, 0x3c)
        check(!src.contentEquals(dst))
        val it = mockSyncSession(src = src, dst = dst)
        val it1 = mockSyncSession(src = src, dst = dst)
        assertTrue(it == it1)
        assertFalse(it == mockSyncSession(src = dst, dst = dst))
        assertFalse(it == mockSyncSession(src = src, dst = src))
        assertNotEquals(it, Unit)
    }

    @Test
    fun hashCodeTest() {
        val src = byteArrayOf(0x0a, 0x0b, 0x0c)
        val dst = byteArrayOf(0x1a, 0x2b, 0x3c)
        val it = mockSyncSession(src = src, dst = dst)
        val expected = Objects.hash(
            src.contentHashCode(),
            dst.contentHashCode(),
        )
        val actual = it.hashCode()
        assertEquals(expected, actual)
    }
}
