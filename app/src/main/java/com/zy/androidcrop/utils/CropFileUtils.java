package com.zy.androidcrop.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.os.Environment.DIRECTORY_PICTURES;

/**
 * @Author: fzy
 * @Date: 2022/2/17
 * @Description:
 */
public class CropFileUtils {

    public static File getAppRootDirPath(Context context) {
        return context.getExternalFilesDir(null).getAbsoluteFile();
    }
    public static Uri uri;
    public static File createImageFile(Context context, boolean isCrop) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = "";
            if (isCrop) {
                fileName = "IMG_"+timeStamp+"_CROP.jpg";
            } else {
                fileName = "IMG_"+timeStamp+".jpg";
            }
            File rootFile = new File(getAppRootDirPath(context) + File.separator + "capture");
            if (!rootFile.exists()) {
                rootFile.mkdirs();
            }
            File imgFile;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                imgFile = new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_PICTURES) + File.separator + fileName);
                // 通过 MediaStore API 插入file 为了拿到系统裁剪要保存到的uri（因为App没有权限不能访问公共存储空间，需要通过 MediaStore API来操作）
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, imgFile.getAbsolutePath());
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            }else {
                imgFile = new File(rootFile.getAbsolutePath() + File.separator + fileName);
            }
            return imgFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static File getCropFile(Context context, Uri uri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(uri, proj, null, null, null);

        if (cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            String path = cursor.getString(columnIndex);
            cursor.close();
            return new File(path);
        }
        return null;
    }

}
