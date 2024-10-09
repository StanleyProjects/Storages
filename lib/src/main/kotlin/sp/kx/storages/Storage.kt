package sp.kx.storages

import java.util.UUID

/**
 * Data reading tool.
 *
 * Usage:
 * ```
 * val storage: Storage<Foo> = ...
 * val items = storage.items
 * println(item.size)
 * for (item in items) {
 *     println(item.toString())
 * }
 * ```
 * @author [Stanley Wintergreen](https://github.com/kepocnhh)
 * @since 0.4.1
 */
interface Storage<T : Any> {
    val id: UUID
    val hash: ByteArray
    val items: List<Payload<T>>

    operator fun get(id: UUID): Payload<T>?

    fun require(id: UUID): Payload<T> {
        return get(id = id) ?: error("No payload by id: $id")
    }
}
