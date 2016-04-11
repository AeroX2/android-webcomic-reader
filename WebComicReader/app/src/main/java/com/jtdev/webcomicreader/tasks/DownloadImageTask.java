package com.jtdev.webcomicreader.tasks;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.View;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.jtdev.webcomicreader.MainActivity;
import com.jtdev.webcomicreader.R;
import com.jtdev.webcomicreader.helpers.DiskHelper;
import com.jtdev.webcomicreader.helpers.NetworkHelper;
import com.jtdev.webcomicreader.models.Image;
import com.jtdev.webcomicreader.utils.CustomImageRequest;

/**
 * Copyright (c) 2016 James Ridey <james@snoopyaustralia.com>
 *
 * All rights reserved. No warranty, explicit or implicit, provided.
 *
 * File created on 1/26/16
 */
public class DownloadImageTask
{
	private static LruCache<String, BitmapDrawable> memoryCache;
	private static DiskHelper diskCache;

	private final MainActivity main;
	private int reqWidth;
	private int reqHeight;

	public DownloadImageTask(MainActivity main)
	{
		this.main = main;
	}

	public void execute(final Image image)
	{
		//TODO Create disk cache
		onPreExecute();

		final String imageUrl = image.getImageUrl();
		Log.d("debug", "Downloading image: " + imageUrl);

		Drawable cachedDrawable = getDrawableFromMemCache(imageUrl);
		if (cachedDrawable != null)
		{
			Log.d("debug", "Getting image drawable from cache");
			image.setDrawable(cachedDrawable);
			onPostExecute(image);
			return;
		}

		CustomImageRequest request = new CustomImageRequest(imageUrl, reqWidth, reqHeight,
				new Response.Listener<Bitmap>()
				{
					@Override
					public void onResponse(Bitmap bitmap)
					{
						BitmapDrawable bitmapDrawable = new BitmapDrawable(main.getResources(), bitmap);
						putDrawableIntoMemoryCache(imageUrl, bitmapDrawable);

						image.setDrawable(bitmapDrawable);
						onPostExecute(image);
					}
				},
				new Response.ErrorListener()
				{
					@Override
					public void onErrorResponse(VolleyError error)
					{
						error.printStackTrace();
						image.setErrorCode(main.getString(R.string.error_failed_image_download));
						onPostExecute(image);
					}
				});
		NetworkHelper.getInstance(main).add(request,"download");
	}

	private void onPreExecute()
	{
		NetworkHelper.getInstance(main).cancel();

		//initMemoryCache();
		main.getProgressBar().setVisibility(View.VISIBLE);

		reqWidth = main.getImageSwitcher().getWidth();
		reqHeight = main.getImageSwitcher().getHeight();
	}

	private void onPostExecute(Image image)
	{
		main.getProgressBar().setVisibility(View.INVISIBLE);
		if (image.getErrorCode() != null) main.getErrorMessage().setText(image.getErrorCode());
		else main.getImageSwitcher().setImageDrawable(image.getDrawable());
	}

	private void initMemoryCache()
	{
		if (memoryCache == null)
		{
			// Get max available VM memory, exceeding this amount will throw an
			// OutOfMemory exception. Stored in kilobytes as LruCache takes an
			// int in its constructor.

			ActivityManager am = (ActivityManager) main.getSystemService(Context.ACTIVITY_SERVICE);
			int maxMemory = am.getMemoryClass() * 1024 * 1024;
			Log.d("debug", "Max memory: " + maxMemory);

			// Use 1/8th of the available memory for this memory cache.
			int cacheSize = maxMemory / 8;
			Log.d("debug", "Cache size: " + cacheSize);

			memoryCache = new LruCache<String, BitmapDrawable>(cacheSize)
			{
				@Override
				protected int sizeOf(String key, BitmapDrawable bitmapDrawable)
				{
					//Log.d("debug", "Bitmap drawable size: " + bitmapDrawable.getBitmap().getByteCount());
					return bitmapDrawable.getBitmap().getByteCount();
				}
			};
		}
	}

	private void initDiskCache()
	{
		if (diskCache == null)
		{
			diskCache = DiskHelper.getInstance(main);
		}
	}

	private void putDrawableIntoMemoryCache(String key, BitmapDrawable bitmapDrawable)
	{
		//if (getDrawableFromMemCache(key) == null) memoryCache.put(key, bitmapDrawable);
	}

	private BitmapDrawable getDrawableFromMemCache(String key)
	{
		return null; //return memoryCache.get(key);
	}

	public static void clearCache()
	{
		memoryCache.evictAll();
	}
}
