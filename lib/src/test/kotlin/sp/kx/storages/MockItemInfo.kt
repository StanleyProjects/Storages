package sp.kx.storages

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal fun mockItemInfo(
    updated: Duration = mockDuration(1),
    hash: ByteArray = mockByteArray(1),
    size: Int = 1,
): ItemInfo {
    return ItemInfo(
        updated = updated,
        hash = hash,
        size = size,
    )
}

internal fun mockItemInfo(pointer: Int): ItemInfo {
    val decoded = "item:hash:$pointer"
    return mockItemInfo(
        updated = mockDuration(pointer = pointer),
        hash = MockHashFunction.map(decoded),
        size = decoded.toByteArray().size,
    )
}
