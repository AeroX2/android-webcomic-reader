package com.jtdev.webcomicreader.tasks;

import android.app.AlertDialog;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.jtdev.webcomicreader.MainActivity;
import com.jtdev.webcomicreader.R;
import com.jtdev.webcomicreader.helpers.NetworkHelper;
import com.jtdev.webcomicreader.models.Image;
import com.jtdev.webcomicreader.models.Webcomic;
import com.jtdev.webcomicreader.utils.Utils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;

/**
 * Copyright (c) 2016 James Ridey <james@snoopyaustralia.com>
 *
 * All rights reserved. No warranty, explicit or implicit, provided.
 *
 * File created on 1/26/16
 */
public class ImageExtractTask
{
	private final MainActivity main;
	private final Object lock = new Object();

	private static final SparseArray<Image> websiteCache = new SparseArray<>();
	private static final SparseArray<Point> sizesCache = new SparseArray<>();

	private static final int LOW = 1;
	private static final int MEDIUM = 2;
	private static final int HIGH = 3;

	private boolean canceled = false;

	public ImageExtractTask(MainActivity main)
	{
		this.main = main;
	}

	public void execute(final Webcomic webcomic)
	{
		onPreExecute();

		//this.webcomic = webcomic;

		if (websiteCache.get(webcomic.getCombinedUrl().hashCode()) != null)
		{
			Log.d("debug", "Getting image url from websiteCache");
			onPostExecute(websiteCache.get(webcomic.getCombinedUrl().hashCode()));
			return;
		}

		Log.d("debug", "Image extraction method: " + webcomic.getMethod());
		Log.d("debug", "Image extraction url: " + webcomic.getCombinedUrl());

		NetworkHelper.getInstance(main).add(new StringRequest(webcomic.getCombinedUrl(),
				new Response.Listener<String>()
				{
					@Override
					public void onResponse(final String response)
					{
						new Thread(new Runnable()
						{
							@Override
							public void run()
							{
								Document document = Jsoup.parse(response, webcomic.getHost());
								Image image = null;
								switch (webcomic.getMethod())
								{
									case "structureFind":
										image = structure(webcomic, document);
										break;
									case "id":
										image = id(webcomic, document);
										break;
									case "heuristic":
										image = heuristic(webcomic, document);
										break;
									case "neural":
										image = neural(webcomic, document);
										break;
								}
								if (image != null) websiteCache.put(webcomic.getCombinedUrl().hashCode(), image);

								final Image finalImage = image;
								main.runOnUiThread(new Runnable()
								{
									@Override
									public void run()
									{
										onPostExecute(finalImage);
									}
								});
							}
						}).start();
					}
				},
				new Response.ErrorListener()
				{
					@Override
					public void onErrorResponse(VolleyError error)
					{
						error.printStackTrace();
						Image image = new Image("");
						image.setErrorCode(main.getString(R.string.error_failed_finding_image));

						onPostExecute(image);
					}
				}), "extract");
	}


	private void onPreExecute()
	{
		NetworkHelper.getInstance(main).cancel();
		main.getImageSwitcher().setImageDrawable(new ColorDrawable(Color.WHITE));
		main.getProgressBar().setVisibility(View.VISIBLE);
		main.getCaptionButton().setVisibility(View.INVISIBLE);
		main.getErrorMessage().setText("");
	}

	private void onPostExecute(final Image image)
	{
		if (canceled) return;

		if (image != null)
		{
			if (image.getErrorCode() != null)
			{
				main.getProgressBar().setVisibility(View.INVISIBLE);
				main.getErrorMessage().setText(image.getErrorCode());
			}
			else
			{
				Log.d("debug", "Image extraction url: " + image.getImageUrl());
				new DownloadImageTask(main).execute(image);
				if (image.getAltText() != null)
				{
					Log.d("debug", "Image extraction alt text: " + image.getAltText());

					main.getCaptionButton().setOnClickListener(new View.OnClickListener()
					{
						@Override
						public void onClick(View v)
						{
							new AlertDialog.Builder(main)
									.setTitle("Alt Text")
									.setMessage(image.getAltText())
									.setNeutralButton("Done", null)
									.show();
						}
					});
					main.getCaptionButton().setVisibility(View.VISIBLE);
				}
			}
		}
		else
		{
			main.getProgressBar().setVisibility(View.INVISIBLE);
			main.getErrorMessage().setText(main.getString(R.string.error_no_image_found));
		}
	}

	private Image structure(Webcomic webcomic, Document document)
	{
		//String url = webcomic.getCombinedUrl();
		String[] structure = webcomic.getStructure().split(",");

		Log.d("debug", "Structure: " + Arrays.toString(structure));

		Element currentElement = Utils.structureFind(structure, document);

		if (currentElement != null)
		{
			String src = currentElement.attr("src");
			Log.d("debug", "Element src: " + src);
			if (!src.isEmpty())
			{
				Image image = new Image(Utils.combineUrl(webcomic.getUrl(), src));
				String altText = currentElement.attr("title");
				if (!altText.isEmpty()) image.setAltText(altText);
				return image;
			}
		}

		return null;
	}

	private Image id(Webcomic webcomic, Document document)
	{
		//String url = webcomic.getCombinedUrl();

		Element element = document.select("img[id=" + webcomic.getId() + "]").first();
		if (element == null) element = document.select("div[id=" + webcomic.getId() + "]").select("img[src]").first();

		if (element != null)
		{
			String src = element.attr("src");
			if (!src.isEmpty())
			{
				Image image = new Image(Utils.combineUrl(webcomic.getUrl(), src));
				String altText = element.attr("title");
				if (!altText.isEmpty()) image.setAltText(altText);
				return image;
			}
		}

		return null;
	}

	private Image heuristic(Webcomic webcomic, Document document)
	{
		Image image = null;

		URL websiteUrl = webcomic.getUrl();
		Elements elements = document.select("img[src]");
		SparseArray<Point> imageSizes = new SparseArray<>();

		long timer = System.currentTimeMillis();
		for (Element element : elements)
		{
			String imageUrl = Utils.combineUrl(websiteUrl, element.attr("src"));
			Point point = getImageSize(element, imageUrl);
			imageSizes.put(imageUrl.hashCode(), point);
		}
		Log.d("debug", "Time taken: " + (System.currentTimeMillis() - timer));

		int maxSize = 0;
		int maxPoints = 0;
		for (int i = 0; i < imageSizes.size(); i++)
		{
			Point point = imageSizes.valueAt(i);
			maxSize = Math.max(maxSize, point.x * point.y);
		}
		Log.d("debug", "Largest image size: " + maxSize);

		for (Element element : elements)
		{
			int currentPoints = 0;
			String src = element.attr("src");
			if (!src.startsWith("http://") || src.startsWith(websiteUrl.toString())) currentPoints += HIGH;
			String alt = element.attr("title");
			if (!alt.isEmpty()) currentPoints += HIGH;
			String id = element.attr("id");
			if (!id.isEmpty()) currentPoints += MEDIUM;

			if (element.parent().tagName().equals("div")) currentPoints += HIGH;

			//Point point = getImageSize(element, url);
			String imageUrl = Utils.combineUrl(websiteUrl, src);
			Point point = imageSizes.get(imageUrl.hashCode());

			if (point != null)
			{
				int width = point.x;
				int height = point.y;

				if (width > 100) currentPoints += LOW;
				if (height > 100) currentPoints += LOW;
				if (height / width > 3) currentPoints += HIGH;
				if (height * width == maxSize) currentPoints += HIGH;
			}

			if (currentPoints > maxPoints)
			{
				maxPoints = currentPoints;
				image = new Image(imageUrl);
				if (!alt.isEmpty()) image.setAltText(alt);
			}
		}

		if (image != null)
		{
			Log.d("debug", "Max points: " + maxPoints);
			Log.d("debug", "Best image src: " + image.getImageUrl());
		}

		return image;
	}

	@SuppressWarnings("all")
	private Image neural(Webcomic webcomic, Document document)
	{
		return null;
	}

	private Point getImageSize(Element image, final String imageUrl)
	{
		Log.d("debug", "Checking image: " + imageUrl);

		String widthString = image.attr("width");
		int width = Utils.parseNumber(widthString);

		String heightString = image.attr("height");
		int height = Utils.parseNumber(heightString);

		String css = image.attr("style");
		if (width == -1) width = Utils.parseCss(css, "width:");
		if (height == -1) height = Utils.parseCss(css, "height:");

		//Log.d("debug", "Image src: " + image.attr("src"));
		//Log.d("debug", "Image width via html or css: " + width);
		//Log.d("debug", "Image height via html or css: " + height);

		if (width != -1 && height != -1) return new Point(width, height);
		else
		{
			if (sizesCache.get(imageUrl.hashCode()) != null)
			{
				Log.d("debug", "Getting image size from sizesCache");
				return sizesCache.get(imageUrl.hashCode());
			}

			Log.d("debug", "Downloading image size: " + imageUrl);
			final Point[] point = {null};
			synchronized (lock)
			{
				final int finalHeight = height;
				final int finalWidth = width;
				new Thread(new Runnable()
				{
					@Override
					public void run()
					{
						try
						{
							URL url = new URL(imageUrl);
							InputStream inputStream = url.openStream();

							BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
							//noinspection deprecation
							decodeOptions.inPurgeable = true;
							decodeOptions.inJustDecodeBounds = true;

							BitmapFactory.decodeStream(inputStream, null, decodeOptions);
							point[0] = new Point(decodeOptions.outWidth, decodeOptions.outHeight);
							inputStream.close();

							sizesCache.put(imageUrl.hashCode(), point[0]);

							if (finalWidth != -1) point[0].x = finalWidth;
							if (finalHeight != -1) point[0].y = finalHeight;

							synchronized (lock)
							{
								lock.notify();
							}
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
					}
				}).start();

				try
				{
					lock.wait(5000);
					if (point[0] != null) return point[0];
				}
				catch (InterruptedException e) { e.printStackTrace(); }
			}

			/*RequestFuture<byte[]> future = RequestFuture.newFuture();
			NetworkHelper.getInstance(main).add(new InputStreamRequest(Request.Method.GET, imageUrl, future, future), "image_sizes");
			try
			{
				byte[] bytes = future.get(5, TimeUnit.SECONDS);
				InputStream inputStream = new ByteArrayInputStream(bytes);

				BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
				//noinspection deprecation
				decodeOptions.inPurgeable = true;
				decodeOptions.inJustDecodeBounds = true;

				BitmapFactory.decodeStream(inputStream, null, decodeOptions);
				Point point = new Point(decodeOptions.outWidth, decodeOptions.outHeight);
				inputStream.close();

				sizesCache.put(imageUrl.hashCode(), point);

				if (width != -1) point.x = width;
				if (height != -1) point.y = height;

				return point;
			}
			catch (Exception e) { e.printStackTrace(); }*/
		}
		return new Point(1,1);
	}

	public void cancel()
	{
		canceled = true;
	}

	public static void clearCache()
	{
		websiteCache.clear();
		sizesCache.clear();
	}
}
