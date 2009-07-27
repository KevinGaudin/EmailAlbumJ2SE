package com.kg.emailalbum.mobile;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.zip.ZipFile;

import android.app.Activity;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Window;
import android.view.GestureDetector.OnGestureListener;
import android.widget.ImageView;
import android.widget.Toast;

public class ShowPics extends Activity implements OnGestureListener {
	private ImageView mImgView;
	private ArrayList<String> mImageNames;
	private String mAlbumName;
	private int mPosition;
	private GestureDetector mGestureScanner;
	private ZipFile archive;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.content_view);

		mImgView = (ImageView) findViewById(R.id.showpic);
		mGestureScanner = new GestureDetector(this);

		if (getIntent() != null) {
			mImageNames = getIntent().getStringArrayListExtra("PICS");
			mAlbumName = getIntent().getStringExtra("ALBUM");
			mPosition = getIntent().getIntExtra("POSITION", 0);
		}

		if (savedInstanceState != null) {
			mImageNames = savedInstanceState.getStringArrayList("PICS") != null ? savedInstanceState
					.getStringArrayList("PICS")
					: mImageNames;
			mAlbumName = savedInstanceState.getString("ALBUM") != null ? savedInstanceState.getString("ALBUM") : mAlbumName;
			mPosition = savedInstanceState.getInt("POSITION");
		}

		try {
			File albumFile = new File(new URI(mAlbumName.toString().replace(
					" ", "%20")));
			archive = new ZipFile(albumFile);
			showPicture();
		} catch (Exception e) {
			Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
		}
	}

	private void showPicture() {
		try {
			InputStream firstImageIS = archive.getInputStream(archive
					.getEntry(mImageNames.get(mPosition)));
			BitmapDrawable image = new BitmapDrawable(firstImageIS);
			mImgView.setImageDrawable(image);
		} catch (Exception e) {
			Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent me) {
		return mGestureScanner.onTouchEvent(me);
	}

	@Override
	public boolean onDown(MotionEvent arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		int oldPosition = mPosition;
		if (velocityX < 0) {
			mPosition = Math.min(mImageNames.size() - 1, mPosition + 1);
		} else if (velocityX > 0) {
			mPosition = Math.max(0, mPosition - 1);
		}
		
		if(oldPosition != mPosition) {
			showPicture();
			return true;
		} else {
			if(mPosition == 0) {
				Toast.makeText(this, R.string.first_pic, Toast.LENGTH_SHORT).show();
			} else if (mPosition == mImageNames.size() - 1) {
				Toast.makeText(this, R.string.last_pic, Toast.LENGTH_SHORT).show();
			}
		}
		
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt("POSITION", mPosition);
		outState.putStringArrayList("PICS", mImageNames);
		outState.putString("ALBUM", mAlbumName);
		super.onSaveInstanceState(outState);
	}

}
