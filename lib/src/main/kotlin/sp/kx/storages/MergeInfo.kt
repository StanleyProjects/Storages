package sp.kx.storages

import java.util.UUID

/**
 * Information obtained as a result of comparing data from the receiving storage to the transmitting storage.
 *
 * Usage:
 * ```
 * val receiver: SyncStorage<Foo> = ...
 * val transmitter: SyncStorage<Foo> = ...
 * val syncInfo = receiver.getSyncInfo()
 * val mergeInfo = transmitter.getMergeInfo(syncInfo)
 * ```
 * @author [Stanley Wintergreen](https://github.com/kepocnhh)
 * @since 0.4.1
 */
data class MergeInfo(
    val downloaded: Set<UUID>,
    val items: List<Described<ByteArray>>,
    val deleted: Set<UUID>,
)
