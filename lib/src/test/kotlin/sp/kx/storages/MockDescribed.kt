package sp.kx.storages

import java.util.UUID

internal fun <T : Any> mockDescribed(
    id: UUID = mockUUID(1),
    info: ItemInfo = mockItemInfo(),
    item: T = TODO("mock:described"),
): Described<T> {
    return Described(
        id = id,
        info = info,
        item = item,
    )
}

internal fun mockDescribed(pointer: Int): Described<String> {
    return mockDescribed(
        id = mockUUID(pointer = pointer),
        info = mockItemInfo(pointer = pointer),
        item = "item:$pointer",
    )
}
