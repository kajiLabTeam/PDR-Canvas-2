package io.github.harutiro.pdrcanvas2;

import static io.github.harutiro.pdrcanvas2.AccCanvas.dpi;
import static io.github.harutiro.pdrcanvas2.AccCanvas.winx;
import static io.github.harutiro.pdrcanvas2.AccCanvas.winy;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class AccCanvasView extends SurfaceView implements SensorEventListener,SurfaceHolder.Callback {

    private Activity context;
    private SurfaceHolder holder;
    private float[][] data;
    private float accX = winx/2.0f;
    private float accY = winy/2.0f;
    private static final int num = 25;
    protected static Canvas canvas;
    private long timestamp = 0;
    private float[] acc = new float[3];
    private float[] lowpass = new float[3];
    private static final float NS2S = 1.0f / 1000000000.0f;
    private boolean isCalculateLean = false;
    private MadgwickAHRS madgwick = new MadgwickAHRS();
    private Angle angle = new Angle();

    public AccCanvasView(Context context) {
        super(context);
        init(context);
    }

    public AccCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    // 初期化
    public void init(Context context) {
        this.context = (Activity)context;
        holder = getHolder();
        holder.addCallback(this);
        setFocusable(true);
        requestFocus();
        data = new float[num][2];
        setAllData(winx/2.0f, winy/2.0f);
    }

    public void setAllData(float x, float y) {
        for (int i = 0; i < data.length; i++) {
            data[i][0] = x;
            data[i][1] = y;
        }
    }

    public void start() {
        SensorManager manager = (SensorManager)context.getSystemService(Activity.SENSOR_SERVICE);
        if (manager != null) {
            manager.registerListener(this, manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
            manager.registerListener(this, manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);
        }
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                addPoint(accX, accY);
                draw();
            }
        }, 25, 25, TimeUnit.MILLISECONDS);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        draw();
        start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {}

    public void addPoint(float x, float y) {
        for (int i = data.length - 2; i >= 0; i--) {
            data[i + 1][0] = data[i][0];
            data[i + 1][1] = data[i][1];
        }
        data[0][0] = x;
        data[0][1] = y;
    }

    public void draw() {
        canvas = holder.lockCanvas();
        canvas.drawColor(Color.WHITE);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        for (int i = 0; i < data.length; i++) {
            float x = data[i][0];
            float y = data[i][1];
            float radius = (35.0f - 25.0f * i / num);

            //円を画面内に描画
            //radius：半径
            //winx：画面横幅
            //winy：画面高さ
            if (x <= radius) {
                x = radius;
            } else if (x >= winx - radius) {
                x = winx - radius;
            }
            if (y <= radius) {
                y = radius;
            } else if (y >= winy - radius) {
                y = winy - radius;
            }

            // 円半径変化
            RectF r = new RectF(x - radius, y - radius, x + radius, y + radius);
            // 色変化
            paint.setColor(Color.HSVToColor(new float[] {180 + 200 * i / num, 1, 1}));
            // 透明度変化
            paint.setAlpha(255 - 230 * i / num);
            //canvas.drawArc(r, 0f, 360f, true, paint);
            canvas.drawOval(r, paint);
        }
        holder.unlockCanvasAndPost(canvas);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            acc = event.values.clone();
            if (acc[0] == 0.0f && acc[1] == 0.0f && acc[2] == 0.0f) {
                return;
            }

            if (!isCalculateLean) {
                angle.calculateLean(acc);
                madgwick.EulerAnglesToQuaternion(angle.getPitch(), angle.getRoll(), 0.0f);
                isCalculateLean = true;
            }

            madgwick.update(0.0f, 0.0f, 0.0f, acc[0], acc[1], acc[2]);

            angle.setPitch(madgwick.getPitch());
            angle.setRoll(madgwick.getRoll());
            float[] absacc = angle.rotateAxis(acc);

            if (timestamp == 0) {
                lowpass[0] = absacc[0];
                lowpass[1] = absacc[1];
                lowpass[2] = absacc[2];
            }

            // IIRローパスフィルタ
            float alpha = 0.08f;
            lowpass[0] += (absacc[0] - lowpass[0]) * alpha;
            lowpass[1] += (absacc[1] - lowpass[1]) * alpha;
            lowpass[2] += (absacc[2] - lowpass[2]) * alpha;

            this.accX =  lowpass[0] * dpi * 0.8f + winx/2.0f;
            this.accY = -lowpass[1] * dpi * 0.8f + winy/2.0f;
        }
        else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float[] gyro = event.values.clone();
            // This timestep's delta rotation to be multiplied by the current rotation
            // after computing it from the gyro sample data.
            if (timestamp != 0) {
                final float dT = (event.timestamp - timestamp) * NS2S;
                if (dT > 0.0f) {
                    madgwick.setSamplePeriod(dT);
                    madgwick.update(gyro, acc);
                }
            }
            timestamp = event.timestamp;
        }
    }

}