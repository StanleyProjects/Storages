package sp.kx.storages

import java.util.Objects
import kotlin.time.Duration

/**
 * Information about an object in the repository.
 * Can be used for searching, sorting, comparing and synchronizing.
 *
 * @author [Stanley Wintergreen](https://github.com/kepocnhh)
 * @since 0.4.1
 */
class ItemInfo(
    val created: Duration,
    val updated: Duration,
    val hash: ByteArray,
) {
    fun copy(
        updated: Duration,
        hash: ByteArray,
    ): ItemInfo {
        return ItemInfo(
            created = created,
            updated = updated,
            hash = hash,
        )
    }

    override fun toString(): String {
        return "{" +
            "created: ${created.inWholeMilliseconds}ms, " +
            "updated: ${updated.inWholeMilliseconds}ms, " +
            "hash: \"${hash.toHEX()}\"" +
            "}"
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is ItemInfo -> other.created == created && other.updated == updated && other.hash.contentEquals(hash)
            else -> false
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(
            created,
            updated,
            hash.contentHashCode(),
        )
    }
}
