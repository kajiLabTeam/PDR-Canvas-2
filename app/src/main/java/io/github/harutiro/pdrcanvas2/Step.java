package io.github.harutiro.pdrcanvas2;

public class Step {

    private long[] time;
    private float[] x;
    private float[] y;
    private float[] z;
    private float[] angle;
    private int size;
    private long period;
    private float averageX = 0.0f;
    private float averageY = 0.0f;
    private float averageZ = 0.0f;
    private boolean averageComputed = false;

    Step(int size) {
        if (size > 0) {
            time = new long[size];
            x = new float[size];
            y = new float[size];
            z = new float[size];
            angle = new float[size];
            this.size = size;
            period = 10L;
        }
        else {
            throw new IllegalArgumentException("Illegal Argument:" + size);
        }
    }

    private void shift() {
        for (int i = 0; i < size - 1; i++) {
            time[i] = time[i + 1];
            x[i] = x[i + 1];
            y[i] = y[i + 1];
            z[i] = z[i + 1];
            angle[i] = angle[i + 1];
        }
    }

    public void add(long time, float x, float y, float z, float angle) {
        shift();
        this.time[size - 1] = time;
        this.x[size - 1] = x;
        this.y[size - 1] = y;
        this.z[size - 1] = z;
        this.angle[size-1] = angle;
        averageComputed = false;
    }

    public void clear() {
        for (int i = 0; i < size; i++) {
            time[i] = 0L;
            x[i] = 0.0f;
            y[i] = 0.0f;
            z[i] = 0.0f;
            angle[i] = 0.0f;
        }
    }

    public void clear(int index) {
        if (index < 0 || size < index) {
            return;
        }
        for (int i = 0; i <= index; i++) {
            time[i] = 0L;
            x[i] = 0.0f;
            y[i] = 0.0f;
            z[i] = 0.0f;
            angle[i] = 0.0f;
        }
    }

    public void clearXY() {
        for (int i = 0; i < size; i++) {
            time[i] = 0L;
            x[i] = 0.0f;
            y[i] = 0.0f;
            angle[i] = 0.0f;
        }
    }

    public void setPeriod(long period) {
        this.period = period;
    }

    public long getPeriod() {
        return period;
    }

    public void setGravity(float gravity) {
        for (int i = 0; i < size; i++) {
            z[i] = gravity;
        }
    }

    private void computeAverage() {
        for (int i = 0; i < size; i++) {
            averageX = (averageX * i + x[i]) / (i + 1);
            averageY = (averageY * i + y[i]) / (i + 1);
            averageZ = (averageZ * i + z[i]) / (i + 1);
        }
        averageComputed = true;
    }

    public float getAverageX() {
        if (!averageComputed) {
            computeAverage();
        }
        return averageX;
    }

    public float getAverageY() {
        if (!averageComputed) {
            computeAverage();
        }
        return averageY;
    }

    public float getAverageZ() {
        if (!averageComputed) {
            computeAverage();
        }
        return averageZ;
    }

    private int getIndex(int index) {
        return ((index % size) + size) % size;
    }

    public long getTime() {
        return time[size - 1];
    }

    public long getTime(int index) {
        return time[getIndex(index)];
    }

    public long getdT() {
        if (time[size - 1] == 0L || time[size - 2] == 0L) {
            return period;
        }
        return time[size - 1] - time[size - 2];
    }

    public float getX() {
        return x[size - 1];
    }

    public float getX(int index) {
        return x[getIndex(index)];
    }

    public float getY() {
        return y[size - 1];
    }

    public float getY(int index) {
        return y[getIndex(index)];
    }

    public float getZ() {
        return z[size - 1];
    }

    public float getZ(int index) {
        return z[getIndex(index)];
    }

    public float getAngle() {
        return angle[size - 1];
    }

    public float getAngle(int index) {
        return angle[getIndex(index)];
    }

    public int getSize() {
        return size;
    }

    public float getRotateX(int index) {
        return (float) Math.cos(angle[getIndex(index)]) * (x[getIndex(index)]-getAverageX()) - (float) Math.sin(angle[getIndex(index)]) * (y[getIndex(index)]-getAverageY());
    }

    public float[] getRotateX() {
        float[] rotateX = new float[size];
        for (int i = 0; i < size; i++) {
            rotateX[i] = getRotateX(i);
        }
        return rotateX;
    }

    public float getRotateX(int index, float x0, float y0) {
        return (float) Math.cos(angle[getIndex(index)]) * (x[getIndex(index)]-x0) - (float) Math.sin(angle[getIndex(index)]) * (y[getIndex(index)]-y0);
    }

    public float getRotateY(int index) {
        return (float) Math.sin(angle[getIndex(index)]) * (x[getIndex(index)]-getAverageX()) + (float) Math.cos(angle[getIndex(index)]) * (y[getIndex(index)]-getAverageY());
    }

    public float[] getRotateY() {
        float[] rotateY = new float[size];
        for (int i = 0; i < size; i++) {
            rotateY[i] = getRotateY(i);
        }
        return rotateY;
    }

    public float getRotateY(int index, float x0, float y0) {
        return (float) Math.sin(angle[getIndex(index)]) * (x[getIndex(index)]-x0) + (float) Math.cos(angle[getIndex(index)]) * (y[getIndex(index)]-y0);
    }

    public float getNorm() {
        return (float) Math.sqrt((x[size - 1] - getAverageX()) * (x[size - 1] - getAverageX()) + (y[size - 1] - getAverageY()) * (y[size - 1] - getAverageY()) + (z[size - 1] - getAverageZ()) * (z[size - 1] - getAverageZ()));
    }

    public float getNorm(int index) {
        return (float) Math.sqrt((x[getIndex(index)] - getAverageX()) * (x[getIndex(index)] - getAverageX()) + (y[getIndex(index)] - getAverageY()) * (y[getIndex(index)] - getAverageY()) + (z[getIndex(index)] - getAverageZ()) * (z[getIndex(index)] - getAverageZ()));
    }

    public float getPlaneNorm() {
        return (float) Math.sqrt((x[size - 1] - getAverageX()/2.0f) * (x[size - 1] - getAverageX()/2.0f) + (y[size - 1] - getAverageY()/2.0f) * (y[size - 1] - getAverageY()/2.0f));
    }

    public float getPlaneNorm(int index) {
        return (float) Math.sqrt((x[getIndex(index)] - getAverageX()) * (x[getIndex(index)] - getAverageX()) + (y[getIndex(index)] - getAverageY()) * (y[getIndex(index)] - getAverageY()));
    }

    public float getMaxNorm() {
        float max = 0.0f;
        for (int i = 0; i < size; i++) {
            if (max < getPlaneNorm(i)) {
                max = getPlaneNorm(i);
            }
        }
        return max;
    }

    public float getNormAverage() {
        float average = 0.0f;
        for (int i = 0; i < size; i++) {
            average = (average * i + getPlaneNorm(i)) / (i + 1);
        }
        return average;
    }

    public float getPlaneNormSlope(int index) {
        return getPlaneNorm(index) - getPlaneNorm(index - 1);
    }

    public String getString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            sb.append(getString(i));
        }
        return sb.toString();
    }

    private String getString(int index) {
        return time[getIndex(index)] + "," + x[getIndex(index)] + "," + y[getIndex(index)] + "," + z[getIndex(index)] + "," + angle[getIndex(index)] + "\n";
    }

    public float getLengthSquare(int index1, int index2) {
        return (x[getIndex(index2)]-x[getIndex(index1)])*(x[getIndex(index2)]-x[getIndex(index1)]) + (y[getIndex(index2)]-y[getIndex(index1)])*(y[getIndex(index2)]-y[getIndex(index1)]);
    }

    private long[] setArraySize(long[] array, int length) {
        long[] copy = new long[length];
        // rshift
        //System.arraycopy(array, 0, copy, 0, copy.length > array.length ? array.length : copy.length);
        // shift
        System.arraycopy(array, copy.length > array.length ? 0 : array.length-copy.length, copy, copy.length > array.length ? copy.length-array.length : 0, copy.length > array.length ? array.length : copy.length);
        return copy;
    }

    private float[] setArraySize(float[] array, int length) {
        float[] copy = new float[length];
        // rshift
        //System.arraycopy(array, 0, copy, 0, copy.length > array.length ? array.length : copy.length);
        // shift
        System.arraycopy(array, copy.length > array.length ? 0 : array.length-copy.length, copy, copy.length > array.length ? copy.length-array.length : 0, copy.length > array.length ? array.length : copy.length);
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
        angle = setArraySize(angle, length);
        size = length;
    }

    public void addArraySize(long time, float x, float y, float z, float angle) {
        setSize(size + 1);
        add(time, x, y, z, angle);
    }

}