package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

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
}
