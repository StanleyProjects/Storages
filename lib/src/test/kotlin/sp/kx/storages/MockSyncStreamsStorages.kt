package sp.kx.storages

import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal fun SyncStreamsStorages.Builder.mock(
    hashes: List<Pair<ByteArray, ByteArray>> = emptyList(),
    timeProvider: MockProvider<Duration> = MockProvider { 1.milliseconds },
    uuidProvider: MockProvider<UUID> = MockProvider { UUID.fromString("d2d7c21b-f99a-4f78-80d4-8bf05ee25f62") },
    streamerProvider: SyncStreamsStorages.StreamerProvider = MockStreamerProvider(),
) = build(
    hf = MockHashFunction(hashes = hashes),
    pointers = MockPointers(),
    env = object : SyncStreamsStorage.Environment {
        override fun now(): Duration {
            return timeProvider.provide()
        }

        override fun randomUUID(): UUID {
            return uuidProvider.provide()
        }
    },
    streamerProvider = streamerProvider,
)
