package io.github.harutiro.pdrcanvas2

class Average {
    private var count = 0
    private var average = 0.0f

    // 平均値更新
    fun updateAverage(value: Float) {
        average = (average * count + value) / (count + 1)
        count++
    }

    fun getAverage(): Float {
        if (count == 0) {
            return 0.0f
        }
        return average
    }

    fun resetAverage() {
        average = 0f
        count = 0
    }

    // 引数の平均
    fun getAverage(vararg values: Float): Float {
        var sum = 0.0f
        for (value in values) {
            sum += value
        }
        return sum / values.size
    }
}