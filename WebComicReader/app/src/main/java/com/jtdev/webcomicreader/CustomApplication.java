package com.jtdev.webcomicreader;

import android.app.Application;
import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender;

/**
 * Copyright (c) 2016 James Ridey <james@snoopyaustralia.com>
 * <p/>
 * All rights reserved. No warranty, explicit or implicit, provided.
 * <p/>
 * File created on 2/12/16
 */

@ReportsCrashes(
	httpMethod = HttpSender.Method.PUT,
	reportType = HttpSender.Type.JSON,
	formUri = "http://jtteamdev.cloudant.com/acra-com-jtdev-webcomicreader/_design/acra-storage/_update/report",
	formUriBasicAuthLogin = "ingerndriciessideverseac",
	formUriBasicAuthPassword = "cff52d6c2d39af34db0cf71e3c69d077e72de664"
)
public class CustomApplication extends Application
{
	@Override
	public void onCreate()
	{
		super.onCreate();
		ACRA.init(this);
	}
}
