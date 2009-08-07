package com.kg.emailalbum.mobile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.zip.ZipFile;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
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
	private final static int WALLPAPER_ID = 1;
	private static final int SAVE_ID = 2;
	private static final int SEND_ID = 3;
	private static final int ACTIVITY_PICK_DIRECTORY_TO_SAVE = 0;
	private BitmapDrawable image;

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
			mAlbumName = savedInstanceState.getString("ALBUM") != null ? savedInstanceState
					.getString("ALBUM")
					: mAlbumName;
			mPosition = savedInstanceState.getInt("POSITION");
		}

		try {
			File albumFile = new File(new URI(mAlbumName.toString().replace(
					" ", "%20")));
			archive = new ZipFile(albumFile);
			showPicture();
		} catch (Exception e) {
			Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT)
					.show();
		}
	}

	private void showPicture() {
		try {
			InputStream imageIS = archive.getInputStream(archive
					.getEntry(mImageNames.get(mPosition)));
			image = new BitmapDrawable(imageIS);
			mImgView.setImageDrawable(image);
		} catch (Exception e) {
			Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT)
					.show();
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

		if (oldPosition != mPosition) {
			showPicture();
			return true;
		} else {
			if (mPosition == 0) {
				Toast.makeText(this, R.string.first_pic, Toast.LENGTH_SHORT)
						.show();
			} else if (mPosition == mImageNames.size() - 1) {
				Toast.makeText(this, R.string.last_pic, Toast.LENGTH_SHORT)
						.show();
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

	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0, WALLPAPER_ID, 0, R.string.menu_wallpaper);
		menu.add(0, SAVE_ID, 0, R.string.menu_save);
		menu.add(0, SEND_ID, 0, R.string.menu_share);
		return result;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		Intent intent = null;
		switch (item.getItemId()) {
		case WALLPAPER_ID:
			new Thread() {

				/*
				 * (non-Javadoc)
				 * 
				 * @see java.lang.Thread#run()
				 */
				@Override
				public void run() {
					try {
						InputStream imageIS = archive.getInputStream(archive
								.getEntry(mImageNames.get(mPosition)));
						BitmapDrawable source = new BitmapDrawable(imageIS);
						int srcW = source.getBitmap().getWidth();
						int srcH = source.getBitmap().getHeight();
						float srcRatio = (float) srcW / (float) srcH;
						int dstW = getWallpaperDesiredMinimumWidth();
						int dstH = (int) (dstW / srcRatio);
						Bitmap wallpaper = Bitmap.createScaledBitmap(source
								.getBitmap(), dstW, dstH, true);
						setWallpaper(wallpaper);
					} catch (IOException e) {
						Log.e(this.getClass().getName(), "Wallpaper error", e);
					}
				}

			}.start();
			Toast.makeText(this, getText(R.string.wallpaper_in_progress), Toast.LENGTH_LONG).show();
			return true;
		case SAVE_ID:
			intent = new Intent("org.openintents.action.PICK_DIRECTORY");
			intent.setData(Uri.parse("file:///sdcard"));
			intent.putExtra("org.openintents.extra.TITLE",
					getText(R.string.select_file));
			intent.putExtra("org.openintents.extra.BUTTON_TEXT",
					getText(R.string.btn_select_file));

			startActivityForResult(intent, ACTIVITY_PICK_DIRECTORY_TO_SAVE);
			return true;
		case SEND_ID:
			try {
				File tmpFile = savePicture("file:///sdcard/tmp/");
				intent = new Intent(Intent.ACTION_SEND);
				intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(tmpFile.toURI()
						.toString()));
				intent.setType("image/jpeg");
				
				startActivity(Intent.createChooser(intent,
						getText(R.string.chooser_share)));

			} catch (IOException e) {
				Toast.makeText(
						this,
						getText(R.string.error_other_intents) + " : "
								+ e.getLocalizedMessage(), Toast.LENGTH_SHORT)
						.show();
			}

			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onActivityResult(int, int,
	 * android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case ACTIVITY_PICK_DIRECTORY_TO_SAVE:
			if (resultCode == RESULT_OK && data != null) {
				// obtain the filename
				String dirname = data.getDataString();
				try {
					savePicture(dirname);
				} catch (IOException e) {
					e.printStackTrace();
					Toast.makeText(this, e.getLocalizedMessage(),
							Toast.LENGTH_SHORT).show();
				}

			}
			break;
		}
	}

	private File savePicture(String dirname) throws IOException {
		File destFile = null;
		if (dirname != null) {
			File destDir = new File(Uri.parse(dirname).getEncodedPath());
			if (!destDir.exists()) {
				destDir.mkdirs();
			}
			destFile = new File(destDir, mImageNames.get(mPosition).substring(
					mImageNames.get(mPosition).lastIndexOf('/')).toLowerCase());

			OutputStream destFileOS = new FileOutputStream(destFile);
			image.getBitmap().compress(CompressFormat.JPEG, 75, destFileOS);
			destFileOS.close();
		}
		return destFile;
	}

}
