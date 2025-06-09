package io.github.harutiro.pdrcanvas2

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AccCanvasView : SurfaceView, SensorEventListener, SurfaceHolder.Callback {
    private var context: Activity? = null
    private var holder: SurfaceHolder? = null
    private lateinit var data: Array<FloatArray?>
    private var accX = AccCanvas.Companion.winx / 2.0f
    private var accY = AccCanvas.Companion.winy / 2.0f
    private var timestamp: Long = 0
    private var acc = FloatArray(3)
    private val lowpass = FloatArray(3)
    private var isCalculateLean = false
    private val madgwick = MadgwickAHRS()
    private val angle = Angle()

    constructor(context: Context?) : super(context) {
        init(context)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    // 初期化
    fun init(context: Context?) {
        this.context = context as Activity
        holder = getHolder()
        holder!!.addCallback(this)
        setFocusable(true)
        requestFocus()
        data = Array<FloatArray?>(num) { FloatArray(2) }
        setAllData(AccCanvas.Companion.winx / 2.0f, AccCanvas.Companion.winy / 2.0f)
    }

    fun setAllData(x: Float, y: Float) {
        for (i in data.indices) {
            data[i]!![0] = x
            data[i]!![1] = y
        }
    }

    fun start() {
        val manager = context!!.getSystemService(Activity.SENSOR_SERVICE) as SensorManager?
        if (manager != null) {
            manager.registerListener(
                this,
                manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST
            )
            manager.registerListener(
                this,
                manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_FASTEST
            )
        }
        val executor = Executors.newSingleThreadScheduledExecutor()
        executor.scheduleWithFixedDelay(object : Runnable {
            override fun run() {
                addPoint(accX, accY)
                draw()
            }
        }, 25, 25, TimeUnit.MILLISECONDS)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        draw()
        start()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {}

    fun addPoint(x: Float, y: Float) {
        for (i in data.size - 2 downTo 0) {
            data[i + 1]!![0] = data[i]!![0]
            data[i + 1]!![1] = data[i]!![1]
        }
        data[0]!![0] = x
        data[0]!![1] = y
    }

    fun draw() {
        canvas = holder!!.lockCanvas()
        canvas!!.drawColor(Color.WHITE)
        val paint = Paint()
        paint.setStyle(Paint.Style.FILL)
        paint.setColor(Color.BLACK)
        for (i in data.indices) {
            var x = data[i]!![0]
            var y = data[i]!![1]
            val radius: Float = (35.0f - 25.0f * i / num)

            //円を画面内に描画
            //radius：半径
            //winx：画面横幅
            //winy：画面高さ
            if (x <= radius) {
                x = radius
            } else if (x >= AccCanvas.Companion.winx - radius) {
                x = AccCanvas.Companion.winx - radius
            }
            if (y <= radius) {
                y = radius
            } else if (y >= AccCanvas.Companion.winy - radius) {
                y = AccCanvas.Companion.winy - radius
            }

            // 円半径変化
            val r = RectF(x - radius, y - radius, x + radius, y + radius)
            // 色変化
            paint.setColor(Color.HSVToColor(floatArrayOf((180 + 200 * i / num).toFloat(), 1f, 1f)))
            // 透明度変化
            paint.setAlpha(255 - 230 * i / num)
            //canvas.drawArc(r, 0f, 360f, true, paint);
            canvas!!.drawOval(r, paint)
        }
        holder!!.unlockCanvasAndPost(canvas)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            acc = event.values.clone()
            if (acc[0] == 0.0f && acc[1] == 0.0f && acc[2] == 0.0f) {
                return
            }

            if (!isCalculateLean) {
                angle.calculateLean(acc)
                madgwick.EulerAnglesToQuaternion(angle.pitch, angle.roll, 0.0f)
                isCalculateLean = true
            }

            madgwick.update(0.0f, 0.0f, 0.0f, acc[0], acc[1], acc[2])

            angle.pitch = madgwick.getPitch()
            angle.roll = madgwick.getRoll()
            val absacc = angle.rotateAxis(acc)

            if (timestamp == 0L) {
                lowpass[0] = absacc[0]
                lowpass[1] = absacc[1]
                lowpass[2] = absacc[2]
            }

            // IIRローパスフィルタ
            val alpha = 0.08f
            lowpass[0] += (absacc[0] - lowpass[0]) * alpha
            lowpass[1] += (absacc[1] - lowpass[1]) * alpha
            lowpass[2] += (absacc[2] - lowpass[2]) * alpha

            this.accX =
                lowpass[0] * AccCanvas.Companion.dpi * 0.8f + AccCanvas.Companion.winx / 2.0f
            this.accY =
                -lowpass[1] * AccCanvas.Companion.dpi * 0.8f + AccCanvas.Companion.winy / 2.0f
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            val gyro = event.values.clone()
            // This timestep's delta rotation to be multiplied by the current rotation
            // after computing it from the gyro sample data.
            if (timestamp != 0L) {
                val dT: Float = (event.timestamp - timestamp) * NS2S
                if (dT > 0.0f) {
                    madgwick.samplePeriod = dT
                    madgwick.update(gyro, acc)
                }
            }
            timestamp = event.timestamp
        }
    }

    companion object {
        private const val num = 25
        protected var canvas: Canvas? = null
        private val NS2S = 1.0f / 1000000000.0f
    }
}