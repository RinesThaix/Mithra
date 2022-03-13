package sexy.kostya.mithra.engine

import it.unimi.dsi.fastutil.ints.IntRBTreeSet
import it.unimi.dsi.fastutil.ints.IntSet
import it.unimi.dsi.fastutil.longs.Long2ObjectFunction
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import sexy.kostya.mithra.bridge.*
import java.util.BitSet

class LightModification {

    companion object {
        fun compileUpdate(world: World, minSectionIndex: Int, sky: LightModification?, blocks: LightModification?): List<ChunkLightUpdate> {
            if (sky == null && blocks == null) {
                return emptyList()
            }
            val chunks = LongOpenHashSet((sky?.modifications?.size ?: 0) + (blocks?.modifications?.size ?: 0))
            sky?.let { chunks.addAll(it.modifications.keys) }
            blocks?.let { chunks.addAll(it.modifications.keys) }
            if (chunks.isEmpty()) {
                return emptyList()
            }
            val result = ArrayList<ChunkLightUpdate>(chunks.size)
            val iterator = chunks.longIterator()
            while (iterator.hasNext()) {
                val chunkIndex = iterator.nextLong()
                val chunk = world.getChunk(chunkIndex.toChunkX(), chunkIndex.toChunkZ()) ?: continue

                val skyLightMask = BitSet()
                val emptySkyLightMask = BitSet()
                val blocksLightMask = BitSet()
                val emptyBlocksLightMask = BitSet()
                val skyLights = ArrayList<ByteArray>()
                val blockLights = ArrayList<ByteArray>()

                synchronized(chunk) {
                    sky?.let {
                        populateLightUpdate(
                            "sky",
                            minSectionIndex,
                            chunk,
                            it.modifications[chunkIndex]!!,
                            skyLightMask,
                            emptySkyLightMask,
                            skyLights,
                            ChunkSection::skyLightEntries,
                            ChunkSection::skyLight
                        )
                    }
                    blocks?.let {
                        populateLightUpdate(
                            "blocks",
                            minSectionIndex,
                            chunk,
                            it.modifications[chunkIndex]!!,
                            blocksLightMask,
                            emptyBlocksLightMask,
                            blockLights,
                            ChunkSection::blockLightEntries,
                            ChunkSection::blockLight
                        )
                    }
                }

                result.add(ChunkLightUpdate(chunk, skyLightMask, emptySkyLightMask, blocksLightMask, emptyBlocksLightMask, skyLights, blockLights))
            }
            return result
        }

        private fun populateLightUpdate(
            debugTip: String,
            minSectionIndex: Int,
            chunk: Chunk,
            sections: IntSet,
            nonZeroes: BitSet,
            allZeroes: BitSet,
            lights: MutableList<ByteArray>,
            entriesCountGetter: (ChunkSection) -> Int,
            dataGetter: (ChunkSection) -> ByteArray
        ) {
            val iterator = sections.intIterator()
            while (iterator.hasNext()) {
                val sectionIndex = iterator.nextInt()
                val section = chunk.getSection(sectionIndex)
                val trueSectionIndex = sectionIndex - minSectionIndex + 1
                if (entriesCountGetter(section) == 0) {
                    allZeroes.set(trueSectionIndex)
                } else {
                    nonZeroes.set(trueSectionIndex)
                    lights.add(dataGetter(section))
                }
                println("(${chunk.x};$trueSectionIndex;${chunk.z}) -- ${entriesCountGetter(section)} $debugTip")
            }
        }
    }

    private val modifications = Long2ObjectOpenHashMap<IntSet>()

    fun add(location: BlockLocation) {
        val sections = modifications.computeIfAbsent(location.chunk.id, Long2ObjectFunction { IntRBTreeSet() })
        sections.add(location.sectionIndex)
    }

    fun isEmpty() = modifications.isEmpty()

}