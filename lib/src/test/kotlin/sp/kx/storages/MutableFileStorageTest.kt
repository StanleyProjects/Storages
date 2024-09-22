package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import sp.kx.bytes.toHEX
import java.util.UUID

internal class MutableFileStorageTest {
    @Test
    fun deleteTest() {
        val storage: MutableFileStorage = MockMutableFileStorage(
            values = mapOf(
                Raw(id = mockUUID(1), info = mockItemInfo(1)) to mockByteArray(1),
            ),
        )
        assertEquals(storage.items.sortedBy { it.id }, listOf(Raw(id = mockUUID(1), info = mockItemInfo(1))))
        assertFalse(storage.delete(mockUUID(2)))
        assertEquals(storage.items.sortedBy { it.id }, listOf(Raw(id = mockUUID(1), info = mockItemInfo(1))))
        assertTrue(storage.delete(mockUUID(1)))
        assertEquals(0, storage.items.sortedBy { it.id }.size)
    }

    @Test
    fun addTest() {
        val raw1 = Raw(id = mockUUID(1), info = mockItemInfo(1))
        var uuid: UUID = mockUUID(1)
        val storage: MutableFileStorage = MockMutableFileStorage(
            values = mapOf(raw1 to mockByteArray(1)),
            uuidProvider = {uuid},
        )
        assertEquals(storage.items.sortedBy { it.id }, listOf(raw1))
        uuid = mockUUID(2)
        val raw2 = storage.add(mockByteArray(2))
        assertEquals(storage.items.sortedBy { it.id }, listOf(raw1, raw2))
        val actual = storage.getBytes(id = raw2.id)
        val expected = mockByteArray(2)
        assertTrue(actual.contentEquals(expected), "a: ${actual.toHEX()}, e: ${expected.toHEX()}")
    }

    @Test
    fun updateTest() {
        val raw1 = Raw(id = mockUUID(1), info = mockItemInfo(1))
        val storage: MutableFileStorage = MockMutableFileStorage(
            values = mapOf(raw1 to mockByteArray(1)),
        )
        assertEquals(storage.items.sortedBy { it.id }, listOf(raw1))
        assertTrue(mockByteArray(1).contentEquals(storage.getBytes(id = raw1.id)))
        assertNull(storage.update(mockUUID(2), mockByteArray(2)))
        val info = storage.update(raw1.id, mockByteArray(2))
        checkNotNull(info)
        assertEquals(storage.items.sortedBy { it.id }, listOf(Raw(id = mockUUID(1), info = info)))
        assertTrue(mockByteArray(2).contentEquals(storage.getBytes(id = raw1.id)))
    }
}
