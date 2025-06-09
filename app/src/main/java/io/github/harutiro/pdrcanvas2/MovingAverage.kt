package io.github.harutiro.pdrcanvas2

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class MovingAverage internal constructor(size: Int) {
    private var time: LongArray
    private var x: FloatArray
    private var y: FloatArray
    private var z: FloatArray
    private var size = 0
    var period: Long = 0
    private var cutoff = 0f
    private var averageX = 0.0f
    private var averageY = 0.0f
    private var averageZ = 0.0f
    private var averageComputed = false

    init {
        if (size > 0) {
            time = LongArray(size)
            x = FloatArray(size)
            y = FloatArray(size)
            z = FloatArray(size)
            this.size = size
            period = 10L
            cutoff = 2.0f
        } else {
            throw IllegalArgumentException("Illegal Argument:" + size)
        }
    }

    private fun rshift() {
        for (i in size - 1 downTo 1) {
            time[i] = time[i - 1]
            x[i] = x[i - 1]
            y[i] = y[i - 1]
            z[i] = z[i - 1]
        }
    }

    fun add(time: Long, x: Float, y: Float, z: Float) {
        rshift()
        this.time[0] = time
        this.x[0] = x
        this.y[0] = y
        this.z[0] = z
        averageComputed = false
    }

    fun setCutoff(cutoff: Float) {
        this.cutoff = cutoff
    }

    fun setGravity(gravity: Float) {
        for (i in 0..<size) {
            z[i] = gravity
        }
    }

    private fun computeMovingAverage() {
        // 0 < n < size
        val n = max(
            1.0,
            min(size.toDouble(), sqrt(((0.443 * 1.0 / (getdT() * NS2S)) / cutoff).pow(2.0)) + 1)
        ).toInt()
        for (i in 0..<n) {
            averageX = (averageX * i + x[i]) / (i + 1)
            averageY = (averageY * i + y[i]) / (i + 1)
            averageZ = (averageZ * i + z[i]) / (i + 1)
        }
        averageComputed = true
    }

    val movingAverageX: Float
        get() {
            if (!averageComputed) {
                computeMovingAverage()
            }
            return averageX
        }

    val movingAverageY: Float
        get() {
            if (!averageComputed) {
                computeMovingAverage()
            }
            return averageY
        }

    val movingAverageZ: Float
        get() {
            if (!averageComputed) {
                computeMovingAverage()
            }
            return averageZ
        }

    private fun getIndex(index: Int): Int {
        return ((index % size) + size) % size
    }

    fun getTime(): Long {
        return time[0]
    }

    fun getTime(index: Int): Long {
        return time[getIndex(index)]
    }

    fun getdT(): Long {
        if (time[0] == 0L || time[1] == 0L) {
            return period
        }
        return time[0] - time[1]
    }

    fun getX(): Float {
        return x[0]
    }

    fun getX(index: Int): Float {
        return x[getIndex(index)]
    }

    fun getY(): Float {
        return y[0]
    }

    fun getY(index: Int): Float {
        return y[getIndex(index)]
    }

    fun getZ(): Float {
        return z[0]
    }

    fun getZ(index: Int): Float {
        return z[getIndex(index)]
    }

    fun getSize(): Int {
        return size
    }

    val string: String
        get() {
            val sb = StringBuilder()
            for (i in 0..<size) {
                sb.append(getString(i))
            }
            return sb.toString()
        }

    fun getString(index: Int): String {
        return time[getIndex(index)].toString() + "," + x[getIndex(index)] + "," + y[getIndex(index)] + "," + z[getIndex(
            index
        )] + "\n"
    }

    private fun setArraySize(array: LongArray, length: Int): LongArray {
        val copy = LongArray(length)
        // rshift
        System.arraycopy(array, 0, copy, 0, if (copy.size > array.size) array.size else copy.size)
        // shift
        //System.arraycopy(array, copy.length > array.length ? 0 : array.length-copy.length, copy, copy.length > array.length ? copy.length-array.length : 0, copy.length > array.length ? array.length : copy.length);
        return copy
    }

    private fun setArraySize(array: FloatArray, length: Int): FloatArray {
        val copy = FloatArray(length)
        // rshift
        System.arraycopy(array, 0, copy, 0, if (copy.size > array.size) array.size else copy.size)
        // shift
        //System.arraycopy(array, copy.length > array.length ? 0 : array.length-copy.length, copy, copy.length > array.length ? copy.length-array.length : 0, copy.length > array.length ? array.length : copy.length);
        return copy
    }

    fun setSize(length: Int) {
        if (length <= 0) {
            return
        }
        time = setArraySize(time, length)
        x = setArraySize(x, length)
        y = setArraySize(y, length)
        z = setArraySize(z, length)
        size = length
    }

    fun addArraySize(time: Long, x: Float, y: Float, z: Float) {
        setSize(size + 1)
        add(time, x, y, z)
    }

    companion object {
        private val NS2S = 1.0f / 1000000000.0f
    }
}