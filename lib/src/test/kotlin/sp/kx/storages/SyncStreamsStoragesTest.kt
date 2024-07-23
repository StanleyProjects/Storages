package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.File
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

internal class SyncStreamsStoragesTest {
    @Test
    fun addTest() {
        val id1 = mockUUID(1)
        val builder = SyncStreamsStorages.Builder()
        builder.add(id1, String::class.java, StringTransformer)
        assertThrows(IllegalStateException::class.java) {
            builder.add(id1, String::class.java, StringTransformer)
        }
    }

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

    @Test
    fun hashesTest() {
        val storage1Items = listOf(
            mockDescribed(pointer = 11),
        )
        val storage2Items = listOf(
            mockDescribed(pointer = 21, 21),
        )
        val hashes = MockHashFunction.hashes(
            storage1Items to "1:default",
            storage2Items to "2:default",
        ) + storage1Items.map {
            StringTransformer.encode(it.item) to it.info.hash
        } + storage2Items.map {
            IntTransformer.encode(it.item) to it.info.hash
        }
        var time = 1.milliseconds
        val timeProvider = MockProvider { time }
        var itemId = mockUUID()
        val uuidProvider = MockProvider { itemId }
        val streamers = mapOf(
            mockUUID(1) to FileStreamer(File.createTempFile("storage", "1")),
            mockUUID(2) to FileStreamer(File.createTempFile("storage", "2")),
        )
        val storages = SyncStreamsStorages.Builder()
            .add(
                id = mockUUID(1),
                type = String::class.java,
                transformer = StringTransformer,
            )
            .add(
                id = mockUUID(2),
                type = Int::class.java,
                transformer = IntTransformer,
            )
            .mock(
                hashes = hashes,
                timeProvider = timeProvider,
                uuidProvider = uuidProvider,
                streamerProvider = mockStreamerProvider(streamers = streamers),
            )
        storage1Items.forEach { described ->
            itemId = described.id
            time = described.info.created
            storages.get(String::class.java)!!.add(described.item)
        }
        storage2Items.forEach { described ->
            itemId = described.id
            time = described.info.created
            storages.get(Int::class.java)!!.add(described.item)
        }
        val expected = mapOf(
            mockUUID(1) to MockHashFunction.map("1:default"),
            mockUUID(2) to MockHashFunction.map("2:default"),
        )
        storages.assertHashes(expected = expected)
    }

    companion object {
        private fun SyncStreamsStorages.assertHashes(expected: Map<UUID, ByteArray>) {
            val actual = hashes()
            assertEquals(expected.size, actual.size, "hashes:\n$expected\n$actual\n")
            for ((ei, eh) in expected) {
                val ah = actual[ei] ?: error("No hash by ID: \"$ei\"!")
                assertEquals(eh.toHEX(), ah.toHEX())
            }
        }
    }
}
