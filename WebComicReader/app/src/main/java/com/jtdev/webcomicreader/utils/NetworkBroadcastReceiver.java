package com.jtdev.webcomicreader.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Copyright (c) 2016 James Ridey <james@snoopyaustralia.com>
 *
 * All rights reserved. No warranty, explicit or implicit, provided.
 *
 * File created on 1/27/16
 */
public class NetworkBroadcastReceiver extends BroadcastReceiver
{
	private NetworkChangeListener listener;

	public interface NetworkChangeListener
	{
		void networkAvaliable();
		void networkUnavaliable();
	}

	//Has to be here for reasons
	public NetworkBroadcastReceiver() { }

	public NetworkBroadcastReceiver(NetworkChangeListener listener)
	{
		this.listener = listener;
	}

	@Override
	public void onReceive(Context context, Intent intent)
	{
		if (listener != null)
		{
			ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

			//Airplane mode, networkInfo will be null
			if (networkInfo != null && networkInfo.isConnected()) listener.networkAvaliable();
			else listener.networkUnavaliable();
		}
	}

	public static boolean forceCheck(Context context)
	{
		ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

		//Airplane mode, networkInfo will be null
		return (networkInfo != null && networkInfo.isConnected());
	}

	public void register(Context context)
	{
		context.registerReceiver(this, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
	}

	public void unregister(Context context)
	{
		context.unregisterReceiver(this);
	}
}
