package io.github.harutiro.pdrcanvas2;

public class MovingAverage {

    private static final float NS2S = 1.0f / 1000000000.0f;
    private long[] time;
    private float[] x;
    private float[] y;
    private float[] z;
    private int size;
    private long period;
    private float cutoff;
    private float averageX = 0.0f;
    private float averageY = 0.0f;
    private float averageZ = 0.0f;
    private boolean averageComputed = false;

    MovingAverage(int size) {
        if (size > 0) {
            time = new long[size];
            x = new float[size];
            y = new float[size];
            z = new float[size];
            this.size = size;
            period = 10L;
            cutoff = 2.0f;
        }
        else {
            throw new IllegalArgumentException("Illegal Argument:" + size);
        }
    }

    private void rshift() {
        for (int i = size - 1; i > 0; i--) {
            time[i] = time[i - 1];
            x[i] = x[i - 1];
            y[i] = y[i - 1];
            z[i] = z[i - 1];
        }
    }

    public void add(long time, float x, float y, float z) {
        rshift();
        this.time[0] = time;
        this.x[0] = x;
        this.y[0] = y;
        this.z[0] = z;
        averageComputed = false;
    }

    public void setPeriod(long period) {
        this.period = period;
    }

    public long getPeriod() {
        return period;
    }

    public void setCutoff(float cutoff) {
        this.cutoff = cutoff;
    }

    public void setGravity(float gravity) {
        for (int i = 0; i < size; i++) {
            z[i] = gravity;
        }
    }

    private void computeMovingAverage() {
        // 0 < n < size
        int n = (int) Math.max(1, Math.min(size, Math.sqrt(Math.pow(((0.443 * 1.0 / (getdT() * NS2S)) / cutoff), 2.0)) + 1));
        for (int i = 0; i < n; i++) {
            averageX = (averageX * i + x[i]) / (i + 1);
            averageY = (averageY * i + y[i]) / (i + 1);
            averageZ = (averageZ * i + z[i]) / (i + 1);
        }
        averageComputed = true;
    }

    public float getMovingAverageX() {
        if (!averageComputed) {
            computeMovingAverage();
        }
        return averageX;
    }

    public float getMovingAverageY() {
        if (!averageComputed) {
            computeMovingAverage();
        }
        return averageY;
    }

    public float getMovingAverageZ() {
        if (!averageComputed) {
            computeMovingAverage();
        }
        return averageZ;
    }

    private int getIndex(int index) {
        return ((index % size) + size) % size;
    }

    public long getTime() {
        return time[0];
    }

    public long getTime(int index) {
        return time[getIndex(index)];
    }

    public long getdT() {
        if (time[0] == 0L || time[1] == 0L) {
            return period;
        }
        return time[0] - time[1];
    }

    public float getX() {
        return x[0];
    }

    public float getX(int index) {
        return x[getIndex(index)];
    }

    public float getY() {
        return y[0];
    }

    public float getY(int index) {
        return y[getIndex(index)];
    }

    public float getZ() {
        return z[0];
    }

    public float getZ(int index) {
        return z[getIndex(index)];
    }

    public int getSize() {
        return size;
    }

    public String getString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            sb.append(getString(i));
        }
        return sb.toString();
    }

    public String getString(int index) {
        return time[getIndex(index)] + "," + x[getIndex(index)] + "," + y[getIndex(index)] + "," + z[getIndex(index)] + "\n";
    }

    private long[] setArraySize(long[] array, int length) {
        long[] copy = new long[length];
        // rshift
        System.arraycopy(array, 0, copy, 0, copy.length > array.length ? array.length : copy.length);
        // shift
        //System.arraycopy(array, copy.length > array.length ? 0 : array.length-copy.length, copy, copy.length > array.length ? copy.length-array.length : 0, copy.length > array.length ? array.length : copy.length);
        return copy;
    }

    private float[] setArraySize(float[] array, int length) {
        float[] copy = new float[length];
        // rshift
        System.arraycopy(array, 0, copy, 0, copy.length > array.length ? array.length : copy.length);
        // shift
        //System.arraycopy(array, copy.length > array.length ? 0 : array.length-copy.length, copy, copy.length > array.length ? copy.length-array.length : 0, copy.length > array.length ? array.length : copy.length);
        return copy;
    }

    public void setSize(int length) {
        if (length <= 0) {
            return;
        }
        time = setArraySize(time, length);
        x = setArraySize(x, length);
        y = setArraySize(y, length);
        z = setArraySize(z, length);
        size = length;
    }

    public void addArraySize(long time, float x, float y, float z) {
        setSize(size + 1);
        add(time, x, y, z);
    }

}