package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

internal class SyncStreamsStorageTest {
    @Test
    fun idTest() {
        val id = UUID.fromString("dc4092c6-e7a1-433e-9169-c2f6f92fc4c1")
        val storageHash = "storageHash"
        val storage = MockSyncStreamsStorage<String>(
            id = id,
            hashes = listOf(
                "".toByteArray() to storageHash,
            ),
        )
        assertEquals(id, storage.id)
        assertEquals(storageHash, storage.hash)
        assertTrue(storage.deleted.isEmpty())
    }

    @Test
    fun hashTest() {TODO("SyncStreamsStorageTest:hashTest")}

    @Test
    fun itemsTest() {TODO("SyncStreamsStorageTest:itemsTest")}

    @Test
    fun deletedTest() {TODO("SyncStreamsStorageTest:deletedTest")}

    @Test
    fun deleteTest() {TODO("SyncStreamsStorageTest:deleteTest")}

    @Test
    fun addTest() {TODO("SyncStreamsStorageTest:addTest")}

    @Test
    fun updateTest() {TODO("SyncStreamsStorageTest:updateTest")}

    @Test
    fun mergeTest() {TODO("SyncStreamsStorageTest:mergeTest")}

    @Test
    fun getSyncInfoTest() {TODO("SyncStreamsStorageTest:getSyncInfoTest")}

    @Test
    fun getMergeInfoTest() {TODO("SyncStreamsStorageTest:getMergeInfoTest")}
}
