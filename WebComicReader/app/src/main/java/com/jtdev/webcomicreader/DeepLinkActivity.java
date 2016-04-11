package com.jtdev.webcomicreader;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Copyright (c) 2016 James Ridey <james@snoopyaustralia.com>
 *
 * All rights reserved. No warranty, explicit or implicit, provided.
 *
 * File created on 2/12/16
 */
public class DeepLinkActivity extends AppCompatActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Intent intent = new Intent(this, MainActivity.class);
		startActivity(intent);
	}
}
