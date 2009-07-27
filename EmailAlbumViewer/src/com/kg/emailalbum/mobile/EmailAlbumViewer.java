/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kg.emailalbum.mobile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class EmailAlbumViewer extends ListActivity {
	private static final int INSERT_ID = Menu.FIRST;
	private static final int ACTIVITY_PICK_FILE = 1;
	private ArrayList<Map<String, String>> mContentModel;
	private Uri mAlbumFileUri = null;
	private ZipFile mArchive = null;
	private ProgressDialog mProgress;
	private Context context;
	
	private class ArchiveRetriever implements Runnable {

		@Override
		public void run() {
			try {
				InputStream intentInputStream = getContentResolver()
						.openInputStream(mAlbumFileUri);
				File tempArchive = File
						.createTempFile("emailalbum", ".zip", getCacheDir());
				OutputStream tempOS = new FileOutputStream(tempArchive);

				byte[] buffer = new byte[256];
				int readBytes = -1;
				while ((readBytes = intentInputStream.read(buffer)) != -1) {
					tempOS.write(buffer, 0, readBytes);
				}
				mAlbumFileUri = Uri.parse(tempArchive.toURI().toString());
				tempOS.close();
				intentInputStream.close();
				handler.sendEmptyMessage(0);

			} catch (IOException e) {
				Message msg = new Message();
				Bundle data = new Bundle();
				msg.arg1 = -1;
				data.putString("EXCEPTION", e.getLocalizedMessage());
				msg.setData(data );
				handler.sendMessage(msg);
			}
		}
		
	}

	private Handler handler = new Handler() {

		/* (non-Javadoc)
		 * @see android.os.Handler#handleMessage(android.os.Message)
		 */
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			mProgress.dismiss();
			if(msg.arg1 < 0) {
				Toast.makeText(context, msg.getData().getShort("EXCEPTION"), Toast.LENGTH_SHORT).show();
			} 
			fillData();
		}
		
	};
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		context = this;
//		if (getIntent() != null) {
//			Toast.makeText(this, "Intent action : " + getIntent().getAction(),
//					Toast.LENGTH_SHORT).show();
//		}

		
		if (savedInstanceState != null) {
			if (savedInstanceState.getString("albumFileUri") != null) {
				mAlbumFileUri = Uri.parse(savedInstanceState
						.getString("albumFileUri"));
			}
		} else if (getIntent() != null
				&& Intent.ACTION_VIEW.equals(getIntent().getAction())) {
			mAlbumFileUri = getIntent().getData();
			if (mAlbumFileUri.getScheme()
					.equals(ContentResolver.SCHEME_CONTENT)) {
				
				mProgress = ProgressDialog.show(this, getText(R.string.title_prog_retrieve_archive), getText(R.string.msg_prog_retrieve_archive), true, false);
				new Thread(new ArchiveRetriever()).start();
			}
		} 

		if (mAlbumFileUri == null) {
			Intent intent = new Intent("org.openintents.action.PICK_FILE");
			intent.setData(Uri.parse("file:///sdcard"));
			intent.putExtra("org.openintents.extra.TITLE",
					"Please select an album...");
			intent.putExtra("org.openintents.extra.BUTTON_TEXT",
					"There it is !");

			startActivityForResult(intent, ACTIVITY_PICK_FILE);

			setContentView(R.layout.content_list);
			fillData();
		}

	}

	private void fillData() {
		if (mAlbumFileUri != null) {
			try {
				File selectedFile = new File(new URI(mAlbumFileUri.toString()
						.replace(" ", "%20")));
				mArchive = new ZipFile(selectedFile);
			} catch (Exception e) {
				Toast.makeText(this,
						R.string.open_archive_error + e.getLocalizedMessage(),
						Toast.LENGTH_SHORT).show();
			}

			if (mArchive == null) {
				Toast.makeText(this, R.string.open_archive_error,
						Toast.LENGTH_SHORT).show();
			} else {
				try {
					// Now create an array adapter and set it to display using
					// our row
					mContentModel = new ArrayList<Map<String, String>>();
					setTitle(mAlbumFileUri.getLastPathSegment() + " content");
					ZipEntry entry = null;
					Enumeration<? extends ZipEntry> entries = mArchive.entries();
					while (entries.hasMoreElements()) {
						entry = entries.nextElement();
						if (entry.getName().endsWith(".jpg")
								|| entry.getName().endsWith(".JPG")
								|| entry.getName().endsWith(".gif")
								|| entry.getName().endsWith(".GIF")
								|| entry.getName().endsWith(".png")
								|| entry.getName().endsWith(".PNG")) {
							Map<String, String> contentLine = new HashMap<String, String>();
							contentLine.put("imageName", entry.getName());
							contentLine.put("imageShortName", entry.getName().substring(entry.getName().lastIndexOf('/') + 1));
							mContentModel.add(contentLine);
						}

						// Create an array to specify the fields we want to

					}
					// display in the
					// list (only TITLE)
					String[] from = new String[] { "imageShortName" };
					// and an array of the fields we want to bind those fields
					// to (in
					// this case just text1)
					int[] to = new int[] { R.id.text1 };

					SimpleAdapter content = new SimpleAdapter(this,
							mContentModel, R.layout.content_row, from, to);
					setListAdapter(content);
				} catch (Exception e) {
					Toast.makeText(this, e.getLocalizedMessage(),
							Toast.LENGTH_SHORT).show();
				}
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		boolean result = super.onCreateOptionsMenu(menu);
		return result;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch (item.getItemId()) {
		case INSERT_ID:
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
		case ACTIVITY_PICK_FILE:
			if (resultCode == RESULT_OK && data != null) {
				// obtain the filename
				String filename = data.getDataString();
				if (filename != null) {
					mAlbumFileUri = Uri.parse(filename);
				}
				fillData();
			}
			break;
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mAlbumFileUri != null) {
			outState.putString("albumFileUri", mAlbumFileUri.toString());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.ListActivity#onListItemClick(android.widget.ListView,
	 * android.view.View, int, long)
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Intent i = new Intent(this, ShowPics.class);

		ArrayList<String> imageNames = new ArrayList<String>();
		for (Map<String, String> contentItem : mContentModel) {
			imageNames.add(contentItem.get("imageName"));
		}

		i.putExtra("PICS", imageNames);
		i.putExtra("ALBUM", mAlbumFileUri.toString());
		i.putExtra("POSITION", position);
		startActivity(i);
	}

}
