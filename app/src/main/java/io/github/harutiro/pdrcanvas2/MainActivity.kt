package io.github.harutiro.pdrcanvas2

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Point
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ActionMenuView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.view.menu.ActionMenuItemView
import kotlin.math.atan2
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {
    private var manager: SensorManager? = null
    private var acc = FloatArray(3)
    private val MA = FloatArray(3)
    private var timestamp: Long = 0
    private var steptimestamp: Long = 0
    private var steppedtimestamp: Long = 0

    private var stepcount = 0
    private var gravity = 0f

    private var min = 0f
    private var max = 0f

    private var isCalculateLean = false
    private var isSensorActive = true
    private var approach = 0

    private var runnable: Runnable? = null
    private var updateHandle = true

    private var toggle_sensor: MenuItem? = null
    private var pressFlag = false

    private val average = arrayOfNulls<Average>(3)
    private var angle: Angle? = null
    private var madgwick: MadgwickAHRS? = null
    private var canvasView: CanvasView? = null
    private var capture: Capture? = null
    private var step: Step? = null
    private var movingAverage: MovingAverage? = null
    private var countFlag: CountFlag? = null

    private lateinit var toolbarTop: androidx.appcompat.widget.Toolbar
    private lateinit var toolbarBottom: androidx.appcompat.widget.Toolbar
    private var toast: Toast? = null
    private var exitFlag = false

    public override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        // 画面縦向き固定
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
        size = getDisplaySize(this)
        setContentView(R.layout.activity_main)
        setTitle(R.string.approach1)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        average[0] = Average()
        average[1] = Average()
        average[2] = Average()
        angle = Angle()
        madgwick = MadgwickAHRS()
        canvasView = findViewById<View?>(R.id.drawing_view) as CanvasView
        capture = Capture(this, canvasView!!)
        // size個の加速度平面成分と角度のデータを保持
        step = Step(50)
        movingAverage = MovingAverage(30)
        movingAverage!!.setCutoff(2.0f)
        // countに変化がなくupdateがsize回呼ばれたらcountをreset
        countFlag = CountFlag(30)
        // toolbar設定
        toolbarTop = findViewById(R.id.toolbar_top)
        setSupportActionBar(toolbarTop)
        supportActionBar?.title = getString(R.string.approach1)
        toolbarTop.title = getString(R.string.approach1)
        toolbarTop.setTitleTextColor(Color.WHITE)
        setupEvenlyDistributedToolbar(toolbarTop)

        toolbarBottom = findViewById(R.id.toolbar_bottom)
        toolbarBottom.inflateMenu(R.menu.tool)
        setupEvenlyDistributedToolbar(toolbarBottom)
        toolbarBottom.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.reset_position -> {
                    canvasView!!.resetMove()
                    canvasView!!.invalidate()
                    true
                }
                R.id.reset_angle -> {
                    canvasView!!.resetAngle()
                    canvasView!!.invalidate()
                    true
                }
                R.id.zoom_out -> {
                    canvasView!!.zoomScale(-0.5f)
                    canvasView!!.invalidate()
                    true
                }
                R.id.zoom_in -> {
                    canvasView!!.zoomScale(0.5f)
                    canvasView!!.invalidate()
                    true
                }
                R.id.clear_trajectory -> {
                    stepcount = 0
                    canvasView!!.allDelete()
                    canvasView!!.invalidate()
                    true
                }
                else -> false
            }
        }
        manager = getSystemService(SENSOR_SERVICE) as SensorManager
    }

    // ToolBar Layout
    @SuppressLint("RestrictedApi")
    fun setupEvenlyDistributedToolbar(toolbar: androidx.appcompat.widget.Toolbar) {
        // Use Display metrics to get Screen Dimensions
        val display = getWindowManager().getDefaultDisplay()
        val metrics = DisplayMetrics()
        display.getMetrics(metrics)

        // Toolbar
        // toolbar = findViewById<View?>(R.id.toolbar_main) as Toolbar // ← ここはonCreateでやる

        // Inflate your menu
        // checkNotNull(toolbar)
        // toolbar!!.inflateMenu(R.menu.tool) // ← ここを削除

        // Add 10 spacing on either side of the toolbar
        toolbar.setContentInsetsAbsolute(10, 10)

        // Get the ChildCount of your Toolbar, this should only be 1
        val childCount = toolbar.childCount
        // Get the Screen Width in pixels
        val screenWidth = metrics.widthPixels

        // Create the Toolbar Params based on the screenWidth
        val toolbarParams = androidx.appcompat.widget.Toolbar.LayoutParams(screenWidth, androidx.appcompat.widget.Toolbar.LayoutParams.WRAP_CONTENT)

        // Loop through the child Items
        for (i in 0..<childCount) {
            // Get the item at the current index
            val childView = toolbar.getChildAt(i)
            // If its a ViewGroup
            if (childView is ViewGroup) {
                // Set its layout params
                childView.setLayoutParams(toolbarParams)
                // Get the child count of this view group, and compute the item widths based on this count & screen size
                val innerChildCount = childView.getChildCount()
                val itemWidth = (screenWidth / innerChildCount)
                // Create layout params for the ActionMenuView
                val params =
                    android.view.ViewGroup.LayoutParams(itemWidth, androidx.appcompat.widget.Toolbar.LayoutParams.WRAP_CONTENT)
                // Loop through the children
                for (j in 0..<innerChildCount) {
                    val grandChild = childView.getChildAt(j)
                    if (grandChild is ActionMenuItemView) {
                        // set the layout parameters on each View
                        grandChild.setLayoutParams(params)
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.option, menu)
        toggle_sensor = menu.findItem(R.id.toggle_sensor)
        return true
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.getItemId()

        if (itemId == R.id.change_approach) {
            //approach = (approach + 1) % 3;
            // 0 <-- toggle --> 2
            approach = 2 - approach
            // この内側の switch 文は 'approach' がローカル変数なので問題ありません
            when (approach) {
                0 -> supportActionBar?.title = getString(R.string.approach1)
                1 -> supportActionBar?.title = getString(R.string.approach2)
                2 -> supportActionBar?.title = getString(R.string.approach3)
            }
        } else if (itemId == R.id.toggle_sensor) {
            toggleSensor()
        } else if (itemId == R.id.step_count) {
            toast(stepcount.toString() + " step")
        } else if (itemId == R.id.capture_trajectory) {
            capture!!.captureTrajectory()
            capture!!.sendPushNotification(stepcount)
        } else if (itemId == R.id.set_config) {
            // SetConfigActivity
            val config = Intent(getApplication(), SetConfigActivity::class.java)
            config.putExtra("size", canvasView!!.pathSize)
            config.putExtra("transparent", canvasView!!.transparent)
            startActivityForResult(config, 1)
        } else if (itemId == R.id.acc_canvas) {
            try {
                val intent = Intent(this, AccCanvas::class.java)
                startActivity(intent)
                // ここで super.onOptionsItemSelected(item) を返すか、
                // アイテムが処理された場合は 'return true;' とするか検討してください。
                // 一般的には、アイテムを処理した場合は true を返します。
            } catch (e: Exception) {
                toast(e.message)
            }
        } else if (itemId == R.id.exit_app) {
            exitApp()
        } else {
            // どの if/else if にも一致しなかった場合、super を呼び出す
            return super.onOptionsItemSelected(item)
        }

        // いずれかの if/else if ブロックでアイテムが処理された場合は true を返す
        return true
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        when (event.getAction()) {
            KeyEvent.ACTION_DOWN -> when (event.getKeyCode()) {
                KeyEvent.KEYCODE_HEADSETHOOK -> {
                    if (event.isLongPress()) {
                        toggleSensor()
                        pressFlag = true
                    }
                    return true
                }

                KeyEvent.KEYCODE_VOLUME_UP -> {
                    canvasView!!.zoomScale(0.2f)
                    canvasView!!.invalidate()
                    return true
                }

                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    canvasView!!.zoomScale(-0.2f)
                    canvasView!!.invalidate()
                    return true
                }

                KeyEvent.KEYCODE_CAMERA -> return true
                KeyEvent.KEYCODE_FOCUS -> return true
                KeyEvent.KEYCODE_BACK -> return true
                else -> {}
            }

            KeyEvent.ACTION_UP -> when (event.getKeyCode()) {
                KeyEvent.KEYCODE_HEADSETHOOK -> {
                    if (!pressFlag) {
                        if (isSensorActive) {
                            stepcount = 0
                            canvasView!!.allDelete()
                            canvasView!!.invalidate()
                        } else {
                            capture!!.captureTrajectory()
                            capture!!.sendPushNotification(stepcount)
                        }
                    } else {
                        pressFlag = false
                    }
                    return true
                }

                KeyEvent.KEYCODE_VOLUME_UP -> return true
                KeyEvent.KEYCODE_VOLUME_DOWN -> return true
                KeyEvent.KEYCODE_CAMERA -> {
                    capture!!.captureTrajectory()
                    capture!!.sendPushNotification(stepcount)
                    pressFlag = true
                    return true
                }

                KeyEvent.KEYCODE_FOCUS -> {
                    if (pressFlag) {
                        pressFlag = false
                        return true
                    }
                    canvasView!!.resetScale()
                    canvasView!!.invalidate()
                    return true
                }

                KeyEvent.KEYCODE_BACK -> {
                    if (toast == null || !toast!!.view!!.isShown() || !exitFlag) {
                        toast("再度タップすると終了します")
                        exitFlag = true
                    } else {
                        toast!!.cancel()
                        exitApp()
                    }
                    return true
                }

                else -> {}
            }

            else -> {}
        }

        return super.dispatchKeyEvent(event)
    }

    // toggle ( sensor & icon )
    private fun toggleSensor() {
        isSensorActive = !isSensorActive
        if (!isSensorActive) {
            onPause()
            toggle_sensor!!.setIcon(R.drawable.ic_sensor_off)
        } else {
            onResume()
            toggle_sensor!!.setIcon(R.drawable.ic_sensor_on)
        }
    }

    // アプリ終了
    private fun exitApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask()
        } else {
            finish()
        }
    }

    override fun onResume() {
        if (!isSensorActive) {
            onPause()
            return
        }

        super.onResume()

        timestamp = 0
        canvasView!!.resetMove()
        canvasView!!.resetAngle()
        isCalculateLean = false

        manager!!.registerListener(
            this,
            manager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_FASTEST
        )
        manager!!.registerListener(
            this,
            manager!!.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
            SensorManager.SENSOR_DELAY_FASTEST
        )
        timerSet()
    }

    override fun onPause() {
        super.onPause()

        stopTimerTask()
        manager!!.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val type = event.sensor.getType()
        if (type == Sensor.TYPE_ACCELEROMETER) {
            acc = event.values.clone()
            if (acc[0] == 0.0f && acc[1] == 0.0f && acc[2] == 0.0f) {
                return
            }

            // 重力加速度の値から姿勢推定
            if (!isCalculateLean) {
                angle!!.calculateLean(acc)
                madgwick!!.EulerAnglesToQuaternion(angle!!.pitch, angle!!.roll, madgwick!!.getYaw())
                gravity =
                    sqrt((acc[0] * acc[0] + acc[1] * acc[1] + acc[2] * acc[2]).toDouble()).toFloat()
                step!!.setGravity(gravity)
                movingAverage!!.setGravity(gravity)
                isCalculateLean = true
            }

            // 姿勢更新
            madgwick!!.update(0.0f, 0.0f, 0.0f, acc[0], acc[1], acc[2])

            // 世界座標系変換
            val absacc: FloatArray?
            if (approach == 0) {
                absacc = madgwick!!.BodyAccelToRefAccel(acc)
            } else {
                angle!!.pitch = madgwick!!.getPitch()
                angle!!.roll = madgwick!!.getRoll()
                absacc = angle!!.rotateAxis(acc)
            }

            if (updateHandle) {
                average[0]!!.resetAverage()
                average[1]!!.resetAverage()
                average[2]!!.resetAverage()
                updateHandle = false
            }
            average[0]!!.updateAverage(absacc[0])
            average[1]!!.updateAverage(absacc[1])
            average[2]!!.updateAverage(absacc[2])
        } else if (type == Sensor.TYPE_GYROSCOPE) {
            val gyro = event.values.clone()
            if (timestamp != 0L) {
                val dT: Float = (event.timestamp - timestamp) * NS2S
                //final float dT = (System.nanoTime() - timestamp) * NS2S;
                if (dT > 0.0f) {
                    // 姿勢更新
                    madgwick!!.samplePeriod = dT
                    madgwick!!.update(gyro, acc)
                    //canvasView.rotateTrajectory(angle.loopYawRadian(madgwick.getYaw())*180.0f/(float) Math.PI);
                }
            }
            timestamp = event.timestamp
            //timestamp = System.nanoTime();
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    // ステップ検出
    // TODO 精度向上
    private fun step() {
        // 移動平均

        movingAverage!!.add(
            System.nanoTime(),
            average[0]!!.getAverage(),
            average[1]!!.getAverage(),
            average[2]!!.getAverage()
        )
        MA[0] = movingAverage!!.getX()
        MA[1] = movingAverage!!.getY()
        MA[2] = movingAverage!!.getZ()
        step!!.add(System.nanoTime(), MA[0], MA[1], MA[2], madgwick!!.getYaw())

        // IIRローパスフィルタ
        gravity += (step!!.getAverageZ() - gravity) * 0.10f

        if (countFlag!!.equals(0)) {
            if (min > MA[2] - gravity) {
                min = MA[2] - gravity
            }
            if (MA[2] / gravity > 1.05f) {
                max = MA[2] - gravity
                countFlag!!.addCount()
            }
        } else if (countFlag!!.equals(1)) {
            if (max < MA[2] - gravity) {
                max = MA[2] - gravity
            }
            if (max > MA[2] - gravity) {
                countFlag!!.addCount()
                steptimestamp = step!!.getTime()
            }
        } else if (countFlag!!.equals(2)) {
            if (max < MA[2] - gravity) {
                max = MA[2] - gravity
                countFlag!!.downCount()
            } else if ((step!!.getTime() - steptimestamp) * NS2S > 0.08f) {
                countFlag!!.addCount()
            }
        } else if (countFlag!!.equals(3)) {
            if (step!!.getPlaneNormSlope(-1) < 0) {
                countFlag!!.addCount()
            }
        } else if (countFlag!!.equals(4)) {
            if ((step!!.getTime() - steppedtimestamp) * NS2S > 0.35f) {
                // 1.5秒以上経過 --> 静止状態
                // 歩き始めは閾値を低く
                if (max - min > (if ((step!!.getTime() - steppedtimestamp) * NS2S > 1.50f) 0.60f else 1.20f) && step!!.maxNorm > 0.85f) {
                    when (approach) {
                        0 -> canvasView!!.drawTrajectory(
                            angle!!.loopYawRadian(
                                getAccDirection(
                                    step!!
                                )
                            )
                        )

                        1 -> canvasView!!.drawTrajectory(angle!!.loopYawRadian(getAccDirection(step!!) + madgwick!!.getYaw()))
                        2 -> canvasView!!.drawTrajectory(angle!!.loopYawRadian(madgwick!!.getYaw()))
                        else -> {}
                    }
                    steppedtimestamp = step!!.getTime()
                    stepcount++
                }
            }
            min = gravity
            step!!.clearXY()
            countFlag!!.addCount()
        } else if (countFlag!!.equals(5)) {
            if (MA[2] / gravity < 1.05f) {
                countFlag!!.resetCount()
            }
        }

        countFlag!!.update()
    }

    // handle 一定間隔 処理
    private fun timerSet() {
        runnable = object : Runnable {
            override fun run() {
                if (!updateHandle) {
                    step()
                    canvasView!!.rotateTrajectory(angle!!.loopYawRadian(madgwick!!.getYaw()) * 180.0f / Math.PI.toFloat())
                    canvasView!!.invalidate()
                    updateHandle = true
                    handler.postDelayed(this, 10L)
                } else {
                    handler.postDelayed(this, 0L)
                }
            }
        }

        handler.post(runnable!!)
    }

    private fun stopTimerTask() {
        handler.removeCallbacks(runnable!!)
    }

    // 進行方向推定
    // TODO 閾値を変数化 & コード改善
    private fun getAccDirection(step: Step): Float {
        val direction: Float
        val stepnum = IntArray(2)
        var isInit = false
        var length = 0.0f
        var max = 0.0f
        for (i in step.getSize() - 2 downTo 2) {
            val norm = step.getPlaneNorm(i)
            if (!isInit) {
                if (step.getPlaneNormSlope(i) >= 0 && step.getPlaneNormSlope(i + 1) < 0 && norm > 0.60f) {
                    if (max < norm) {
                        max = norm
                        stepnum[0] = i
                    }
                }
                if (norm < 0.50f && stepnum[0] != 0) {
                    isInit = true
                }
            } else {
                if (step.getPlaneNormSlope(i) >= 0 && step.getPlaneNormSlope(i + 1) < 0) {
                    if (length < step.getLengthSquare(stepnum[0], i)) {
                        length = step.getLengthSquare(stepnum[0], i)
                        stepnum[1] = i
                    }
                }
                if ((step.getTime(-1) - step.getTime(i)) * NS2S > 0.75f && stepnum[1] != 0) {
                    break
                }
            }
        }
        if (stepnum[0] != 0 && stepnum[1] != 0) {
            direction = atan2(
                (step.getY(stepnum[1]) - step.getY(stepnum[0])).toDouble(),
                (step.getX(stepnum[1]) - step.getX(stepnum[0])).toDouble()
            ).toFloat() - Math.PI.toFloat() / 2
        } else if (stepnum[0] != 0) {
            direction = atan2(
                (step.getAverageY() - step.getY(stepnum[0])).toDouble(),
                (step.getAverageX() - step.getX(stepnum[0])).toDouble()
            ).toFloat() - Math.PI.toFloat() / 2
        } else {
            direction = madgwick!!.getYaw()
        }
        return direction
    }

    fun toast(text: String?) {
        // toast 重複 待機状態 回避
        if (toast != null && toast!!.getView()!!.isShown()) {
            toast!!.cancel()
        }
        toast = Toast.makeText(this, text, Toast.LENGTH_SHORT)
        toast!!.show()
        exitFlag = false
    }

    // 結果受取
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            (1) -> if (resultCode == RESULT_OK) {
                // OKボタンを押して戻ってきたときの処理
                canvasView!!.pathSize = data?.getIntExtra("size", canvasView!!.pathSize)!!
                canvasView!!.transparent =
                    data.getBooleanExtra("transparent", canvasView!!.transparent)
                canvasView!!.invalidate()
            } else if (resultCode == RESULT_CANCELED) {
                // CANCELEDボタンを押して戻ってきたときの処理
            } else {
                // その他
            }

            else -> {}
        }
    }

    companion object {
        // display size
        var size: Point? = null
        private val NS2S = 1.0f / 1000000000.0f

        private val handler = Handler()

        // 画面サイズ取得
        fun getDisplaySize(activity: Activity): Point {
            val display = activity.getWindowManager().getDefaultDisplay()
            val point = Point()
            display.getSize(point)
            return point
        }
    }
}