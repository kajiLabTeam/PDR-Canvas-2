package io.github.harutiro.pdrcanvas2

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class CanvasView : View {
    private var pathList: Array<Path?>

    //private Path path;
    private val paint: Paint
    private var posx = 0.0f
    private var posy = 0.0f
    private val dpi = getResources().getDisplayMetrics().densityDpi.toFloat()
    private var direction = 0.0f
    private var offsetX = 0.0f
    private var offsetY = 0.0f
    private var translateX = 0.0f
    private var translateY = 0.0f
    private var offsetAngle = 0.0f
    private var rotateAngle = 0.0f
    private var scale = 1.0f
    private var isGesture = false
    private val gesDetect: ScaleGestureDetector
    var transparent: Boolean = true

    constructor(context: Context) : super(context) {
        // 初期化
        // Path関連

        // 配列の作成
        pathList = arrayOfNulls<Path>(50)

        //path = new Path();

        // Paint関連
        paint = Paint()

        // 色の指定
        paint.setColor(Color.BLACK)
        // 描画設定を'線'に設定
        paint.style = Paint.Style.STROKE
        // アンチエイリアスの適応
        paint.isAntiAlias = true
        // 線の太さ
        paint.strokeWidth = MainActivity.size?.x?.div(dpi)?.times(3.0f) ?: 3.0f

        gesDetect = ScaleGestureDetector(context, onScaleGestureListener)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        // 初期化
        // Path関連

        // 配列の作成
        pathList = arrayOfNulls<Path>(50)

        //path = new Path();

        // Paint関連
        paint = Paint()
        // 色の指定
        paint.setColor(Color.BLACK)
        // 描画設定を'線'に設定
        paint.style = Paint.Style.STROKE
        // アンチエイリアスの適応
        paint.isAntiAlias = true
        // 線の太さ
        paint.strokeWidth = MainActivity.size?.x?.div(dpi)?.times(3.0f) ?: 3.0f

        gesDetect = ScaleGestureDetector(context, onScaleGestureListener)
    }

    private fun rshift() {
        System.arraycopy(pathList, 0, pathList, 1, pathList.size - 1)
    }

    private fun shift() {
        System.arraycopy(pathList, 1, pathList, 0, pathList.size - 1)
    }

    private fun add(path: Path?) {
        // null return
        //rshift();
        //pathList[0] = path;

        // null continue

        shift()
        pathList[pathList.size - 1] = path
    }

    private fun clear() {
        for (i in pathList.indices) {
            // null check : NullPointerException
            //pathList[i].reset();
            pathList[i] = null
        }
    }

    private fun setArraySize(array: Array<Path?>, length: Int): Array<Path?> {
        val copy = arrayOfNulls<Path>(length)
        // rshift
        //System.arraycopy(array, 0, copy, 0, copy.length > array.length ? array.length : copy.length);
        // shift
        System.arraycopy(
            array,
            if (copy.size > array.size) 0 else array.size - copy.size,
            copy,
            if (copy.size > array.size) copy.size - array.size else 0,
            if (copy.size > array.size) array.size else copy.size
        )
        return copy
    }

    var pathSize: Int
        get() = pathList.size
        set(size) {
            if (pathList.size == size) {
                return
            }
            pathList = setArraySize(pathList, size)
        }

    private fun loopRotateAngle(degree: Float): Float {
        var degree = degree
        degree %= 360.0f
        if (degree > 180.0f) {
            degree -= 360.0f
        } else if (degree < -180.0f) {
            degree += 360.0f
        }
        return degree
    }

    private fun getAngle(event: MotionEvent): Float {
        return atan2(
            (event.getY(1) - event.getY(0)).toDouble(),
            (event.getX(1) - event.getX(0)).toDouble()
        ).toFloat() * 180.0f / Math.PI.toFloat()
    }

    //======================================================================================
    //--  描画メソッド
    //======================================================================================
    override fun onDraw(canvas: Canvas) {
        // 移動
        canvas.translate(
            MainActivity.size?.x?.div(2)?.minus(posx)?.plus(translateX) ?: 2.0f,
            MainActivity.size?.y?.div(2)?.minus(posy)?.plus(translateY) ?: 2.0f
        )

        // 回転
        canvas.rotate(loopRotateAngle(direction + rotateAngle), posx, posy)

        // 拡大・縮小
        canvas.scale(scale, scale, posx, posy)

        /*** */
        for (i in pathList.indices) {
            // pathList.isEmpty() 初期化無 : NullPointerException
            if (pathList[i] == null || pathList[i]!!.isEmpty()) {
                continue
                //return
            }
            // Pathの描画
            //paint.setColor(Color.HSVToColor(new float[]{ 380 - (200 * (i + 1) / pathList.length), 1.0f, 1.0f}));
            if (this.transparent) {
                paint.setAlpha(5 + (250 * (i + 1) / pathList.size))
            }
            canvas.drawPath(pathList[i]!!, paint)
        }

        //*/
        //canvas.drawPath(path, paint);

        // 再描画
        //invalidate();
    }

    // 進行方向軌跡描画(長さlength)
    // 進行方向軌跡描画(長さ固定)
    @JvmOverloads
    fun drawTrajectory(angle: Float, length: Float = 1.0f) {
        resetMove()
        resetAngle()

        val drawingPath = Path()

        // 始点を設定
        //path.moveTo(posx, posy);
        drawingPath.moveTo(posx, posy)

        // x = 0.0 , y = 1.0 --> 前を基準
        // Rx = cos(angle) * x - sin(angle) * y;
        // Ry = sin(angle) * x + cos(angle) * y;
        // ------------------------------------
        // cos(angle) * 0.0 - sin(angle) * 1.0;
        // --> x : -sin(angle)
        // sin(angle) * 0.0 + cos(angle) * 1.0;
        // --> y : cos(angle)
        //   0° : x =  0 , y =  1 --> 前
        //  90° : x = -1 , y =  0 --> 左
        // -90° : x =  1 , y =  0 --> 右
        // 180° : x =  0 , y = -1 --> 後
        // yはcanvasだと上がマイナス --> *(-1.0)
        // --> y : -cos(angle)
        posx = posx - sin(angle.toDouble()).toFloat() * ((MainActivity.size?.y?.toFloat() ?: 3.0f) / dpi) * 3.0f * length
        posy = posy - cos(angle.toDouble()).toFloat() * ((MainActivity.size?.y?.toFloat() ?: 3.0f) / dpi) * 3.0f * length

        // 移動先の追加
        //path.lineTo(posx, posy);
        drawingPath.lineTo(posx, posy)

        // 配列にPathを追加
        add(drawingPath)
    }

    // 画面回転角度
    fun rotateTrajectory(angle: Float) {
        direction = angle
    }

    // 初期位置
    fun resetPos() {
        posx = 0.0f
        posy = 0.0f
    }

    // 移動初期化
    fun resetMove() {
        offsetX = 0.0f
        offsetY = 0.0f
        translateX = 0.0f
        translateY = 0.0f
    }

    // 回転角度初期化
    fun resetAngle() {
        rotateAngle = 0.0f
    }

    // 拡大縮小
    fun zoomScale(zoom: Float) {
        scale = max(0.20, min((scale + zoom).toDouble(), 10.0)).toFloat()
    }

    // 拡大縮小初期化
    fun resetScale() {
        scale = 1.0f
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.getAction()) {
            MotionEvent.ACTION_DOWN -> {
                offsetX = event.getRawX()
                offsetY = event.getRawY()
            }

            MotionEvent.ACTION_UP -> isGesture = false
            MotionEvent.ACTION_MOVE -> if (event.getPointerCount() == 2) {
                if (!isGesture) {
                    offsetAngle = getAngle(event)
                    isGesture = true
                }
                rotateAngle += getAngle(event) - offsetAngle
                offsetAngle = getAngle(event)
            } else if (!isGesture) {
                translateX += event.getRawX() - offsetX
                translateY += event.getRawY() - offsetY
                offsetX = event.getRawX()
                offsetY = event.getRawY()
            }

            MotionEvent.ACTION_CANCEL -> isGesture = false
            else -> {}
        }

        gesDetect.onTouchEvent(event)

        // 再描画
        invalidate()

        return true
    }

    private val onScaleGestureListener: SimpleOnScaleGestureListener =
        object : SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scale += (detector.getScaleFactor() - 1.0f) * scale * dpi / 6400.0f
                scale = max(0.20, min(scale.toDouble(), 10.0)).toFloat()
                return super.onScale(detector)
            }

            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                isGesture = true
                return super.onScaleBegin(detector)
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                //isGesture = false;
                super.onScaleEnd(detector)
            }
        }

    //======================================================================================
    //--  削除メソッド
    //======================================================================================
    fun allDelete() {
        resetPos()
        resetMove()
        resetAngle()

        clear()
        //path.reset();
    }
}