package sexy.kostya.mithra.engine.blocks

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import sexy.kostya.mithra.bridge.Block
import sexy.kostya.mithra.bridge.Chunk
import sexy.kostya.mithra.bridge.ChunkSection
import sexy.kostya.mithra.bridge.fromChunkCoordinate
import sexy.kostya.mithra.engine.BlockLocation
import sexy.kostya.mithra.engine.LightModification
import sexy.kostya.mithra.engine.graph.*
import java.util.*
import kotlin.math.max

class BlocksGraphBasedLightProcessor : GraphBasedLightProcessor() {

    override fun handleUpdate(location: BlockLocation, previous: Block, new: Block): LightModification? {
        val connections = getConnections(location)
        checkNotNull(connections)
        val previousLevel = location.getBlockLightLevel()
        val lightEmission = new.lightEmission
        val nextLevel = max(0, lightEmission - 1)
        val queue = LinkedList<Entry>()
        if (previousLevel < lightEmission) {
            disconnectAll(location)
            location.setBlockLightLevel(lightEmission)
            @Suppress("DEPRECATION")
            for (dir in Directions.All) {
                location.relative(dir)?.let { queue.add(Entry(location, it, nextLevel)) }
            }
        } else {
            if (connections.isNotEmpty()) {
                check(!previous.opaque)
                if (!new.opaque && previousLevel > lightEmission) {
                    return null
                }
                disconnectAll(location)
            }
            location.setBlockLightLevel(lightEmission)
            @Suppress("ConvertArgumentToSet")
            for (dir in Directions.All - connections) {
                location.relative(dir)?.let { queue.add(Entry(location, it, nextLevel)) }
            }
        }
        val modification = LightModification()
        processUpdates(modification, queue)
        if (previousLevel != location.getBlockLightLevel()) {
            modification.add(location)
        }
        return if (modification.isEmpty()) null else modification
    }

    private fun processUpdates(
        modification: LightModification,
        queue: Queue<Entry>
    ) {
        val cache = Object2IntOpenHashMap<BlockLocation>()
        cache.defaultReturnValue(-1)
        var iterations = 0
        var validatedIterations = 0
        while (queue.isNotEmpty()) {
            ++iterations
            val (from, to, level) = queue.poll()
            if (cache.getInt(to) >= level) {
                continue
            }
            ++validatedIterations
            val fromDirection = from.getDirection(to)
            disconnect(fromDirection, to)
            val connections = getConnections(to)
            checkNotNull(connections)
            val previousLevel = to.getBlockLightLevel()
            println("blocks $from -> $to with $level (previously $previousLevel, connections=$connections, fromDir=$fromDirection)")
            if (previousLevel < level) {
                if (to.block.opaque) {
                    continue
                }
                println("#1")
                disconnectAll(to)
                to.setBlockLightLevel(level)
                modification.add(to)
                connect(fromDirection, to)
                cache[to] = level
                propagate(queue, fromDirection, to, max(0, level - 1))
            } else if (previousLevel == level) {
                println("#2")
                if (level != 0 && level != to.block.lightEmission) {
                    connect(fromDirection, to)
                }
            } else if (connections.isNotEmpty()) {
                println("#3")
                if (previousLevel > level + 2) {
                    println("#3.2")
                    queue.clear()
                    queue.add(Entry(to, from, previousLevel - 1))
                }
            } else {
                val lightEmission = to.block.lightEmission
                println("#4, lightEmission=$lightEmission")
                if (lightEmission >= level) {
                    println("#4.2")
                    if (lightEmission < previousLevel) {
                        println("#4.2.2")
                        to.setBlockLightLevel(lightEmission)
                        modification.add(to)
                    }
                    if (lightEmission > level + 2) {
                        println("#4.2.3")
                        queue.clear()
                        queue.add(Entry(to, from, lightEmission - 1))
                    }
                    cache[to] = lightEmission
                    propagate(queue, fromDirection, to, max(0, lightEmission - 1))
                } else {
                    println("#4.3")
                    check(!to.block.opaque)
                    to.setBlockLightLevel(level)
                    modification.add(to)
                    if (level != 0) {
                        connect(fromDirection, to)
                    }
                    cache[to] = level
                    propagate(queue, fromDirection, to, max(0, level - 1))
                }
            }
        }
        println("blocks $validatedIterations/$iterations (${cache.size})")
    }

    private fun propagate(
        queue: Queue<Entry>,
        fromDirection: Direction,
        to: BlockLocation,
        nextLevel: Int
    ) {
        @Suppress("DEPRECATION")
        for (dir in Directions.All) {
            if (dir != fromDirection) {
                to.relative(dir)?.let { queue.add(Entry(to, it, nextLevel)) }
            }
        }
    }

    override fun populateChunkSubgraph(chunk: Chunk, subgraph: ChunkSubgraph) {
        val chunkStartX = chunk.x.fromChunkCoordinate()
        val chunkStartZ = chunk.z.fromChunkCoordinate()
        for (sectionIndex in chunk.minSection until chunk.maxSection) {
            val section = chunk.getSection(sectionIndex)
            val sectionSubgraph = subgraph.getSectionSubgraph(sectionIndex)
            val sectionStartY = sectionIndex.fromChunkCoordinate()
            fun require(condition: Boolean, type: String) = require(condition) {
                "Bad block light data at chunk (%d; %d) section %d: %s".format(chunk.x, chunk.z, sectionIndex, type)
            }
            for (x in ChunkSection.SizeRange) {
                for (y in ChunkSection.SizeRange) {
                    for (z in ChunkSection.SizeRange) {
                        val level = section.getBlockLightLevel(x, y, z)
                        if (level == 0) {
                            continue
                        }
                        val location = BlockLocation(
                            chunk,
                            sectionIndex,
                            chunkStartX + x,
                            sectionStartY + y,
                            chunkStartZ + z
                        )
                        val block = location.block
                        if (block.lightEmission == level) {
                            continue
                        }
                        require(
                            !block.opaque,
                            "block (%d;%d;%d) is opaque, but has light value higher than it's own emission".format(x, y, z)
                        )
                        var parents = 0
                        val iterator = Directions.All.intIterator()
                        while (iterator.hasNext()) {
                            val dir = iterator.nextInt().toByte()
                            val relative = location.relative(dir) ?: continue
                            val relativeLevel = relative.getBlockLightLevel()
                            if (relativeLevel == level + 1) {
                                sectionSubgraph.set(dir, location, true)
                                ++parents
                            } else {
                                require(
                                    relativeLevel < level + 1,
                                    "block (%d;%d;%d) must have had higher light value".format(x, y, z)
                                )
                            }
                        }
                        require(
                            parents > 0,
                            "block (%d;%d;%d) has light value out of nowhere".format(x, y, z)
                        )
                    }
                }
            }
        }
    }
}