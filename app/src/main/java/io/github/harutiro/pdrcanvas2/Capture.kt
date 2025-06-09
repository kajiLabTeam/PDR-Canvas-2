package io.github.harutiro.pdrcanvas2

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import androidx.core.content.FileProvider

class Capture internal constructor(private val context: Context, private val view: View) {
    private var datName: String? = null

    fun captureTrajectory() {
        val file: File = File(SDCARD_FOLDER)
        if (!file.exists()) {
            if (!file.mkdir()) {
                return
            }
        }
        @SuppressLint("SimpleDateFormat") val sf = SimpleDateFormat("yyyyMMdd_HHmmss")
        datName = sf.format(Date()) + ".png"
        val path: File = File(SDCARD_FOLDER + datName)
        saveCapture(view.getRootView(), path)
    }

    // スクリーンショットを取得して保存する
    private fun saveCapture(view: View, file: File?) {
        val capture = getViewCapture(view)
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(file)
            // 画像のフォーマットと画質と出力先を指定して保存
            if (capture != null) {
                capture.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            fos.flush()
        } catch (ignored: Exception) {
        } finally {
            try {
                if (fos != null) {
                    registAndroidDB(SDCARD_FOLDER + datName)
                    fos.close()
                }
            } catch (ignored: Exception) {
            }
        }
    }

    // スクリーンショットを取得する
    private fun getViewCapture(view: View): Bitmap? {
        view.setDrawingCacheEnabled(true)
        val cache = view.getDrawingCache()
        if (cache == null) {
            return null
        }
        val screen_shot = Bitmap.createBitmap(cache)
        view.setDrawingCacheEnabled(false)
        return screen_shot
    }

    // アンドロイドのデータベースへ画像のパスを登録
    private fun registAndroidDB(path: String?) {
        // アンドロイドのデータベースへ登録
        // (登録しないとギャラリーなどにすぐに反映されないため)
        val values = ContentValues()
        val contentResolver = context.getContentResolver()
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        values.put("_data", path)
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    // 通知
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun sendPushNotification(stepcount: Int) {
        val intent = Intent(Intent.ACTION_VIEW)
        val file = File(SDCARD_FOLDER + datName)
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        intent.setDataAndType(uri, "image/png")
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        intent.setFlags(
            (Intent.FLAG_ACTIVITY_CLEAR_TOP
                    or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        )

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context.getApplicationContext())
        builder.setSmallIcon(R.drawable.ic_capture_notification)
        builder.setContentTitle("PDR Canvas")
        builder.setContentText(SDCARD_FOLDER + datName)
        builder.setSubText("Open Viewer")
        builder.setContentInfo(stepcount.toString() + " step")
        builder.setTicker("Capture")

        builder.setContentIntent(contentIntent)
        builder.setAutoCancel(true)

        val notification = setNotificationBigPictureStyle(builder)

        val notificationManagerCompat =
            NotificationManagerCompat.from(context.getApplicationContext())
        notificationManagerCompat.notify(1, notification)
    }

    // 大きい画像を表示
    private fun setNotificationBigPictureStyle(builder: NotificationCompat.Builder): Notification {
        val bigPictureStyle = NotificationCompat.BigPictureStyle(builder)
        bigPictureStyle.bigPicture(BitmapFactory.decodeFile(SDCARD_FOLDER + datName))
        return builder.build()
    }

    companion object {
        private val SDCARD_FOLDER =
            Environment.getExternalStorageDirectory().getPath() + "/PDRCanvas/"
    }
}