package com.jtdev.webcomicreader.utils;

import android.graphics.Point;

import android.graphics.BitmapFactory;
import com.android.volley.*;
import com.android.volley.toolbox.HttpHeaderParser;

/**
 * Copyright (c) 2016 James Ridey <james@snoopyaustralia.com>
 *
 * All rights reserved. No warranty, explicit or implicit, provided.
 *
 * File created on 2/6/16
 */
public class ImageSizeRequest extends Request<Point>
{
	private static final int IMAGE_TIMEOUT_MS = 1000;
	private static final int IMAGE_MAX_RETRIES = 2;
	private static final float IMAGE_BACKOFF_MULT = 2f;

	private final Response.Listener<Point> mListener;

	/**
	 * Decoding lock so that we don't decode more than one image at a time (to avoid OOM's)
	 */
	private static final Object sDecodeLock = new Object();

	public ImageSizeRequest(String url, Response.Listener<Point> listener, Response.ErrorListener errorListener)
	{
		super(Method.GET, url, errorListener);
		setRetryPolicy(new DefaultRetryPolicy(IMAGE_TIMEOUT_MS, IMAGE_MAX_RETRIES, IMAGE_BACKOFF_MULT));
		mListener = listener;
	}

	@Override
	public Priority getPriority()
	{
		return Priority.LOW;
	}

	@Override
	protected Response<Point> parseNetworkResponse(NetworkResponse response)
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



	private Response<Point> doParse(NetworkResponse response)
	{
		byte[] data = response.data;
		BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
		decodeOptions.inJustDecodeBounds = true;

		//BitmapFactory.decodeStream()
		BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
		Point point = new Point(decodeOptions.outWidth, decodeOptions.outHeight);

		if (point.x == 0 && point.y == 0) return Response.error(new ParseError(response));
		else return Response.success(point, HttpHeaderParser.parseCacheHeaders(response));
	}

	@Override
	protected void deliverResponse(Point response)
	{
		mListener.onResponse(response);
	}
}

