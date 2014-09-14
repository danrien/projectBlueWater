package com.lasthopesoftware.bluewater.data.service.access;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.lasthopesoftware.bluewater.data.service.access.connection.ConnectionManager;
import com.lasthopesoftware.bluewater.data.service.helpers.FileCache;
import com.lasthopesoftware.bluewater.data.service.objects.File;
import com.lasthopesoftware.bluewater.data.sqlite.access.LibrarySession;
import com.lasthopesoftware.bluewater.data.sqlite.objects.Library;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTask;

public class ImageAccess extends SimpleTask<Void, Void, Bitmap> {
	
	public static final String IMAGE_FORMAT = "jpg";
	
	public ImageAccess(final Context context, final int fileKey) {
		this(context, new File(fileKey));
	}
	
	public ImageAccess(final Context context, final File file) {
		super();
		
		super.setOnExecuteListener(new GetFileImageOnExecute(context, file));
	}
	

	@Override
	public final void setOnExecuteListener(OnExecuteListener<Void, Void, Bitmap> listener) {
		throw new UnsupportedOperationException("The on execute listener cannot be set for an ImageTask. It is already set in the constructor.");
	}
		
	private static class GetFileImageOnExecute implements OnExecuteListener<Void, Void, Bitmap> {
		private static final int maxSize = 100 * 1024 * 1024; // 1024 * 1024 * 1024 for a gig of cache
		private static final Bitmap mFillerBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
		private static final String IMAGES_CACHE_NAME = "images";
		
		private final Context mContext;
		private final File mFile;
		
		public GetFileImageOnExecute(final Context context, final File file) {
			mContext = context;
			mFile = file;
		}
		
		@Override
		public Bitmap onExecute(ISimpleTask<Void, Void, Bitmap> owner, Void... params) throws Exception {
			final Library library = LibrarySession.GetLibrary(mContext);
			final FileCache imageCache = new FileCache(mContext, library, IMAGES_CACHE_NAME, maxSize);
			
			String uniqueKey = null;
			try {
				uniqueKey = mFile.getProperty(FileProperties.ARTIST) + ":" + mFile.getProperty(FileProperties.ALBUM);
			} catch (IOException ioE) {
				LoggerFactory.getLogger(getClass()).error("Error getting file properties.", ioE);
				return getFillerBitmap();
			}
			
			final java.io.File imageCacheFile = imageCache.get(uniqueKey);
			if (imageCacheFile != null) {
				try {
					return BitmapFactory.decodeFile(imageCacheFile.getCanonicalPath());
				} catch (IOException ioE) {
					LoggerFactory.getLogger(getClass()).error("Error getting file path.", ioE);
				}
			}
			
			try {
				HttpURLConnection conn = ConnectionManager.getConnection(
											"File/GetImage", 
											"File=" + String.valueOf(mFile.getKey()), 
											"Type=Full", 
											"Pad=1",
											"Format=" + IMAGE_FORMAT,
											"FillTransparency=ffffff");
				
				// Connection failed to build or isCancelled was called, return an empty bitmap
				// but do not put it into the cache
				if (conn == null || owner.isCancelled()) return getFillerBitmap();
				
				byte[] imageBytes = null;
				try {
					imageBytes = IOUtils.toByteArray(conn.getInputStream());
					if (imageBytes.length == 0)
						return getFillerBitmap();
				} catch (FileNotFoundException fe) {
					LoggerFactory.getLogger(getClass()).warn("Image not found!");
					return getFillerBitmap();
				} finally {
					conn.disconnect();
				}
				
				final java.io.File cacheDir = FileCache.getDiskCacheDir(mContext, IMAGES_CACHE_NAME);
				if (!cacheDir.exists())
					cacheDir.mkdirs();
				final java.io.File file = java.io.File.createTempFile(String.valueOf(library.getId()) + "-" + IMAGES_CACHE_NAME, "." + IMAGE_FORMAT, cacheDir);
				
				imageCache.put(uniqueKey, file, imageBytes);
					
				return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
			} catch (Exception e) {
				LoggerFactory.getLogger(getClass()).error(e.toString(), e);
			}
			
			return null;
		}

		private static final Bitmap getBitmapCopy(Bitmap src) {
			return src.copy(src.getConfig(), false);
		}
		
		private static final Bitmap getFillerBitmap() {
			return getBitmapCopy(mFillerBitmap);
		}
	}
}
