package com.kg.emailalbum.mobile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.zip.ZipFile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.view.GestureDetector.OnGestureListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class ShowPics extends Activity implements OnGestureListener {
	private final static int SET_AS_ID = 1;
	private static final int SAVE_ID = 2;
	private static final int SEND_ID = 3;
	private static final int OPEN_WITH_ID = 4;
	private static final int EDIT_ID = 5;
	private static final int ABOUT_ID = 6;

	private static final int BACKWARD = -1;
	private static final int FORWARD = 1;

	private static final int ACTIVITY_PICK_DIRECTORY_TO_SAVE = 0;

	private static Uri sStorageURI = Images.Media.EXTERNAL_CONTENT_URI;

	private ImageView[] mImgViews = new ImageView[3];
	private ViewFlipper mFlipper;
	private ArrayList<String> mImageNames;
	private String mAlbumName;
	private int mPosition;
	private int mOldPosition;
	private int mLatestMove = FORWARD;
	private int prevPic, curPic, nextPic;
	private GestureDetector mGestureScanner;
	private ZipFile archive;
	private Animation mAnimPushLeftIn;
	private Animation mAnimPushLeftOut;
	private Animation mAnimPushRightOut;
	private Animation mAnimPushRightIn;
	private boolean mIsFlipping = false;
	// Not Enough Memory Mode
	private boolean mNEMMode = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.content_view);

		curPic = 0;
		mImgViews[curPic] = (ImageView) findViewById(R.id.pic0);
		nextPic = 1;
		mImgViews[nextPic] = (ImageView) findViewById(R.id.pic1);
		prevPic = 2;
		mImgViews[prevPic] = (ImageView) findViewById(R.id.pic2);

		// Preload all animations and set listener on only one of each in/out
		// pair as they are
		// run simultaneously
		mAnimPushLeftIn = AnimationUtils.loadAnimation(getApplicationContext(),
				R.anim.push_left_in);
		mAnimPushLeftIn.setAnimationListener(animListener);
		mAnimPushLeftOut = AnimationUtils.loadAnimation(
				getApplicationContext(), R.anim.push_left_out);
		mAnimPushRightIn = AnimationUtils.loadAnimation(
				getApplicationContext(), R.anim.push_right_in);
		mAnimPushRightIn.setAnimationListener(animListener);
		mAnimPushRightOut = AnimationUtils.loadAnimation(
				getApplicationContext(), R.anim.push_right_out);

		mFlipper = (ViewFlipper) findViewById(R.id.flipper);
		mFlipper.setInAnimation(mAnimPushLeftIn);
		mFlipper.setOutAnimation(mAnimPushLeftOut);
		mFlipper.setPersistentDrawingCache(ViewGroup.PERSISTENT_NO_CACHE);
		mGestureScanner = new GestureDetector(this);

		mOldPosition = -1;

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
			Log.e(this.getClass().getName(), "onCreate() exception", e);
			Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT)
					.show();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (isFinishing()) {
			for (ImageView view : mImgViews) {
				Drawable toRecycle = view.getDrawable();
				if (toRecycle != null) {
					Bitmap bmpToRecycle = ((BitmapDrawable) toRecycle)
							.getBitmap();
					if (bmpToRecycle != null) {
						bmpToRecycle.recycle();
					}
				}
			}
		}
	}

	private void resetViews() {
		try {
			if (mOldPosition < mPosition) {
				// Gone forward
				prevPic = (prevPic + 1) % 3;
				curPic = (curPic + 1) % 3;
				nextPic = (nextPic + 1) % 3;
				if (mPosition + 1 < mImageNames.size()) {
					if (mImgViews[nextPic].getDrawable() != null) {
						((BitmapDrawable) mImgViews[nextPic].getDrawable())
								.getBitmap().recycle();
					}

					mImgViews[nextPic]
							.setImageBitmap(loadPicture(mPosition + 1));
				}
			} else if (mOldPosition > mPosition) {
				// Gone backward
				prevPic = (prevPic == 0) ? 2 : prevPic - 1;
				curPic = (curPic == 0) ? 2 : curPic - 1;
				nextPic = (nextPic == 0) ? 2 : nextPic - 1;
				if (mPosition - 1 >= 0) {
					if (mImgViews[prevPic].getDrawable() != null) {
						((BitmapDrawable) mImgViews[prevPic].getDrawable())
								.getBitmap().recycle();
					}

					mImgViews[prevPic]
							.setImageBitmap(loadPicture(mPosition - 1));
				}
			}
		} catch (Exception e) {
			Log.e(this.getClass().getName(), "ResetViews() exception", e);
			Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT)
					.show();
		} catch (OutOfMemoryError e) {
			Log.e(this.getClass().getName(), "showPicture() error", e);
			Toast.makeText(this, R.string.error_out_of_mem, Toast.LENGTH_SHORT)
					.show();
		}
	}

	private void showPicture() {
		try {
			if (mOldPosition == -1 || mNEMMode) {
				// First load, initialize all ImageViews from archive
				Object data = getLastNonConfigurationInstance();
				if (data != null) {
					Bitmap[] bitmaps = (Bitmap[]) data;
					mImgViews[curPic].setImageBitmap(bitmaps[0]);
					mImgViews[nextPic].setImageBitmap(bitmaps[1]);
					mImgViews[prevPic].setImageBitmap(bitmaps[2]);
				} else {
					// Load first picture
					if (mImgViews[curPic].getDrawable() != null) {
						mImgViews[curPic].getDrawable().setCallback(null);
						Bitmap toRecycle = ((BitmapDrawable) mImgViews[curPic]
								.getDrawable()).getBitmap();
						if (toRecycle != null) {
							toRecycle.recycle();
						}
					}
					mImgViews[curPic].setImageBitmap(loadPicture(mPosition));

					// If memory is not a problem, preload pictures
					if (!mNEMMode) {
						// Load previous picture
						if (mPosition > 0) {
							mImgViews[prevPic]
									.setImageBitmap(loadPicture(mPosition - 1));
						}

						// Load next picture
						if (mPosition < mImageNames.size() - 1) {
							mImgViews[nextPic]
									.setImageBitmap(loadPicture(mPosition + 1));
						}
					}
				}
			} else if (mOldPosition < mPosition) {
				// Going forward
				applyTransition(FORWARD);
			} else if (mOldPosition > mPosition) {
				// Going backward
				applyTransition(BACKWARD);
			}
		} catch (Exception e) {
			Log.e(this.getClass().getName(), "showPicture() exception", e);
			Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT)
					.show();
		} catch (OutOfMemoryError e) {
			Log.e(this.getClass().getName(), "showPicture() error", e);
			Toast.makeText(this, R.string.error_out_of_mem, Toast.LENGTH_SHORT)
					.show();
			if (!mNEMMode
					&& ((BitmapDrawable) mImgViews[curPic].getDrawable())
							.getBitmap() != null) {
				mNEMMode = true;
				Toast
						.makeText(this, R.string.enter_nemmode,
								Toast.LENGTH_SHORT).show();
			} else {
				finish();
			}
		}
	}

	private Bitmap loadPicture(int position) throws IOException {
		Log.d(this.getClass().getName(), "Load image "
				+ mImageNames.get(mPosition));
		Bitmap result = BitmapLoader.load(getApplicationContext(), archive,
				archive.getEntry(mImageNames.get(position)), null, null);
		return result;
	}

	private void applyTransition(int direction) {

		switch (direction) {
		case FORWARD:
			if (mLatestMove != FORWARD) {
				mFlipper.setInAnimation(mAnimPushLeftIn);
				mFlipper.setOutAnimation(mAnimPushLeftOut);
			}

			mFlipper.showNext();
			mLatestMove = FORWARD;

			break;
		case BACKWARD:
			if (mLatestMove != BACKWARD) {
				mFlipper.setInAnimation(mAnimPushRightIn);
				mFlipper.setOutAnimation(mAnimPushRightOut);
			}
			mFlipper.showPrevious();
			mLatestMove = BACKWARD;
			break;
		}
	}

	private void goBack() {
		if (!mIsFlipping) {
			mIsFlipping = true;
			mOldPosition = mPosition;
			mPosition = Math.max(0, mPosition - 1);
			if (mOldPosition != mPosition) {
				showPicture();
			} else {
				toastFirstOrLast();
				mIsFlipping = false;
			}
		}
	}

	private void goForward() {
		if (!mIsFlipping) {
			mIsFlipping = true;
			mOldPosition = mPosition;
			mPosition = Math.min(mImageNames.size() - 1, mPosition + 1);
			if (mOldPosition != mPosition) {
				showPicture();
			} else {
				toastFirstOrLast();
				mIsFlipping = false;
			}
		}
	}

	private void toastFirstOrLast() {
		if (mPosition == 0) {
			Toast.makeText(this, R.string.first_pic, Toast.LENGTH_SHORT).show();
		} else if (mPosition == mImageNames.size() - 1) {
			Toast.makeText(this, R.string.last_pic, Toast.LENGTH_SHORT).show();
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
		if (velocityX < 0) {
			goForward();
			return true;
		} else if (velocityX > 0) {
			goBack();
			return true;
		}

		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		openOptionsMenu();
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
		openOptionsMenu();
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_LEFT:
		case KeyEvent.KEYCODE_DPAD_UP:
			goBack();
			return true;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
		case KeyEvent.KEYCODE_DPAD_DOWN:
			goForward();
			return true;
		case KeyEvent.KEYCODE_DPAD_CENTER:
			openOptionsMenu();
		}
		return super.onKeyDown(keyCode, event);
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onRetainNonConfigurationInstance()
	 */
	@Override
	public Object onRetainNonConfigurationInstance() {
		Bitmap[] bitmaps = new Bitmap[3];
		Drawable draw = mImgViews[curPic].getDrawable();
		if (draw != null)
			bitmaps[0] = ((BitmapDrawable) draw).getBitmap();
		draw = mImgViews[nextPic].getDrawable();
		if (draw != null)
			bitmaps[1] = ((BitmapDrawable) draw).getBitmap();
		draw = mImgViews[prevPic].getDrawable();
		if (draw != null)
			bitmaps[2] = ((BitmapDrawable) draw).getBitmap();
		return bitmaps;
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		boolean result = super.onCreateOptionsMenu(menu);
		MenuItem item = menu.add(0, SET_AS_ID, 0, R.string.menu_set_as);
		item.setIcon(android.R.drawable.ic_menu_set_as);
		item = menu.add(0, SEND_ID, 0, R.string.menu_share);
		item.setIcon(android.R.drawable.ic_menu_share);
		item = menu.add(0, SAVE_ID, 0, R.string.menu_save);
		item.setIcon(android.R.drawable.ic_menu_save);
		item = menu.add(0, OPEN_WITH_ID, 0, R.string.menu_open_with);
		item.setIcon(android.R.drawable.ic_menu_view);
		Intent intent = new Intent(Intent.ACTION_EDIT);
		intent.setType("image/jpeg");
		if(getPackageManager().resolveActivity(intent, 0) != null) {
			item = menu.add(0, EDIT_ID, 0, R.string.menu_edit);
			item.setIcon(android.R.drawable.ic_menu_edit);
		}
		item = menu.add(0, ABOUT_ID, 0, R.string.menu_about);
		item.setIcon(android.R.drawable.ic_menu_help);
		return result;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		Intent intent = null;
		switch (item.getItemId()) {
		case SAVE_ID:
			intent = new Intent("org.openintents.action.PICK_DIRECTORY");
			intent.setData(Uri.parse("file:///sdcard"));
			intent.putExtra("org.openintents.extra.TITLE",
					getText(R.string.select_directory));
			intent.putExtra("org.openintents.extra.BUTTON_TEXT",
					getText(R.string.btn_select_directory));
			try {
				startActivityForResult(intent, ACTIVITY_PICK_DIRECTORY_TO_SAVE);
			} catch (ActivityNotFoundException e) {
				alertOIFileManagerIsMissing();
			}
			return true;
		case SEND_ID:
		case SET_AS_ID:
			try {
				Uri fileUri = storePicture(saveTmpPicture(new File(
						EmailAlbumViewer.TMP_DIR_NAME)));
				if (item.getItemId() == SEND_ID) {
					intent = new Intent(Intent.ACTION_SEND, fileUri);
					intent.putExtra(Intent.EXTRA_STREAM, fileUri);
					intent.setType("image/jpeg");
				} else if (item.getItemId() == SET_AS_ID) {
					intent = new Intent(Intent.ACTION_ATTACH_DATA, fileUri);

				}
				startActivity(Intent.createChooser(intent,
						getText(R.string.chooser_share)));

			} catch (IOException e) {
				Log.e(this.getClass().getName(), "other intents exception", e);
				Toast.makeText(
						this,
						getText(R.string.error_other_intents) + " : "
								+ e.getLocalizedMessage(), Toast.LENGTH_SHORT)
						.show();
			}

			return true;

		case ABOUT_ID:
			intent = new Intent(this, AboutDialog.class);
			startActivity(intent);
			return true;
		case OPEN_WITH_ID:
		case EDIT_ID:
			try {
				Uri fileUri = Uri.parse("file://" + saveTmpPicture(new File(
						EmailAlbumViewer.TMP_DIR_NAME)).getAbsolutePath());
				Log.d(this.getClass().getSimpleName(), "Open Uri : " + fileUri);
				if(item.getItemId() == OPEN_WITH_ID) {
					intent = new Intent(Intent.ACTION_VIEW);
				} else {
					intent = new Intent(Intent.ACTION_EDIT);
				}
				Toast.makeText(getApplicationContext(), R.string.alert_different_viewer, Toast.LENGTH_LONG).show();
				intent.setDataAndType(fileUri, "image/jpeg");
				startActivity(Intent.createChooser(intent,
						getText(R.string.menu_open_with)));
			} catch (IOException e) {
				Log.e(this.getClass().getSimpleName(),
						"Error while creating temp file", e);
				Toast.makeText(getApplicationContext(),
						"Error while creating temp file.", Toast.LENGTH_LONG)
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
					savePicture(new File(Uri.parse(dirname).getEncodedPath()));
				} catch (IOException e) {
					Log.e(this.getClass().getName(),
							"onActivityResult() exception", e);
					Toast.makeText(this, e.getLocalizedMessage(),
							Toast.LENGTH_SHORT).show();
				}

			}
			break;
		}
	}

	private File saveTmpPicture(File destDir) throws IOException {
		File destFile = null;
		if (destDir != null) {
			if (!destDir.exists()) {
				try {
					destDir.mkdirs();
				} catch (Exception e) {
					destDir = null;
				}
			} else if (!destDir.isDirectory()) {
				destDir = null;
			}
		}

		if (destDir == null) {
			destDir = getCacheDir();
		}

		try {
			destFile = savePicture(destDir);
		} catch (IOException e) {
			if (!destDir.equals(getCacheDir())) {
				destFile = savePicture(getCacheDir());
			}
		}
		return destFile;
	}

	private File savePicture(File destDir) throws FileNotFoundException,
			IOException {
		File destFile;
		String fileName = mImageNames.get(mPosition);
		int trailSlashIndex = fileName.lastIndexOf('/');
		if(trailSlashIndex >= 0) {
			fileName = fileName.substring(trailSlashIndex);
		}
		destFile = new File(destDir, fileName.toLowerCase());

		OutputStream destFileOS = new FileOutputStream(destFile);
		InputStream imageIS = ZipUtil.getInputStream(archive, archive
				.getEntry(mImageNames.get(mPosition)));
		byte[] buffer = new byte[2048];
		int len = 0;
		while ((len = imageIS.read(buffer)) >= 0) {
			destFileOS.write(buffer, 0, len);
		}
		destFileOS.close();
		imageIS.close();
		return destFile;
	}

	private Uri storePicture(File imageFile) {
		ContentResolver cr = getContentResolver();
		String imageName = mImageNames.get(mPosition);
		imageName = imageName.substring(imageName.lastIndexOf('/') + 1);
		ContentValues values = new ContentValues(7);
		values.put(Images.Media.TITLE, imageName);
		values.put(Images.Media.DISPLAY_NAME, imageName);
		values.put(Images.Media.DESCRIPTION, "");
		values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis());
		values.put(Images.Media.MIME_TYPE, "image/jpeg");
		values.put(Images.Media.ORIENTATION, 0);
		File parentFile = imageFile.getParentFile();
		String path = parentFile.toString().toLowerCase();
		String name = parentFile.getName().toLowerCase();
		values.put(Images.ImageColumns.BUCKET_ID, path.hashCode());
		values.put(Images.ImageColumns.BUCKET_DISPLAY_NAME, name);
		values.put("_data", imageFile.toString());

		Uri uri = cr.insert(sStorageURI, values);

		return uri;
	}

	private Animation.AnimationListener animListener = new Animation.AnimationListener() {
		@Override
		public void onAnimationEnd(Animation animation) {
			mFlipper.postDelayed(new Runnable() {

				@Override
				public void run() {
					resetViews();
					mIsFlipping = false;
				}
			}, 10);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onAnimationStart(Animation animation) {
			// TODO Auto-generated method stub
		}
	};

	private void alertOIFileManagerIsMissing() {
		AlertDialog.Builder builder = new AlertDialog.Builder(
				getApplicationContext());
		builder.setMessage(R.string.alert_filemanager_missing).setCancelable(
				false).setPositiveButton(android.R.string.yes,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						getOIFileManager();
					}
				}).setNegativeButton(android.R.string.no,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

	protected void getOIFileManager() {
		Intent i = new Intent(Intent.ACTION_VIEW, Uri
				.parse("market://search?q=pname:org.openintents.filemanager"));
		startActivity(i);
	}
}
