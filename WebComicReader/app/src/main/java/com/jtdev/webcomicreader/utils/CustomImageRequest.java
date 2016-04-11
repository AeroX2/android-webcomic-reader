package com.jtdev.webcomicreader.utils;

import android.graphics.Bitmap;
import android.graphics.Point;

import android.graphics.BitmapFactory;
import com.android.volley.*;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.ImageRequest;

import java.io.InputStream;

/**
 * Copyright (c) 2016 James Ridey <james@snoopyaustralia.com>
 *
 * All rights reserved. No warranty, explicit or implicit, provided.
 *
 * File created on 2/6/16
 */
public class CustomImageRequest extends Request<Bitmap>
{
	private static final int IMAGE_TIMEOUT_MS = 1000;
	private static final int IMAGE_MAX_RETRIES = 2;
	private static final float IMAGE_BACKOFF_MULT = 2f;

	private final Response.Listener<Bitmap> mListener;

	/**
	 * Decoding lock so that we don't decode more than one image at a time (to avoid OOM's)
	 */
	private static final Object sDecodeLock = new Object();
	private final int reqWidth;
	private final int reqHeight;

	public CustomImageRequest(String url, int reqWidth, int reqHeight, Response.Listener<Bitmap> listener, Response.ErrorListener errorListener)
	{
		super(Method.GET, url, errorListener);
		setRetryPolicy(new DefaultRetryPolicy(IMAGE_TIMEOUT_MS, IMAGE_MAX_RETRIES, IMAGE_BACKOFF_MULT));
		mListener = listener;
		this.reqWidth = reqWidth;
		this.reqHeight = reqHeight;
	}

	@Override
	public Priority getPriority()
	{
		return Priority.LOW;
	}

	@Override
	protected Response<Bitmap> parseNetworkResponse(NetworkResponse response)
	{
		// Serialize all decode on a global lock to reduce concurrent heap usage.
		synchronized (sDecodeLock)
		{
			try
			{
				return doParse(response);
			}
			catch (OutOfMemoryError e)
			{
				VolleyLog.e("Caught OOM for %d byte image, url=%s", response.data.length, getUrl());
				return Response.error(new ParseError(e));
			}
		}
	}

	private Response<Bitmap> doParse(NetworkResponse response)
	{
		Bitmap bitmap = decodeSampledBitmapFromResource(response.data, reqWidth, reqHeight);

		if (bitmap == null) return Response.error(new ParseError(response));
		else return Response.success(bitmap, HttpHeaderParser.parseCacheHeaders(response));
	}

	@Override
	protected void deliverResponse(Bitmap response)
	{
		mListener.onResponse(response);
	}

	private Bitmap decodeSampledBitmapFromResource(byte[] data, int reqWidth, int reqHeight)
	{
		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		//noinspection deprecation
		options.inPurgeable = true;
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeByteArray(data, 0, data.length, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;

		return BitmapFactory.decodeByteArray(data, 0, data.length, options);
	}

	private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight)
	{
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth)
		{

			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			// Calculate the largest inSampleSize value that is a power of 2 and keeps both
			// height and width larger than the requested height and width.
			while ((halfHeight / inSampleSize) > reqHeight &&
					(halfWidth / inSampleSize) > reqWidth)
			{
				inSampleSize *= 2;
			}
		}

		return inSampleSize;
	}
}

