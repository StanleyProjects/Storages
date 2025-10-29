package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class ValueStateTest {
    @Test
    fun toStringTest() {
        val ms = 42
        val size = 4
        val issuer = ValueState(
            updated = ms.milliseconds,
            hash = ByteArray(size),
        )
        val expected = "ValueState(updated: $ms, hash: $size)"
        assertEquals(expected, issuer.toString())
    }

    @Test
    fun equalsTest() {
        val i0 = ValueState(
            updated = 42.milliseconds,
            hash = ByteArray(4),
        )
        val i1 = ValueState(
            updated = 42.milliseconds,
            hash = ByteArray(4),
        )
        assertTrue(i0 == i1)
        val i2 = ValueState(
            updated = Duration.ZERO,
            hash = ByteArray(4),
        )
        assertFalse(i0 == i2)
        val i3 = ValueState(
            updated = 42.milliseconds,
            hash = ByteArray(0),
        )
        assertFalse(i0 == i3)
        assertFalse(i0.equals(Unit))
    }

    @Test
    fun hashCodeTest() {
        val issuer = ValueState(
            updated = 42.milliseconds,
            hash = byteArrayOf(4, 3, 2, 1),
        )
        val expected = -1689920704
        assertEquals(expected, issuer.hashCode())
    }

    @Test
    fun getTest() {
        val updated = 42.milliseconds
        val hash = byteArrayOf(4, 3, 2, 1)
        val issuer = ValueState(
            updated = updated,
            hash = hash,
        )
        assertEquals(updated, issuer.updated)
        assertTrue(hash.contentEquals(issuer.hash))
    }
}
