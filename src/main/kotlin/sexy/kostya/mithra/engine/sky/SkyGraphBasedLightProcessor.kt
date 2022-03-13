package sexy.kostya.mithra.engine.sky

import it.unimi.dsi.fastutil.bytes.ByteArraySet
import it.unimi.dsi.fastutil.bytes.ByteSets
import sexy.kostya.mithra.bridge.Block
import sexy.kostya.mithra.bridge.Chunk
import sexy.kostya.mithra.bridge.ChunkSection
import sexy.kostya.mithra.bridge.fromChunkCoordinate
import sexy.kostya.mithra.engine.BlockLocation
import sexy.kostya.mithra.engine.LightModification
import sexy.kostya.mithra.engine.LightProcessor
import sexy.kostya.mithra.engine.graph.*
import java.util.*
import kotlin.math.max

class SkyGraphBasedLightProcessor(
    private val maxY: Int
) : GraphBasedLightProcessor() {

    override fun handleUpdate(location: BlockLocation, previous: Block, new: Block): LightModification? {
        val connections = getConnections(location)
        checkNotNull(connections)
        val previousLevel = location.getSkyLightLevel()
        val modification = LightModification()
        val stack = Stack<Entry>()
        when {
            new.opaque -> {
                val level = 0
                disconnectAll(location)
                if (previousLevel == level) {
                    return null
                }
                location.setSkyLightLevel(level)
                for (dir in Directions.All - connections) {
                    location.relative(dir)?.let { stack.add(Entry(location, it, 0)) }
                }
            }

            connections.isNotEmpty() -> {
                check(!previous.opaque)
                if (previousLevel != LightProcessor.MaxLightLevel) {
                    return null
                }
                val bottomLevel = when (new.propagatesSkylightDown) {
                    previous.propagatesSkylightDown -> return null
                    true -> previousLevel
                    else -> previousLevel - 1
                }
                location.relative(Directions.Bottom)?.let { stack.add(Entry(location, it, bottomLevel)) }
            }

            location.y == maxY -> {
                val level = LightProcessor.MaxLightLevel
                if (previousLevel == level) {
                    val bottomLevel = when (new.propagatesSkylightDown) {
                        previous.propagatesSkylightDown -> return null
                        true -> LightProcessor.MaxLightLevel
                        else -> max(0, previousLevel - 1)
                    }
                    location.relative(Directions.Bottom)?.let { stack.add(Entry(location, it, bottomLevel)) }
                } else {
                    location.setSkyLightLevel(level)
                    val sideLevel = level - 1
                    val bottomLevel = if (new.propagatesSkylightDown) level else sideLevel
                    for (dir in listOf(Directions.West, Directions.East, Directions.South, Directions.North)) {
                        location.relative(dir)?.let { stack.add(Entry(location, it, sideLevel)) }
                    }
                    location.relative(Directions.Bottom)?.let { stack.add(Entry(location, it, bottomLevel)) }
                }
            }

            else -> {
                check(previousLevel == 0)
                val (level, parents) = getSkyLightFromNeighbors(location)
                if (level == 0) {
                    return null
                }
                location.setSkyLightLevel(level)
                val sideLevel = max(0, level - 1)
                val bottomLevel =
                    if (level == LightProcessor.MaxLightLevel && new.propagatesSkylightDown) level else sideLevel
                for (dir in Directions.All - parents - Directions.Bottom) {
                    location.relative(dir)?.let { stack.add(Entry(location, it, sideLevel)) }
                }
                if (!parents.contains(Directions.Bottom)) {
                    location.relative(Directions.Bottom)?.let { stack.add(Entry(location, it, bottomLevel)) }
                }
            }
        }
        processUpdates(modification, stack)
        if (previousLevel != location.getSkyLightLevel()) {
            modification.add(location)
        }
        return if (modification.isEmpty()) null else modification
    }

    private fun processUpdates(
        modification: LightModification,
        stack: Stack<Entry>
    ) {
        while (stack.isNotEmpty()) {
            val (from, to, level) = stack.pop()
            if (to.block.opaque) {
                continue
            }
            val fromDirection = from.getDirection(to)
            disconnect(fromDirection, to)
            val connections = getConnections(to)
            checkNotNull(connections)
            val previousLevel = to.getSkyLightLevel()
            if (previousLevel < level) {
                disconnectAll(to)
                to.setSkyLightLevel(level)
                modification.add(to)
                connect(fromDirection, to)
                propogate(stack, fromDirection, to, level)
            } else if (previousLevel == level) {
                if (level != 0) {
                    connect(fromDirection, to)
                }
            } else if (connections.isNotEmpty()) {
                val reverseLevel =
                    if (fromDirection == Directions.Bottom && previousLevel == LightProcessor.MaxLightLevel && to.block.propagatesSkylightDown) {
                        previousLevel
                    } else {
                        max(0, previousLevel - 1)
                    }
                if (reverseLevel > from.getSkyLightLevel()) {
                    stack.clear()
                    stack.add(Entry(to, from, reverseLevel))
                }
            } else {
                to.setSkyLightLevel(level)
                modification.add(to)
                connect(fromDirection, to)
                propogate(stack, fromDirection, to, level)
            }
        }
    }

    private fun propogate(
        stack: Stack<Entry>,
        fromDirection: Direction,
        to: BlockLocation,
        currentLevel: Int
    ) {
        val sideLevel = max(0, currentLevel - 1)
        val bottomLevel = if (currentLevel == LightProcessor.MaxLightLevel && to.block.propagatesSkylightDown) {
            currentLevel
        } else {
            sideLevel
        }
        for (dir in Directions.All - fromDirection - Directions.Bottom) {
            to.relative(dir)?.let { stack.add(Entry(to, it, sideLevel)) }
        }
        if (fromDirection != Directions.Bottom) {
            to.relative(Directions.Bottom)?.let { stack.add(Entry(to, it, bottomLevel)) }
        }
    }

    private fun getSkyLightFromNeighbors(location: BlockLocation): Pair<Int, Set<Direction>> {
        var level = 0
        val parents = ByteArraySet()
        @Suppress("DEPRECATION")
        for (dir in Directions.All) {
            val relative = location.relative(dir) ?: continue
            val relLevel = relative.getSkyLightLevel()
            if (dir == Directions.Top && relLevel == LightProcessor.MaxLightLevel && relative.block.propagatesSkylightDown) {
                return LightProcessor.MaxLightLevel to ByteSets.singleton(dir)
            }
            val nextLevel = relLevel - 1
            if (nextLevel > level) {
                parents.clear()
                level = nextLevel
                parents.add(dir)
            } else if (nextLevel == level) {
                parents.add(dir)
            }
        }
        return level to parents
    }

    override fun populateChunkSubgraph(chunk: Chunk, subgraph: ChunkSubgraph) {
        val chunkStartX = chunk.x.fromChunkCoordinate()
        val chunkStartZ = chunk.z.fromChunkCoordinate()
        for (sectionIndex in chunk.minSection until chunk.maxSection) {
            val section = chunk.getSection(sectionIndex)
            val sectionSubgraph = subgraph.getSectionSubgraph(sectionIndex)
            val sectionStartY = sectionIndex.fromChunkCoordinate()
            fun require(condition: Boolean, type: String) = require(condition) {
                "Bad skylight data at chunk (%d; %d) section %d: %s".format(chunk.x, chunk.z, sectionIndex, type)
            }
            for (y in ChunkSection.SizeRange) {
                if (y == maxY) {
                    continue
                }
                for (x in ChunkSection.SizeRange) {
                    for (z in ChunkSection.SizeRange) {
                        val level = section.getSkyLightLevel(x, y, z)
                        if (level == 0) {
                            continue
                        }
                        if (level == LightProcessor.MaxLightLevel) {
                            val upperLevel = if (y == ChunkSection.Size - 1) {
                                chunk.getSection(sectionIndex + 1).getSkyLightLevel(x, 0, z)
                            } else {
                                section.getSkyLightLevel(x, y, z)
                            }
                            require(
                                upperLevel == LightProcessor.MaxLightLevel,
                                "block (%d;%d;%d) has max light value out of nowhere".format(x, y, z)
                            )
                            sectionSubgraph.set(Directions.Top, x, y, z, true)
                        } else {
                            val location = BlockLocation(
                                chunk,
                                sectionIndex,
                                chunkStartX + x,
                                sectionStartY + y,
                                chunkStartZ + z
                            )
                            val iterator = Directions.All.intIterator()
                            var parents = 0
                            while (iterator.hasNext()) {
                                val dir = iterator.nextInt().toByte()
                                val relative = location.relative(dir) ?: continue
                                val relativeLevel = relative.getSkyLightLevel()
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
}