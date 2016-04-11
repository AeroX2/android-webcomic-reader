package com.jtdev.webcomicreader;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.*;
import android.view.animation.Animation;
import android.widget.*;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.jtdev.webcomicreader.helpers.BackupManager;
import com.jtdev.webcomicreader.helpers.DatabaseHelper;
import com.jtdev.webcomicreader.helpers.NetworkHelper;
import com.jtdev.webcomicreader.models.Webcomic;
import com.jtdev.webcomicreader.tasks.ImageExtractTask;
import com.jtdev.webcomicreader.utils.NetworkBroadcastReceiver;
import com.jtdev.webcomicreader.utils.Utils;
import com.jtdev.webcomicreader.views.ZoomableImageCallback;
import com.jtdev.webcomicreader.views.ZoomableImageView;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, NetworkBroadcastReceiver.NetworkChangeListener, ZoomableImageCallback
{
	private NavigationView navigationView;
	private ImageSwitcher imageSwitcher;
	private ProgressBar progressBar;
	private TextView errorMessage;
	private ImageButton captionButton;

	private DatabaseHelper databaseHelper;
	private NetworkBroadcastReceiver networkReceiver;
	private SharedPreferences sharedPreference;
	private SharedPreferences.Editor sharedPreferenceEditor;

	private boolean internetAvaliable;
	private Webcomic webcomic;
	private Animation.AnimationListener animationListener;
	private ImageExtractTask currentTask;

	@SuppressLint("CommitPrefEdits") @Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
				this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
		drawer.setDrawerListener(toggle);
		toggle.syncState();

		navigationView = (NavigationView) findViewById(R.id.nav_view);
		navigationView.setNavigationItemSelectedListener(this);

		imageSwitcher = (ImageSwitcher) findViewById(R.id.comic_image);
		imageSwitcher.setFactory(new ViewSwitcher.ViewFactory()
		{
			@Override
			public View makeView()
			{
				ZoomableImageView zoomableImageView = new ZoomableImageView(MainActivity.this);
				zoomableImageView.setScaleType(ImageView.ScaleType.MATRIX);
				zoomableImageView.setLayoutParams(new ImageSwitcher.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
				return zoomableImageView;
			}
		});
		imageSwitcher.setImageDrawable(new ColorDrawable(Color.WHITE));

		animationListener = new Animation.AnimationListener()
		{
			Bitmap previousBitmap;

			@Override
			public void onAnimationStart(Animation animation)
			{
				Log.d("debug", "Animation started");
				ZoomableImageView zoomableImageView = (ZoomableImageView) imageSwitcher.getCurrentView();
				if (zoomableImageView != null) previousBitmap = zoomableImageView.getPreviousBitmap();
			}

			@Override
			public void onAnimationEnd(Animation animation)
			{
				Log.d("debug", "Animation ended");
				if (previousBitmap != null) previousBitmap.recycle();
			}

			@Override
			public void onAnimationRepeat(Animation animation)
			{
			}
		};

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

		captionButton = (ImageButton) findViewById(R.id.caption_button);
		progressBar = (ProgressBar) findViewById(R.id.progress_image);
		errorMessage = (TextView) findViewById(R.id.error_message);

		databaseHelper = DatabaseHelper.getInstance(this);

		networkReceiver = new NetworkBroadcastReceiver(this);
		//networkReceiver.register(this);

		sharedPreference = getSharedPreferences(getString(R.string.preferences), MODE_PRIVATE);
		sharedPreferenceEditor = sharedPreference.edit();

		String alias = sharedPreference.getString(getString(R.string.preference_left_off_alias), "");
		Log.d("debug", "Preference alias: " + alias);
		webcomic = databaseHelper.getWebcomic(alias);
		if (alias.isEmpty() || webcomic == null) errorMessage.setText(getString(R.string.error_no_loaded_webcomic));
		else
		{
			Log.d("debug","Updating from init");
			updateWebcomic(webcomic, true);
		}

		Log.d("debug", "Finished initialising");
	}

	@Override
	protected void onResume()
	{
		//On resume from ComicActivity refresh the menu
		Log.d("debug", "Resuming");
		super.onResume();
		networkReceiver.register(this);
		refreshMenu();
	}

	@Override
	protected void onPause()
	{
		Log.d("debug", "Pausing");
		super.onPause();
		networkReceiver.unregister(this);
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		//Stop every network task
		NetworkHelper.getInstance(this).cancel();
	}


	@Override
	public void onBackPressed()
	{
		//Close drawer on back button
		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		if (drawer.isDrawerOpen(GravityCompat.START)) drawer.closeDrawer(GravityCompat.START);
		else super.onBackPressed();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.action_offline:
				boolean checked = !item.isChecked();
				item.setChecked(checked);
				sharedPreferenceEditor.putBoolean(getString(R.string.preference_offline), checked);
				sharedPreferenceEditor.commit();
				BackupManager.getInstance(this).dataChanged();
				break;
			case R.id.action_clear_cache:
				ImageExtractTask.clearCache();
				//DownloadImageTask.clearCache();
				break;
			case R.id.action_settings:
				//TODO Settings activity
				Toast.makeText(this, "Not implemented", Toast.LENGTH_LONG).show();
				break;
			case R.id.action_help:
				Intent intent = new Intent(this, HelpActivity.class);
				startActivity(intent);
				break;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onNavigationItemSelected(MenuItem item)
	{
		// Handle navigation view item clicks here.
		int id = item.getItemId();

		if (id == R.id.add_button)
		{
			// Handle the add button action
			Intent intent = new Intent(this, ComicActivity.class);
			startActivity(intent);
		}
		else
		{
			String alias = item.getTitle().toString();
			webcomic = databaseHelper.getWebcomic(alias);
			if (webcomic != null) updateWebcomic(webcomic, false);
		}

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawer.closeDrawer(GravityCompat.START);
		return true;
	}

	@Override
	public void networkAvaliable()
	{
		Log.d("debug", "Network Available");

		internetAvaliable = true;
		if (webcomic == null) errorMessage.setText(getString(R.string.error_no_loaded_webcomic));
		else
		{
			Log.d("debug","Updating from network");
			webcomic = databaseHelper.getWebcomic(webcomic.getAlias());
			if (webcomic != null) updateWebcomic(webcomic, false);
		}
	}

	@Override
	public void networkUnavaliable()
	{
		Log.d("debug", "Network Unavailable");
		internetAvaliable = false;
	}

	private void refreshMenu()
	{
		SubMenu subMenu = navigationView.getMenu().findItem(R.id.comic_menu).getSubMenu();
		subMenu.clear();
		for (Webcomic webcomic : databaseHelper.getAllWebcomics())
		{
			final MenuItem menuItem = subMenu.add(webcomic.getAlias());
			menuItem.setActionView(R.layout.actionview_menu_item);
			View item = menuItem.getActionView().findViewById(R.id.delete_button);
			item.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					Toast.makeText(MainActivity.this, "Long click to delete", Toast.LENGTH_SHORT).show();
				}
			});
			item.setOnLongClickListener(new View.OnLongClickListener()
			{
				@Override
				public boolean onLongClick(View v)
				{
					databaseHelper.deleteWebcomic(menuItem.getTitle().toString());
					refreshMenu();
					return true;
				}
			});
		}
		if (subMenu.size() < 1) subMenu.add(getString(R.string.error_no_webcomics));
	}

	private void updateWebcomic(Webcomic webcomic, boolean forceCheck)
	{
		if (forceCheck) internetAvaliable = NetworkBroadcastReceiver.forceCheck(this);
		if (internetAvaliable)
		{
			if (currentTask != null) currentTask.cancel();
			currentTask = new ImageExtractTask(this);
			currentTask.execute(webcomic);
			sharedPreferenceEditor.putString(getString(R.string.preference_left_off_alias), webcomic.getAlias());
			sharedPreferenceEditor.apply();
			BackupManager.getInstance(this).dataChanged();
		}
		else errorMessage.setText(getString(R.string.error_no_internet));
	}

	public void previousImage()
	{
		if (webcomic == null) return;
		if (sharedPreference.getBoolean(getString(R.string.preference_offline),false))
		{
			//TODO Offline navigation
			Toast.makeText(this, "Not implemented", Toast.LENGTH_LONG).show();
		}
		else
		{
			if (webcomic.getPreviousStructure() == null)
				Toast.makeText(this, getString(R.string.error_webcomic_doesnt_support), Toast.LENGTH_LONG).show();
			else
			{
				imageSwitcher.setInAnimation(this, android.R.anim.slide_in_left);
				imageSwitcher.setOutAnimation(this, android.R.anim.slide_out_right);
				imageSwitcher.getOutAnimation().setAnimationListener(animationListener);
				getComicFromButton(webcomic, false);
			}
		}
	}

	public void nextImage()
	{
		if (webcomic == null) return;
		if (sharedPreference.getBoolean(getString(R.string.preference_offline),false))
		{
			//TODO Offline navigation
			Toast.makeText(this, "Not implemented", Toast.LENGTH_LONG).show();
		}
		else
		{
			if (webcomic.getNextStructure() == null)
				Toast.makeText(this, getString(R.string.error_webcomic_doesnt_support), Toast.LENGTH_LONG).show();
			else
			{
				imageSwitcher.setInAnimation(this, R.anim.slide_in_right);
				imageSwitcher.setOutAnimation(this, R.anim.slide_out_left);
				imageSwitcher.getOutAnimation().setAnimationListener(animationListener);
				getComicFromButton(webcomic, true);
			}
		}
	}

	public ImageSwitcher getImageSwitcher()
	{
		return imageSwitcher;
	}

	public ProgressBar getProgressBar()
	{
		return progressBar;
	}

	public TextView getErrorMessage()
	{
		return errorMessage;
	}

	public ImageButton getCaptionButton()
	{
		return captionButton;
	}

	private void getComicFromButton(final Webcomic webcomic, final boolean next)
	{
		//If image is downloaded
		if (((ZoomableImageView) imageSwitcher.getCurrentView()).getDrawable() instanceof BitmapDrawable || !errorMessage.getText().toString().isEmpty())
		{
			progressBar.setVisibility(View.VISIBLE);
			NetworkHelper.getInstance(this).add(new StringRequest(webcomic.getCombinedUrl(),
					new Response.Listener<String>()
					{
						@Override
						public void onResponse(String response)
						{
							String href = null;
							Document document = Jsoup.parse(response);

							String[] structure;
							if (next) structure = webcomic.getNextStructure().split(",");
							else structure = webcomic.getPreviousStructure().split(",");

							Log.d("debug", "Button structureFind: " + Arrays.toString(structure));

							Element currentElement = Utils.structureFind(structure, document);
							if (currentElement != null)
							{
								href = currentElement.attr("href");
								if (!href.isEmpty()) Log.d("debug", "Button href: " + href);
							}

							if (href != null)
							{
								webcomic.setHref(href);
								currentTask = new ImageExtractTask(MainActivity.this);
								currentTask.execute(webcomic);
							}
							else
							{
								progressBar.setVisibility(View.INVISIBLE);
								errorMessage.setText(getString(R.string.error_buttons_not_found));
							}
						}
					},
					new Response.ErrorListener()
					{
						@Override
						public void onErrorResponse(VolleyError error)
						{
							error.printStackTrace();
							progressBar.setVisibility(View.INVISIBLE);
							errorMessage.setText(getString(R.string.error_failed_button));
						}
					}),"get_button");
		}
	}
}
