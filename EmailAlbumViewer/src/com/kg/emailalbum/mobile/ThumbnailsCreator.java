package com.kg.emailalbum.mobile;

import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class ThumbnailsCreator extends Thread {
	private ArrayList<String> mPictures;
	private HashMap<String, String> mThumbnails = new HashMap<String, String>();
	private Handler mHandler;
	private ZipFile mArchive;
	private Context mContext;
	private boolean mClearThumbnails;
	private boolean mContinueCreation = true;

	public ThumbnailsCreator(Context c, ZipFile archive,
			ArrayList<String> pictures, Handler handler, boolean clearThumbnails) {
		mContext = c;
		mArchive = archive;
		mPictures = pictures;
		mHandler = handler;
		mClearThumbnails = clearThumbnails;
	}

	@Override
	public void run() {
		if (mArchive == null) {
			sendError(new FileNotFoundException("Archive not found."));
		} else {
			try {
				ZipEntry entry = null;
				String entryName = null;
				if(mClearThumbnails) {
					clearFiles();
				}
				Iterator<String> iPictures = mPictures.iterator();
				int pos = 0;
				List<String> files = Arrays.asList(mContext.fileList());
				while (mContinueCreation && iPictures.hasNext()) {
					entryName = iPictures.next();
					entry = mArchive.getEntry(entryName);
					String thumbName = entryName.substring(entryName
							.lastIndexOf('/') + 1);
					if(!files.contains(thumbName)) {
					
							BitmapDrawable source = new BitmapDrawable(mArchive
									.getInputStream(entry));
							float ratio = (float) (source.getBitmap().getWidth())
									/ (float) (source.getBitmap().getHeight());
							int dstW, dstH;
							dstW = 80;
							dstH = (int) (dstW / ratio);
							Bitmap thumb = Bitmap.createScaledBitmap(
									source.getBitmap(), dstW, dstH, true);
		
							OutputStream thumbOS = mContext.openFileOutput(thumbName,
									Context.MODE_PRIVATE);
		
							thumb.compress(CompressFormat.JPEG, 75, thumbOS);
							thumbOS.close();
					}
					
					mThumbnails.put(entryName, mContext.getFileStreamPath(
							thumbName).getAbsolutePath());
					sendThumbnail(pos);
					pos++;
				}
			} catch (Exception e) {
				sendError(e);
			}
		}
	}

	private void clearFiles() {
		String[] files = mContext.fileList();
		for (String fileName : files) {
			mContext.deleteFile(fileName);
		}
	}

	private void sendThumbnail(int pos) {
		Message msg = new Message();
		Bundle data = new Bundle();
		msg.arg1 = 0;
		data.putInt(EmailAlbumViewer.KEY_THMBCREAT_ENTRY_POSITION, pos);
		data.putString(EmailAlbumViewer.KEY_THMBCREAT_THUMB_NAME, mThumbnails
				.get(mPictures.get(pos)));
		msg.setData(data);
		mHandler.sendMessage(msg);
	}

	private void sendError(Exception e) {
		Message msg = new Message();
		Bundle data = new Bundle();
		msg.arg1 = -1;
		data.putString("EXCEPTION", e.getLocalizedMessage());
		msg.setData(data);
		mHandler.sendMessage(msg);
	}
	
	public void stopCreation(){
		mContinueCreation = false;
	}

}
