package io.github.harutiro.pdrcanvas2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ActionMenuView;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.view.menu.ActionMenuItemView;
import io.github.harutiro.pdrcanvas2.R;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // display size
    protected static Point size;
    private static final float NS2S = 1.0f / 1000000000.0f;

    private SensorManager manager;
    private float[] acc = new float[3];
    private float[] MA = new float[3];
    private long timestamp = 0;
    private long steptimestamp = 0;
    private long steppedtimestamp = 0;

    private int stepcount = 0;
    private float gravity;

    private float min;
    private float max;

    private boolean isCalculateLean = false;
    private boolean isSensorActive = true;
    private int approach = 0;

    private static final Handler handler = new Handler();
    private Runnable runnable;
    private boolean updateHandle = true;

    private MenuItem toggle_sensor = null;
    private boolean pressFlag = false;

    private Average[] average = new Average[3];
    private Angle angle;
    private MadgwickAHRS madgwick;
    private CanvasView canvasView;
    private Capture capture;
    private Step step;
    private MovingAverage movingAverage;
    private CountFlag countFlag;

    private Toolbar toolbar;
    private Toast toast;
    private boolean exitFlag = false;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // 画面縦向き固定
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        size = getDisplaySize(this);
        setContentView(R.layout.activity_main);
        setTitle(R.string.approach1);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);


        average[0] = new Average();
        average[1] = new Average();
        average[2] = new Average();
        angle = new Angle();
        madgwick = new MadgwickAHRS();
        canvasView = (CanvasView) findViewById(R.id.drawing_view);
        capture = new Capture(this, canvasView);
        // size個の加速度平面成分と角度のデータを保持
        step = new Step(50);
        movingAverage = new MovingAverage(30);
        movingAverage.setCutoff(2.0f);
        // countに変化がなくupdateがsize回呼ばれたらcountをreset
        countFlag = new CountFlag(30);
        // toolbar設定
        setupEvenlyDistributedToolbar();
        onToolbarOptionsItemSelected();
        manager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
    }

    // ToolBar Layout
    @SuppressLint("RestrictedApi")
    public void setupEvenlyDistributedToolbar(){
        // Use Display metrics to get Screen Dimensions
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        // Toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar_main);

        // Inflate your menu
        assert toolbar != null;
        toolbar.inflateMenu(R.menu.tool);

        // Add 10 spacing on either side of the toolbar
        toolbar.setContentInsetsAbsolute(10, 10);

        // Get the ChildCount of your Toolbar, this should only be 1
        int childCount = toolbar.getChildCount();
        // Get the Screen Width in pixels
        int screenWidth = metrics.widthPixels;

        // Create the Toolbar Params based on the screenWidth
        Toolbar.LayoutParams toolbarParams = new Toolbar.LayoutParams(screenWidth, Toolbar.LayoutParams.WRAP_CONTENT);

        // Loop through the child Items
        for(int i = 0; i < childCount; i++){
            // Get the item at the current index
            View childView = toolbar.getChildAt(i);
            // If its a ViewGroup
            if(childView instanceof ViewGroup){
                // Set its layout params
                childView.setLayoutParams(toolbarParams);
                // Get the child count of this view group, and compute the item widths based on this count & screen size
                int innerChildCount = ((ViewGroup) childView).getChildCount();
                int itemWidth = (screenWidth/innerChildCount);
                // Create layout params for the ActionMenuView
                ActionMenuView.LayoutParams params = new ActionMenuView.LayoutParams(itemWidth, Toolbar.LayoutParams.WRAP_CONTENT);
                // Loop through the children
                for(int j = 0; j < innerChildCount; j++){
                    View grandChild = ((ViewGroup) childView).getChildAt(j);
                    if(grandChild instanceof ActionMenuItemView){
                        // set the layout parameters on each View
                        grandChild.setLayoutParams(params);
                    }
                }
            }
        }
    }

    public void onToolbarOptionsItemSelected() {
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                int itemId = item.getItemId(); // アイテムIDを取得

                if (itemId == R.id.reset_position) {
                    canvasView.resetMove();
                } else if (itemId == R.id.reset_angle) {
                    canvasView.resetAngle();
                } else if (itemId == R.id.zoom_out) {
                    canvasView.zoomScale(-0.5f);
                } else if (itemId == R.id.zoom_in) {
                    canvasView.zoomScale(0.5f);
                } else if (itemId == R.id.clear_trajectory) {
                    stepcount = 0;
                    canvasView.allDelete();
                }

                canvasView.invalidate();

                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.option, menu);
        toggle_sensor = menu.findItem(R.id.toggle_sensor);
        return super.onCreateOptionsMenu(menu);
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int itemId = item.getItemId();

        if (itemId == R.id.change_approach) {
            //approach = (approach + 1) % 3;
            // 0 <-- toggle --> 2
            approach = 2 - approach;
            // この内側の switch 文は 'approach' がローカル変数なので問題ありません
            switch (approach) {
                case 0:
                    setTitle(R.string.approach1);
                    break;
                case 1:
                    setTitle(R.string.approach2);
                    break;
                case 2:
                    setTitle(R.string.approach3);
                    break;
                default:
                    break;
            }
        } else if (itemId == R.id.toggle_sensor) {
            toggleSensor();
        } else if (itemId == R.id.step_count) {
            toast(stepcount + " step");
        } else if (itemId == R.id.capture_trajectory) {
            capture.captureTrajectory();
            capture.sendPushNotification(stepcount);
        } else if (itemId == R.id.set_config) {
            // SetConfigActivity
            Intent config = new Intent(getApplication(), SetConfigActivity.class);
            config.putExtra("size", canvasView.getPathSize());
            config.putExtra("transparent", canvasView.getTransparent());
            startActivityForResult(config, 1);
        } else if (itemId == R.id.acc_canvas) {
            try {
                Intent intent = new Intent(this, AccCanvas.class);
                startActivity(intent);
                // ここで super.onOptionsItemSelected(item) を返すか、
                // アイテムが処理された場合は 'return true;' とするか検討してください。
                // 一般的には、アイテムを処理した場合は true を返します。
            } catch (Exception e) {
                toast(e.getMessage());
            }
        } else if (itemId == R.id.exit_app) {
            exitApp();
        } else {
            // どの if/else if にも一致しなかった場合、super を呼び出す
            return super.onOptionsItemSelected(item);
        }

        // いずれかの if/else if ブロックでアイテムが処理された場合は true を返す
        return true;
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        switch (event.getAction()) {
            case KeyEvent.ACTION_DOWN:

                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_HEADSETHOOK:
                        if (event.isLongPress()) {
                            toggleSensor();
                            pressFlag = true;
                        }
                        return true;
                    case KeyEvent.KEYCODE_VOLUME_UP:
                        canvasView.zoomScale(0.2f);
                        canvasView.invalidate();
                        return true;
                    case KeyEvent.KEYCODE_VOLUME_DOWN:
                        canvasView.zoomScale(-0.2f);
                        canvasView.invalidate();
                        return true;
                    case KeyEvent.KEYCODE_CAMERA:
                        return true;
                    case KeyEvent.KEYCODE_FOCUS:
                        return true;
                    case KeyEvent.KEYCODE_BACK:
                        return true;
                    default:
                        break;
                }
                break;

            case KeyEvent.ACTION_UP:
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_HEADSETHOOK:
                        if (!pressFlag) {
                            if (isSensorActive) {
                                stepcount = 0;
                                canvasView.allDelete();
                                canvasView.invalidate();
                            } else {
                                capture.captureTrajectory();
                                capture.sendPushNotification(stepcount);
                            }
                        }
                        else {
                            pressFlag = false;
                        }
                        return true;
                    // 操作音消去
                    case KeyEvent.KEYCODE_VOLUME_UP:
                        return true;
                    case KeyEvent.KEYCODE_VOLUME_DOWN:
                        return true;
                    case KeyEvent.KEYCODE_CAMERA:
                        capture.captureTrajectory();
                        capture.sendPushNotification(stepcount);
                        pressFlag = true;
                        return true;
                    case KeyEvent.KEYCODE_FOCUS:
                        if (pressFlag) {
                            pressFlag = false;
                            break;
                        }
                        canvasView.resetScale();
                        canvasView.invalidate();
                        return true;
                    case KeyEvent.KEYCODE_BACK:
                        if (toast == null || !toast.getView().isShown() || !exitFlag) {
                            toast("再度タップすると終了します");
                            exitFlag = true;
                        }
                        else {
                            toast.cancel();
                            exitApp();
                        }
                        return true;
                    default:
                        break;
                }
                break;

            default:
                break;
        }

        return super.dispatchKeyEvent(event);
    }

    // toggle ( sensor & icon )
    private void toggleSensor() {
        isSensorActive = !isSensorActive;
        if (!isSensorActive) {
            onPause();
            toggle_sensor.setIcon(R.drawable.ic_sensor_off);
        }
        else {
            onResume();
            toggle_sensor.setIcon(R.drawable.ic_sensor_on);
        }
    }

    // アプリ終了
    private void exitApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask();
        } else {
            finish();
        }
    }

    @Override
    protected void onResume() {
        if (!isSensorActive) {
            onPause();
            return;
        }

        super.onResume();

        timestamp = 0;
        canvasView.resetMove();
        canvasView.resetAngle();
        isCalculateLean = false;

        manager.registerListener(this, manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
        manager.registerListener(this, manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);
        timerSet();
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopTimerTask();
        manager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int type = event.sensor.getType();
        if(type == Sensor.TYPE_ACCELEROMETER) {
            acc = event.values.clone();
            if (acc[0] == 0.0f && acc[1] == 0.0f && acc[2] == 0.0f) {
                return;
            }

            // 重力加速度の値から姿勢推定
            if (!isCalculateLean) {
                angle.calculateLean(acc);
                madgwick.EulerAnglesToQuaternion(angle.getPitch(), angle.getRoll(), madgwick.getYaw());
                gravity = (float) Math.sqrt(acc[0] * acc[0] + acc[1] * acc[1] + acc[2] * acc[2]);
                step.setGravity(gravity);
                movingAverage.setGravity(gravity);
                isCalculateLean = true;
            }

            // 姿勢更新
            madgwick.update(0.0f, 0.0f, 0.0f, acc[0], acc[1], acc[2]);

            // 世界座標系変換
            float[] absacc;
            if (approach == 0) {
                absacc = madgwick.BodyAccelToRefAccel(acc);
            }
            else {
                angle.setPitch(madgwick.getPitch());
                angle.setRoll(madgwick.getRoll());
                absacc = angle.rotateAxis(acc);
            }

            if (updateHandle) {
                average[0].resetAverage();
                average[1].resetAverage();
                average[2].resetAverage();
                updateHandle = false;
            }
            average[0].updateAverage(absacc[0]);
            average[1].updateAverage(absacc[1]);
            average[2].updateAverage(absacc[2]);
        }
        else if(type == Sensor.TYPE_GYROSCOPE) {
            float[] gyro = event.values.clone();
            if (timestamp != 0) {
                final float dT = (event.timestamp - timestamp) * NS2S;
                //final float dT = (System.nanoTime() - timestamp) * NS2S;
                if (dT > 0.0f) {
                    // 姿勢更新
                    madgwick.setSamplePeriod(dT);
                    madgwick.update(gyro, acc);
                    //canvasView.rotateTrajectory(angle.loopYawRadian(madgwick.getYaw())*180.0f/(float) Math.PI);
                }
            }
            timestamp = event.timestamp;
            //timestamp = System.nanoTime();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    // ステップ検出
    // TODO 精度向上
    private void step() {

        // 移動平均
        movingAverage.add(System.nanoTime(), average[0].getAverage(), average[1].getAverage(), average[2].getAverage());
        MA[0] = movingAverage.getMovingAverageX();
        MA[1] = movingAverage.getMovingAverageY();
        MA[2] = movingAverage.getMovingAverageZ();
        step.add(System.nanoTime(), MA[0], MA[1], MA[2], madgwick.getYaw());

        // IIRローパスフィルタ
        gravity += (step.getAverageZ() - gravity) * 0.10f;

        if (countFlag.equals(0)) {
            if (min > MA[2] - gravity) {
                min = MA[2] - gravity;
            }
            if (MA[2] / gravity > 1.05f) {
                max = MA[2] - gravity;
                countFlag.addCount();
            }
        }
        else if (countFlag.equals(1)) {
            if (max < MA[2] - gravity) {
                max = MA[2] - gravity;
            }
            if (max > MA[2] - gravity) {
                countFlag.addCount();
                steptimestamp = step.getTime();
            }
        }
        else if (countFlag.equals(2)) {
            if (max < MA[2] - gravity) {
                max = MA[2] - gravity;
                countFlag.downCount();
            }
            else if ((step.getTime() - steptimestamp) * NS2S > 0.08f) {
                countFlag.addCount();
            }
        }
        else if (countFlag.equals(3)) {
            if (step.getPlaneNormSlope(-1) < 0) {
                countFlag.addCount();
            }
        }
        else if (countFlag.equals(4)) {
            if ((step.getTime() - steppedtimestamp) * NS2S > 0.35f) {
                // 1.5秒以上経過 --> 静止状態
                // 歩き始めは閾値を低く
                if (max - min > ((step.getTime() - steppedtimestamp) * NS2S > 1.50f ? 0.60f : 1.20f) && step.getMaxNorm() > 0.85f) {
                    switch (approach) {
                        case 0:
                            canvasView.drawTrajectory(angle.loopYawRadian(getAccDirection(step)));
                            break;
                        case 1:
                            canvasView.drawTrajectory(angle.loopYawRadian(getAccDirection(step) + madgwick.getYaw()));
                            break;
                        case 2:
                            canvasView.drawTrajectory(angle.loopYawRadian(madgwick.getYaw()));
                            break;
                        default:
                            break;
                    }
                    steppedtimestamp = step.getTime();
                    stepcount++;
                }
            }
            min = gravity;
            step.clearXY();
            countFlag.addCount();
        }
        else if (countFlag.equals(5)) {
            if (MA[2] / gravity < 1.05f) {
                countFlag.resetCount();
            }
        }

        countFlag.update();

    }

    // handle 一定間隔 処理
    private void timerSet(){
        runnable = new Runnable() {
            @Override
            public void run() {
                if (!updateHandle) {
                    step();
                    canvasView.rotateTrajectory(angle.loopYawRadian(madgwick.getYaw())*180.0f/(float) Math.PI);
                    canvasView.invalidate();
                    updateHandle = true;
                    handler.postDelayed(this, 10L);
                }
                else {
                    handler.postDelayed(this, 0L);
                }
            }
        };

        handler.post(runnable);
    }

    private void stopTimerTask(){
        handler.removeCallbacks(runnable);
    }

    // 進行方向推定
    // TODO 閾値を変数化 & コード改善
    private float getAccDirection(Step step) {
        float direction;
        int[] stepnum = new int[2];
        boolean isInit = false;
        float length = 0.0f;
        float max = 0.0f;
        for (int i = step.getSize() - 2; i > 1; i--) {
            float norm = step.getPlaneNorm(i);
            if (!isInit) {
                if (step.getPlaneNormSlope(i) >= 0 && step.getPlaneNormSlope(i + 1) < 0 && norm > 0.60f) {
                    if (max < norm) {
                        max = norm;
                        stepnum[0] = i;
                    }
                }
                if (norm < 0.50f && stepnum[0] != 0) {
                    isInit = true;
                }
            }
            else {
                if (step.getPlaneNormSlope(i) >= 0 && step.getPlaneNormSlope(i + 1) < 0) {
                    if (length < step.getLengthSquare(stepnum[0], i)) {
                        length = step.getLengthSquare(stepnum[0], i);
                        stepnum[1] = i;
                    }
                }
                if ((step.getTime(-1) - step.getTime(i)) * NS2S > 0.75f && stepnum[1] != 0) {
                    break;
                }
            }
        }
        if (stepnum[0] != 0 && stepnum[1] != 0) {
            direction = (float) Math.atan2(step.getY(stepnum[1]) - step.getY(stepnum[0]), step.getX(stepnum[1]) - step.getX(stepnum[0])) - (float) Math.PI/2;
        }
        else if (stepnum[0] != 0){
            direction = (float) Math.atan2(step.getAverageY() - step.getY(stepnum[0]), step.getAverageX() - step.getX(stepnum[0])) - (float) Math.PI/2;
        }
        else {
            direction = madgwick.getYaw();
        }
        return direction;
    }

    // 画面サイズ取得
    public static Point getDisplaySize(Activity activity){
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        return point;
    }

    public void toast(String text) {
        // toast 重複 待機状態 回避
        if (toast != null && toast.getView().isShown()) {
            toast.cancel();
        }
        toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        toast.show();
        exitFlag = false;
    }

    // 結果受取
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode){
            case(1):
                if(resultCode == RESULT_OK){
                    // OKボタンを押して戻ってきたときの処理
                    canvasView.setPathSize(data.getIntExtra("size", canvasView.getPathSize()));
                    canvasView.setTransparent(data.getBooleanExtra("transparent", canvasView.getTransparent()));
                    canvasView.invalidate();
                }
                else if(resultCode == RESULT_CANCELED){
                    // CANCELEDボタンを押して戻ってきたときの処理
                }
                else{
                    // その他
                }
                break;
            default:
                break;
        }
    }

}