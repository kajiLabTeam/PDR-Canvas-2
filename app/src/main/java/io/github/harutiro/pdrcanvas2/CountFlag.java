package io.github.harutiro.pdrcanvas2;

public class CountFlag {

    private int count;
    private int resetSize;
    private int resetCount;

    CountFlag(int size) {
        if (size >= 0) {
            count = 0;
            resetSize = size;
            resetCount = 0;
        }
        else {
            throw new IllegalArgumentException("Illegal Argument:" + size);
        }
    }

    public void addCount() {
        count++;
        resetCount = 0;
    }

    public void downCount() {
        if (count > 0) {
            count--;
            resetCount = 0;
        }
    }

    public boolean equals(int index) {
        return count == index;
    }

    public void resetCount() {
        count = 0;
        resetCount = 0;
    }

    public int getCount() {
        return count;
    }

    public String getString() {
        return Integer.toString(count);
    }

    public void update() {
        if (resetSize == 0 || count == 0) {
            return;
        }

        if (resetCount > resetSize) {
            resetCount();
        }
        resetCount++;
    }

}