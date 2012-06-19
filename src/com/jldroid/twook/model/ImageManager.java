package com.jldroid.twook.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.util.Log;

import com.jdroid.utils.FastBufferedInputStream;
import com.jdroid.utils.StorageManager;
import com.jdroid.utils.StorageManager.StorageBundle;
import com.jdroid.utils.Threads;

public class ImageManager {
	
	private static final long FLUSH_DELAY = 10000;
	
	public static final int REF_WEAK = 1;
	public static final int REF_SOFT = 2;
	
	private static ImageManager sInstance;
	
	protected Context mContext;
	
	protected StorageManager mStorageManager;
	
	protected File mCacheDir;
	
	private HashMap<String, CachedImage> mCachedImages;
	
	private int mFilenameCounter;
	
	private final int mProfilePictureSize;
	
	protected static final Options sNetworkDecodingOptions = new Options();
	protected static final Options sLocalDecodingOptions = new Options();
	
	static {
		sNetworkDecodingOptions.inTempStorage = new byte[1024 * 16];
		sLocalDecodingOptions.inTempStorage = new byte[1024 * 16];
	}
	
	protected static final ThreadLocal<FastBufferedInputStream> sBufStreamPool = new ThreadLocal<FastBufferedInputStream>() {
		protected FastBufferedInputStream initialValue() {
			return new FastBufferedInputStream(null, 1024 * 32);
		};
	};
	
	private ImageManager(Context c) {
		mContext = c;
		mCacheDir = mContext.getDir("cache", Context.MODE_PRIVATE);
		mProfilePictureSize = (int) (mContext.getResources().getDisplayMetrics().density * 48);
		
		mStorageManager = new StorageManager(mContext, "PROFILE_PICTURE_MANAGER");
		mFilenameCounter = mStorageManager.readInt("COUNTER", 0);
		StorageBundle[] bundles = mStorageManager.readBundleArray("IMAGES", null);
		if (bundles != null) {
			mCachedImages = new HashMap<String, CachedImage>(bundles.length);
			for (int i = 0; i < bundles.length; i++) {
				CachedImage img = new CachedImage(bundles[i]);
				mCachedImages.put(img.getUri(), img);
			}
		} else {
			mCachedImages = new HashMap<String, CachedImage>(10);
		}
		checkCache();
	}
	
	public int getProfilePictureSize() {
		return mProfilePictureSize;
	}
	
	public CachedImage peekCachedImage(String uri) {
		return uri != null ? mCachedImages.get(uri) : null;
	}
	
	public Bitmap peekImage(String uri) {
		CachedImage cachedImage = peekCachedImage(uri);
		return cachedImage != null ? cachedImage.peekBmd() : null;
	}
	
	public String peekCachePath(String uri) {
		CachedImage cachedImage = peekCachedImage(uri);
		if (cachedImage != null) {
			if (cachedImage.getAbsolutePath() != null && new File(cachedImage.getAbsolutePath()).exists()) {
				return cachedImage.getAbsolutePath();
			}
		}
		return null;
	}
	
	public String getCachePath(String uri) {
		CachedImage cachedImage = peekCachedImage(uri);
		if (cachedImage != null) {
			synchronized (cachedImage) {
				if (cachedImage.getAbsolutePath() != null && new File(cachedImage.getAbsolutePath()).exists()) {
					return cachedImage.getAbsolutePath();
				}
				try {
					cachedImage.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
					return getCachePath(uri);
				}
				return cachedImage.getAbsolutePath();
			}
		}
		return null;
	}
	
	public void loadProfilePicture(LoadBitmapCallback callback, String uri, DeletionTrigger deletionTrigger) {
		loadImage(callback, uri, deletionTrigger, REF_SOFT, mProfilePictureSize, mProfilePictureSize, 80);
	}
	
	public void loadImage(LoadBitmapCallback callback, String uri, DeletionTrigger deletionTrigger, int refType,
			int outWidth, int outHeight, int quality) {
		if (uri == null) {
			throw new IllegalStateException("uri cannot be null");
		}
		CachedImage cachedImage = peekCachedImage(uri);
		if (cachedImage == null) {
			cachedImage = new CachedImage(uri, deletionTrigger, refType);
			mCachedImages.put(uri, cachedImage);
			updateStorage();
		} else {
			if (deletionTrigger.isLonger(cachedImage.getDeletionTrigger())) {
				cachedImage.setDeletionTrigger(deletionTrigger);
			}
		}
		cachedImage.loadBitmap(callback, outWidth, outHeight, quality);
	}
	
	public void cancelLoad(String uri) {
		CachedImage cachedImage = peekCachedImage(uri);
		if (cachedImage != null) {
			cachedImage.cancel();
		}
	}
	
	public void unloadImage(String uri) {
		CachedImage cachedImage = peekCachedImage(uri);
		if (cachedImage != null) {
			unloadImage(cachedImage);
		}
	}
	
	private synchronized void unloadImage(CachedImage img) {
		mCachedImages.remove(img.getUri());
		cleanupImage(img);
	}
	
	private void cleanupImage(CachedImage img) {
		img.releaseBmd();
		String filename = img.getFilename();
		if (filename != null) {
			File file = new File(mCacheDir.getAbsolutePath() + "/" + filename);
			file.delete();
		}
	}
	
	public synchronized void deleteAll() {
		for (CachedImage img : mCachedImages.values()) {
			cleanupImage(img);
		}
		mCachedImages.clear();
		mFilenameCounter = 0;
		mStorageManager.write("COUNTER", mFilenameCounter);
		updateStorage();
	}
	
	private synchronized void updateStorage() {
		StorageBundle[] bundles = new StorageBundle[mCachedImages.size()];
		int i = 0;
		for (CachedImage img : mCachedImages.values()) {
			bundles[i] = img.getBundle();
			i++;
		}
		mStorageManager.write("IMAGES", bundles);
		mStorageManager.flushAsync(FLUSH_DELAY);
	}
	
	public synchronized void releaseCache() {
		for (CachedImage img : mCachedImages.values()) {
			img.releaseBmd();
		}
	}
	
	private synchronized String generateFilename() {
		String str = "img" + mFilenameCounter + ".jpg";
		mFilenameCounter++;
		mStorageManager.write("COUNTER", mFilenameCounter);
		return str;
	}
	
	public void flush() {
		mStorageManager.flushAsync(0);
	}
	
	public synchronized void checkCache() {
		int removedCount = 0;
		
		Iterator<CachedImage> iterator = mCachedImages.values().iterator();
		
		while (iterator.hasNext()) {
			CachedImage cachedImg = iterator.next();
			if (cachedImg.getDeletionTrigger().isDirty(cachedImg.getLoadTime(), cachedImg.getLastUsedTime())) {
				iterator.remove();
				cleanupImage(cachedImg);
				removedCount++;
			}
		}
		if (removedCount > 0) {
			updateStorage();
			Log.i("JDROID", removedCount + " cached images removed from cache");
		}
	}
	
	public static ImageManager getInstance(Context c) {
		if (sInstance == null) {
			sInstance = new ImageManager(c.getApplicationContext());
		}
		return sInstance;
	}
	
	public static interface LoadBitmapCallback {
		public void onBitmapLoaded(String uri, Bitmap bmd);
		public void onFailed(String uri);
	}
	
	public class CachedImage {
		
		private String mUri;
		private String mFilename;
		
		private Object mRef;
		private int mRefType;
		private DeletionTrigger mDeletionTrigger;
		
		private StorageBundle mBundle;
		
		private ArrayList<LoadBitmapCallback> mCallbackQueue;
		private boolean mLoading = false;
		
		private Runnable mCancelRunnable;
		
		private long mLoadTime = -1;
		private long mLastUsedTime = -1;
		
		public CachedImage() {
			mBundle = new StorageBundle(2);
		}
		
		public CachedImage(StorageBundle bundle) {
			mBundle = bundle;
			mUri = bundle.readString("URI", null);
			mDeletionTrigger = values[bundle.readInt("DELETIONTRIGGER", 0)];
			mRefType = bundle.readInt("REFTYPE", REF_SOFT);
			mFilename = bundle.readString("FILENAME", null);
			mLoadTime = bundle.readLong("LOADTIME", -1);
			mLastUsedTime = bundle.readLong("LASTUSEDTIME", -1);
		}
		
		public CachedImage(String uri, DeletionTrigger trigger, int refType) {
			this();
			setRefType(refType);
			setDeletionTrigger(trigger);
			setUri(uri);
		}
		
		public synchronized Bitmap peekBmd() {
			if (mRef == null) {
				return null;
			}
			setLastUsedTime(System.currentTimeMillis());
			switch (mRefType) {
			case REF_WEAK:
				return ((WeakReference<Bitmap>) mRef).get();
			case REF_SOFT:
				return ((SoftReference<Bitmap>) mRef).get();
			default:
				throw new IllegalStateException("UNKNOWN REF TYPE: " + mRefType);
			}
		}
		
		private synchronized void setBitmap(Bitmap bmd) {
			switch (mRefType) {
			case REF_WEAK:
				mRef = new WeakReference<Bitmap>(bmd);
				break;
			case REF_SOFT:
			default:
				mRefType = REF_SOFT;
				mRef = new SoftReference<Bitmap>(bmd);
			}
			setLastUsedTime(System.currentTimeMillis());
			for (int i = mCallbackQueue.size() - 1; i >= 0; i--) {
				LoadBitmapCallback callback = mCallbackQueue.get(i);
				if (callback != null) {
					callback.onBitmapLoaded(mUri, bmd);
				}
			}
			mCallbackQueue.clear();
			mLoading = false;
			
			/*int cacheSize = 0;
			for (CachedImage img : mCachedImages.values()) {
				bmd = img.peekBmd();
				if (bmd != null) {
					cacheSize += bmd.getByteCount();
				}
			}
			Log.i("JDROID", "CACHE SIZE: " + cacheSize / 1024f + "kb");*/
		}
		
		public synchronized void cancel() {
			if (mCancelRunnable != null) {
				Threads.getNetworkWorker().remove(mCancelRunnable);
				mCancelRunnable = null;
				mLoading = false;
				for (int i = mCallbackQueue.size() - 1; i >= 0; i--) {
					LoadBitmapCallback callback = mCallbackQueue.get(i);
					if (callback != null) {
						callback.onFailed(mUri);
					}
				}
			}
		}
		
		public synchronized void loadBitmap(final LoadBitmapCallback callback, final int outWidth, final int outHeight, final int quality) {
			Bitmap bmd = peekBmd();
			if (bmd != null) {
				if (callback != null) callback.onBitmapLoaded(mUri, bmd);
				return;
			}
			if (mCallbackQueue == null) {
				mCallbackQueue = new ArrayList<LoadBitmapCallback>(1);
			}
			mCallbackQueue.add(callback);
			if (mLoading) {
				return;
			}
			mLoading = true;
			Threads.runOnIOThread(new Runnable() {
				@Override
				public void run() {
					Bitmap bmd = loadBitmapFromStorage();
					if (bmd != null) {
						setBitmap(bmd);
						return;
					}
					Threads.runOnNetworkThread(mCancelRunnable = new Runnable() {
						@Override
						public void run() {
							mCancelRunnable = null;
							Bitmap bmd = downloadBitmap();
							if (bmd != null) {
								final float ratio = (float) bmd.getWidth() / (float) bmd.getHeight();
								int w = outWidth;
								int h = outHeight;
								if (w == -1 && h == -1) {
									w = bmd.getWidth();
									h = bmd.getHeight();
								} else if (w == -1) {
									w = (int) (h * ratio);
								} else if (h == -1) {
									h = (int) (w / ratio);
								}
								if (w != bmd.getWidth() || h != bmd.getHeight()) {
									if (false && w / h == bmd.getWidth() / bmd.getHeight() && w > bmd.getWidth() && h > bmd.getHeight()) {
										// scaling is wasted here...
									} else {
										Bitmap scaledBmd = Bitmap.createScaledBitmap(bmd, w, h, true);
										bmd.recycle();
										bmd = scaledBmd;
									}
									
								}
								setBitmap(bmd);
								setLoadTime(System.currentTimeMillis());
								if (mDeletionTrigger.isSaveToStorage()) {
									Threads.runOnIOThread(new Runnable() {
										@Override
										public void run() {
											saveBitmapToStorage(quality);
										}
									});
								}
								return;
							}
							synchronized (CachedImage.this) {
								for (int i = mCallbackQueue.size() - 1; i >= 0; i--) {
									LoadBitmapCallback callback = mCallbackQueue.get(i);
									if (callback != null) {
										callback.onFailed(mUri);
									}
								}
								mCallbackQueue.clear();
								mLoading = false;
							}
							return;
						}
					});
				}
			});
		}
		
		private synchronized void saveBitmapToStorage(int quality) {
			Bitmap bmd = peekBmd();
			if (bmd != null) {
				String filename = generateFilename();
				try {
					FileOutputStream fos = new FileOutputStream(mCacheDir.getAbsolutePath() + "/" + filename);
					bmd.compress(CompressFormat.JPEG, quality, fos);
					fos.flush();
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				setFilename(filename);
				mStorageManager.flushAsync(FLUSH_DELAY);
				notifyAll();
			}
		}
		
		private Bitmap downloadBitmap() {
			HttpGet request = new HttpGet(mUri);
			DefaultHttpClient client = new DefaultHttpClient();
			FastBufferedInputStream fbis = sBufStreamPool.get();
		    try {
		        HttpResponse response = client.execute(request);
		        
		        final HttpEntity entity = response.getEntity();
		        if (entity != null) {
		            InputStream inputStream = null;
		            try {
		                inputStream = entity.getContent(); 
		                fbis.setInputStream(inputStream);
		                return BitmapFactory.decodeStream(fbis);
		            } finally {
		                if (inputStream != null) {
		                	fbis.close();
		                }
		                entity.consumeContent();
		            }
		        }
		    } catch (Exception e) {
		        request.abort();
		        Log.w("ImageDownloader", "Error while retrieving bitmap from " + mUri);
		    }
		    return null;
		}
		
		private Bitmap loadBitmapFromStorage() {
			if (mFilename == null) {
				return null;
			}
			File file = new File(getAbsolutePath());
			try {
				FileInputStream fis = new FileInputStream(file);
				FastBufferedInputStream fbis = sBufStreamPool.get();
				try {
					fbis.setInputStream(fis);
					Bitmap bmd = BitmapFactory.decodeStream(fbis, null, sLocalDecodingOptions);
					if (bmd != null) {
						return bmd;
					}
				} catch (OutOfMemoryError e) {
					System.gc();
				} finally {
					try {
						fbis.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				mFilename = null;
			}
			return null;
		}
		
		public String getUri() {
			return mUri;
		}
		
		private void setUri(String pUri) {
			mUri = pUri;
			mBundle.write("URI", pUri);
		}
		
		public int getRefType() {
			return mRefType;
		}
		
		public void setRefType(int pRefType) {
			mRefType = pRefType;
			mBundle.write("REFTYPE", pRefType);
		}
		
		public DeletionTrigger getDeletionTrigger() {
			return mDeletionTrigger;
		}
		
		public long getLoadTime() {
			return mLoadTime;
		}
		
		public long getLastUsedTime() {
			return mLastUsedTime;
		}
		
		public void setLoadTime(long pLoadTime) {
			mLoadTime = pLoadTime;
			mBundle.write("LOADTIME", pLoadTime);
		}
		
		public void setLastUsedTime(long pLastUsedTime) {
			mLastUsedTime = pLastUsedTime;
			mBundle.write("LASTUSEDTIME", pLastUsedTime);
		}
		
		public void setDeletionTrigger(DeletionTrigger pDeletionTrigger) {
			mDeletionTrigger = pDeletionTrigger;
			mBundle.write("DELETIONTRIGGER", pDeletionTrigger.ordinal());
		}
		
		public String getFilename() {
			return mFilename;
		}
		
		public String getAbsolutePath() {
			return mCacheDir.getAbsolutePath() + "/" + mFilename;
		}
		
		public void setFilename(String pFilename) {
			mFilename = pFilename;
			mBundle.write("FILENAME", pFilename);
		}
		
		public synchronized void releaseBmd() {
			Object obj = mRef;
			mRef = null;
			if (obj instanceof Reference<?>) {
				Reference<Bitmap> ref = (Reference<Bitmap>) obj;
				ref.clear();
			}
		}
		
		public StorageBundle getBundle() {
			return mBundle;
		}
		
		@Override
		public int hashCode() {
			return mUri.hashCode();
		}
		
		@Override
		public boolean equals(Object pO) {
			if (pO instanceof CachedImage) {
				CachedImage ci = (CachedImage) pO;
				return mUri.equals(ci.mUri);
			}
			return false;
		}
	}
	
	private static long DAY = 24 * 3600000;
	private static final DeletionTrigger[] values = DeletionTrigger.values();
	
	public static enum DeletionTrigger {
		IMMEDIATELY(false, false, -1),
		
		AFTER_ONE_DAY(true, false, DAY),
		AFTER_ONE_WEEK(true, false, DAY * 7),
		AFTER_ONE_MONTH(true, false, DAY * 30),
		AFTER_ONE_QUARTER(true, false, DAY * 90),
		
		AFTER_ONE_DAY_UNUSED(true, true, DAY),
		AFTER_ONE_WEEK_UNUSED(true, true, DAY * 7),
		AFTER_ONE_MONTH_UNUSED(true, true, DAY * 30),
		AFTER_ONE_QUARTER_UNUSED(true, true, DAY * 90);
		
		private boolean isSaveToStorage;
		private boolean isResetTimeAfterUsage;
		private long mTime;
		
		private DeletionTrigger(boolean saveToStorage, boolean resetTimeAfterUsage, long time) {
			isSaveToStorage = saveToStorage;
			isResetTimeAfterUsage = resetTimeAfterUsage;
			mTime = time;
		}
		
		public boolean isLonger(DeletionTrigger pDeletionTrigger) {
			if (isSaveToStorage != pDeletionTrigger.isSaveToStorage) {
				return isSaveToStorage;
			}
			return mTime > pDeletionTrigger.mTime;
		}

		public boolean isSaveToStorage() {
			return isSaveToStorage;
		}
		
		public boolean isResetTimeAfterUsage() {
			return isResetTimeAfterUsage;
		}
		
		public long getTime() {
			return mTime;
		}
		
		public boolean isDirty(long loadTime, long lastUsedTime) {
			if (!isSaveToStorage) {
				return true;
			}
			long now = System.currentTimeMillis();
			if (isResetTimeAfterUsage) {
				return now - lastUsedTime > mTime;
			} else {
				return now - loadTime > mTime;
			}
		}
	}
}
