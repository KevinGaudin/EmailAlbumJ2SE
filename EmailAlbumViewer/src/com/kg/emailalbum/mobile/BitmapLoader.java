package com.kg.emailalbum.mobile;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Display;
import android.view.WindowManager;

public class BitmapLoader {
	public static Bitmap load(Context context, ZipFile archive, ZipEntry entry, Integer width, Integer height) throws IOException {
		Bitmap result = null;
		int destWidth = -1, destHeight = -1;
		InputStream input = ZipUtil.getInputStream(archive, entry);
		if(input == null) return null;
		
		
		if (input != null) {
			BitmapFactory.Options options = new BitmapFactory.Options();
			
			// First, get image size
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(input, null, options);
			int srcWidth = options.outWidth;
			int srcHeight = options.outHeight;
			float imageFactor = (float) srcWidth / (float) srcHeight;
			
			// If no resolution given, use the device screen resolution
			if (width == null && height == null){
				Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay(); 
				destWidth = display.getWidth();
				destHeight = display.getHeight();
			} else if(width == null) {
				// If only one dimension is given, keep source proportions
				destWidth = (int) (height * imageFactor); 
				destHeight = height;
			} else if (height == null) {
				// If only one dimension is given, keep source proportions
				destHeight = (int) (width / imageFactor);
				destWidth = width;
			} else {
				// 2 dimensions given, adjust for best fit with proportions
				// preserved
				
		        float newImageFactor = (float) width / (float) height;
	
		        // Calculates wich dimension should be used to preserve image proportions.
		        if (newImageFactor <= imageFactor) {
		            destHeight = (int) (width / imageFactor);
		            destWidth = width;
		        } else if (newImageFactor > imageFactor) {
		            destWidth = (int) (height * imageFactor);
		            destHeight = height;
		        }
			}
			
			// Calculate the sample size needed to load image with only required memory consuption
			if(srcWidth > destWidth) {
				options.inSampleSize = srcWidth / destWidth;
			}
			
			// Reload the input stream for the second pass
			input.close();
			input = ZipUtil.getInputStream(archive, entry);

			options.inJustDecodeBounds = false;
			options.inPreferredConfig = Bitmap.Config.RGB_565;
			
			Bitmap source = BitmapFactory.decodeStream(input, null, options);
			input.close();
			
			result = Bitmap.createScaledBitmap(source, destWidth,
					destHeight, true);
			source.recycle();
		}
		return result;
	}
}
