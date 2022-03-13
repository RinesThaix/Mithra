package sexy.kostya.mithra.engine.graph

import it.unimi.dsi.fastutil.bytes.ByteArrayList
import it.unimi.dsi.fastutil.bytes.ByteList
import sexy.kostya.mithra.bridge.ChunkSection
import sexy.kostya.mithra.engine.BlockLocation

class ChunkSectionSubgraph {

    companion object {
        private val EmptyArray = ByteArray(0)
        private val NewArray: ByteArray
            get() = ByteArray(3072)

        private fun index(x: Int, y: Int, z: Int) = ChunkSection.index(x, y, z)

        const val Connected = 0x1
    }

    private var graph = EmptyArray
    private var entries = 0

    fun set(fromDirection: Direction, location: BlockLocation, connected: Boolean) =
        set(fromDirection, location.x, location.y, location.z, connected)

    fun set(fromDirection: Direction, x: Int, y: Int, z: Int, connected: Boolean) =
        set(index(x, y, z), fromDirection, connected)

    private fun set(index: Int, direction: Direction, connected: Boolean): Boolean {
        if (graph.isEmpty()) {
            if (!connected) {
                return false
            }
            graph = NewArray
        }
        val pos = index.shr(2) * 3
        val dir = direction.toInt()
        val changed = when (index and 3) {
            0 -> changeEdge(pos, dir, connected)
            1 -> if (dir <= 1) {
                changeEdge(pos, dir + 6, connected)
            } else {
                changeEdge(pos + 1, dir - 2, connected)
            }
            2 -> if (dir <= 3) {
                changeEdge(pos + 1, dir + 4, connected)
            } else {
                changeEdge(pos + 2, dir - 4, connected)
            }
            else -> changeEdge(pos + 2, dir + 2, connected)
        }
        if (changed) {
            if (!connected) {
                if (--entries == 0) {
                    graph = EmptyArray
                }
            } else {
                ++entries
            }
            return true
        }
        return false
    }

    private fun changeEdge(pos: Int, offset: Int, connected: Boolean): Boolean {
        val prev = graph[pos].toInt()
        val new = if (connected) {
            prev or Connected.shl(offset)
        } else {
            prev and Connected.shl(offset).inv()
        }
        return if (prev != new) {
            graph[pos] = new.toByte()
            true
        } else {
            false
        }
    }

    fun disconnectAll(location: BlockLocation) = disconnectAll(index(location.x, location.y, location.z))

    private fun disconnectAll(index: Int): Boolean {
        if (graph.isEmpty()) {
            return false
        }
        val pos = index.shr(2) * 3
        val disconnected = when (index and 3) {
            0 -> deleteEdges(pos, 0..5)
            1 -> {
                deleteEdges(pos, 6..7)
                deleteEdges(pos + 1, 0..3)
            }
            2 -> {
                deleteEdges(pos + 1, 4..7)
                deleteEdges(pos + 2, 0..1)
            }
            else -> deleteEdges(pos + 2, 2..7)
        }
        return if (disconnected > 0) {
            entries -= disconnected
            if (entries == 0) {
                graph = EmptyArray
            }
            true
        } else {
            false
        }
    }

    private fun deleteEdges(pos: Int, range: IntRange): Int {
        val prev = graph[pos].toInt()
        var mask = 0
        var count = 0
        for (i in range) {
            val bit = Connected shl i
            if (prev and bit == bit) {
                ++count
                mask = mask or bit
            }
        }
        if (count > 0) {
            graph[pos] = prev.and(mask.inv()).toByte()
        }
        return count
    }

    fun get(location: BlockLocation): List<Direction> {
        if (graph.isEmpty()) {
            return emptyList()
        }
        val index = index(location.x, location.y, location.z)
        val result = ByteArrayList(6)
        val pos = index.shr(2) * 3
        when (index and 3) {
            0 -> readDirections(pos, 0..5, 0, result)
            1 -> {
                readDirections(pos, 6..7, -6, result)
                readDirections(pos + 1, 0..3, 2, result)
            }
            2 -> {
                readDirections(pos + 1, 4..7, -4, result)
                readDirections(pos + 2, 0..1, 4, result)
            }
            else -> readDirections(pos + 2, 2..7, -2, result)
        }
        return result
    }

    private fun readDirections(pos: Int, range: IntRange, offset: Int, result: ByteList) {
        val value = graph[pos].toInt()
        for (i in range) {
            if (value.shr(i).and(Connected) == Connected) {
                result.add((i + offset).toByte())
            }
        }
    }

}