package sp.service.sample

import sp.kx.storages.HashFunction
import sp.kx.storages.SyncStreamsStorage
import sp.kx.storages.SyncStreamsStorages
import sp.kx.storages.Transformer
import sp.kx.storages.require
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private data class Foo(val text: String)

private object FooTransformer : Transformer<Foo> {
    override fun encode(decoded: Foo): ByteArray {
        return decoded.text.toByteArray()
    }

    override fun decode(encoded: ByteArray): Foo {
        return Foo(text = String(encoded))
    }
}

private object DefaultEnvironment : SyncStreamsStorage.Environment {
    private var pointer = 1

    override fun now(): Duration {
        Thread.sleep(250)
        return System.currentTimeMillis().milliseconds
    }

    override fun randomUUID(): UUID {
        return UUID.fromString("00000000-0000-0000-0000-0000000${10_000 + pointer++ % 1024}")
    }
}

private fun Byte.toHEX(): String {
    return String.format(Locale.US, "%02x", toInt() and 0xff)
}

private fun ByteArray.toHEX(): String {
    return joinToString(separator = "") { it.toHEX() }
}

private fun SyncStreamsStorages.println() {
    val builder = StringBuilder()
    val hashes = hashes()
    builder.append("\n")
        .append(" hashes:")
    hashes.forEach { (id, bytes) ->
        builder.append("\n").append("$id: ${bytes.toHEX()}")
    }
    hashes.forEach { (id, _) ->
        builder.append("\n")
            .append(" items:")
            .append("\n")
            .append("$id")
        val storages = require(id = id)
        storages.items.forEachIndexed { index, item ->
            builder.append("\n")
                .append("  $index] ${item.id}: ${item.item}")
        }
    }
    println(builder.toString())
}

private var current = 1
private fun nextInt(): Int {
    return current++
}

private fun commit(
    srcStorages: SyncStreamsStorages,
    dstStorages: SyncStreamsStorages,
) {
    println("\ncommit...")
    val hashes = srcStorages.hashes()
//    println("hashes: ${hashes.map { (id, bytes) -> "$id: ${bytes.toHEX()}" }}")
    val sis = dstStorages.getSyncInfo(hashes = hashes)
    for ((storageId, si) in sis) {
        println("dst:SyncInfo: $storageId")
        si.infos.keys.sorted().forEachIndexed { index, itemId ->
            println("$index] $itemId: ${si.infos[itemId]!!.hash.toHEX()}")
        }
        println("deleted:")
        si.deleted.forEachIndexed { index, id ->
            println("$index] $id")
        }
        println("src:")
        srcStorages.require(id = storageId).items.forEachIndexed { index, it ->
            println("$index] ${it.id}: ${it.info.hash.toHEX()}")
        }
    }
    val mis = srcStorages.getMergeInfo(infos = sis)
    for ((storageId, mi) in mis) {
        println("src:MergeInfo: $storageId")
        mi.items.forEachIndexed { index, item ->
            println("$index] ${item.id}: ${String(item.item)}")
        }
        println("deleted:")
        mi.deleted.forEachIndexed { index, id ->
            println("$index] $id")
        }
        println("dst:")
        dstStorages.require(id = storageId).items.forEachIndexed { index, it ->
            println("$index] ${it.id}: ${it.item}")
        }
    }
    val cis = dstStorages.merge(infos = mis)
    for ((storageId, ci) in cis) {
        println("dst:CommitInfo: $storageId")
        ci.items.forEachIndexed { index, item ->
            println("$index] ${item.id}: ${String(item.item)}")
        }
        println("deleted:")
        ci.deleted.forEachIndexed { index, id ->
            println("$index] $id")
        }
    }
    srcStorages.commit(infos = cis)
    check(srcStorages.hashes().keys.all { id -> srcStorages.require(id = id).items == dstStorages.require(id = id).items })
//    storages.println()
    println("")
}

fun main() {
    val dir = File("/tmp/sample")
    dir.deleteRecursively()
    val hf: HashFunction = MDHashFunction("MD5")
    val idFoo = UUID.fromString("548ba538-0ff1-43ba-8b36-4bdbe4c32aef")
    val tStorages = SyncStreamsStorages.Builder()
        .add(idFoo, FooTransformer)
        .build(
            hf = hf,
            env = DefaultEnvironment,
            dir = File("/tmp/sample/t"),
        )
    tStorages.println()
    Foo(text = "foo:${nextInt()}").also { item ->
        println("add: $item")
        tStorages.require<Foo>().add(item)
    }
    val tDescribed = Foo(text = "foo:${nextInt()}").let { item ->
        println("add: $item")
        tStorages.require<Foo>().add(item)
    }
    tStorages.println()
    //
    val rStorages = SyncStreamsStorages.Builder()
        .add(idFoo, FooTransformer)
        .build(
            hf = hf,
            env = DefaultEnvironment,
            dir = File("/tmp/sample/r"),
        )
    rStorages.println()
    Foo(text = "foo:${nextInt()}").also { item ->
        println("add: $item")
        rStorages.require<Foo>().add(item)
    }
    val rDescribed = Foo(text = "foo:${nextInt()}").let { item ->
        println("add: $item")
        rStorages.require<Foo>().add(item)
    }
    rStorages.println()
    //
    commit(srcStorages = tStorages, dstStorages = rStorages)
    //
    println("delete: ${tDescribed.id}")
    val deleted = tStorages.require<Foo>().delete(id = tDescribed.id)
    check(deleted)
    tStorages.println()
    println("update: ${rDescribed.id}")
    val rItemInfo = rStorages.require<Foo>().update(id = rDescribed.id, item = Foo(text = "foo:${nextInt()}"))
    checkNotNull(rItemInfo)
    rStorages.println()
    //
    commit(srcStorages = tStorages, dstStorages = rStorages)
    //
    println("update: ${rDescribed.id}")
    tStorages.require<Foo>().update(id = rDescribed.id, item = Foo(text = "foo:${nextInt()}"))
    //
    commit(srcStorages = tStorages, dstStorages = rStorages)
    //
    println("transmitter:")
    tStorages.println()
    println("receiver:")
    rStorages.println()
}
