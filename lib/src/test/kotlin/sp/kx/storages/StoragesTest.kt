package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

internal class StoragesTest {
    @Test
    fun requireTest() {
        val id1 = mockUUID(1)
        val id2 = mockUUID(2)
        check(id1 != id2)
        val storages: Storages = MockStorages(storages = listOf(MockStorage(id = id1)))
        assertEquals(storages.require(id = id1).id, id1)
        assertThrows(IllegalStateException::class.java) {
            storages.require(id = id2)
        }
    }
}
