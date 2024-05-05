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
}
