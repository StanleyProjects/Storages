package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrowsExactly
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

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

    @Test
    fun hashesTest() {
        TODO("SyncStoragesTest:hashesTest")
    }
}
