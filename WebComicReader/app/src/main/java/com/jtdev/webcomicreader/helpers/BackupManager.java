package com.jtdev.webcomicreader.helpers;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

/**
 * Copyright (c) 2016 James Ridey <james@snoopyaustralia.com>
 *
 * All rights reserved. No warranty, explicit or implicit, provided.
 *
 * File created on 2/10/16
 */
public class BackupManager extends android.app.backup.BackupManager
{
	private static BackupManager backupManager;

	private BackupManager(Context context)
	{
		super(context);
	}

	@Override
	public void dataChanged()
	{
		super.dataChanged();
		Log.d("debug", "Data changed");
	}

	public static BackupManager getInstance(Context context)
	{
		if (backupManager == null) backupManager = new BackupManager(context.getApplicationContext());
		return backupManager;
	}
}
