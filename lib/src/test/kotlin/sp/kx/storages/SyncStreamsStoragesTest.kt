package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

internal class SyncStreamsStoragesTest {
    @Test
    fun noStoragesTest() {
        assertThrows(IllegalStateException::class.java) {
            SyncStreamsStorages.Builder().mock()
        }
    }

    @Test
    fun getTest() {
        val streamer = MockStreamer()
        val id1 = mockUUID(1)
        val storages = SyncStreamsStorages.Builder()
            .add(id1, String::class.java, StringTransformer)
            .mock(
                streamerProvider = mockStreamerProvider(streamers = mapOf(id1 to streamer)),
            )
        val storage = storages.get(id = id1)
        assertNotNull(storage)
        checkNotNull(storage)
        val id2 = mockUUID(2)
        check(id1 != id2)
        assertNull(storages.get(id = id2))
    }

    @Test
    fun getTypeTest() {
        val id1 = mockUUID(1)
        val id2 = mockUUID(2)
        check(id1 != id2)
        val streamers = mapOf(
            id1 to MockStreamer(),
            id2 to MockStreamer(),
        )
        val storages = SyncStreamsStorages.Builder()
            .add(id1, String::class.java, StringTransformer)
            .add(id2, Int::class.java, IntTransformer)
            .mock(
                streamerProvider = mockStreamerProvider(streamers = streamers),
            )
        storages.get(type = String::class.java).also { storage ->
            assertNotNull(storage)
            checkNotNull(storage)
            assertEquals(id1, storage.id)
        }
        storages.get(type = Int::class.java).also { storage ->
            assertNotNull(storage)
            checkNotNull(storage)
            assertEquals(id2, storage.id, "Storage: $storage")
        }
        storages.get(type = Boolean::class.java).also { storage ->
            assertNull(storage)
        }
    }
}
