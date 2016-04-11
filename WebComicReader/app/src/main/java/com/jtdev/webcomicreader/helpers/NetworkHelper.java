package com.jtdev.webcomicreader.helpers;

import android.content.Context;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.Volley;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Copyright (c) 2016 James Ridey <james@snoopyaustralia.com>
 *
 * All rights reserved. No warranty, explicit or implicit, provided.
 *
 * File created on 5/02/16
 */
public class NetworkHelper
{
	private static final int RETRY_TIMES = 3;

	private static NetworkHelper networkHelperInstance;
	private final Context context;
	private RequestQueue requestQueue;

	private NetworkHelper(Context context)
	{
		this.context = context;
	}

	private RequestQueue getRequestQueue()
	{
		//TODO Disk size for volley?
		if (requestQueue == null) requestQueue = Volley.newRequestQueue(context.getApplicationContext()); /*new HurlStack()
		{
			@Override
			protected HttpURLConnection createConnection(URL url) throws IOException
			{
				HttpURLConnection connection = super.createConnection(url);
				// Fix for bug in Android runtime(!!!):
				// https://code.google.com/p/android/issues/detail?id=24672
				connection.setRequestProperty("Accept-Encoding", "");

				return connection;
			}
		});*/
		return requestQueue;
	}

	public <T> Request<T> add(Request<T> request, String tag)
	{
		request.setTag(tag);
		request.setRetryPolicy(new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
												      RETRY_TIMES,
													  DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
		return getRequestQueue().add(request);
	}

	public void cancel()
	{
		getRequestQueue().cancelAll(new RequestQueue.RequestFilter()
		{
			@Override
			public boolean apply(Request<?> request)
			{
				return true;
			}
		});
	}

	public void cancel(String tag)
	{
		getRequestQueue().cancelAll(tag);
	}

	public static synchronized NetworkHelper getInstance(Context context)
	{
		if (networkHelperInstance == null) networkHelperInstance = new NetworkHelper(context);
		return networkHelperInstance;
	}
}
