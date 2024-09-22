package sp.kx.storages

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal fun mockDuration(pointer: Int = 0): Duration {
    return pointer.milliseconds
}
