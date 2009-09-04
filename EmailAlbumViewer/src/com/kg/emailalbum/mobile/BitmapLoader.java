package com.kg.emailalbum.mobile;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

public class BitmapLoader {
	public static Bitmap load(Context context, ZipFile archive, ZipEntry entry,
			Integer width, Integer height) throws IOException {
		Bitmap result = null;
		int destWidth = -1, destHeight = -1;
		InputStream input = ZipUtil.getInputStream(archive, entry);
		if (input == null)
			return null;

		if (input != null) {
			BitmapFactory.Options options = new BitmapFactory.Options();

			// First, get image size
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(input, null, options);
			int srcWidth = options.outWidth;
			int srcHeight = options.outHeight;
			Log.d(BitmapLoader.class.getSimpleName(), "Source picture has dimension " + srcWidth + " x " + srcHeight);

			float srcImageFactor = (float) srcWidth / (float) srcHeight;

			// If no resolution given, use the device screen resolution
			if (width == null && height == null) {
				Display display = ((WindowManager) context
						.getSystemService(Context.WINDOW_SERVICE))
						.getDefaultDisplay();
				destWidth = display.getWidth();
				destHeight = display.getHeight();
				Log.d(BitmapLoader.class.getSimpleName(), "Display is : " + destWidth + " x " + destHeight);
			} else if (width == null) {
				// If only one dimension is given, keep source proportions
				destWidth = (int) (height * srcImageFactor);
				destHeight = height;
			} else if (height == null) {
				// If only one dimension is given, keep source proportions
				destHeight = (int) (width / srcImageFactor);
				destWidth = width;
			} else {
				destWidth = width;
				destHeight = height;
			}
			
			float requestedImageFactor = (float) destWidth / (float) destHeight;

			// Switch requested orientation to allow best quality without reloading
			// if device orientation changes
			if( (srcImageFactor > 1 && requestedImageFactor < 1)
					|| (srcImageFactor < 1 && requestedImageFactor > 1))
			{
				int oldValue = destWidth;
				destWidth = destHeight;
				destHeight = oldValue;
				requestedImageFactor = 1 / requestedImageFactor;
			}

			// 2 final requested dimensions are now given, adjust for best fit with
			// aspect ratio preserved


			// Calculates which dimension should be used to preserve aspect ratio
			if (requestedImageFactor <= srcImageFactor) {
				destHeight = (int) (destWidth / srcImageFactor);
			} else if (requestedImageFactor > srcImageFactor) {
				destWidth = (int) (destHeight * srcImageFactor);
			}

			// Calculate the sample size needed to load image with minimum required
			// memory consumption.
			// We eventually load a larger bitmap if orientation is different so
			// that if device orientation changes, we don't have to reload a better
			// finer sampled bitmap
			if (srcWidth > destWidth) {
				options.inSampleSize = srcWidth / destWidth;
			}

			// Reload the input stream for the second pass
			input.close();
			input = ZipUtil.getInputStream(archive, entry);

			options.inJustDecodeBounds = false;
			options.inPreferredConfig = Bitmap.Config.RGB_565;

			Bitmap source = BitmapFactory.decodeStream(input, null, options);
			Log.d(BitmapLoader.class.getSimpleName(), "Loaded picture with dimension " + options.outWidth + " x " + options.outHeight);
			input.close();

			result = Bitmap.createScaledBitmap(source, destWidth, destHeight,
					true);
			source.recycle();
			Log.d(BitmapLoader.class.getSimpleName(), "Resized picture to dimension " + destWidth + " x " + destHeight);
		}
		return result;
	}
}
