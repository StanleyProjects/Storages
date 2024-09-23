package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import sp.kx.bytes.toHEX
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

internal class MutableFileStorageTest {
    @Test
    fun deleteTest() {
        val meta = mockMetadata(1)
        val storage: MutableFileStorage = MockMutableFileStorage(
            values = mapOf(
                meta to mockByteArray(1),
            ),
        )
        assertEquals(storage.items.sortedBy { it.id }, listOf(meta))
        assertFalse(storage.delete(mockUUID(2)))
        assertEquals(storage.items.sortedBy { it.id }, listOf(meta))
        assertTrue(storage.delete(mockUUID(1)))
        assertEquals(0, storage.items.sortedBy { it.id }.size)
    }

    @Test
    fun addTest() {
        val m1 = mockMetadata(1)
        var uuid: UUID = mockUUID(1)
        val storage: MutableFileStorage = MockMutableFileStorage(
            values = mapOf(m1 to mockByteArray(1)),
            uuidProvider = {uuid},
        )
        assertEquals(storage.items.sortedBy { it.id }, listOf(m1))
        uuid = mockUUID(2)
        val m2 = storage.add(mockByteArray(2))
        assertEquals(storage.items.sortedBy { it.id }, listOf(m1, m2))
        val actual = storage.getBytes(id = m2.id)
        val expected = mockByteArray(2)
        assertTrue(actual.contentEquals(expected), "a: ${actual.toHEX()}, e: ${expected.toHEX()}")
    }

    @Test
    fun updateTest() {
        val m1 = mockMetadata(1)
        var time = mockDuration(1)
        val storage: MutableFileStorage = MockMutableFileStorage(
            values = mapOf(m1 to mockByteArray(1)),
            timeProvider = { time },
            hf = MockHashFunction(
                hashes = listOf(mockByteArray(2) to "foobar".toByteArray()),
            ),
        )
        assertEquals(storage.items.sortedBy { it.id }, listOf(m1))
        assertTrue(mockByteArray(1).contentEquals(storage.getBytes(id = m1.id)))
        assertNull(storage.update(mockUUID(2), mockByteArray(2)))
        time = 128.milliseconds
        val info = storage.update(m1.id, mockByteArray(2))
        checkNotNull(info)
        val mu = m1.copy(updated = time, hash = "foobar".toByteArray(), size = 2)
        val items = storage.items
        assertEquals(1, items.size)
        assertEquals(mu, items.single())
        assertTrue(mockByteArray(2).contentEquals(storage.getBytes(id = m1.id)))
    }
}
