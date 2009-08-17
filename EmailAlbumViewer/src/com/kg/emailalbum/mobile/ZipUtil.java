package com.kg.emailalbum.mobile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import android.util.Log;

public class ZipUtil {
	public static InputStream getInputStream(ZipFile archive, ZipEntry entry) throws IOException {
		if(archive.getEntry("META-INF/MANIFEST.MF") != null) {
			return archive.getInputStream(entry);
		}
		
		
		File zipFile = new File(archive.getName());
		ZipInputStream zio = new ZipInputStream(new FileInputStream(zipFile));
		boolean found = false;
		String currentEntryName = null;
		while((zio.available() > 0) && !found) {
			
			try {
				currentEntryName = zio.getNextEntry().getName();
			} catch (Exception e) {
				// Might be a wrong character encoding... file won't be read anyway.
				Log.e(ZipUtil.class.getSimpleName(), "Read a bad UTF-8 entry name", e);
				return null;
			}
			if(currentEntryName != null && currentEntryName.equals(entry.getName())) {
				found = true;
			}
		}
		if(found) {
			return zio;
		} else {
			return null;
		}
	}
}
