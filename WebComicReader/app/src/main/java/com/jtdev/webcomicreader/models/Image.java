package com.jtdev.webcomicreader.models;

import android.graphics.drawable.Drawable;

import java.net.URL;

/**
 * Copyright (c) 2016 James Ridey <james@snoopyaustralia.com>
 *
 * All rights reserved. No warranty, explicit or implicit, provided.
 *
 * File created on 1/27/16
 */
public class Image
{
	private Drawable drawable;
	private String imageUrl;
	private String altText;
	private String errorCode;

	public Image(String imageUrl)
	{
		this.imageUrl = imageUrl;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null) return false;
		else if (getClass() != obj.getClass()) return false;

		final Image other = (Image) obj;
		if (!other.getImageUrl().equals(getImageUrl())) return false;
		return true;
	}

	public Drawable getDrawable()
	{
		return drawable;
	}

	public void setDrawable(Drawable drawable)
	{
		this.drawable = drawable;
	}

	public String getImageUrl()
	{
		return imageUrl;
	}

	public void setImageUrl(String imageUrl)
	{
		this.imageUrl = imageUrl;
	}

	public String getAltText()
	{
		return altText;
	}

	public void setAltText(String altText)
	{
		this.altText = altText;
	}

	public String getErrorCode()
	{
		return errorCode;
	}

	public void setErrorCode(String errorCode)
	{
		this.errorCode = errorCode;
	}
}
