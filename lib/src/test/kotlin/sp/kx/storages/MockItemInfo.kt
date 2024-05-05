package sp.kx.storages

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal fun mockItemInfo(
    created: Duration = 1.milliseconds,
    updated: Duration = 2.milliseconds,
    hash: String = "foo",
): ItemInfo {
    return ItemInfo(
        created = created,
        updated = updated,
        hash = hash,
    )
}

internal fun mockItemInfo(pointer: Int): ItemInfo {
    return mockItemInfo(
        created = (1_000 + pointer).milliseconds,
        updated = (1_000 + pointer).milliseconds,
        hash = "item:hash:$pointer",
    )
}
