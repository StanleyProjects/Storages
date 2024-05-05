package sp.kx.storages

/**
 * Data synchronization tool.
 *
 * Usage:
 * ```
 * val receiver: SyncStorage<Foo> = ...
 * val transmitter: SyncStorage<Foo> = ...
 * assertEquals(receiver.id, transmitter.id)
 * assertNotEquals(receiver.hash, transmitter.hash)
 * val syncInfo = receiver.getSyncInfo()
 * val mergeInfo = transmitter.getMergeInfo(syncInfo)
 * val commitInfo = receiver.merge(mergeInfo)
 * transmitter.merge(commitInfo)
 * assertEquals(receiver.hash, transmitter.hash)
 * ```
 * @author [Stanley Wintergreen](https://github.com/kepocnhh)
 * @since 0.4.1
 */
interface SyncStorage<T : Any> : MutableStorage<T> {
    fun getSyncInfo(): SyncInfo
    fun getMergeInfo(info: SyncInfo): MergeInfo
    fun merge(info: MergeInfo): CommitInfo
    fun merge(info: CommitInfo)
}
