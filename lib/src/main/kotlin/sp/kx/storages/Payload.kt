package sp.kx.storages

import kotlin.time.Duration

class Payload<T : Any>(
    val meta: Metadata,
    val value: T,
) {
    fun copy(
        updated: Duration,
        hash: ByteArray,
        size: Int,
        value: T,
    ): Payload<T> {
        return Payload(
            meta = meta.copy(updated = updated, hash = hash, size = size),
            value = value,
        )
    }
}
