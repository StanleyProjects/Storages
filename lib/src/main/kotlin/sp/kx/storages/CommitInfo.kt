package sp.kx.storages

import java.util.UUID

/**
 * Information obtained as a result of merging data from the transmitter storage to the receiving storage.
 *
 * Usage:
 * ```
 * val receiver: SyncStorage<Foo> = ...
 * val transmitter: SyncStorage<Foo> = ...
 * val syncInfo = receiver.getSyncInfo()
 * val mergeInfo = transmitter.getMergeInfo(syncInfo)
 * val commitInfo = receiver.merge(mergeInfo)
 * ```
 * @author [Stanley Wintergreen](https://github.com/kepocnhh)
 * @since 0.3.1
 */
data class CommitInfo(
    val hash: String,
    val items: List<Described<ByteArray>>,
    val deleted: Set<UUID>,
)
