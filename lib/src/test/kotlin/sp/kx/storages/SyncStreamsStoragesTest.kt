package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class SyncStreamsStoragesTest {
    @Test
    fun getEmptyTest() {
        val storages: SyncStreamsStorages = MockSyncStreamsStorages()
        val id = mockUUID()
        val storage = storages.get(id = id)
        assertNull(storage)
    }
}
