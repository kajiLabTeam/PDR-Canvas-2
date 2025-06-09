package io.github.harutiro.pdrcanvas2;

public class Average {

    private int count = 0;
    private float average = 0.0f;

    // 平均値更新
    public void updateAverage(float value) {
        average = (average * count + value) / (count + 1);
        count++;
    }

    public float getAverage() {
        if (count == 0) {
            return 0.0f;
        }
        return average;
    }

    public void resetAverage() {
        average = 0;
        count = 0;
    }

    // 引数の平均
    public float getAverage(float... values) {
        float sum = 0.0f;
        for (float value : values) {
            sum += value;
        }
        return sum/values.length;
    }

}