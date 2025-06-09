package io.github.harutiro.pdrcanvas2;

import static io.github.harutiro.pdrcanvas2.MainActivity.size;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

public class CanvasView extends View {

    private Path[] pathList;
    //private Path path;
    private final Paint paint;
    private float posx = 0.0f;
    private float posy = 0.0f;
    private float dpi = getResources().getDisplayMetrics().densityDpi;
    private float direction = 0.0f;
    private float offsetX = 0.0f;
    private float offsetY = 0.0f;
    private float translateX = 0.0f;
    private float translateY = 0.0f;
    private float offsetAngle = 0.0f;
    private float rotateAngle = 0.0f;
    private float scale = 1.0f;
    private boolean isGesture = false;
    private ScaleGestureDetector gesDetect;
    private boolean isTransparent = true;

    public CanvasView(Context context) {
        super(context);

        // 初期化
        // Path関連

        // 配列の作成
        pathList = new Path[50];

        //path = new Path();

        // Paint関連
        paint = new Paint();

        // 色の指定
        paint.setColor(Color.BLACK);
        // 描画設定を'線'に設定
        paint.setStyle(Paint.Style.STROKE);
        // アンチエイリアスの適応
        paint.setAntiAlias(true);
        // 線の太さ
        paint.setStrokeWidth(size.x / dpi * 3.0f);

        gesDetect = new ScaleGestureDetector(context, onScaleGestureListener);
    }

    public CanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // 初期化
        // Path関連

        // 配列の作成
        pathList = new Path[50];

        //path = new Path();

        // Paint関連
        paint = new Paint();
        // 色の指定
        paint.setColor(Color.BLACK);
        // 描画設定を'線'に設定
        paint.setStyle(Paint.Style.STROKE);
        // アンチエイリアスの適応
        paint.setAntiAlias(true);
        // 線の太さ
        paint.setStrokeWidth(size.x / dpi * 3.0f);

        gesDetect = new ScaleGestureDetector(context, onScaleGestureListener);
    }

    private void rshift() {
        System.arraycopy(pathList, 0, pathList, 1, pathList.length - 1);
    }

    private void shift() {
        System.arraycopy(pathList, 1, pathList, 0, pathList.length - 1);
    }

    private void add(Path path) {
        // null return
        //rshift();
        //pathList[0] = path;

        // null continue
        shift();
        pathList[pathList.length - 1] = path;
    }

    private void clear() {
        for (int i = 0; i < pathList.length; i++) {
            // null check : NullPointerException
            //pathList[i].reset();
            pathList[i] = null;
        }
    }

    private Path[] setArraySize(Path[] array, int length) {
        Path[] copy = new Path[length];
        // rshift
        //System.arraycopy(array, 0, copy, 0, copy.length > array.length ? array.length : copy.length);
        // shift
        System.arraycopy(array, copy.length > array.length ? 0 : array.length-copy.length, copy, copy.length > array.length ? copy.length-array.length : 0, copy.length > array.length ? array.length : copy.length);
        return copy;
    }

    public void setPathSize(int size) {
        if (pathList.length == size) {
            return;
        }
        pathList = setArraySize(pathList, size);
    }

    public int getPathSize() {
        return pathList.length;
    }

    public void setTransparent(boolean isTransparent) {
        this.isTransparent = isTransparent;
    }

    public boolean getTransparent() {
        return isTransparent;
    }

    private float loopRotateAngle(float degree) {
        degree %= 360.0f;
        if (degree > 180.0f) {
            degree -= 360.0f;
        }
        else if (degree < -180.0f) {
            degree += 360.0f;
        }
        return degree;
    }

    private float getAngle(MotionEvent event) {
        return (float) Math.atan2(event.getY(1) - event.getY(0),
                event.getX(1) - event.getX(0)) * 180.0f / (float) Math.PI;
    }

    //======================================================================================
    //--  描画メソッド
    //======================================================================================
    @Override
    protected void onDraw(Canvas canvas) {
        // 移動
        canvas.translate(size.x/2 - posx + translateX, size.y/2 - posy + translateY);

        // 回転
        canvas.rotate(loopRotateAngle(direction + rotateAngle), posx, posy);

        // 拡大・縮小
        canvas.scale(scale, scale, posx, posy);
        
        ///*
        for (int i = 0; i < pathList.length; i++) {
            // pathList.isEmpty() 初期化無 : NullPointerException
            if (pathList[i] == null || pathList[i].isEmpty()) {
                continue;
                //return
            }
            // Pathの描画
            //paint.setColor(Color.HSVToColor(new float[]{ 380 - (200 * (i + 1) / pathList.length), 1.0f, 1.0f}));
            if (isTransparent) {
                paint.setAlpha(5 + (250 * (i + 1) / pathList.length));
            }
            canvas.drawPath(pathList[i], paint);
        }

        //*/
        //canvas.drawPath(path, paint);

        // 再描画
        //invalidate();
    }

    // 進行方向軌跡描画(長さ固定)
    public void drawTrajectory(float angle) {
        drawTrajectory(angle, 1.0f);
    }

    // 進行方向軌跡描画(長さlength)
    public void drawTrajectory(float angle, float length) {
        resetMove();
        resetAngle();

        Path drawingPath = new Path();

        // 始点を設定
        //path.moveTo(posx, posy);
        drawingPath.moveTo(posx, posy);

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

        posx = posx - (float) Math.sin(angle) * size.y / dpi * 3.0f * length;
        posy = posy - (float) Math.cos(angle) * size.y / dpi * 3.0f * length;

        // 移動先の追加
        //path.lineTo(posx, posy);
        drawingPath.lineTo(posx, posy);

        // 配列にPathを追加
        add(drawingPath);
    }

    // 画面回転角度
    public void rotateTrajectory(float angle) {
        direction = angle;
    }

    // 初期位置
    public void resetPos() {
        posx = 0.0f;
        posy = 0.0f;
    }

    // 移動初期化
    public void resetMove() {
        offsetX = 0.0f;
        offsetY = 0.0f;
        translateX = 0.0f;
        translateY = 0.0f;
    }

    // 回転角度初期化
    public void resetAngle() {
        rotateAngle = 0.0f;
    }

    // 拡大縮小
    public void zoomScale(float zoom) {
        scale = Math.max(0.20f, Math.min(scale + zoom, 10.0f));
    }

    // 拡大縮小初期化
    public void resetScale() {
        scale = 1.0f;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {

            //- 画面をタッチしたとき
            case MotionEvent.ACTION_DOWN:
                offsetX = event.getRawX();
                offsetY = event.getRawY();
                break;

            //- 画面から指を離したとき
            case MotionEvent.ACTION_UP:
                isGesture = false;
                break;

            //- タッチしながら指をスライドさせたとき
            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() == 2) {
                    if (!isGesture) {
                        offsetAngle = getAngle(event);
                        isGesture = true;
                    }
                    rotateAngle += getAngle(event) - offsetAngle;
                    offsetAngle = getAngle(event);
                }
                else if (!isGesture) {
                    translateX += event.getRawX() - offsetX;
                    translateY += event.getRawY() - offsetY;
                    offsetX = event.getRawX();
                    offsetY = event.getRawY();
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                isGesture = false;
                break;

            default:
                break;

        }

        gesDetect.onTouchEvent(event);

        // 再描画
        invalidate();

        return true;
    }

    private final ScaleGestureDetector.SimpleOnScaleGestureListener onScaleGestureListener = new ScaleGestureDetector.SimpleOnScaleGestureListener(){
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scale += (detector.getScaleFactor() - 1.0f) * scale * dpi / 6400.0f;
            scale = Math.max(0.20f, Math.min(scale, 10.0f));
            return super.onScale(detector);
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            isGesture = true;
            return super.onScaleBegin(detector);
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            //isGesture = false;
            super.onScaleEnd(detector);
        }
    };

    //======================================================================================
    //--  削除メソッド
    //======================================================================================
    public void allDelete() {
        resetPos();
        resetMove();
        resetAngle();

        clear();
        //path.reset();
    }

}