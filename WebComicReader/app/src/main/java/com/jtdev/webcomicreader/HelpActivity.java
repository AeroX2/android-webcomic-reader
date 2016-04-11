package com.jtdev.webcomicreader;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.jtdev.webcomicreader.helpers.DatabaseHelper;
import com.jtdev.webcomicreader.models.Webcomic;
import com.jtdev.webcomicreader.views.ZoomableImageCallback;
import com.jtdev.webcomicreader.views.ZoomableImageView;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;

public class HelpActivity extends AppCompatActivity implements ZoomableImageCallback
{
	private static final int[] HELP_IMAGES = {R.drawable.help_1, R.drawable.help_2, R.drawable.help_3, R.drawable.help_4};
	private static LinkedHashMap<String,String[]> WEBCOMICS = new LinkedHashMap<>();

	private int currentImage = 0;
	private DatabaseHelper databaseHelper;
	private ImageSwitcher imageSwitcher;

	private int reqWidth;
	private int reqHeight;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_help);

		databaseHelper = DatabaseHelper.getInstance(this);
		WEBCOMICS.put("XKCD", new String[]{"http://xkcd.com", "XKCD", "id", "comic", null, "div:1:box:middleContainer, ul:1:comicNav:, li:1::, a:0::", "div:1:box:middleContainer, ul:1:comicNav:, li:3::, a:0::"});
		WEBCOMICS.put("SMBC", new String[]{"http://smbc-comics.com", "SMBC", "id", "comicbody", null, "div:1::mainwrap, div:0::comicleft, div:1:nav:buttonwidth, a:1:prev:", "div:1::mainwrap, div:0::comicleft, div:1:nav:buttonwidth, a:3:next:"});
		WEBCOMICS.put("Cyanide And Happiness", new String[]{"http://explosm.net", "Cyanide and Happiness", "structure", null, "div:0:row space:,div:0:small-12 medium-12 large-12 columns:,a:0::,img:0::featured-comic", "div:0:medium-12 columns end:,ul:0:small-block-grid-5:,li:1::,a:0:previous-comic :", "div:0:small-12 medium-8 large-8 columns end:,ul:0:small-block-grid-5:,li:3::,a:0:next-comic :"});
		WEBCOMICS.put("Ctrl Alt Delete", new String[]{"http://www.cad-comic.com/cad", "Ctrl Alt Delete", "structure", null, "body:1::,div:1::page,div:0::content,img:2::", "div:1::page,div:0::content,div:1:navigation:,a:0:nav-back:", "div:0::menu,div:1:tooltip\t:,div:1:tooltip-content:,a:1::"});

		final Spinner comicSpinner = (Spinner) findViewById(R.id.comic_spinner);

		CharSequence[] methods = WEBCOMICS.keySet().toArray(new CharSequence[WEBCOMICS.size()]);
		ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, methods);
		adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
		comicSpinner.setAdapter(adapter);

		findViewById(R.id.add_comic).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				String[] values = WEBCOMICS.get(comicSpinner.getSelectedItem().toString());
				if (values != null) addWebcomic(values);
			}
		});

		imageSwitcher = (ImageSwitcher) findViewById(R.id.help_imageswitcher);
		imageSwitcher.setFactory(new ViewSwitcher.ViewFactory()
		{
			@Override
			public View makeView()
			{
				ZoomableImageView zoomableImageView = new ZoomableImageView(HelpActivity.this);
				zoomableImageView.setScaleType(ImageView.ScaleType.MATRIX);
				zoomableImageView.setLayoutParams(new ImageSwitcher.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
				return zoomableImageView;
			}
		});
		imageSwitcher.setImageDrawable(new ColorDrawable(Color.WHITE));

		//Done in onWindowFocusChanged due to getWidth and getHeight returning 0
		//imageSwitcher.setImageDrawable(decodeSampledBitmapFromResource(HELP_IMAGES[currentImage], reqWidth, reqHeight));

		ImageButton leftArrowButton = (ImageButton) findViewById(R.id.left_arrow_button);
		leftArrowButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				previousImage();
			}
		});

		ImageButton rightArrowButton = (ImageButton) findViewById(R.id.right_arrow_button);
		rightArrowButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				nextImage();
			}
		});
	}

	private void addWebcomic(String[] values)
	{
		String alias = values[1];
		if (databaseHelper.getWebcomic(alias) == null)
		{
			try
			{
				URL url = new URL(values[0]);
				Log.d("debug", "Adding webcomic to database: " + url.toString());
				Webcomic webcomic = new Webcomic(url, alias, values[2]);
				webcomic.setId(values[3]);
				webcomic.setStructure(values[4]);
				webcomic.setPreviousStructure(values[5]);
				webcomic.setNextStructure(values[6]);
				databaseHelper.createWebcomic(webcomic);

				Toast.makeText(this, "Added webcomic: " + alias, Toast.LENGTH_LONG).show();
			}
			catch (MalformedURLException e) { e.printStackTrace(); }
		}
		else Toast.makeText(this, getString(R.string.error_webcomic_already_added), Toast.LENGTH_LONG).show();
	}

	private BitmapDrawable decodeSampledBitmapFromResource(int id, int reqWidth, int reqHeight)
	{
		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		//noinspection deprecation
		options.inPurgeable = true;
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(getResources(), id, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;

		return new BitmapDrawable(getResources(), BitmapFactory.decodeResource(getResources(), id, options));
	}

	private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight)
	{
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth)
		{

			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			// Calculate the largest inSampleSize value that is a power of 2 and keeps both
			// height and width larger than the requested height and width.
			while ((halfHeight / inSampleSize) > reqHeight &&
					(halfWidth / inSampleSize) > reqWidth)
			{
				inSampleSize *= 2;
			}
		}

		return inSampleSize;
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus)
	{
		super.onWindowFocusChanged(hasFocus);
		if (!(((ZoomableImageView) imageSwitcher.getCurrentView()).getDrawable() instanceof BitmapDrawable))
		{
			reqWidth = imageSwitcher.getWidth();
			reqHeight = imageSwitcher.getHeight();
			imageSwitcher.setImageDrawable(decodeSampledBitmapFromResource(HELP_IMAGES[currentImage], reqWidth, reqHeight));
		}

	}

	@Override
	public void previousImage()
	{
		currentImage--;
		if (currentImage < 0) currentImage = HELP_IMAGES.length-1;
		imageSwitcher.setImageDrawable(decodeSampledBitmapFromResource(HELP_IMAGES[currentImage], reqWidth, reqHeight));
	}

	@Override
	public void nextImage()
	{
		currentImage++;
		if (currentImage > HELP_IMAGES.length-1) currentImage = 0;
		imageSwitcher.setImageDrawable(decodeSampledBitmapFromResource(HELP_IMAGES[currentImage], reqWidth, reqHeight));
	}
}
