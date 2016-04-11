package com.jtdev.webcomicreader.helpers;

import android.app.backup.*;
import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import com.jtdev.webcomicreader.R;

import java.io.File;
import java.io.IOException;

/**
 * Copyright (c) 2016 James Ridey <james@snoopyaustralia.com>
 *
 * All rights reserved. No warranty, explicit or implicit, provided.
 *
 * File created on 2/9/16
 */
public class BackupAgent extends BackupAgentHelper
{
	static final String PREFERENCES_BACKUP_KEY = "preferences";
	static final String FILES_BACKUP_KEY = "files";

	public static final Object lock = new Object();

	// Allocate a helper and add it to the backup agent
	@Override
	public void onCreate()
	{
		SharedPreferencesBackupHelper preferencesHelper = new SharedPreferencesBackupHelper(this, getString(R.string.preferences));
		addHelper(PREFERENCES_BACKUP_KEY, preferencesHelper);

		FileBackupHelper dbHelper = new FileBackupHelper(this, DatabaseHelper.DATABASE_NAME);
		addHelper(FILES_BACKUP_KEY, dbHelper);
	}

	@Override
	public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState) throws IOException
	{
		synchronized (lock)
		{
			super.onRestore(data, appVersionCode, newState);
		}
	}

	@Override
	public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) throws IOException
	{
		synchronized (lock)
		{
			super.onBackup(oldState, data, newState);
		}
	}

	@Override
	public File getFilesDir()
	{
		File path = getDatabasePath(DatabaseHelper.DATABASE_NAME);
		return path.getParentFile();
	}
}
