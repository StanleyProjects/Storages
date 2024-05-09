package sp.kx.storages

import java.util.UUID

/**
 * Information that can be used to compare data in another storage.
 *
 * Usage:
 * ```
 * val storage: SyncStorage<Foo> = ...
 * val syncInfo = storage.getSyncInfo()
 * ```
 * @author [Stanley Wintergreen](https://github.com/kepocnhh)
 * @since 0.4.1
 */
data class SyncInfo(
    val infos: Map<UUID, ItemInfo>,
    val deleted: Set<UUID>,
)
