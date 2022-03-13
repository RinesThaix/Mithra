package sexy.kostya.mithra.engine

import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import sexy.kostya.mithra.bridge.Chunk
import java.util.*

abstract class RecursiveLightProcessor : LightProcessor {

    abstract fun getLightLevel(location: BlockLocation): Int

    abstract fun setLightLevel(location: BlockLocation, level: Int): Boolean

    abstract fun spread(from: BlockLocation, level: Int, except: BlockLocation? = null): List<LightChangeEntry>

    abstract fun handleIncreasingLevel(
        modification: LightModification,
        stack: Stack<LightChangeEntry>,
        cache: Object2IntMap<BlockLocation>,
        from: BlockLocation,
        to: BlockLocation,
        previousLevel: Int,
        level: Int
    )

    protected fun initializeLightSpreading(
        location: BlockLocation,
        modification: LightModification,
        previousLevel: Int,
        newLevel: Int
    ) {
        val stack = Stack<LightChangeEntry>()
        val cache = Object2IntOpenHashMap<BlockLocation>()
        cache.defaultReturnValue(-1)

        setLightLevel(location, newLevel)
        cache[location] = newLevel
        spread(location, newLevel)
        while (stack.isNotEmpty()) {
            val entry = stack.pop()
            spreadTheLight(
                modification,
                stack,
                cache,
                entry.from,
                entry.to,
                entry.level
            )
        }
        if (getLightLevel(location) != previousLevel) {
            modification.add(location)
        }
    }

    private fun spreadTheLight(
        modification: LightModification,
        stack: Stack<LightChangeEntry>,
        cache: Object2IntMap<BlockLocation>,
        from: BlockLocation,
        to: BlockLocation?,
        level: Int
    ) {
        if (to == null) {
            return
        }
        val previousLevel = getLightLevel(to)
        if (previousLevel == level || cache.getInt(to) >= level) {
            return
        }
        if (previousLevel < level) {
            if (to.block.opaque) {
                return
            }
            setLightLevel(to, level)
            cache[to] = level
            modification.add(to)
            spread(to, level, from)
            return
        }
        handleIncreasingLevel(modification, stack, cache, from, to, previousLevel, level)
    }

    override fun handleChunkLoad(chunk: Chunk) {}

    override fun handleChunkUnload(chunk: Chunk) {}

    data class LightChangeEntry(
        val from: BlockLocation,
        val to: BlockLocation,
        val level: Int
    )

}