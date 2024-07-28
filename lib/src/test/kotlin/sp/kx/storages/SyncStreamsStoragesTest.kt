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
        onSyncStreamsStorages { tStorages: SyncStreamsStorages, rStorages: SyncStreamsStorages ->
            tStorages.assertHashes(
                expected = mapOf(
                    mockUUID(1) to MockHashFunction.map("strings:hash:t:updated"),
                    mockUUID(2) to MockHashFunction.map("ints:hash:t:updated"),
                    mockUUID(3) to MockHashFunction.map("longs:hash"),
                ),
            )
            rStorages.assertHashes(
                expected = mapOf(
                    mockUUID(1) to MockHashFunction.map("strings:hash:r:updated"),
                    mockUUID(2) to MockHashFunction.map("ints:hash:r:updated"),
                    mockUUID(3) to MockHashFunction.map("longs:hash"),
                ),
            )
        }
    }

    @Test
    fun getSyncInfoTest() {
        val stringsTUpdated = listOf(
            mockDescribed(12),
            mockDescribed(13).updated(113),
            mockDescribed(14),
            mockDescribed(15),
            mockDescribed(16),
        )
        val stringsRUpdated = listOf(
            mockDescribed(11),
            mockDescribed(13),
            mockDescribed(14).updated(114),
            mockDescribed(15),
            mockDescribed(17),
        )
        val intsTUpdated = listOf(
            mockDescribed(pointer = 22, item = 22),
            mockDescribed(pointer = 23, item = 23).updated(123, 123),
            mockDescribed(pointer = 24, item = 24),
            mockDescribed(pointer = 25, item = 25),
            mockDescribed(pointer = 26, item = 26),
        )
        // 00 01 02 03 04 05 06 07 08 09
        // 21 __ 23 uu 25 27
        val intsRUpdated = listOf(
            mockDescribed(pointer = 21, item = 21),
            mockDescribed(pointer = 23, item = 23),
            mockDescribed(pointer = 24, item = 24).updated(124, 124),
            mockDescribed(pointer = 25, item = 25),
            mockDescribed(pointer = 27, item = 27),
        )
        onSyncStreamsStorages { tStorages: SyncStreamsStorages, rStorages: SyncStreamsStorages ->
            tStorages.assertSyncInfo(
                hashes = mapOf(),
                expected = emptyMap(),
            )
            tStorages.assertSyncInfo(
                hashes = mapOf(mockUUID(11) to ByteArray(0)),
                expected = emptyMap(),
            )
            check(rStorages.hashes().keys.sorted() == (1..3).map { mockUUID(it) })
            tStorages.assertSyncInfo(
                hashes = rStorages.hashes(),
                expected = mapOf(
                    mockUUID(1) to mockSyncInfo(
                        infos = stringsTUpdated.associate { it.id to it.info },
                        deleted = setOf(mockUUID(11)),
                    ),
                    mockUUID(2) to mockSyncInfo(
                        infos = intsTUpdated.associate { it.id to it.info },
                        deleted = setOf(mockUUID(21)),
                    ),
                ),
            )
            rStorages.assertSyncInfo(
                hashes = mapOf(),
                expected = emptyMap(),
            )
            rStorages.assertSyncInfo(
                hashes = mapOf(mockUUID(11) to ByteArray(0)),
                expected = emptyMap(),
            )
            check(tStorages.hashes().keys.sorted() == (1..3).map { mockUUID(it) })
            rStorages.assertSyncInfo(
                hashes = tStorages.hashes(),
                expected = mapOf(
                    mockUUID(1) to mockSyncInfo(
                        infos = stringsRUpdated.associate { it.id to it.info },
                        deleted = setOf(mockUUID(12)),
                    ),
                    mockUUID(2) to mockSyncInfo(
                        infos = intsRUpdated.associate { it.id to it.info },
                        deleted = setOf(mockUUID(22)),
                    ),
                ),
            )
        }
    }

    @Test
    fun getMergeInfoErrorTest() {
        val storages = SyncStreamsStorages.Builder()
            .add(mockUUID(1), StringTransformer)
            .mock()
        assertThrows(IllegalStateException::class.java) {
            storages.getMergeInfo(infos = mapOf(mockUUID(2) to mockSyncInfo()))
        }
    }

    @Test
    fun getMergeInfoTest() {
        onSyncStreamsStorages { tStorages: SyncStreamsStorages, rStorages: SyncStreamsStorages ->
            tStorages.assertMergeInfo(
                storage = rStorages,
                expected = mapOf(
                    mockUUID(1) to mockMergeInfo(
                        download = setOf(mockUUID(14), mockUUID(pointer = 17)),
                        items = listOf(
                            mockDescribed(13).updated(113).map(StringTransformer::encode),
                            mockDescribed(pointer = 16).map(StringTransformer::encode),
                        ),
                        deleted = setOf(mockUUID(11)),
                    ),
                    mockUUID(2) to mockMergeInfo(
                        download = setOf(mockUUID(pointer = 24), mockUUID(pointer = 27)),
                        items = listOf(
                            mockDescribed(pointer = 23, item = 23).updated(123, 123).map(IntTransformer::encode),
                            mockDescribed(pointer = 26, item = 26).map(IntTransformer::encode),
                        ),
                        deleted = setOf(mockUUID(21)),
                    ),
                ),
            )
            rStorages.assertMergeInfo(
                storage = tStorages,
                expected = mapOf(
                    mockUUID(1) to mockMergeInfo(
                        download = setOf(mockUUID(13), mockUUID(pointer = 16)),
                        items = listOf(
                            mockDescribed(14).updated(114).map(StringTransformer::encode),
                            mockDescribed(pointer = 17).map(StringTransformer::encode),
                        ),
                        deleted = setOf(mockUUID(12)),
                    ),
                    mockUUID(2) to mockMergeInfo(
                        download = setOf(mockUUID(23), mockUUID(26)),
                        items = listOf(
                            mockDescribed(pointer = 24, item = 24).updated(124, 124).map(IntTransformer::encode),
                            mockDescribed(pointer = 27, item = 27).map(IntTransformer::encode),
                        ),
                        deleted = setOf(mockUUID(22)),
                    ),
                ),
            )
        }
    }

    /*
    @Test
    fun mergeTest() {
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
        val longs = (1..5).map { number ->
            mockDescribed(pointer = 30 + number, item = number.toLong())
        }
        val hashes = MockHashFunction.hashes(
            strings to "strings:hash",
            ints to "ints:hash",
            longs to "ints:longs",
            stringsTUpdated to "strings:hash:t:updated",
            stringsRUpdated to "strings:hash:R:updated",
            intsTUpdated to "ints:hash:t:updated",
            intsRUpdated to "ints:hash:r:updated",
        ) + strings.map {
            StringTransformer.hashPair(it)
        } + ints.map {
            IntTransformer.hashPair(it)
        } + longs.map {
            LongTransformer.hashPair(it)
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
            mockUUID(3) to FileStreamer(File.createTempFile("storage", "3")),
        )
        val tStorages = SyncStreamsStorages.Builder()
            .add(mockUUID(1), StringTransformer)
            .add(mockUUID(2), IntTransformer)
            .add(mockUUID(3), LongTransformer)
            .mock(
                hashes = hashes,
                timeProvider = timeProvider,
                uuidProvider = uuidProvider,
                streamerProvider = mockStreamerProvider(streamers = tStreamers),
            )
        val rStreamers = mapOf(
            mockUUID(1) to FileStreamer(File.createTempFile("storage", "1")),
            mockUUID(2) to FileStreamer(File.createTempFile("storage", "2")),
            mockUUID(3) to FileStreamer(File.createTempFile("storage", "3")),
        )
        val rStorages = SyncStreamsStorages.Builder()
            .add(mockUUID(1), StringTransformer)
            .add(mockUUID(2), IntTransformer)
            .add(mockUUID(3), LongTransformer)
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
        longs.forEach { described ->
            itemId = described.id
            time = described.info.created
            tStorages.require<Long>().add(described.item)
            rStorages.require<Long>().add(described.item)
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
        TODO("SyncStreamsStoragesTest:mergeTest")
    }
    */

    companion object {
        private fun SyncStreamsStorages.assertHashes(expected: Map<UUID, ByteArray>) {
            val actual = hashes()
            assertEquals(expected.size, actual.size, "hashes:\n$expected\n$actual\n")
            for ((id, eh) in expected) {
                val ah = actual[id] ?: error("No hash by ID: \"$id\"!")
                assertEquals(eh.toHEX(), ah.toHEX(), "id: $id")
            }
        }

        private fun SyncStreamsStorages.assertSyncInfo(hashes: Map<UUID, ByteArray>, expected: Map<UUID, SyncInfo>) {
            val actual = getSyncInfo(hashes)
            assertEquals(expected.size, actual.size, "SyncInfo:\n$expected\n$actual\n")
            for ((id, value) in expected) {
                val syncInfo = actual[id] ?: error("No hash by ID: \"$id\"!")
                assertEquals(value.deleted.size, syncInfo.deleted.size)
                assertEquals(value.deleted.sorted(), syncInfo.deleted.sorted())
                assertEquals(value.infos.size, syncInfo.infos.size)
                for (key in value.infos.keys) {
                    val ei = value.infos[key]
                    checkNotNull(ei)
                    val ai = syncInfo.infos[key]
                    checkNotNull(ai)
                    assertEquals(ei, ai, "id: $id")
                }
                assertEquals(value, syncInfo, "id: $id")
            }
        }

        private fun SyncStreamsStorages.assertMergeInfo(storage: SyncStreamsStorages, expected: Map<UUID, MergeInfo>) {
            val actual = getMergeInfo(storage.getSyncInfo(hashes()))
            assertEquals(expected.size, actual.size, "MergeInfo:\n$expected\n$actual\n")
            for ((id, value) in expected) {
                SyncStreamsStorageTest.assert(
                    expected = value,
                    actual = actual[id] ?: error("No hash by ID: \"$id\"!"),
                )
            }
        }

        private fun <T : Any> Transformer<T>.hashPair(described: Described<T>): Pair<ByteArray, ByteArray> {
            return encode(described.item) to described.info.hash
        }

        private fun Described<String>.updated(pointer: Int): Described<String> {
            return copy(
                updated = (1_000 + pointer).milliseconds,
                hash = MockHashFunction.map("$item:$pointer:hash:updated"),
                item = "$item:$pointer:updated",
            )
        }

        private fun Described<Int>.updated(pointer: Int, item: Int): Described<Int> {
            return copy(
                updated = (1_000 + pointer).milliseconds,
                hash = MockHashFunction.map("$item:$pointer:hash:updated"),
                item = item,
            )
        }

        private fun onSyncStreamsStorages(block: (t: SyncStreamsStorages, r: SyncStreamsStorages) -> Unit) {
            val strings = (1..5).map { number ->
                mockDescribed(pointer = 10 + number)
            }
            val stringTUpdated = strings[2].updated(pointer = 113)
            val stringRUpdated = strings[3].updated(pointer = 114)
            // 00 01 02 03 04 05 06 07 08 09
            // __ 12 uu 14 15 16
            val stringsTUpdated = listOf(
                mockDescribed(12),
                stringTUpdated,
                mockDescribed(14),
                mockDescribed(15),
                mockDescribed(16),
            )
            // 00 01 02 03 04 05 06 07 08 09
            // 11 __ 13 uu 15 17
            val stringsRUpdated = listOf(
                mockDescribed(11),
                mockDescribed(13),
                stringRUpdated,
                mockDescribed(15),
                mockDescribed(17),
            )
            val ints = (1..5).map { number ->
                mockDescribed(pointer = 20 + number, item = 20 + number)
            }
            val intTUpdated = ints[2].updated(pointer = 123, item = 123)
            val intRUpdated = ints[3].updated(pointer = 124, item = 124)
            // 00 01 02 03 04 05 06 07 08 09
            // __ 22 uu 24 25 26
            val intsTUpdated = listOf(
                mockDescribed(pointer = 22, item = 22),
                intTUpdated,
                mockDescribed(pointer = 24, item = 24),
                mockDescribed(pointer = 25, item = 25),
                mockDescribed(pointer = 26, item = 26),
            )
            // 00 01 02 03 04 05 06 07 08 09
            // 21 __ 23 uu 25 27
            val intsRUpdated = listOf(
                mockDescribed(pointer = 21, item = 21),
                mockDescribed(pointer = 23, item = 23),
                intRUpdated,
                mockDescribed(pointer = 25, item = 25),
                mockDescribed(pointer = 27, item = 27),
            )
            val longs = (1..5).map { number ->
                mockDescribed(pointer = 30 + number, item = number.toLong())
            }
            val hashes = MockHashFunction.hashes(
                strings to "strings:hash",
                ints to "ints:hash",
                longs to "longs:hash",
                stringsTUpdated to "strings:hash:t:updated",
                stringsRUpdated to "strings:hash:r:updated",
                intsTUpdated to "ints:hash:t:updated",
                intsRUpdated to "ints:hash:r:updated",
            ) + strings.map {
                StringTransformer.hashPair(it)
            } + ints.map {
                IntTransformer.hashPair(it)
            } + longs.map {
                LongTransformer.hashPair(it)
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
                mockUUID(3) to FileStreamer(File.createTempFile("storage", "3")),
            )
            val tStorages = SyncStreamsStorages.Builder()
                .add(mockUUID(1), StringTransformer)
                .add(mockUUID(2), IntTransformer)
                .add(mockUUID(3), LongTransformer)
                .mock(
                    hashes = hashes,
                    timeProvider = timeProvider,
                    uuidProvider = uuidProvider,
                    streamerProvider = mockStreamerProvider(streamers = tStreamers),
                )
            val rStreamers = mapOf(
                mockUUID(1) to FileStreamer(File.createTempFile("storage", "1")),
                mockUUID(2) to FileStreamer(File.createTempFile("storage", "2")),
                mockUUID(3) to FileStreamer(File.createTempFile("storage", "3")),
            )
            val rStorages = SyncStreamsStorages.Builder()
                .add(mockUUID(1), StringTransformer)
                .add(mockUUID(2), IntTransformer)
                .add(mockUUID(3), LongTransformer)
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
            longs.forEach { described ->
                itemId = described.id
                time = described.info.created
                tStorages.require<Long>().add(described.item)
                rStorages.require<Long>().add(described.item)
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
            block(tStorages, rStorages)
        }
    }
}
