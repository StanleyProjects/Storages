package sp.kx.storages

import kotlin.time.Duration

internal fun mockItemInfo(
    updated: Duration = mockDuration(1),
    hash: ByteArray = mockByteArray(1),
): ItemInfo {
    return ItemInfo(
        updated = updated,
        hash = hash,
    )
}

internal fun mockItemInfo(pointer: Int, value: String = "payload:$pointer"): ItemInfo {
    return mockItemInfo(
        updated = mockDuration(pointer = pointer),
        hash = MockHashFunction.map(value),
    )
}
