package com.jtdev.webcomicreader;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.jtdev.webcomicreader.helpers.NetworkHelper;
import com.jtdev.webcomicreader.models.Webcomic;
import com.jtdev.webcomicreader.helpers.DatabaseHelper;
import com.jtdev.webcomicreader.utils.NetworkBroadcastReceiver;
import com.jtdev.webcomicreader.utils.Utils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class ComicActivity extends AppCompatActivity
{
	private static final int IMAGE = 0;
	private static final int PREVIOUS = 1;
	private static final int NEXT = 2;

	private final LinkedHashMap<CharSequence, String> METHODS = new LinkedHashMap<>();

	private Spinner methodSpinner;

	private EditText aliasEditText;
	private EditText urlEditText;
	private Switch supportsSwitch;
	private TextView errorMessage;
	private Button submitButton;

	private int currentType;
	private boolean websiteChecked;
	private Document document;
	private Element elementImage;
	private Element elementPrevious;
	private Element elementNext;
	private String findPrevious;

	private WebView webView;
	private AlertDialog dialog;
	private DatabaseHelper databaseHelper;

	public ComicActivity()
	{
		super();
		METHODS.put("----STATIC WEBSITE----", "static");
		METHODS.put("Structure", "structureFind");
		METHODS.put("ID", "id");
		METHODS.put("----DYNAMIC WEBSITE----", "dynamic");
		METHODS.put("Heuristic", "heuristic");
		//METHODS.put("Neural Network", "neural");
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_comic);

		setupSpinner();
		setupFields();

		submitButton = (Button) findViewById(R.id.submit_button);
		submitButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Object[] objects = checkFields();
				if (objects != null) addWebcomic(objects);
			}
		});
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		//Stop every network task
		NetworkHelper.getInstance(this).cancel();
	}

	private void setupSpinner()
	{
		//Setup spinner
		methodSpinner = (Spinner) findViewById(R.id.method_spinner);

		CharSequence[] methods = METHODS.keySet().toArray(new CharSequence[METHODS.size()]);
		ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, R.layout.support_simple_spinner_dropdown_item, methods)
		{
			@Override
			public boolean isEnabled(int position)
			{
				CharSequence c = getItem(position);
				if (c.subSequence(0,Math.min(4, c.length())).equals("----")) return false;

				//TODO Write neural network method
				return true;
			}

			@Override
			public View getDropDownView(int position, View convertView, ViewGroup parent)
			{
				View view = super.getDropDownView(position, convertView, parent);
				TextView textView = (TextView) view;
				if (isEnabled(position)) textView.setTextColor(Color.BLACK);
				else  textView.setTextColor(Color.GRAY);
				return textView;
			}
		};
		adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
		methodSpinner.setAdapter(adapter);
		methodSpinner.setSelection(4);
	}

	private void setupFields()
	{
		urlEditText = (EditText) findViewById(R.id.website);
		aliasEditText = (EditText) findViewById(R.id.alias);
		supportsSwitch = (Switch) findViewById(R.id.supports_switch);
		errorMessage = (TextView) findViewById(R.id.error_message);

		databaseHelper = DatabaseHelper.getInstance(this);

		webView = new WebView(this);
		webView.getSettings().setJavaScriptEnabled(false);
		webView.setWebViewClient(new WebViewClient()
		{
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url)
			{
				//Log.d("debug", "Overriding Url loading: " + currentType.toString());
				createWebView(url, currentType);
				//view.loadUrl(url);
				return true;
			}
		});

		dialog = new AlertDialog.Builder(this).create();
		dialog.setView(webView);

		websiteChecked = false;
		elementImage = null;
		elementPrevious = null;
		elementNext = null;
		findPrevious = "";
	}

	/**
	 * Checks all fields (url, alias and method) of the Activity are filled in correctly 
	 * Check url is not null and exists on the internet
	 * Check alias is not empty and doesn't already exist in the database
	 * @returns Object[]{url, alias, method} if all the fields are valid else null
	 */
	private Object[] checkFields()
	{
		URL url = null;
		try
		{
			String urlText = urlEditText.getText().toString();
			if (!urlText.startsWith("http://") && !urlText.startsWith("https://")) urlText = "http://" + urlText;
			url = new URL(urlText);
		}
		catch (MalformedURLException e) { e.printStackTrace(); }

		String alias = aliasEditText.getText().toString();
		String method = METHODS.get(methodSpinner.getSelectedItem().toString());

		if (url == null) errorMessage.setText(getString(R.string.error_url_invalid));
		else if (alias.isEmpty()) errorMessage.setText(getString(R.string.error_alias_empty));
		else if (databaseHelper.getWebcomic(alias) != null) errorMessage.setText(getString(R.string.error_alias_exists));
		else if (NetworkBroadcastReceiver.forceCheck(this) && !websiteChecked) checkWebsite(url.toString());
		else return new Object[]{url, alias, method};

		return null;
	}

	private void addWebcomic(Object[] objects)
	{
		URL url = (URL) objects[0];
		String urlString = url.toString();
		String alias = (String) objects[1];
		String method = (String) objects[2];

		//ID and Structure
		if ((method.equals("id") || method.equals("structureFind")) && elementImage == null)
		{
			currentType = IMAGE;
			dialog.setTitle("Please choose the image by long clicking on it");
			createWebView(urlString, currentType);
			return;
		}
		else if (method.equals("static") || method.equals("dynamic") || method.equals("neural"))
		{
			errorMessage.setText(getString(R.string.error_invalid_method));
			return;
		}

		//Previous and next button
		if (supportsSwitch.isChecked())
		{
			if (elementPrevious == null)
			{
				currentType = PREVIOUS;
				createWebView(urlString, currentType);
				return;
			}
			else if (elementNext == null)
			{
				currentType = NEXT;
				createWebView(urlString, currentType);
				return;
			}
		}

		Log.d("debug", "Adding webcomic to database: " + urlString);
		Webcomic webcomic = new Webcomic(url, alias, method);
		if (method.equals("id"))
		{
			String id = elementImage.attr("id");
			if (id.isEmpty())
			{
				if (elementImage.parent().tagName().equals("div")) id = elementImage.parent().attr("id");
			}
			if (id.isEmpty())
			{
				errorMessage.setText(getString(R.string.error_no_image_id));
				return;
			}
			Log.d("debug", "Image id: " + id);
			webcomic.setId(id);
		}
		else if (method.equals("structureFind"))
		{
			String imageStructure = Utils.structureSave(elementImage);
			Log.d("debug", "Image structureFind: " + imageStructure);
			if (!imageStructure.isEmpty()) webcomic.setStructure(imageStructure);
		}

		if (supportsSwitch.isChecked())
		{
			String previousStructure = Utils.structureSave(elementPrevious);
			Log.d("debug", "Previous button structureFind: " + previousStructure);
			if (!previousStructure.isEmpty()) webcomic.setPreviousStructure(previousStructure);


			String nextStructure = Utils.structureSave(elementNext);
			Log.d("debug", "Next button structureFind: " + nextStructure);
			if (!nextStructure.isEmpty()) webcomic.setNextStructure(nextStructure);

			if (previousStructure.isEmpty() && nextStructure.isEmpty())
			{
				errorMessage.setText(getString(R.string.error_no_link_id));
				return;
			}
		}

		databaseHelper.createWebcomic(webcomic);
		finish();
	}

	private View.OnLongClickListener getListener(final int type)
	{
		return new View.OnLongClickListener()
		{
			@Override
			public boolean onLongClick(View v)
			{
				Handler handler = new Handler()
				{
					@Override
					public void handleMessage(Message msg)
					{
						String extra;
						if (type == IMAGE) extra = (String) msg.getData().get("src");
						else extra = (String) msg.getData().get("url");

						Log.d("debug", msg.getData().toString());
						String find = null;
						if (extra != null && !extra.isEmpty())
						{
							if (extra.endsWith("/")) extra = Utils.removeLastChar(extra);
							find = extra.substring(extra.lastIndexOf("/") + 1, extra.length());
							find = find.substring(find.lastIndexOf("?") + 1, find.length());
						}

						if (find != null && !find.isEmpty())
						{
							if (!findPrevious.equals(find))
							{
								findPrevious = find;
								Log.d("debug", "Find: " + find);
								Toast.makeText(ComicActivity.this, "Long click again to confirm", Toast.LENGTH_SHORT).show();
							}
							else
							{
								//TODO Could fail because first selects first element in html not on page, but why would any page contain the webcomic twice?
								if (type == IMAGE)
								{
									elementImage = document.select("img[src*=" + find + "]").first();
									if (elementImage == null) Toast.makeText(ComicActivity.this, "Could not find image", Toast.LENGTH_LONG).show();
								}
								else if (type == PREVIOUS)
								{
									elementPrevious = document.select("a[href*="+find+"]").first();
									if (elementPrevious == null) Toast.makeText(ComicActivity.this, "Could not find link", Toast.LENGTH_LONG).show();
								}
								else
								{
									elementNext = document.select("a[href*="+find+"]").first();
									if (elementNext == null) Toast.makeText(ComicActivity.this, "Could not find link", Toast.LENGTH_LONG).show();
								}

								submitButton.callOnClick();
								dialog.dismiss();
							}
						}
						else Toast.makeText(ComicActivity.this, (type == IMAGE) ? "Not a image" : "Not a link", Toast.LENGTH_LONG).show();
					}
				};
				webView.requestFocusNodeHref(handler.obtainMessage());

				return true;
			}
		};
	}

	private void checkWebsite(final String website)
	{
		NetworkHelper.getInstance(ComicActivity.this).add(new StringRequest(website,
		new Response.Listener<String>()
		{
			@Override
			public void onResponse(String response)
			{
				websiteChecked = true;
				submitButton.callOnClick();
			}
		},
		new Response.ErrorListener()
		{
			@Override
			public void onErrorResponse(VolleyError error)
			{
				errorMessage.setText(getString(R.string.error_website_doesnt_exist));
			}
		}
		),"check");
	}

	private void createWebView(final String website, final int type)
	{
		if (NetworkBroadcastReceiver.forceCheck(this))
		{
			NetworkHelper.getInstance(ComicActivity.this).add(new StringRequest(website,
			new Response.Listener<String>()
			{
				@Override
				public void onResponse(String response)
				{
					document = Jsoup.parse(response);

					//printLongMessage(document.html());

					if (type == IMAGE) dialog.setTitle("Please choose the image by long clicking on it");
					else if (type == PREVIOUS) dialog.setTitle("Please choose the previous button by long clicking on it");
					else if (type == NEXT) dialog.setTitle("Please choose the next button by long clicking on it");

					webView.loadDataWithBaseURL(website, document.html(), "text/html", "utf-8", null);
					webView.setOnLongClickListener(getListener(type));

					dialog.show();
				}
			},
			new Response.ErrorListener()
			{
				@Override
				public void onErrorResponse(VolleyError error)
				{
					error.printStackTrace();
					errorMessage.setText(getString(R.string.error_failed_network));
				}
			}
			),"webview");
		}
		else errorMessage.setText(getString(R.string.error_no_internet_feature));
	}

	/*public static void printLongMessage(String str)
	{
		if(str.length() > 4000)
		{
			Log.d("debug", str.substring(0, 4000));
			printLongMessage(str.substring(4000));
		}
		else Log.d("debug", str);
	}*/
}
