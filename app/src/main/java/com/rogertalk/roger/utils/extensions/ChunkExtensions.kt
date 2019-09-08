package com.rogertalk.roger.utils.extensions

import com.rogertalk.roger.models.json.Chunk
import java.util.*


/**
 * The duration of all the chunks in the iterable.
 */
val Iterable<Chunk>.duration: Int
    get() = this.sumBy { it.duration }

/**
 * Gets all chunks after the provided chunk (or nothing if the chunk was not found).
 */
fun List<Chunk>.afterChunk(chunk: Chunk): List<Chunk> {
    val index = this.indexOfFirst { it.id == chunk.id }
    if (index == -1) return ArrayList()
    return this.drop(index + 1)
}

/**
 * Gets all chunks after the provided chunk with tha chunk included (or nothing if the chunk was not found).
 */
fun List<Chunk>.fromChunk(chunk: Chunk): List<Chunk> {
    val index = this.indexOfFirst { it.id == chunk.id }
    if (index == -1) return ArrayList()
    return this.drop(index)
}