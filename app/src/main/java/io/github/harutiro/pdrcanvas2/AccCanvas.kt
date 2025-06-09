package io.github.harutiro.pdrcanvas2

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Point
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

class AccCanvas : AppCompatActivity() {
    var accCanvasView: AccCanvasView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        // 画面縦向き固定
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        setContentView(R.layout.activity_acccanvas)
        size = getDisplaySize(this)
        winx = size!!.x
        winy = size!!.y
        dpi = getResources().getDisplayMetrics().densityDpi.toFloat()
        accCanvasView = findViewById<View?>(R.id.acc_canvas_view) as AccCanvasView?
    }

    companion object {
        protected var size: Point? = null
        @JvmField
        var winx: Int = 0
        @JvmField
        var winy: Int = 0
        @JvmField
        var dpi: Float = 0f

        // 画面サイズ取得
        fun getDisplaySize(activity: Activity): Point {
            val display = activity.getWindowManager().getDefaultDisplay()
            val point = Point()
            display.getSize(point)
            return point
        }
    }
}