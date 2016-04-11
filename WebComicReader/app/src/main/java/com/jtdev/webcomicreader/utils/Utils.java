package com.jtdev.webcomicreader.utils;

import android.util.Log;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Copyright (c) 2016 James Ridey <james@snoopyaustralia.com>
 *
 * All rights reserved. No warranty, explicit or implicit, provided.
 *
 * File created on 1/30/16
 */
public class Utils
{
	private static String removeFirstChar(String input)
	{
		return input.substring(1, input.length());
	}
	public static String removeLastChar(String input)
	{
		return input.substring(0, input.length()-1);
	}

	public static String combineUrl(URL rootUrl, String imageUrl)
	{
		try
		{
			int lastSlash = imageUrl.lastIndexOf("/");
			if (lastSlash != -1) imageUrl = imageUrl.substring(0,lastSlash) + imageUrl.substring(lastSlash).replace(" ","%20");
			return new URL(rootUrl, imageUrl).toString();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return "";
	}

	public static int parseNumber(String string)
	{
		try
		{
			return Integer.parseInt(string);
		}
		catch (Exception e)
		{
			return -1;
		}
	}

	public static int parseCss(String css, String toFind)
	{
		int index = css.indexOf(toFind)+toFind.length()-1;
		String number = "";
		if (index != -1)
		{
			while (index < css.length())
			{
				index++;
				char c = css.charAt(index);
				if (Character.isDigit(c)) number += c;
				else if (!Character.isSpaceChar(c)) break;
			}
			try
			{
				if (!number.isEmpty()) return parseNumber(number);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		return -1;
	}

	public static Element structureFind(String[] structure, Document document)
	{
		String[] split = structure[0].split(":");
		Log.d("debug", Arrays.toString(split));
		String tagName = split[0];
		int index;
		String classes;
		String id;

		for (Element element : document.select(tagName))
		{
			//int index_e = Utils.parseNumber(split[1]);
			//element = element.parent().child(index_e);
			Log.d("debug", "New tag");
			Log.d("debug", "Tags: " + document.select(tagName).size());

			for (int indexS = 1; indexS < structure.length; indexS++)
			{
				split = structure[indexS].split(":");
				Log.d("debug", Arrays.toString(split));
				tagName = split[0];
				index = Utils.parseNumber(split[1]);
				classes = "";
				if (split.length > 2) classes = split[2];
				id = "";
				if (split.length > 3) id = split[3];

				try { element = element.child(index); }
				catch (Exception e) { break; }

				Log.d("debug", element.tagName());
				Log.d("debug", element.attr("class"));
				Log.d("debug", element.attr("id"));

				if (!element.tagName().equals(tagName)) break;
				else if (!classes.isEmpty())
				{
					if (!element.attr("class").isEmpty())
					{
						classes = classes.replace(" ", "|");
						if (classes.endsWith("|")) classes = removeLastChar(classes);
						if (!Pattern.matches(".*(" + classes + ")+.*", element.attr("class"))) break;
					}
				}
				else if (!id.isEmpty() && !element.attr("id").equals(id)) break;
				if (indexS >= structure.length-1) return element;
			}
		}
		return null;
	}

	public static String structureSave(Element element)
	{
		int MAX_PARENT_STRUCTURE = 4;
		String structure = "";
		for (int i = 0; i < MAX_PARENT_STRUCTURE; i++)
		{
			if (element != null && !element.tagName().equals("#root"))
			{
				structure = element.tagName()+":"+
						element.elementSiblingIndex()+":"+
						element.attr("class")+":"+
						element.attr("id")+","+
						structure;
			}
			else break;
			element = element.parent();
		}
		structure = Utils.removeLastChar(structure);
		return structure;
	}
}
