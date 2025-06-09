package io.github.harutiro.pdrcanvas2;

// 原論文
// beta = (float) Math.sqrt(3.0 / 4.0f) * (float) Math.PI * (5.0 / 180.0f);

public class MadgwickAHRS {

    private float samplePeriod;
    private float beta;
    private float[] quaternion;
    private float pitch;
    private float roll;
    private float yaw;
    private boolean anglesComputed = false;

    public float getSamplePeriod() {
        return samplePeriod;
    }

    public void setSamplePeriod(float samplePeriod) {
        this.samplePeriod = samplePeriod;
    }

    public float getBeta() {
        return beta;
    }

    public void setBeta(float beta) {
        this.beta = beta;
    }

    public float[] getQuaternion() {
        return quaternion;
    }

    public void resetQuaternion() {
        setQuaternion(1.0f, 0.0f, 0.0f, 0.0f);
    }

    public void setQuaternion(float q0, float q1, float q2, float q3) {
        quaternion[0] = q0;
        quaternion[1] = q1;
        quaternion[2] = q2;
        quaternion[3] = q3;
    }

    public void setQuaternion(float[] quaternion) {
        setQuaternion(quaternion[0], quaternion[1], quaternion[2], quaternion[3]);
    }

    public void EulerAnglesToQuaternion(float pitch, float roll, float yaw) {
        float cosPitch = (float) Math.cos(pitch / 2.0);
        float sinPitch = (float) Math.sin(pitch / 2.0);
        float cosRoll = (float) Math.cos(roll / 2.0);
        float sinRoll = (float) Math.sin(roll / 2.0);
        float cosYaw = (float) Math.cos(yaw / 2.0);
        float sinYaw = (float) Math.sin(yaw / 2.0);

        quaternion[0] = cosPitch * cosRoll * cosYaw + sinPitch * sinRoll * sinYaw;
        quaternion[1] = sinPitch * cosRoll * cosYaw - cosPitch * sinRoll * sinYaw;
        quaternion[2] = cosPitch * sinRoll * cosYaw + sinPitch * cosRoll * sinYaw;
        quaternion[3] = cosPitch * cosRoll * sinYaw - sinPitch * sinRoll * cosYaw;
    }

    public MadgwickAHRS() {
        this(0.01f, (float) Math.sqrt(3.0f / 4.0f) * (float) Math.PI * (5.0f / 180.0f));
    }

    public MadgwickAHRS(float samplePeriod) {
        this(samplePeriod, (float) Math.sqrt(3.0f / 4.0f) * (float) Math.PI * (5.0f / 180.0f));
    }

    public MadgwickAHRS(float samplePeriod, float beta) {
        setSamplePeriod(samplePeriod);
        setBeta(beta);
        this.quaternion = new float[] { 1.0f, 0.0f, 0.0f, 0.0f };
    }

    // 姿勢更新
    public void update(float gx, float gy, float gz, float ax, float ay, float az) {
        float q1 = quaternion[0], q2 = quaternion[1], q3 = quaternion[2], q4 = quaternion[3];

        float norm;
        float s1, s2, s3, s4;
        float qDot1, qDot2, qDot3, qDot4;

        float _2q1 = 2.0f * q1;
        float _2q2 = 2.0f * q2;
        float _2q3 = 2.0f * q3;
        float _2q4 = 2.0f * q4;
        float _4q1 = 4.0f * q1;
        float _4q2 = 4.0f * q2;
        float _4q3 = 4.0f * q3;
        float _8q2 = 8.0f * q2;
        float _8q3 = 8.0f * q3;
        float q1q1 = q1 * q1;
        float q2q2 = q2 * q2;
        float q3q3 = q3 * q3;
        float q4q4 = q4 * q4;

        norm = (float) Math.sqrt(ax * ax + ay * ay + az * az);
        if (norm == 0.0f) {
            return;
        }
        norm = 1.0f / norm;
        ax *= norm;
        ay *= norm;
        az *= norm;

        s1 = _4q1 * q3q3 + _2q3 * ax + _4q1 * q2q2 - _2q2 * ay;
        s2 = _4q2 * q4q4 - _2q4 * ax + 4.0f * q1q1 * q2 - _2q1 * ay - _4q2 + _8q2 * q2q2 + _8q2 * q3q3 + _4q2 * az;
        s3 = 4.0f * q1q1 * q3 + _2q1 * ax + _4q3 * q4q4 - _2q4 * ay - _4q3 + _8q3 * q2q2 + _8q3 * q3q3 + _4q3 * az;
        s4 = 4.0f * q2q2 * q4 - _2q2 * ax + 4.0f * q3q3 * q4 - _2q3 * ay;
        norm = 1.0f / (float) Math.sqrt(s1 * s1 + s2 * s2 + s3 * s3 + s4 * s4);

        s1 *= norm;
        s2 *= norm;
        s3 *= norm;
        s4 *= norm;

        qDot1 = 0.5f * (-q2 * gx - q3 * gy - q4 * gz) - beta * s1;
        qDot2 = 0.5f * (q1 * gx + q3 * gz - q4 * gy) - beta * s2;
        qDot3 = 0.5f * (q1 * gy - q2 * gz + q4 * gx) - beta * s3;
        qDot4 = 0.5f * (q1 * gz + q2 * gy - q3 * gx) - beta * s4;

        q1 += qDot1 * samplePeriod;
        q2 += qDot2 * samplePeriod;
        q3 += qDot3 * samplePeriod;
        q4 += qDot4 * samplePeriod;
        norm = 1.0f / (float) Math.sqrt(q1 * q1 + q2 * q2 + q3 * q3 + q4 * q4);

        quaternion[0] = q1 * norm;
        quaternion[1] = q2 * norm;
        quaternion[2] = q3 * norm;
        quaternion[3] = q4 * norm;

        anglesComputed = false;
    }

    // 姿勢更新
    public void update(float[] gyro, float[] acc) {
        float q1 = quaternion[0], q2 = quaternion[1], q3 = quaternion[2], q4 = quaternion[3];

        float norm;
        float s1, s2, s3, s4;
        float qDot1, qDot2, qDot3, qDot4;

        float _2q1 = 2.0f * q1;
        float _2q2 = 2.0f * q2;
        float _2q3 = 2.0f * q3;
        float _2q4 = 2.0f * q4;
        float _4q1 = 4.0f * q1;
        float _4q2 = 4.0f * q2;
        float _4q3 = 4.0f * q3;
        float _8q2 = 8.0f * q2;
        float _8q3 = 8.0f * q3;
        float q1q1 = q1 * q1;
        float q2q2 = q2 * q2;
        float q3q3 = q3 * q3;
        float q4q4 = q4 * q4;

        norm = (float) Math.sqrt(acc[0] * acc[0] + acc[1] * acc[1] + acc[2] * acc[2]);
        if (norm == 0.0f) {
            return;
        }
        norm = 1.0f / norm;
        acc[0] *= norm;
        acc[1] *= norm;
        acc[2] *= norm;

        s1 = _4q1 * q3q3 + _2q3 * acc[0] + _4q1 * q2q2 - _2q2 * acc[1];
        s2 = _4q2 * q4q4 - _2q4 * acc[0] + 4.0f * q1q1 * q2 - _2q1 * acc[1] - _4q2 + _8q2 * q2q2 + _8q2 * q3q3 + _4q2 * acc[2];
        s3 = 4.0f * q1q1 * q3 + _2q1 * acc[0] + _4q3 * q4q4 - _2q4 * acc[1] - _4q3 + _8q3 * q2q2 + _8q3 * q3q3 + _4q3 * acc[2];
        s4 = 4.0f * q2q2 * q4 - _2q2 * acc[0] + 4.0f * q3q3 * q4 - _2q3 * acc[1];
        norm = 1.0f / (float) Math.sqrt(s1 * s1 + s2 * s2 + s3 * s3 + s4 * s4);

        s1 *= norm;
        s2 *= norm;
        s3 *= norm;
        s4 *= norm;

        qDot1 = 0.5f * (-q2 * gyro[0] - q3 * gyro[1] - q4 * gyro[2]) - beta * s1;
        qDot2 = 0.5f * (q1 * gyro[0] + q3 * gyro[2] - q4 * gyro[1]) - beta * s2;
        qDot3 = 0.5f * (q1 * gyro[1] - q2 * gyro[2] + q4 * gyro[0]) - beta * s3;
        qDot4 = 0.5f * (q1 * gyro[2] + q2 * gyro[1] - q3 * gyro[0]) - beta * s4;

        q1 += qDot1 * samplePeriod;
        q2 += qDot2 * samplePeriod;
        q3 += qDot3 * samplePeriod;
        q4 += qDot4 * samplePeriod;
        norm = 1.0f / (float) Math.sqrt(q1 * q1 + q2 * q2 + q3 * q3 + q4 * q4);

        quaternion[0] = q1 * norm;
        quaternion[1] = q2 * norm;
        quaternion[2] = q3 * norm;
        quaternion[3] = q4 * norm;

        anglesComputed = false;
    }

    // 座標変換
    public float[] BodyAccelToRefAccel(float acc[]) {
        float[] rAxis = new float[3];
        //float q0q0 = quaternion[0] * quaternion[0];
        float q0q1 = quaternion[0] * quaternion[1];
        float q0q2 = quaternion[0] * quaternion[2];
        float q0q3 = quaternion[0] * quaternion[3];
        float q1q1 = quaternion[1] * quaternion[1];
        float q1q2 = quaternion[1] * quaternion[2];
        float q1q3 = quaternion[1] * quaternion[3];
        float q2q2 = quaternion[2] * quaternion[2];
        float q2q3 = quaternion[2] * quaternion[3];
        float q3q3 = quaternion[3] * quaternion[3];

        rAxis[0] = 2.0f * acc[0] * (0.5f - q2q2 -q3q3) + 2.0f * acc[1] * (q1q2 - q0q3) + 2.0f * acc[2] * (q1q3 + q0q2);
        rAxis[1] = 2.0f * acc[0] * (q1q2 + q0q3) + 2.0f * acc[1] * (0.5f - q1q1 - q3q3) + 2.0f * acc[2] * (q2q3 - q0q1);
        rAxis[2] = 2.0f * acc[0] * (q1q3 - q0q2) + 2.0f * acc[1] * (q2q3 + q0q1) + 2.0f * acc[2] * (0.5f - q1q1 - q2q2);

        return rAxis;
    }

    // 角度算出
    private void computeAngles() {
        pitch = (float) Math.atan2(quaternion[0] * quaternion[1] + quaternion[2] * quaternion[3], 0.5f - quaternion[1] * quaternion[1] - quaternion[2] * quaternion[2]);
        roll = (float) Math.asin(-2.0f * (quaternion[1] * quaternion[3] - quaternion[0] * quaternion[2]));
        yaw = (float) Math.atan2(quaternion[1] * quaternion[2] + quaternion[0] * quaternion[3], 0.5f - quaternion[2] * quaternion[2] - quaternion[3] * quaternion[3]);
        anglesComputed = true;
    }

    public float getPitch() {
        if (!anglesComputed) {
            computeAngles();
        }
        return pitch;
    }

    public float getRoll() {
        if (!anglesComputed) {
            computeAngles();
        }
        return roll;
    }

    public float getYaw() {
        if (!anglesComputed) {
            computeAngles();
        }
        return yaw;
    }

}