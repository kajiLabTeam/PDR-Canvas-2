package io.github.harutiro.pdrcanvas2

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Step internal constructor(size: Int) {
    private var time: LongArray
    private var x: FloatArray
    private var y: FloatArray
    private var z: FloatArray
    private var angle: FloatArray
    private var size = 0
    var period: Long = 0
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
            angle = FloatArray(size)
            this.size = size
            period = 10L
        } else {
            throw IllegalArgumentException("Illegal Argument:" + size)
        }
    }

    private fun shift() {
        for (i in 0..<size - 1) {
            time[i] = time[i + 1]
            x[i] = x[i + 1]
            y[i] = y[i + 1]
            z[i] = z[i + 1]
            angle[i] = angle[i + 1]
        }
    }

    fun add(time: Long, x: Float, y: Float, z: Float, angle: Float) {
        shift()
        this.time[size - 1] = time
        this.x[size - 1] = x
        this.y[size - 1] = y
        this.z[size - 1] = z
        this.angle[size - 1] = angle
        averageComputed = false
    }

    fun clear() {
        for (i in 0..<size) {
            time[i] = 0L
            x[i] = 0.0f
            y[i] = 0.0f
            z[i] = 0.0f
            angle[i] = 0.0f
        }
    }

    fun clear(index: Int) {
        if (index < 0 || size < index) {
            return
        }
        for (i in 0..index) {
            time[i] = 0L
            x[i] = 0.0f
            y[i] = 0.0f
            z[i] = 0.0f
            angle[i] = 0.0f
        }
    }

    fun clearXY() {
        for (i in 0..<size) {
            time[i] = 0L
            x[i] = 0.0f
            y[i] = 0.0f
            angle[i] = 0.0f
        }
    }

    fun setGravity(gravity: Float) {
        for (i in 0..<size) {
            z[i] = gravity
        }
    }

    private fun computeAverage() {
        for (i in 0..<size) {
            averageX = (averageX * i + x[i]) / (i + 1)
            averageY = (averageY * i + y[i]) / (i + 1)
            averageZ = (averageZ * i + z[i]) / (i + 1)
        }
        averageComputed = true
    }

    fun getAverageX(): Float {
        if (!averageComputed) {
            computeAverage()
        }
        return averageX
    }

    fun getAverageY(): Float {
        if (!averageComputed) {
            computeAverage()
        }
        return averageY
    }

    fun getAverageZ(): Float {
        if (!averageComputed) {
            computeAverage()
        }
        return averageZ
    }

    private fun getIndex(index: Int): Int {
        return ((index % size) + size) % size
    }

    fun getTime(): Long {
        return time[size - 1]
    }

    fun getTime(index: Int): Long {
        return time[getIndex(index)]
    }

    fun getdT(): Long {
        if (time[size - 1] == 0L || time[size - 2] == 0L) {
            return period
        }
        return time[size - 1] - time[size - 2]
    }

    fun getX(): Float {
        return x[size - 1]
    }

    fun getX(index: Int): Float {
        return x[getIndex(index)]
    }

    fun getY(): Float {
        return y[size - 1]
    }

    fun getY(index: Int): Float {
        return y[getIndex(index)]
    }

    fun getZ(): Float {
        return z[size - 1]
    }

    fun getZ(index: Int): Float {
        return z[getIndex(index)]
    }

    fun getAngle(): Float {
        return angle[size - 1]
    }

    fun getAngle(index: Int): Float {
        return angle[getIndex(index)]
    }

    fun getSize(): Int {
        return size
    }

    fun getRotateX(index: Int): Float {
        return cos(angle[getIndex(index)].toDouble()).toFloat() * (x[getIndex(index)] - getAverageX()) - sin(
            angle[getIndex(index)].toDouble()
        ).toFloat() * (y[getIndex(index)] - getAverageY())
    }

    val rotateX: FloatArray
        get() {
            val rotateX = FloatArray(size)
            for (i in 0..<size) {
                rotateX[i] = getRotateX(i)
            }
            return rotateX
        }

    fun getRotateX(index: Int, x0: Float, y0: Float): Float {
        return cos(angle[getIndex(index)].toDouble()).toFloat() * (x[getIndex(index)] - x0) - sin(
            angle[getIndex(index)].toDouble()
        ).toFloat() * (y[getIndex(index)] - y0)
    }

    fun getRotateY(index: Int): Float {
        return sin(angle[getIndex(index)].toDouble()).toFloat() * (x[getIndex(index)] - getAverageX()) + cos(
            angle[getIndex(index)].toDouble()
        ).toFloat() * (y[getIndex(index)] - getAverageY())
    }

    val rotateY: FloatArray
        get() {
            val rotateY = FloatArray(size)
            for (i in 0..<size) {
                rotateY[i] = getRotateY(i)
            }
            return rotateY
        }

    fun getRotateY(index: Int, x0: Float, y0: Float): Float {
        return sin(angle[getIndex(index)].toDouble()).toFloat() * (x[getIndex(index)] - x0) + cos(
            angle[getIndex(index)].toDouble()
        ).toFloat() * (y[getIndex(index)] - y0)
    }

    val norm: Float
        get() = sqrt(((x[size - 1] - getAverageX()) * (x[size - 1] - getAverageX()) + (y[size - 1] - getAverageY()) * (y[size - 1] - getAverageY()) + (z[size - 1] - getAverageZ()) * (z[size - 1] - getAverageZ())).toDouble())
            .toFloat()

    fun getNorm(index: Int): Float {
        return sqrt(
            ((x[getIndex(index)] - getAverageX()) * (x[getIndex(index)] - getAverageX()) + (y[getIndex(
                index
            )] - getAverageY()) * (y[getIndex(index)] - getAverageY()) + (z[getIndex(index)] - getAverageZ()) * (z[getIndex(
                index
            )] - getAverageZ())).toDouble()
        ).toFloat()
    }

    val planeNorm: Float
        get() = sqrt(((x[size - 1] - getAverageX() / 2.0f) * (x[size - 1] - getAverageX() / 2.0f) + (y[size - 1] - getAverageY() / 2.0f) * (y[size - 1] - getAverageY() / 2.0f)).toDouble())
            .toFloat()

    fun getPlaneNorm(index: Int): Float {
        return sqrt(
            ((x[getIndex(index)] - getAverageX()) * (x[getIndex(index)] - getAverageX()) + (y[getIndex(
                index
            )] - getAverageY()) * (y[getIndex(index)] - getAverageY())).toDouble()
        ).toFloat()
    }

    val maxNorm: Float
        get() {
            var max = 0.0f
            for (i in 0..<size) {
                if (max < getPlaneNorm(i)) {
                    max = getPlaneNorm(i)
                }
            }
            return max
        }

    val normAverage: Float
        get() {
            var average = 0.0f
            for (i in 0..<size) {
                average = (average * i + getPlaneNorm(i)) / (i + 1)
            }
            return average
        }

    fun getPlaneNormSlope(index: Int): Float {
        return getPlaneNorm(index) - getPlaneNorm(index - 1)
    }

    val string: String
        get() {
            val sb = StringBuilder()
            for (i in 0..<size) {
                sb.append(getString(i))
            }
            return sb.toString()
        }

    private fun getString(index: Int): String {
        return time[getIndex(index)].toString() + "," + x[getIndex(index)] + "," + y[getIndex(index)] + "," + z[getIndex(
            index
        )] + "," + angle[getIndex(index)] + "\n"
    }

    fun getLengthSquare(index1: Int, index2: Int): Float {
        return (x[getIndex(index2)] - x[getIndex(index1)]) * (x[getIndex(index2)] - x[getIndex(
            index1
        )]) + (y[getIndex(index2)] - y[getIndex(index1)]) * (y[getIndex(index2)] - y[getIndex(index1)])
    }

    private fun setArraySize(array: LongArray, length: Int): LongArray {
        val copy = LongArray(length)
        // rshift
        //System.arraycopy(array, 0, copy, 0, copy.length > array.length ? array.length : copy.length);
        // shift
        System.arraycopy(
            array,
            if (copy.size > array.size) 0 else array.size - copy.size,
            copy,
            if (copy.size > array.size) copy.size - array.size else 0,
            if (copy.size > array.size) array.size else copy.size
        )
        return copy
    }

    private fun setArraySize(array: FloatArray, length: Int): FloatArray {
        val copy = FloatArray(length)
        // rshift
        //System.arraycopy(array, 0, copy, 0, copy.length > array.length ? array.length : copy.length);
        // shift
        System.arraycopy(
            array,
            if (copy.size > array.size) 0 else array.size - copy.size,
            copy,
            if (copy.size > array.size) copy.size - array.size else 0,
            if (copy.size > array.size) array.size else copy.size
        )
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
        angle = setArraySize(angle, length)
        size = length
    }

    fun addArraySize(time: Long, x: Float, y: Float, z: Float, angle: Float) {
        setSize(size + 1)
        add(time, x, y, z, angle)
    }
}