package com.jtdev.webcomicreader.models;

import com.jtdev.webcomicreader.utils.Utils;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Copyright (c) 2016 James Ridey <james@snoopyaustralia.com>
 *
 * All rights reserved. No warranty, explicit or implicit, provided.
 *
 * File created on 1/23/16
 */
public class Webcomic
{
	private URL url;
	private String alias;
	private String method;
	private String href;

	private String id;
	private String structure;
	private String previousStructure;
	private String nextStructure;

	public Webcomic()
	{
	}

	public Webcomic(URL url, String alias, String method)
	{
		this.url = url;
		this.alias = alias;
		this.method = method;
	}

	public String getAlias()
	{
		return alias;
	}

	public void setAlias(String alias)
	{
		this.alias = alias;
	}

	public URL getUrl()
	{
		return url;
	}

	public void setUrl(URL url)
	{
		this.url = url;
	}
	public String getUrlString()
	{
		return url.toString();
	}

	public void setUrlString(String website) throws MalformedURLException
	{
		this.url = new URL(website);
	}

	public String getHost()
	{
		return url.getHost();
	}

	public String getMethod()
	{
		return method;
	}

	public void setMethod(String method)
	{
		this.method = method;
	}

	private String getHref()
	{
		return href;
	}

	public void setHref(String href)
	{
		this.href = href;
	}

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public String getStructure()
	{
		return structure;
	}

	public void setStructure(String structure)
	{
		this.structure = structure;
	}

	public String getPreviousStructure()
	{
		return previousStructure;
	}

	public void setPreviousStructure(String previousStructure)
	{
		this.previousStructure = previousStructure;
	}

	public String getNextStructure()
	{
		return nextStructure;
	}

	public void setNextStructure(String nextStructure)
	{
		this.nextStructure = nextStructure;
	}

	public String getCombinedUrl()
	{
		if (href == null) return getUrl().toString();
		return Utils.combineUrl(getUrl(), getHref());
	}
}
