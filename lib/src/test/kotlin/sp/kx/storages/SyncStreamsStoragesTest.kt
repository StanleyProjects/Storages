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

    @Test
    fun getSyncInfoTest() {
        val strings = (1..5).map { number ->
            mockDescribed(pointer = 10 + number)
        }
        val stringTUpdated = strings[2].copy(
            updated = (1_000 + 113).milliseconds,
            hash = MockHashFunction.map("item:hash:13:t:updated"),
            item = "item:13:t:updated",
        )
        val stringRUpdated = strings[3].copy(
            updated = (1_000 + 114).milliseconds,
            hash = MockHashFunction.map("item:hash:14:r:updated"),
            item = "item:14:r:updated",
        )
        val stringsTUpdated = strings.toMutableList().also {
            it.removeAt(0)
            it.removeAt(1)
            it.add(1, stringTUpdated)
            it.add(mockDescribed(pointer = 16))
        }.toList()
        val stringsRUpdated = strings.toMutableList().also {
            it.removeAt(1)
            it.removeAt(2)
            it.add(2, stringRUpdated)
            it.add(mockDescribed(pointer = 17))
        }.toList()
        val ints = (1..5).map { number ->
            mockDescribed(pointer = 20 + number, item = 20 + number)
        }
        val intTUpdated = ints[2].copy(
            updated = (1_000 + 123).milliseconds,
            hash = MockHashFunction.map("item:hash:23:t:updated"),
            item = 123,
        )
        val intRUpdated = ints[3].copy(
            updated = (1_000 + 124).milliseconds,
            hash = MockHashFunction.map("item:hash:24:r:updated"),
            item = 124,
        )
        val intsTUpdated = ints.toMutableList().also {
            it.removeAt(0)
            it.removeAt(1)
            it.add(1, intTUpdated)
            it.add(mockDescribed(pointer = 26, item = 26))
        }.toList()
        val intsRUpdated = ints.toMutableList().also {
            it.removeAt(1)
            it.removeAt(2)
            it.add(2, intRUpdated)
            it.add(mockDescribed(pointer = 27, item = 27))
        }.toList()
        val hashes = MockHashFunction.hashes(
            strings to "strings:hash",
            ints to "ints:hash",
            stringsTUpdated to "strings:hash:t:updated",
            stringsRUpdated to "strings:hash:R:updated",
            intsTUpdated to "ints:hash:t:updated",
            intsRUpdated to "ints:hash:r:updated",
        ) + strings.map {
            StringTransformer.hashPair(it)
        } + ints.map {
            IntTransformer.hashPair(it)
        } + listOf(
            StringTransformer.hashPair(mockDescribed(pointer = 16)),
            StringTransformer.hashPair(mockDescribed(pointer = 17)),
            StringTransformer.hashPair(stringTUpdated),
            StringTransformer.hashPair(stringRUpdated),
            IntTransformer.hashPair(mockDescribed(pointer = 26, item = 26)),
            IntTransformer.hashPair(mockDescribed(pointer = 27, item = 27)),
            IntTransformer.hashPair(intTUpdated),
            IntTransformer.hashPair(intRUpdated),
        )
        var time = 1.milliseconds
        val timeProvider = MockProvider { time }
        var itemId = mockUUID()
        val uuidProvider = MockProvider { itemId }
        val tStreamers = mapOf(
            mockUUID(1) to FileStreamer(File.createTempFile("storage", "1")),
            mockUUID(2) to FileStreamer(File.createTempFile("storage", "2")),
        )
        val tStorages = SyncStreamsStorages.Builder()
            .add(mockUUID(1), StringTransformer)
            .add(mockUUID(2), IntTransformer)
            .mock(
                hashes = hashes,
                timeProvider = timeProvider,
                uuidProvider = uuidProvider,
                streamerProvider = mockStreamerProvider(streamers = tStreamers),
            )
        val rStreamers = mapOf(
            mockUUID(1) to FileStreamer(File.createTempFile("storage", "1")),
            mockUUID(2) to FileStreamer(File.createTempFile("storage", "2")),
        )
        val rStorages = SyncStreamsStorages.Builder()
            .add(mockUUID(1), StringTransformer)
            .add(mockUUID(2), IntTransformer)
            .mock(
                hashes = hashes,
                timeProvider = timeProvider,
                uuidProvider = uuidProvider,
                streamerProvider = mockStreamerProvider(streamers = rStreamers),
            )
        strings.forEach { described ->
            itemId = described.id
            time = described.info.created
            tStorages.require<String>().add(described.item)
            rStorages.require<String>().add(described.item)
        }
        ints.forEach { described ->
            itemId = described.id
            time = described.info.created
            tStorages.require<Int>().add(described.item)
            rStorages.require<Int>().add(described.item)
        }
        check(tStorages.hashes() == rStorages.hashes())
        //
        mockDescribed(pointer = 16).also { described ->
            itemId = described.id
            time = described.info.created
            tStorages.require<String>().add(described.item)
        }
        mockDescribed(pointer = 17).also { described ->
            itemId = described.id
            time = described.info.created
            rStorages.require<String>().add(described.item)
        }
        check(tStorages.require<String>().delete(strings[0].id))
        check(rStorages.require<String>().delete(strings[1].id))
        stringTUpdated.also { described ->
            itemId = described.id
            time = described.info.updated
            val info = tStorages.require<String>().update(described.id, described.item)
            checkNotNull(info)
        }
        stringRUpdated.also { described ->
            itemId = described.id
            time = described.info.updated
            val info = rStorages.require<String>().update(described.id, described.item)
            checkNotNull(info)
        }
        //
        mockDescribed(pointer = 26, item = 26).also { described ->
            itemId = described.id
            time = described.info.created
            tStorages.require<Int>().add(described.item)
        }
        mockDescribed(pointer = 27, item = 27).also { described ->
            itemId = described.id
            time = described.info.created
            rStorages.require<Int>().add(described.item)
        }
        check(tStorages.require<Int>().delete(ints[0].id))
        check(rStorages.require<Int>().delete(ints[1].id))
        intTUpdated.also { described ->
            itemId = described.id
            time = described.info.updated
            val info = tStorages.require<Int>().update(described.id, described.item)
            checkNotNull(info)
        }
        intRUpdated.also { described ->
            itemId = described.id
            time = described.info.updated
            val info = rStorages.require<Int>().update(described.id, described.item)
            checkNotNull(info)
        }
        //
        tStorages.assertSyncInfo(
            hashes = rStorages.hashes(),
            expected = mapOf(
                mockUUID(1) to mockSyncInfo(
                    infos = stringsTUpdated.associate { it.id to it.info },
                    deleted = setOf(strings[0].id),
                ),
                mockUUID(2) to mockSyncInfo(
                    infos = intsTUpdated.associate { it.id to it.info },
                    deleted = setOf(ints[0].id),
                ),
            ),
        )
        rStorages.assertSyncInfo(
            hashes = tStorages.hashes(),
            expected = mapOf(
                mockUUID(1) to mockSyncInfo(
                    infos = stringsRUpdated.associate { it.id to it.info },
                    deleted = setOf(strings[1].id),
                ),
                mockUUID(2) to mockSyncInfo(
                    infos = intsRUpdated.associate { it.id to it.info },
                    deleted = setOf(ints[1].id),
                ),
            ),
        )
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

        private fun SyncStreamsStorages.assertSyncInfo(hashes: Map<UUID, ByteArray>, expected: Map<UUID, SyncInfo>) {
            val actual = getSyncInfo(hashes)
            assertEquals(expected.size, actual.size, "SyncInfo:\n$expected\n$actual\n")
            for ((id, value) in expected) {
                assertEquals(value, actual[id] ?: error("No hash by ID: \"$id\"!"))
            }
        }

        private fun <T : Any> Transformer<T>.hashPair(described: Described<T>): Pair<ByteArray, ByteArray> {
            return encode(described.item) to described.info.hash
        }
    }
}
