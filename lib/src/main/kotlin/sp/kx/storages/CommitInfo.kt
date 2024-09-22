package sp.kx.storages

import sp.kx.bytes.toHEX
import java.util.Objects
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
class CommitInfo(
    val hash: ByteArray,
    val items: List<RawPayload>,
    val deleted: Set<UUID>,
) {
    override fun toString(): String {
        return "{" +
            "items: $items, " +
            "deleted: $deleted, " +
            "hash: \"${hash.toHEX()}\"" +
            "}"
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is CommitInfo -> {
                other.hash.contentEquals(hash) && other.items == items && other.deleted == deleted
            }
            else -> false
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(
            hash.contentHashCode(),
            items,
            deleted,
        )
    }
}
