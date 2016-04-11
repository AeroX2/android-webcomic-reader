package com.jtdev.webcomicreader.helpers;

import android.content.Context;
import android.graphics.Bitmap;

/**
 * Copyright (c) 2016 James Ridey <james@snoopyaustralia.com>
 *
 * All rights reserved. No warranty, explicit or implicit, provided.
 *
 * File created on 2/10/16
 */
public class DiskHelper
{
	final Context context;

	public DiskHelper(Context context, long maxSize)
	{
		this.context = context;
	}

	private String getCacheDir()
	{
		return "";
	}

	public void put(String key, Bitmap bitmap)
	{

	}

	public Bitmap get(String key)
	{
		return null;
	}

	public static DiskHelper getInstance(Context context)
	{
		return null;
	}
}
