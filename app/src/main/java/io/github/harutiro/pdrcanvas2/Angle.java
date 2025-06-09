package io.github.harutiro.pdrcanvas2;

public class Angle {

    private float pitch;
    private float roll;

    private float PI = (float) Math.PI;

    private float loopRadian(float radian) {
            radian %= PI * 2.0f;
            if (radian > PI) {
                radian -= PI * 2.0f;
            }
            else if (radian < -PI) {
                radian += PI * 2.0f;
            }
            return radian;
    }

    public float loopPitchRadian(float radian) {
        return loopRadian(radian);
    }

    public float loopYawRadian(float radian) {
        return loopRadian(radian);
    }

    public float loopRollRadian(float radian) {
        radian %= PI * 2.0f;
        if (radian > PI / 2.0f) {
            radian = PI - radian;
        }
        else if (radian < -PI / 2.0f) {
            radian = -PI - radian;
        }
        return radian;
    }

    // 姿勢推定
    public void calculateLean(float acc[]) {
        final float invA = 1.0f / (float) Math.sqrt(acc[0] * acc[0] + acc[1] * acc[1] + acc[2] * acc[2]);
        float x = invA * acc[0];
        float y = invA * acc[1];
        float z = invA * acc[2];

        if (acc[2] >= 0) {
            pitch = (float) Math.atan2(y, Math.sqrt(x * x + z * z));
            roll = (float) Math.atan2(-x, z);
        }
        else {
            pitch = (float) Math.atan2(-y, Math.sqrt(x * x + z * z));
            roll = (float) Math.atan2(-x, z);
        }
    }

    // 座標変換
    public float[] rotateAxis(float axis[]) {
        float[] rAxis = new float[3];

        float sinA = (float) Math.sin(loopPitchRadian(pitch));
        float cosA = (float) Math.cos(loopPitchRadian(pitch));
        float sinB = (float) Math.sin(loopRollRadian(roll));
        float cosB = (float) Math.cos(loopRollRadian(roll));

        rAxis[0] = axis[0] * cosB + axis[1] * sinA * sinB + axis[2] * cosA * sinB;
        rAxis[1] = axis[1] * cosA + axis[2] * (-sinA);
        rAxis[2] = axis[0] * (-sinB) + axis[1] * sinA * cosB + axis[2] * cosA * cosB;

        return rAxis;
    }

    // 平面成分原点中心回転
    public float[] rotatePlane(float x, float y, float angle) {
        return rotatePlane(x, y, 0.0f, 0.0f, angle);
    }

    // 平面成分(x0, y0)中心回転
    public float[] rotatePlane(float x, float y, float x0, float y0, float angle) {
        float[] rotate = new float[2];
        rotate[0] = (float) Math.cos(angle) * (x-x0) - (float) Math.sin(angle) * (y-y0);
        rotate[1] = (float) Math.sin(angle) * (x-x0) + (float) Math.cos(angle) * (y-y0);
        return rotate;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public void setRoll(float roll) {
        this.roll = roll;
    }

    public float getPitch() {
        return pitch;
    }

    public float getRoll() {
        return roll;
    }

}