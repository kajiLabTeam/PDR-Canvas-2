package io.github.harutiro.pdrcanvas2

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Angle {
    @JvmField
    var pitch: Float = 0f
    @JvmField
    var roll: Float = 0f

    private val PI = Math.PI.toFloat()

    private fun loopRadian(radian: Float): Float {
        var radian = radian
        radian %= PI * 2.0f
        if (radian > PI) {
            radian -= PI * 2.0f
        } else if (radian < -PI) {
            radian += PI * 2.0f
        }
        return radian
    }

    fun loopPitchRadian(radian: Float): Float {
        return loopRadian(radian)
    }

    fun loopYawRadian(radian: Float): Float {
        return loopRadian(radian)
    }

    fun loopRollRadian(radian: Float): Float {
        var radian = radian
        radian %= PI * 2.0f
        if (radian > PI / 2.0f) {
            radian = PI - radian
        } else if (radian < -PI / 2.0f) {
            radian = -PI - radian
        }
        return radian
    }

    // 姿勢推定
    fun calculateLean(acc: FloatArray?) {
        val invA =
            1.0f / sqrt((acc!![0] * acc[0] + acc[1] * acc[1] + acc[2] * acc[2]).toDouble()).toFloat()
        val x = invA * acc[0]
        val y = invA * acc[1]
        val z = invA * acc[2]

        if (acc[2] >= 0) {
            pitch = atan2(y.toDouble(), sqrt((x * x + z * z).toDouble())).toFloat()
            roll = atan2(-x.toDouble(), z.toDouble()).toFloat()
        } else {
            pitch = atan2(-y.toDouble(), sqrt((x * x + z * z).toDouble())).toFloat()
            roll = atan2(-x.toDouble(), z.toDouble()).toFloat()
        }
    }

    // 座標変換
    fun rotateAxis(axis: FloatArray?): FloatArray {
        val rAxis = FloatArray(3)

        val sinA = sin(loopPitchRadian(pitch).toDouble()).toFloat()
        val cosA = cos(loopPitchRadian(pitch).toDouble()).toFloat()
        val sinB = sin(loopRollRadian(roll).toDouble()).toFloat()
        val cosB = cos(loopRollRadian(roll).toDouble()).toFloat()

        rAxis[0] = axis!![0] * cosB + axis[1] * sinA * sinB + axis[2] * cosA * sinB
        rAxis[1] = axis[1] * cosA + axis[2] * (-sinA)
        rAxis[2] = axis[0] * (-sinB) + axis[1] * sinA * cosB + axis[2] * cosA * cosB

        return rAxis
    }

    // 平面成分原点中心回転
    fun rotatePlane(x: Float, y: Float, angle: Float): FloatArray {
        return rotatePlane(x, y, 0.0f, 0.0f, angle)
    }

    // 平面成分(x0, y0)中心回転
    fun rotatePlane(x: Float, y: Float, x0: Float, y0: Float, angle: Float): FloatArray {
        val rotate = FloatArray(2)
        rotate[0] =
            cos(angle.toDouble()).toFloat() * (x - x0) - sin(angle.toDouble()).toFloat() * (y - y0)
        rotate[1] =
            sin(angle.toDouble()).toFloat() * (x - x0) + cos(angle.toDouble()).toFloat() * (y - y0)
        return rotate
    }
}