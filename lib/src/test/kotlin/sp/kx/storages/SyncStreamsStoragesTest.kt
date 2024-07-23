package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.UUID

internal class SyncStreamsStoragesTest {
    @Test
    fun getEmptyTest() {
        val storages: SyncStreamsStorages = MockSyncStreamsStorages()
        val id = mockUUID()
        val storage = storages.get(id = id)
        assertNull(storage)
    }

    @Test
    fun getTest() {
        val streamer = MockStreamer()
        val id = mockUUID()
        val storages: SyncStreamsStorages = MockSyncStreamsStorages(
            transformers = mapOf(id to StringTransformer),
            streamerProvider = { _: UUID, _: Long, _: Long ->
                streamer
            },
        )
        val storage = storages.get(id = id)
        assertNotNull(storage)
        checkNotNull(storage)
    }
}
