package io.github.harutiro.pdrcanvas2

class CountFlag internal constructor(size: Int) {
    var count: Int = 0
        private set
    private var resetSize = 0
    private var resetCount = 0

    init {
        if (size >= 0) {
            count = 0
            resetSize = size
            resetCount = 0
        } else {
            throw IllegalArgumentException("Illegal Argument:" + size)
        }
    }

    fun addCount() {
        count++
        resetCount = 0
    }

    fun downCount() {
        if (count > 0) {
            count--
            resetCount = 0
        }
    }

    fun equals(index: Int): Boolean {
        return count == index
    }

    fun resetCount() {
        count = 0
        resetCount = 0
    }

    val string: String
        get() = count.toString()

    fun update() {
        if (resetSize == 0 || count == 0) {
            return
        }

        if (resetCount > resetSize) {
            resetCount()
        }
        resetCount++
    }
}