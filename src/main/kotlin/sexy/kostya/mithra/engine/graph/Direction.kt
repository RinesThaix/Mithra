package sexy.kostya.mithra.engine.graph

import it.unimi.dsi.fastutil.bytes.ByteArrayList
import sexy.kostya.mithra.engine.BlockLocation

typealias Direction = Byte

object Directions {
    const val West: Direction = 0
    const val East: Direction = 1
    const val South: Direction = 2
    const val North: Direction = 3
    const val Bottom: Direction = 4
    const val Top: Direction = 5

    val All = ByteArrayList.of(West, East, South, North, Top, Bottom)
}

fun BlockLocation.getDirection(to: BlockLocation) = when {
    x == to.x - 1 -> Directions.West
    x == to.x + 1 -> Directions.East
    y == to.y - 1 -> Directions.Bottom
    y == to.y + 1 -> Directions.Top
    z == to.z - 1 -> Directions.South
    z == to.z + 1 -> Directions.North
    else -> throw IllegalStateException("$this and $to are not adjacent")
}

fun BlockLocation.relative(direction: Direction) = when (direction) {
    Directions.West -> relative(-1, 0, 0)
    Directions.East -> relative(1, 0, 0)
    Directions.Bottom -> relative(0, -1, 0)
    Directions.Top -> relative(0, 1, 0)
    Directions.South -> relative(0, 0, -1)
    Directions.North -> relative(0, 0, 1)
    else -> throw IllegalArgumentException("unknown direction $direction")
}