package io.github.harutiro.pdrcanvas2;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;

public class AccCanvas extends AppCompatActivity {

    protected static Point size;
    protected static int winx;
    protected static int winy;
    protected static float dpi;
    AccCanvasView accCanvasView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // 画面縦向き固定
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_acccanvas);
        size = getDisplaySize(this);
        winx = size.x;
        winy = size.y;
        dpi = getResources().getDisplayMetrics().densityDpi;
        accCanvasView = (AccCanvasView) findViewById(R.id.acc_canvas_view);
    }

    // 画面サイズ取得
    public static Point getDisplaySize(Activity activity){
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        return point;
    }

}