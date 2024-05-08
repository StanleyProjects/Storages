package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrowsExactly
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

internal class SyncStoragesTest {
    @Test
    fun createTest() {
        val ignored = SyncStorages.create(MockSyncStreamsStorage<String>())
    }

    @Test
    fun buildTest() {
        val ignored = SyncStorages.Builder()
            .add(MockSyncStreamsStorage<String>(id = mockUUID(1)))
            .add(MockSyncStreamsStorage<Int>(id = mockUUID(2)))
            .build()
    }

    @Test
    fun getTest() {
        val storage1 = MockSyncStreamsStorage<String>(id = mockUUID(1))
        val storage2 = MockSyncStreamsStorage<Int>(id = mockUUID(2))
        val storages = SyncStorages.Builder()
            .add(storage1)
            .add(storage2)
            .build()
        val notExists = mockUUID(3)
        check(notExists != storage1.id)
        check(notExists != storage2.id)
        assertNull(storages[notExists])
        assertNull(storages.get<Boolean>())
        storage1.also { expected ->
            storages[expected.id].also { actual ->
                assertNotNull(actual)
                checkNotNull(actual)
                assertEquals(expected.id, actual.id)
                assertTrue(expected === actual)
            }
        }
        storage2.also { expected ->
            storages[expected.id].also { actual ->
                assertNotNull(actual)
                checkNotNull(actual)
                assertEquals(expected.id, actual.id)
                assertTrue(expected === actual)
            }
        }
        storage1.also { expected ->
            storages.get<String>().also { actual ->
                assertNotNull(actual)
                checkNotNull(actual)
                assertEquals(expected.id, actual.id)
                assertTrue(expected === actual)
            }
        }
        storage2.also { expected ->
            storages.get<Int>().also { actual ->
                assertNotNull(actual)
                checkNotNull(actual)
                assertEquals(expected.id, actual.id)
                assertTrue(expected === actual)
            }
        }
    }

    @Test
    fun errorTest() {
        assertThrowsExactly(IllegalStateException::class.java) {
            SyncStorages.Builder().build()
        }
        assertThrowsExactly(IllegalStateException::class.java) {
            SyncStorages.Builder()
                .add(MockSyncStreamsStorage<String>())
                .add(MockSyncStreamsStorage<String>())
                .build()
        }
        assertThrowsExactly(IllegalStateException::class.java) {
            SyncStorages.Builder()
                .add(MockSyncStreamsStorage<String>())
                .add(MockSyncStreamsStorage<Int>())
                .build()
        }
    }

    @Test
    fun requireTest() {
        val storage1 = MockSyncStreamsStorage<String>(id = mockUUID(1))
        val storage2 = MockSyncStreamsStorage<Int>(id = mockUUID(2))
        val storages = SyncStorages.Builder()
            .add(storage1)
            .add(storage2)
            .build()
        val notExists = mockUUID(3)
        check(notExists != storage1.id)
        check(notExists != storage2.id)
        assertThrowsExactly(IllegalStateException::class.java) {
            storages.require(notExists)
        }
        assertThrowsExactly(IllegalStateException::class.java) {
            storages.require<Boolean>()
        }
        storage1.also { expected ->
            storages.require(expected.id).also { actual ->
                assertEquals(expected.id, actual.id)
                assertTrue(expected === actual)
            }
        }
        storage2.also { expected ->
            storages.require(expected.id).also { actual ->
                assertEquals(expected.id, actual.id)
                assertTrue(expected === actual)
            }
        }
        storage1.also { expected ->
            storages.require<String>().also { actual ->
                assertEquals(expected.id, actual.id)
                assertTrue(expected === actual)
            }
        }
        storage2.also { expected ->
            storages.require<Int>().also { actual ->
                assertEquals(expected.id, actual.id)
                assertTrue(expected === actual)
            }
        }
    }

    private fun SyncStorages.assertHashes(expected: Map<UUID, String>) {
        val actual = hashes()
        assertEquals(expected.size, actual.size, "hashes:\n$expected\n$actual\n")
        for ((ei, eh) in expected) {
            val ah = actual[ei] ?: error("No hash by ID: \"$ei\"!")
            assertEquals(eh, ah)
        }
    }

    private fun SyncStorages.assertSyncInfo(hashes: Map<UUID, String>, expected: Map<UUID, SyncInfo>) {
        val actual = getSyncInfo(hashes)
        assertEquals(expected.size, actual.size, "SyncInfo:\n$expected\n$actual\n")
        for ((id, value) in expected) {
            assertEquals(value, actual[id] ?: error("No hash by ID: \"$id\"!"))
        }
    }

    private fun SyncStorages.assertMergeInfo(storage: SyncStorages, expected: Map<UUID, MergeInfo>) {
        val actual = getMergeInfo(storage.getSyncInfo(hashes()))
        assertEquals(expected.size, actual.size, "MergeInfo:\n$expected\n$actual\n")
        for ((id, value) in expected) {
            SyncStreamsStorageTest.assert(
                expected = value,
                actual = actual[id] ?: error("No hash by ID: \"$id\"!"),
            )
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
        val hashes = listOf(
            storage1Items.joinToString(separator = "") { it.info.hash }.toByteArray() to "1:default",
            storage2Items.joinToString(separator = "") { it.info.hash }.toByteArray() to "2:default",
        ) + storage1Items.map {
            it.item.toByteArray() to it.info.hash
        } + storage2Items.map {
            it.item.toString().toByteArray() to it.info.hash
        }
        var time = 1.milliseconds
        val timeProvider = mockProvider { time }
        var itemId = mockUUID()
        val uuidProvider = mockProvider { itemId }
        val storages = SyncStorages.Builder()
            .add(
                MockSyncStreamsStorage<String>(
                    id = mockUUID(1),
                    hashes = hashes,
                    timeProvider = timeProvider,
                    uuidProvider = uuidProvider,
                    transformer = storage1Items.map {
                        it.item.toByteArray() to it.item
                    },
                ),
            )
            .add(
                MockSyncStreamsStorage<Int>(
                    id = mockUUID(2),
                    hashes = hashes,
                    timeProvider = timeProvider,
                    uuidProvider = uuidProvider,
                    transformer = storage2Items.map {
                        it.item.toString().toByteArray() to it.item
                    },
                ),
            )
            .build()
        storage1Items.forEach { described ->
            itemId = described.id
            time = described.info.created
            storages.require<String>().add(described.item)
        }
        storage2Items.forEach { described ->
            itemId = described.id
            time = described.info.created
            storages.require<Int>().add(described.item)
        }
        val expected = mapOf(
            mockUUID(1) to "1:default",
            mockUUID(2) to "2:default",
        )
        storages.assertHashes(expected = expected)
    }

    private fun <T : Any> List<Described<T>>.hash(): ByteArray {
        return joinToString(separator = "") { it.info.hash }.toByteArray()
    }

    @Test
    fun getSyncInfoTest() {
        val strings = (1..5).map { number ->
            mockDescribed(pointer = 10 + number)
        }
        val stringTUpdated = strings[2].copy(
            updated = (1_000 + 113).milliseconds,
            hash = "item:hash:13:t:updated",
            item = "item:13:t:updated",
        )
        val stringRUpdated = strings[3].copy(
            updated = (1_000 + 114).milliseconds,
            hash = "item:hash:14:r:updated",
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
            mockDescribed(pointer = 20 + number, item = number)
        }
        val intTUpdated = ints[2].copy(
            updated = (1_000 + 123).milliseconds,
            hash = "item:hash:23:t:updated",
            item = 123,
        )
        val intRUpdated = ints[3].copy(
            updated = (1_000 + 124).milliseconds,
            hash = "item:hash:24:r:updated",
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
        val stringsTransformer = strings.map {
            it.item.toByteArray() to it.item
        } + listOf(
            mockDescribed(pointer = 16).let { it.item.toByteArray() to it.item },
            mockDescribed(pointer = 17).let { it.item.toByteArray() to it.item },
            stringTUpdated.let { it.item.toByteArray() to it.item },
            stringRUpdated.let { it.item.toByteArray() to it.item },
        )
        val intsTransformer = ints.map {
            it.item.toString().toByteArray() to it.item
        } + listOf(
            mockDescribed(pointer = 26, item = 26).let { it.item.toString().toByteArray() to it.item },
            mockDescribed(pointer = 27, item = 27).let { it.item.toString().toByteArray() to it.item },
            intTUpdated.let { it.item.toString().toByteArray() to it.item },
            intRUpdated.let { it.item.toString().toByteArray() to it.item },
        )
        val hashes = listOf(
            strings.hash() to "strings:hash",
            ints.hash() to "ints:hash",
            stringsTUpdated.hash() to "strings:hash:t:updated",
            stringsRUpdated.hash() to "strings:hash:R:updated",
            intsTUpdated.hash() to "ints:hash:t:updated",
            intsRUpdated.hash() to "ints:hash:r:updated",
        ) + strings.map {
            it.item.toByteArray() to it.info.hash
        } + ints.map {
            it.item.toString().toByteArray() to it.info.hash
        } + listOf(
            mockDescribed(pointer = 16).let { it.item.toByteArray() to it.info.hash },
            mockDescribed(pointer = 17).let { it.item.toByteArray() to it.info.hash },
            stringTUpdated.let { it.item.toByteArray() to it.info.hash },
            stringRUpdated.let { it.item.toByteArray() to it.info.hash },
            mockDescribed(pointer = 26, item = 26).let { it.item.toString().toByteArray() to it.info.hash },
            mockDescribed(pointer = 27, item = 27).let { it.item.toString().toByteArray() to it.info.hash },
            intTUpdated.let { it.item.toString().toByteArray() to it.info.hash },
            intRUpdated.let { it.item.toString().toByteArray() to it.info.hash },
        )
        var time = 1.milliseconds
        val timeProvider = mockProvider { time }
        var itemId = mockUUID()
        val uuidProvider = mockProvider { itemId }
        val tStorages = SyncStorages.Builder()
            .add(
                MockSyncStreamsStorage<String>(
                    id = mockUUID(1),
                    hashes = hashes,
                    timeProvider = timeProvider,
                    uuidProvider = uuidProvider,
                    transformer = stringsTransformer,
                ),
            )
            .add(
                MockSyncStreamsStorage<Int>(
                    id = mockUUID(2),
                    hashes = hashes,
                    timeProvider = timeProvider,
                    uuidProvider = uuidProvider,
                    transformer = intsTransformer,
                ),
            )
            .build()
        val rStorages = SyncStorages.Builder()
            .add(
                MockSyncStreamsStorage<String>(
                    id = mockUUID(1),
                    hashes = hashes,
                    timeProvider = timeProvider,
                    uuidProvider = uuidProvider,
                    transformer = stringsTransformer,
                ),
            )
            .add(
                MockSyncStreamsStorage<Int>(
                    id = mockUUID(2),
                    hashes = hashes,
                    timeProvider = timeProvider,
                    uuidProvider = uuidProvider,
                    transformer = intsTransformer,
                ),
            )
            .build()
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

    @Test
    fun getMergeInfoTest() {
        val strings = (1..5).map { number ->
            mockDescribed(pointer = 10 + number)
        }
        val stringTUpdated = strings[2].copy(
            updated = (1_000 + 113).milliseconds,
            hash = "item:hash:13:t:updated",
            item = "item:13:t:updated",
        )
        val stringRUpdated = strings[3].copy(
            updated = (1_000 + 114).milliseconds,
            hash = "item:hash:14:r:updated",
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
            mockDescribed(pointer = 20 + number, item = number)
        }
        val intTUpdated = ints[2].copy(
            updated = (1_000 + 123).milliseconds,
            hash = "item:hash:23:t:updated",
            item = 123,
        )
        val intRUpdated = ints[3].copy(
            updated = (1_000 + 124).milliseconds,
            hash = "item:hash:24:r:updated",
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
        val stringsTransformer = strings.map {
            it.item.toByteArray() to it.item
        } + listOf(
            mockDescribed(pointer = 16).let { it.item.toByteArray() to it.item },
            mockDescribed(pointer = 17).let { it.item.toByteArray() to it.item },
            stringTUpdated.let { it.item.toByteArray() to it.item },
            stringRUpdated.let { it.item.toByteArray() to it.item },
        )
        val intsTransformer = ints.map {
            it.item.toString().toByteArray() to it.item
        } + listOf(
            mockDescribed(pointer = 26, item = 26).let { it.item.toString().toByteArray() to it.item },
            mockDescribed(pointer = 27, item = 27).let { it.item.toString().toByteArray() to it.item },
            intTUpdated.let { it.item.toString().toByteArray() to it.item },
            intRUpdated.let { it.item.toString().toByteArray() to it.item },
        )
        val hashes = listOf(
            strings.hash() to "strings:hash",
            ints.hash() to "ints:hash",
            stringsTUpdated.hash() to "strings:hash:t:updated",
            stringsRUpdated.hash() to "strings:hash:R:updated",
            intsTUpdated.hash() to "ints:hash:t:updated",
            intsRUpdated.hash() to "ints:hash:r:updated",
        ) + strings.map {
            it.item.toByteArray() to it.info.hash
        } + ints.map {
            it.item.toString().toByteArray() to it.info.hash
        } + listOf(
            mockDescribed(pointer = 16).let { it.item.toByteArray() to it.info.hash },
            mockDescribed(pointer = 17).let { it.item.toByteArray() to it.info.hash },
            stringTUpdated.let { it.item.toByteArray() to it.info.hash },
            stringRUpdated.let { it.item.toByteArray() to it.info.hash },
            mockDescribed(pointer = 26, item = 26).let { it.item.toString().toByteArray() to it.info.hash },
            mockDescribed(pointer = 27, item = 27).let { it.item.toString().toByteArray() to it.info.hash },
            intTUpdated.let { it.item.toString().toByteArray() to it.info.hash },
            intRUpdated.let { it.item.toString().toByteArray() to it.info.hash },
        )
        var time = 1.milliseconds
        val timeProvider = mockProvider { time }
        var itemId = mockUUID()
        val uuidProvider = mockProvider { itemId }
        val tStorages = SyncStorages.Builder()
            .add(
                MockSyncStreamsStorage<String>(
                    id = mockUUID(1),
                    hashes = hashes,
                    timeProvider = timeProvider,
                    uuidProvider = uuidProvider,
                    transformer = stringsTransformer,
                ),
            )
            .add(
                MockSyncStreamsStorage<Int>(
                    id = mockUUID(2),
                    hashes = hashes,
                    timeProvider = timeProvider,
                    uuidProvider = uuidProvider,
                    transformer = intsTransformer,
                ),
            )
            .build()
        val rStorages = SyncStorages.Builder()
            .add(
                MockSyncStreamsStorage<String>(
                    id = mockUUID(1),
                    hashes = hashes,
                    timeProvider = timeProvider,
                    uuidProvider = uuidProvider,
                    transformer = stringsTransformer,
                ),
            )
            .add(
                MockSyncStreamsStorage<Int>(
                    id = mockUUID(2),
                    hashes = hashes,
                    timeProvider = timeProvider,
                    uuidProvider = uuidProvider,
                    transformer = intsTransformer,
                ),
            )
            .build()
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
        //        01 23 4 5
        // items: 12|34|5|
        //         0 12 3 4
        //     T: _2|T4|5|6
        //     R: 1_|3R|5| 7
        tStorages.assertMergeInfo(
            storage = rStorages,
            expected = mapOf(
                mockUUID(1) to mockMergeInfo(
                    download = setOf(strings[3].id, mockDescribed(pointer = 17).id),
                    items = listOf(
                        stringTUpdated.map { it.toByteArray() },
                        mockDescribed(pointer = 16).map { it.toByteArray() },
                    ),
                    deleted = setOf(strings[0].id),
                ),
                mockUUID(2) to mockMergeInfo(
                    download = setOf(ints[3].id, mockDescribed(pointer = 27, item = 27).id),
                    items = listOf(
                        intTUpdated.map { it.toString().toByteArray() },
                        mockDescribed(pointer = 26, item = 26).map { it.toString().toByteArray() },
                    ),
                    deleted = setOf(ints[0].id),
                ),
            ),
        )
        rStorages.assertMergeInfo(
            storage = tStorages,
            expected = mapOf(
                mockUUID(1) to mockMergeInfo(
                    download = setOf(strings[2].id, mockDescribed(pointer = 16).id),
                    items = listOf(
                        stringRUpdated.map { it.toByteArray() },
                        mockDescribed(pointer = 17).map { it.toByteArray() },
                    ),
                    deleted = setOf(strings[1].id),
                ),
                mockUUID(2) to mockMergeInfo(
                    download = setOf(ints[2].id, mockDescribed(pointer = 26, item = 26).id),
                    items = listOf(
                        intRUpdated.map { it.toString().toByteArray() },
                        mockDescribed(pointer = 27, item = 27).map { it.toString().toByteArray() },
                    ),
                    deleted = setOf(ints[1].id),
                ),
            ),
        )
    }

    @Test
    fun mergeTest() {
        val strings = (1..5).map { number ->
            mockDescribed(pointer = 10 + number)
        }
        val stringTUpdated = strings[2].copy(
            updated = (1_000 + 113).milliseconds,
            hash = "item:hash:13:t:updated",
            item = "item:13:t:updated",
        )
        val stringRUpdated = strings[3].copy(
            updated = (1_000 + 114).milliseconds,
            hash = "item:hash:14:r:updated",
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
        val stringsFinal = listOf(
            stringTUpdated,
            stringRUpdated,
        ) + (15..17).map { pointer ->
            mockDescribed(pointer = pointer)
        }
        val ints = (1..5).map { number ->
            mockDescribed(pointer = 20 + number, item = number)
        }
        val intTUpdated = ints[2].copy(
            updated = (1_000 + 123).milliseconds,
            hash = "item:hash:23:t:updated",
            item = 123,
        )
        val intRUpdated = ints[3].copy(
            updated = (1_000 + 124).milliseconds,
            hash = "item:hash:24:r:updated",
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
        val intsFinal = listOf(
            intTUpdated,
            intRUpdated,
        ) + (25..27).map { pointer ->
            mockDescribed(pointer = pointer, item = pointer)
        }
        val stringsTransformer = strings.map {
            it.item.toByteArray() to it.item
        } + listOf(
            mockDescribed(pointer = 16).let { it.item.toByteArray() to it.item },
            mockDescribed(pointer = 17).let { it.item.toByteArray() to it.item },
            stringTUpdated.let { it.item.toByteArray() to it.item },
            stringRUpdated.let { it.item.toByteArray() to it.item },
        )
        val intsTransformer = ints.map {
            it.item.toString().toByteArray() to it.item
        } + listOf(
            mockDescribed(pointer = 26, item = 26).let { it.item.toString().toByteArray() to it.item },
            mockDescribed(pointer = 27, item = 27).let { it.item.toString().toByteArray() to it.item },
            intTUpdated.let { it.item.toString().toByteArray() to it.item },
            intRUpdated.let { it.item.toString().toByteArray() to it.item },
        )
        val hashes = listOf(
            strings.hash() to "strings:hash",
            ints.hash() to "ints:hash",
            stringsTUpdated.hash() to "strings:hash:t:updated",
            stringsRUpdated.hash() to "strings:hash:R:updated",
            stringsFinal.hash() to "strings:hash:final",
            intsTUpdated.hash() to "ints:hash:t:updated",
            intsRUpdated.hash() to "ints:hash:r:updated",
            intsFinal.hash() to "ints:hash:final",
        ) + strings.map {
            it.item.toByteArray() to it.info.hash
        } + ints.map {
            it.item.toString().toByteArray() to it.info.hash
        } + listOf(
            mockDescribed(pointer = 16).let { it.item.toByteArray() to it.info.hash },
            mockDescribed(pointer = 17).let { it.item.toByteArray() to it.info.hash },
            stringTUpdated.let { it.item.toByteArray() to it.info.hash },
            stringRUpdated.let { it.item.toByteArray() to it.info.hash },
            mockDescribed(pointer = 26, item = 26).let { it.item.toString().toByteArray() to it.info.hash },
            mockDescribed(pointer = 27, item = 27).let { it.item.toString().toByteArray() to it.info.hash },
            intTUpdated.let { it.item.toString().toByteArray() to it.info.hash },
            intRUpdated.let { it.item.toString().toByteArray() to it.info.hash },
        )
        var time = 1.milliseconds
        val timeProvider = mockProvider { time }
        var itemId = mockUUID()
        val uuidProvider = mockProvider { itemId }
        val tStorages = SyncStorages.Builder()
            .add(
                MockSyncStreamsStorage<String>(
                    id = mockUUID(1),
                    hashes = hashes,
                    timeProvider = timeProvider,
                    uuidProvider = uuidProvider,
                    transformer = stringsTransformer,
                ),
            )
            .add(
                MockSyncStreamsStorage<Int>(
                    id = mockUUID(2),
                    hashes = hashes,
                    timeProvider = timeProvider,
                    uuidProvider = uuidProvider,
                    transformer = intsTransformer,
                ),
            )
            .build()
        val rStorages = SyncStorages.Builder()
            .add(
                MockSyncStreamsStorage<String>(
                    id = mockUUID(1),
                    hashes = hashes,
                    timeProvider = timeProvider,
                    uuidProvider = uuidProvider,
                    transformer = stringsTransformer,
                ),
            )
            .add(
                MockSyncStreamsStorage<Int>(
                    id = mockUUID(2),
                    hashes = hashes,
                    timeProvider = timeProvider,
                    uuidProvider = uuidProvider,
                    transformer = intsTransformer,
                ),
            )
            .build()
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
        //        01 23 4 5
        // items: 12|34|5|
        //         0 12 3 4
        //     T: _2|T4|5|6
        //     R: 1_|3R|5| 7
        // final:    TR|5|67
        val tCommitInfo = tStorages.merge(rStorages.getMergeInfo(tStorages.getSyncInfo(rStorages.hashes())))
        check(tCommitInfo.keys.sorted() == listOf(mockUUID(1), mockUUID(2)))
        SyncStreamsStorageTest.assert(
            expected = mockCommitInfo(
                hash = "strings:hash:final",
                items = listOf(
                    stringsTUpdated[1].map { it.toByteArray() },
                    stringsTUpdated[4].map { it.toByteArray() },
                ),
                deleted = setOf(strings[0].id),
            ),
            actual = tCommitInfo[mockUUID(1)] ?: TODO(),
        )
        SyncStreamsStorageTest.assert(
            expected = mockCommitInfo(
                hash = "ints:hash:final",
                items = listOf(
                    intsTUpdated[1].map { it.toString().toByteArray() },
                    intsTUpdated[4].map { it.toString().toByteArray() },
                ),
                deleted = setOf(ints[0].id),
            ),
            actual = tCommitInfo[mockUUID(2)] ?: TODO(),
        )
        TODO("tStorages:items")
        TODO("rStorages:merge")
        TODO("rStorages:items")
    }

    @Test
    fun commitTest() {
        TODO("SyncStoragesTest:commitTest")
    }
}
