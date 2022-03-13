package sexy.kostya.mithra.engine.graph

import sexy.kostya.mithra.bridge.Chunk
import sexy.kostya.mithra.engine.BlockLocation
import sexy.kostya.mithra.engine.LightProcessor
import java.util.concurrent.ConcurrentHashMap

abstract class GraphBasedLightProcessor : LightProcessor {

    private val chunkSubgraphs = ConcurrentHashMap<Long, ChunkSubgraph>()

    final override fun handleChunkLoad(chunk: Chunk) {
        val subgraph = ChunkSubgraph(chunk)
        synchronized(chunk) {
            populateChunkSubgraph(chunk, subgraph)
        }
        chunkSubgraphs[chunk.id] = subgraph
    }

    final override fun handleChunkUnload(chunk: Chunk) {
        chunkSubgraphs.remove(chunk.id)
    }

    abstract fun populateChunkSubgraph(chunk: Chunk, subgraph: ChunkSubgraph)

    protected fun getConnections(location: BlockLocation) = getSubgraph(location)?.get(location)

    protected fun connect(fromDirection: Direction, to: BlockLocation) =
        getSubgraph(to)?.set(fromDirection, to, true)

    protected fun disconnect(fromDirection: Direction, to: BlockLocation) =
        getSubgraph(to)?.set(fromDirection, to, false)

    protected fun disconnectAll(location: BlockLocation) = getSubgraph(location)?.disconnectAll(location)

    internal fun getSubgraph(location: BlockLocation) =
        chunkSubgraphs[location.chunk.id]?.getSectionSubgraph(location.sectionIndex)

    protected data class Entry(
        val from: BlockLocation,
        val to: BlockLocation,
        val level: Int
    )

}