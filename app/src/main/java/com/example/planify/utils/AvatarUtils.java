package com.example.planify.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class AvatarUtils {

    public static String saveAvatarToInternalStorage(Context context, Uri sourceUri, int userId) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
            if (inputStream == null) return null;

            // Decode with scaling to avoid OOM
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            int targetSize = 512; // Max dimension
            int scale = 1;
            if (options.outHeight > targetSize || options.outWidth > targetSize) {
                scale = (int) Math.pow(2, (int) Math.round(Math.log(targetSize / 
                  (double) Math.max(options.outHeight, options.outWidth)) / Math.log(0.5)));
            }

            BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inSampleSize = scale;

            inputStream = context.getContentResolver().openInputStream(sourceUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions);
            inputStream.close();
            
            if(bitmap == null) return null;

            // Crop to square
            int dimension = Math.min(bitmap.getWidth(), bitmap.getHeight());
            int x = (bitmap.getWidth() - dimension) / 2;
            int y = (bitmap.getHeight() - dimension) / 2;
            Bitmap squareBitmap = Bitmap.createBitmap(bitmap, x, y, dimension, dimension);
            
            if (squareBitmap != bitmap) {
                bitmap.recycle();
            }

            File dir = new File(context.getFilesDir(), "avatars");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File avatarFile = new File(dir, "avatar_" + userId + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(avatarFile);
            
            squareBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream);

            outputStream.flush();
            outputStream.close();
            squareBitmap.recycle();

            return avatarFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e("AvatarUtils", "Failed to save avatar", e);
            return null;
        }
    }

    public static Bitmap loadAvatar(String path) {
        if (path == null || path.isEmpty()) return null;
        File imgFile = new File(path);
        if (imgFile.exists()) {
            return BitmapFactory.decodeFile(imgFile.getAbsolutePath());
        }
        return null;
    }
}
