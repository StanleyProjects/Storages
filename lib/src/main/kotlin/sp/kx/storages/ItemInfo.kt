package sp.kx.storages

import java.util.Date
import java.util.Objects
import kotlin.time.Duration

class ItemInfo(
    val created: Duration,
    val updated: Duration,
    val hash: String,
) {
    fun copy(
        updated: Duration,
        hash: String,
    ): ItemInfo {
        return ItemInfo(
            created = created,
            updated = updated,
            hash = hash,
        )
    }

    override fun toString(): String {
        return "{created: ${Date(created.inWholeMilliseconds)}, updated: ${Date(updated.inWholeMilliseconds)}, hash: \"$hash\"}"
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is ItemInfo -> other.created == created && other.updated == updated && other.hash == hash
            else -> false
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(
            created,
            updated,
            hash,
        )
    }
}
