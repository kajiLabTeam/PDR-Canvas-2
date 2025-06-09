package io.github.harutiro.pdrcanvas2

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt // 原論文

// beta = (float) Math.sqrt(3.0 / 4.0f) * (float) Math.PI * (5.0 / 180.0f);

class MadgwickAHRS @JvmOverloads constructor(
    samplePeriod: Float = 0.01f,
    beta: Float = sqrt((3.0f / 4.0f).toDouble()).toFloat() * Math.PI.toFloat() * (5.0f / 180.0f)
) {
    @JvmField
    var samplePeriod: Float = 0f
    var beta: Float = 0f
    private val quaternion: FloatArray
    private var pitch = 0f
    private var roll = 0f
    private var yaw = 0f
    private var anglesComputed = false

    fun getQuaternion(): FloatArray {
        return quaternion
    }

    fun resetQuaternion() {
        setQuaternion(1.0f, 0.0f, 0.0f, 0.0f)
    }

    fun setQuaternion(q0: Float, q1: Float, q2: Float, q3: Float) {
        quaternion[0] = q0
        quaternion[1] = q1
        quaternion[2] = q2
        quaternion[3] = q3
    }

    fun setQuaternion(quaternion: FloatArray) {
        setQuaternion(quaternion[0], quaternion[1], quaternion[2], quaternion[3])
    }

    fun EulerAnglesToQuaternion(pitch: Float, roll: Float, yaw: Float) {
        val cosPitch = cos(pitch / 2.0).toFloat()
        val sinPitch = sin(pitch / 2.0).toFloat()
        val cosRoll = cos(roll / 2.0).toFloat()
        val sinRoll = sin(roll / 2.0).toFloat()
        val cosYaw = cos(yaw / 2.0).toFloat()
        val sinYaw = sin(yaw / 2.0).toFloat()

        quaternion[0] = cosPitch * cosRoll * cosYaw + sinPitch * sinRoll * sinYaw
        quaternion[1] = sinPitch * cosRoll * cosYaw - cosPitch * sinRoll * sinYaw
        quaternion[2] = cosPitch * sinRoll * cosYaw + sinPitch * cosRoll * sinYaw
        quaternion[3] = cosPitch * cosRoll * sinYaw - sinPitch * sinRoll * cosYaw
    }

    init {
        this.samplePeriod = samplePeriod
        this.beta = beta
        this.quaternion = floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f)
    }

    // 姿勢更新
    fun update(gx: Float, gy: Float, gz: Float, ax: Float, ay: Float, az: Float) {
        var ax = ax
        var ay = ay
        var az = az
        var q1 = quaternion[0]
        var q2 = quaternion[1]
        var q3 = quaternion[2]
        var q4 = quaternion[3]

        var norm: Float
        var s1: Float
        var s2: Float
        var s3: Float
        var s4: Float
        val qDot1: Float
        val qDot2: Float
        val qDot3: Float
        val qDot4: Float

        val _2q1 = 2.0f * q1
        val _2q2 = 2.0f * q2
        val _2q3 = 2.0f * q3
        val _2q4 = 2.0f * q4
        val _4q1 = 4.0f * q1
        val _4q2 = 4.0f * q2
        val _4q3 = 4.0f * q3
        val _8q2 = 8.0f * q2
        val _8q3 = 8.0f * q3
        val q1q1 = q1 * q1
        val q2q2 = q2 * q2
        val q3q3 = q3 * q3
        val q4q4 = q4 * q4

        norm = sqrt((ax * ax + ay * ay + az * az).toDouble()).toFloat()
        if (norm == 0.0f) {
            return
        }
        norm = 1.0f / norm
        ax *= norm
        ay *= norm
        az *= norm

        s1 = _4q1 * q3q3 + _2q3 * ax + _4q1 * q2q2 - _2q2 * ay
        s2 =
            _4q2 * q4q4 - _2q4 * ax + 4.0f * q1q1 * q2 - _2q1 * ay - _4q2 + _8q2 * q2q2 + _8q2 * q3q3 + _4q2 * az
        s3 =
            4.0f * q1q1 * q3 + _2q1 * ax + _4q3 * q4q4 - _2q4 * ay - _4q3 + _8q3 * q2q2 + _8q3 * q3q3 + _4q3 * az
        s4 = 4.0f * q2q2 * q4 - _2q2 * ax + 4.0f * q3q3 * q4 - _2q3 * ay
        norm = 1.0f / sqrt((s1 * s1 + s2 * s2 + s3 * s3 + s4 * s4).toDouble()).toFloat()

        s1 *= norm
        s2 *= norm
        s3 *= norm
        s4 *= norm

        qDot1 = 0.5f * (-q2 * gx - q3 * gy - q4 * gz) - beta * s1
        qDot2 = 0.5f * (q1 * gx + q3 * gz - q4 * gy) - beta * s2
        qDot3 = 0.5f * (q1 * gy - q2 * gz + q4 * gx) - beta * s3
        qDot4 = 0.5f * (q1 * gz + q2 * gy - q3 * gx) - beta * s4

        q1 += qDot1 * samplePeriod
        q2 += qDot2 * samplePeriod
        q3 += qDot3 * samplePeriod
        q4 += qDot4 * samplePeriod
        norm = 1.0f / sqrt((q1 * q1 + q2 * q2 + q3 * q3 + q4 * q4).toDouble()).toFloat()

        quaternion[0] = q1 * norm
        quaternion[1] = q2 * norm
        quaternion[2] = q3 * norm
        quaternion[3] = q4 * norm

        anglesComputed = false
    }

    // 姿勢更新
    fun update(gyro: FloatArray, acc: FloatArray) {
        var q1 = quaternion[0]
        var q2 = quaternion[1]
        var q3 = quaternion[2]
        var q4 = quaternion[3]

        var norm: Float
        var s1: Float
        var s2: Float
        var s3: Float
        var s4: Float
        val qDot1: Float
        val qDot2: Float
        val qDot3: Float
        val qDot4: Float

        val _2q1 = 2.0f * q1
        val _2q2 = 2.0f * q2
        val _2q3 = 2.0f * q3
        val _2q4 = 2.0f * q4
        val _4q1 = 4.0f * q1
        val _4q2 = 4.0f * q2
        val _4q3 = 4.0f * q3
        val _8q2 = 8.0f * q2
        val _8q3 = 8.0f * q3
        val q1q1 = q1 * q1
        val q2q2 = q2 * q2
        val q3q3 = q3 * q3
        val q4q4 = q4 * q4

        norm = sqrt((acc[0] * acc[0] + acc[1] * acc[1] + acc[2] * acc[2]).toDouble()).toFloat()
        if (norm == 0.0f) {
            return
        }
        norm = 1.0f / norm
        acc[0] *= norm
        acc[1] *= norm
        acc[2] *= norm

        s1 = _4q1 * q3q3 + _2q3 * acc[0] + _4q1 * q2q2 - _2q2 * acc[1]
        s2 =
            _4q2 * q4q4 - _2q4 * acc[0] + 4.0f * q1q1 * q2 - _2q1 * acc[1] - _4q2 + _8q2 * q2q2 + _8q2 * q3q3 + _4q2 * acc[2]
        s3 =
            4.0f * q1q1 * q3 + _2q1 * acc[0] + _4q3 * q4q4 - _2q4 * acc[1] - _4q3 + _8q3 * q2q2 + _8q3 * q3q3 + _4q3 * acc[2]
        s4 = 4.0f * q2q2 * q4 - _2q2 * acc[0] + 4.0f * q3q3 * q4 - _2q3 * acc[1]
        norm = 1.0f / sqrt((s1 * s1 + s2 * s2 + s3 * s3 + s4 * s4).toDouble()).toFloat()

        s1 *= norm
        s2 *= norm
        s3 *= norm
        s4 *= norm

        qDot1 = 0.5f * (-q2 * gyro[0] - q3 * gyro[1] - q4 * gyro[2]) - beta * s1
        qDot2 = 0.5f * (q1 * gyro[0] + q3 * gyro[2] - q4 * gyro[1]) - beta * s2
        qDot3 = 0.5f * (q1 * gyro[1] - q2 * gyro[2] + q4 * gyro[0]) - beta * s3
        qDot4 = 0.5f * (q1 * gyro[2] + q2 * gyro[1] - q3 * gyro[0]) - beta * s4

        q1 += qDot1 * samplePeriod
        q2 += qDot2 * samplePeriod
        q3 += qDot3 * samplePeriod
        q4 += qDot4 * samplePeriod
        norm = 1.0f / sqrt((q1 * q1 + q2 * q2 + q3 * q3 + q4 * q4).toDouble()).toFloat()

        quaternion[0] = q1 * norm
        quaternion[1] = q2 * norm
        quaternion[2] = q3 * norm
        quaternion[3] = q4 * norm

        anglesComputed = false
    }

    // 座標変換
    fun BodyAccelToRefAccel(acc: FloatArray?): FloatArray {
        val rAxis = FloatArray(3)
        //float q0q0 = quaternion[0] * quaternion[0];
        val q0q1 = quaternion[0] * quaternion[1]
        val q0q2 = quaternion[0] * quaternion[2]
        val q0q3 = quaternion[0] * quaternion[3]
        val q1q1 = quaternion[1] * quaternion[1]
        val q1q2 = quaternion[1] * quaternion[2]
        val q1q3 = quaternion[1] * quaternion[3]
        val q2q2 = quaternion[2] * quaternion[2]
        val q2q3 = quaternion[2] * quaternion[3]
        val q3q3 = quaternion[3] * quaternion[3]

        rAxis[0] =
            2.0f * acc!![0] * (0.5f - q2q2 - q3q3) + 2.0f * acc[1] * (q1q2 - q0q3) + 2.0f * acc[2] * (q1q3 + q0q2)
        rAxis[1] =
            2.0f * acc[0] * (q1q2 + q0q3) + 2.0f * acc[1] * (0.5f - q1q1 - q3q3) + 2.0f * acc[2] * (q2q3 - q0q1)
        rAxis[2] =
            2.0f * acc[0] * (q1q3 - q0q2) + 2.0f * acc[1] * (q2q3 + q0q1) + 2.0f * acc[2] * (0.5f - q1q1 - q2q2)

        return rAxis
    }

    // 角度算出
    private fun computeAngles() {
        pitch = atan2(
            (quaternion[0] * quaternion[1] + quaternion[2] * quaternion[3]).toDouble(),
            (0.5f - quaternion[1] * quaternion[1] - quaternion[2] * quaternion[2]).toDouble()
        ).toFloat()
        roll =
            asin((-2.0f * (quaternion[1] * quaternion[3] - quaternion[0] * quaternion[2])).toDouble()).toFloat()
        yaw = atan2(
            (quaternion[1] * quaternion[2] + quaternion[0] * quaternion[3]).toDouble(),
            (0.5f - quaternion[2] * quaternion[2] - quaternion[3] * quaternion[3]).toDouble()
        ).toFloat()
        anglesComputed = true
    }

    fun getPitch(): Float {
        if (!anglesComputed) {
            computeAngles()
        }
        return pitch
    }

    fun getRoll(): Float {
        if (!anglesComputed) {
            computeAngles()
        }
        return roll
    }

    fun getYaw(): Float {
        if (!anglesComputed) {
            computeAngles()
        }
        return yaw
    }
}