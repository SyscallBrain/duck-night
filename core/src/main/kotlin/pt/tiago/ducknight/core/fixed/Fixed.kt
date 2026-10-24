package pt.tiago.ducknight.core.fixed

/** Number of fractional bits in a fixed-point value. */
const val FX_SHIFT = 8

/** One world unit in fixed-point steps. */
const val FX_ONE = 1 shl FX_SHIFT

/** The simulation runs at 60 ticks per second. */
const val TICK_HZ = 60

/** The world is 240 x 300 units, portrait 4:5. */
const val WORLD_WIDTH = 240
const val WORLD_HEIGHT = 300

/** The vegetation line, where birds rise from and the dog appears. */
const val VEGETATION_Y = 232

/** Playfield margins: side 16 units, ceiling 18 units. */
const val SIDE_MARGIN = 16
const val CEILING = 18

fun fx(units: Int): Int = units shl FX_SHIFT

/** Rounds a fixed-point value to the nearest whole unit, half away from zero. */
fun toUnits(value: Int): Int =
    if (value >= 0) (value + FX_ONE / 2) shr FX_SHIFT else -((-value + FX_ONE / 2) shr FX_SHIFT)

/** Fixed-point multiplication: the result keeps the same scale. */
fun fxMul(a: Int, b: Int): Int {
    val product = a.toLong() * b
    return if (product >= 0) {
        ((product + FX_ONE / 2) shr FX_SHIFT).toInt()
    } else {
        -(((-product + FX_ONE / 2) shr FX_SHIFT).toInt())
    }
}

/** Fixed-point division by a plain integer scalar, rounded half away from zero. */
fun fxDiv(value: Int, divisor: Int): Int {
    require(divisor != 0) { "Fixed-point division by zero" }
    val negative = (value < 0) xor (divisor < 0)
    val numerator = kotlin.math.abs(value.toLong())
    val denominator = kotlin.math.abs(divisor.toLong())
    val quotient = (numerator + denominator / 2) / denominator
    return if (negative) -quotient.toInt() else quotient.toInt()
}

/** Integer square root, floored, without floating point. */
fun isqrt(value: Long): Long {
    require(value >= 0) { "Square root of a negative value" }
    if (value < 2) return value
    var x = value
    var y = (x + 1) / 2
    while (y < x) {
        x = y
        y = (x + value / x) / 2
    }
    return x
}

/** Distance between two fixed-point points, in fixed-point units. */
fun distance(x1: Int, y1: Int, x2: Int, y2: Int): Int {
    val dx = x1.toLong() - x2
    val dy = y1.toLong() - y2
    return isqrt(dx * dx + dy * dy).toInt()
}

/** Converts a speed in units per second to fixed-point units per tick. */
fun fpsToFxPerTick(unitsPerSecond: Int): Int =
    (unitsPerSecond.toLong() * FX_ONE + TICK_HZ / 2).div(TICK_HZ).toInt()

fun clamp(value: Int, min: Int, max: Int): Int = when {
    value < min -> min
    value > max -> max
    else -> value
}

/** Converts degrees to the 4096-step angle circle. */
fun degreesToAngle(degrees: Int): Int {
    val normalized = ((degrees % 360) + 360) % 360
    return (normalized * TrigTable.STEPS + 180) / 360 % TrigTable.STEPS
}

/**
 * Sine table with 4096 steps per turn and 256 fixed-point steps per unit.
 * It is built once with StrictMath.sin, which is deterministic on every platform; the
 * simulation itself never calls a floating-point trigonometric function.
 */
object TrigTable {
    const val STEPS = 4096
    private const val QUARTER_TURN = STEPS / 4

    private val SINES = IntArray(STEPS) { index ->
        kotlin.math.round(
            StrictMath.sin(index.toDouble() * 2.0 * StrictMath.PI / STEPS) * FX_ONE,
        ).toInt()
    }

    fun sin(angle: Int): Int = SINES[angle and (STEPS - 1)]

    fun cos(angle: Int): Int = sin(angle + QUARTER_TURN)

    /** Stable hash of the whole table; the value is locked by a test. */
    fun tableHash(): Long {
        var hash = 0x9E3779B97F4A7C15UL.toLong()
        for (value in SINES) {
            hash = pt.tiago.ducknight.core.random.SplitMix64.mix(
                hash xor (value.toLong() and 0xFFFF_FFFFL),
            )
        }
        return hash
    }

    /** Exposed for the hasher test only; never mutate the returned array. */
    internal fun raw(index: Int): Int = SINES[index]
}
