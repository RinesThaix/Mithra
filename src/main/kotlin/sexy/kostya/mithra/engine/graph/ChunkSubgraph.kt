package sexy.kostya.mithra.engine.graph

import sexy.kostya.mithra.bridge.Chunk

class ChunkSubgraph(
    chunk: Chunk
) {

    private val minSection = chunk.minSection
    private val sections = Array(chunk.maxSection - chunk.minSection) { ChunkSectionSubgraph() }

    fun getSectionSubgraph(sectionIndex: Int) = sections[sectionIndex - minSection]

}