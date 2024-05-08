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

    private fun SyncStorages.assertMergeInfo(infos: Map<UUID, SyncInfo>, expected: Map<UUID, MergeInfo>) {
        val actual = getMergeInfo(infos)
        assertEquals(expected.size, actual.size, "MergeInfo:\n$expected\n$actual\n")
        for ((id, value) in expected) {
            assertEquals(value, actual[id] ?: error("No hash by ID: \"$id\"!"))
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
        val strings = (1..7).map { number ->
            mockDescribed(pointer = 10 + number)
        }
        val stringUpdated = strings[2].copy(
            updated = 13.milliseconds,
            hash = "item:hash:13:updated",
            item = "item:13:updated",
        )
        val stringsUpdated = strings.toMutableList().also {
            it.removeAt(0)
            it.removeAt(1)
            it.add(1, stringUpdated)
            it.add(mockDescribed(pointer = 18))
        }.toList()
        val ints = (1..7).map { number ->
            mockDescribed(pointer = 20 + number, item = number)
        }
        val intUpdated = ints[2].copy(
            updated = 23.milliseconds,
            hash = "item:hash:23:updated",
            item = 123,
        )
        val intsUpdated = ints.toMutableList().also {
            it.removeAt(1)
            it.removeAt(1)
            it.add(1, intUpdated)
            it.add(mockDescribed(pointer = 28, item = 28))
        }.toList()
        val stringsTransformer = strings.map {
            it.item.toByteArray() to it.item
        } + listOf(
            mockDescribed(pointer = 18).let { it.item.toByteArray() to it.item },
            stringUpdated.let { it.item.toByteArray() to it.item },
        )
        val intsTransformer = ints.map {
            it.item.toString().toByteArray() to it.item
        } + listOf(
            mockDescribed(pointer = 28, item = 28).let { it.item.toString().toByteArray() to it.item },
            intUpdated.let { it.item.toString().toByteArray() to it.item },
        )
        val hashes = listOf(
            strings.hash() to "strings:hash",
            ints.hash() to "ints:hash",
            stringsUpdated.hash() to "strings:hash:updated",
            intsUpdated.hash() to "ints:hash:updated",
        ) + strings.map {
            it.item.toByteArray() to it.info.hash
        } + ints.map {
            it.item.toString().toByteArray() to it.info.hash
        } + listOf(
            mockDescribed(pointer = 18).let { it.item.toByteArray() to it.info.hash },
            stringUpdated.let { it.item.toByteArray() to it.info.hash },
            mockDescribed(pointer = 28, item = 28).let { it.item.toString().toByteArray() to it.info.hash },
            intUpdated.let { it.item.toString().toByteArray() to it.info.hash },
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
        mockDescribed(pointer = 18).also { described ->
            itemId = described.id
            time = described.info.created
            tStorages.require<String>().add(described.item)
        }
        check(tStorages.require<String>().delete(strings[0].id))
        stringUpdated.also { described ->
            itemId = described.id
            time = described.info.updated
            val info = tStorages.require<String>().update(described.id, described.item)
            checkNotNull(info)
        }
        //
        mockDescribed(pointer = 28, item = 28).also { described ->
            itemId = described.id
            time = described.info.created
            tStorages.require<Int>().add(described.item)
        }
        check(tStorages.require<Int>().delete(ints[1].id))
        intUpdated.also { described ->
            itemId = described.id
            time = described.info.updated
            val info = tStorages.require<Int>().update(described.id, described.item)
            checkNotNull(info)
        }
        //
//        error(strings.map {it.item}) // todo
        tStorages.assertSyncInfo(
            hashes = rStorages.hashes(),
            expected = mapOf(),
        )
        rStorages.assertSyncInfo(
            hashes = tStorages.hashes(),
            expected = mapOf(),
        )
    }

    @Test
    fun getMergeInfoTest() {
        TODO("SyncStoragesTest:getMergeInfoTest")
    }

    @Test
    fun mergeTest() {
        TODO("SyncStoragesTest:mergeTest")
    }

    @Test
    fun commitTest() {
        TODO("SyncStoragesTest:commitTest")
    }
}
