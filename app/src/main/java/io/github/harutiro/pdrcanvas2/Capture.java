package io.github.harutiro.pdrcanvas2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;

import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Capture {

    private Context context;
    private static final String SDCARD_FOLDER = Environment.getExternalStorageDirectory().getPath() + "/PDRCanvas/";
    private String datName;
    private View view;

    Capture(Context context, View view) {
        this.context = context;
        this.view = view;
    }

    public void captureTrajectory() {
        File file = new File(SDCARD_FOLDER);
        if(!file.exists()) {
            if (!file.mkdir()) {
                return;
            }
        }
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        datName = sf.format(new Date()) + ".png";
        File path = new File(SDCARD_FOLDER + datName);
        saveCapture(view.getRootView(), path);
    }

    // スクリーンショットを取得して保存する
    private void saveCapture(View view, File file) {
        Bitmap capture = getViewCapture(view);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            // 画像のフォーマットと画質と出力先を指定して保存
            if (capture != null) {
                capture.compress(Bitmap.CompressFormat.PNG, 100, fos);
            }
            fos.flush();
        } catch (Exception ignored) {
        } finally {
            try {
                if (fos != null) {
                    registAndroidDB(SDCARD_FOLDER + datName);
                    fos.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    // スクリーンショットを取得する
    private Bitmap getViewCapture(View view) {
        view.setDrawingCacheEnabled(true);
        Bitmap cache = view.getDrawingCache();
        if(cache == null) {
            return null;
        }
        Bitmap screen_shot = Bitmap.createBitmap(cache);
        view.setDrawingCacheEnabled(false);
        return screen_shot;
    }

    // アンドロイドのデータベースへ画像のパスを登録
    private void registAndroidDB(String path) {
        // アンドロイドのデータベースへ登録
        // (登録しないとギャラリーなどにすぐに反映されないため)
        ContentValues values = new ContentValues();
        ContentResolver contentResolver = context.getContentResolver();
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put("_data", path);
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    // 通知
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    public void sendPushNotification(int stepcount) {

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new File(SDCARD_FOLDER + datName)), "image/png");
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(SDCARD_FOLDER + datName)));

        intent.setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        );

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context.getApplicationContext());
        builder.setSmallIcon(R.drawable.ic_capture_notification);
        builder.setContentTitle("PDR Canvas");
        builder.setContentText(SDCARD_FOLDER + datName);
        builder.setSubText("Open Viewer");
        builder.setContentInfo(stepcount + " step");
        builder.setTicker("Capture");

        builder.setContentIntent(contentIntent);
        builder.setAutoCancel(true);

        Notification notification = setNotificationBigPictureStyle(builder);

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context.getApplicationContext());
        notificationManagerCompat.notify(1, notification);
    }

    // 大きい画像を表示
    private Notification setNotificationBigPictureStyle(NotificationCompat.Builder builder) {
        NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle(builder);
        bigPictureStyle.bigPicture(BitmapFactory.decodeFile(SDCARD_FOLDER + datName));
        return builder.build();
    }

}