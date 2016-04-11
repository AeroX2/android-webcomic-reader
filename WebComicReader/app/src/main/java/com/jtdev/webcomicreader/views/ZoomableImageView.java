package com.jtdev.webcomicreader.views;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.animation.*;
import android.widget.ImageView;

import java.util.EnumSet;

/**
 * Copyright (c) 2016 James Ridey <james@snoopyaustralia.com>
 *
 * All rights reserved. No warranty, explicit or implicit, provided.
 *
 * File created on 1/24/16
 */
public class ZoomableImageView extends ImageView
{
	private static final float FLING_VELOCITY = 2400;

	private ZoomableImageCallback callback = null;

	private Bitmap previousBitmap;
	private ZoomImageGestureDetector zoomImageGestureDetector;
	private GestureDetectorCompat gestureDetector;
	private ScaleGestureDetector scaleGestureDetector;

	private boolean doubleTapped;
	private float oldScale;
	private float oldX;
	private float oldY;
	private float density;

	public ZoomableImageView(Context context)
	{
		super(context);
		if (context instanceof ZoomableImageCallback) initZoomableImage(context, callback);
		else initZoomableImage(context, null);
	}

	public ZoomableImageView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		if (context instanceof ZoomableImageCallback) initZoomableImage(context, callback);
		else initZoomableImage(context, null);
	}

	public ZoomableImageView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		if (context instanceof ZoomableImageCallback) initZoomableImage(context, callback);
		else initZoomableImage(context, null);
	}

	public ZoomableImageView(Context context, ZoomableImageCallback callback)
	{
		super(context);
		initZoomableImage(context, callback);
	}

	private void initZoomableImage(Context context, ZoomableImageCallback callback)
	{
		this.callback = callback;
		zoomImageGestureDetector = new ZoomImageGestureDetector();
		gestureDetector = new GestureDetectorCompat(context, zoomImageGestureDetector);
		scaleGestureDetector = new ScaleGestureDetector(context, zoomImageGestureDetector);
		doubleTapped = false;

		DisplayMetrics displayMetrics = new DisplayMetrics();
		((Activity) getContext()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		density = displayMetrics.density;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		gestureDetector.onTouchEvent(event);
		scaleGestureDetector.onTouchEvent(event);

		boolean scaled = false;
		float scale = 1.0f;

		float x = 0.0f;
		float y = 0.0f;
		float zoomX = 0.0f;
		float zoomY = 0.0f;

		final Matrix imageMatrix = new Matrix(getImageMatrix());

		float[] f = new float[9];
		imageMatrix.getValues(f);

		//Scale is the same for Y
		final float currentScale = f[Matrix.MSCALE_X];

		EnumSet<ZoomImageGestureDetector.Events> events = zoomImageGestureDetector.getCurrentEvents();
		if (events.contains(ZoomImageGestureDetector.Events.FLING))
		{
			if (currentScale <= oldScale)
			{
				float flingVelocity = zoomImageGestureDetector.getFlingVelocity() / density;
				//Log.d("debug", flingVelocity+"");
				if (callback != null)
				{
					if (flingVelocity < -FLING_VELOCITY) callback.nextImage();
					else if (flingVelocity > FLING_VELOCITY) callback.previousImage();
				}
			}
		}
		else if (events.contains(ZoomImageGestureDetector.Events.DOUBLE_TAP))
		{
			//scale = 2.0f;
			//scaled = true;

			final float doubleTapX = zoomImageGestureDetector.getDoubleTapX();
			final float doubleTapY = zoomImageGestureDetector.getDoubleTapY();

			final AccelerateDecelerateInterpolator accelerateDecelerateInterpolator = new AccelerateDecelerateInterpolator();
			final long startTime = System.currentTimeMillis();
			final long duration = 400;
			final float target = doubleTapped ? 0.5f : 2.0f;
			doubleTapped = !doubleTapped;

			Runnable scaleUp = new Runnable()
			{
				float k0 = 1.0f;
				float k1;

				@Override
				public void run()
				{
					float t = (float) (System.currentTimeMillis() - startTime) / duration;
					t = t > 1.0f ? 1.0f : t;

					float ratio = accelerateDecelerateInterpolator.getInterpolation(t);
					k1 = (1+ratio*(target-1));
					float tempScale = k1 / k0;
					k0 = k1;

					imageMatrix.postScale(tempScale, tempScale, doubleTapX, doubleTapY);
					setImageMatrix(imageMatrix);

					if (t < 1.0f) {
						post(this);
					}
				}
			};
			post(scaleUp);
		}
		else if (events.contains(ZoomImageGestureDetector.Events.PINCHING))
		{
			scale = zoomImageGestureDetector.getScale();
			scaled = true;

			zoomX = zoomImageGestureDetector.getFocusX();
			zoomY = zoomImageGestureDetector.getFocusY();

			if (currentScale * scale < oldScale)
			{
				scaled = false;

				imageMatrix.setScale(oldScale, oldScale);
				imageMatrix.postTranslate(oldX, oldY);
			}
		}
		if (events.contains(ZoomImageGestureDetector.Events.SCROLL))
		{
			x = -zoomImageGestureDetector.getScrollDistanceX();
			y = -zoomImageGestureDetector.getScrollDistanceY();
		}

		if (scaled) imageMatrix.postScale(scale, scale, zoomX, zoomY);
		if (x != 0 || y != 0) imageMatrix.postTranslate(x, y);

		zoomImageGestureDetector.clearCurrentEvents();

		setImageMatrix(imageMatrix);
		return true;
	}

	private void centerToFit()
	{
		Log.d("debug", "Centering to fit");

		//Borrowed from Android Source code ImageView: configureBounds
		Matrix mDrawMatrix = new Matrix();
		float scale;
		float dx;
		float dy;

		int dwidth = getDrawable().getIntrinsicWidth();
		int dheight = getDrawable().getIntrinsicHeight();

		int vwidth = getWidth() - getPaddingLeft() - getPaddingRight();
		int vheight = getHeight() - getPaddingTop() - getPaddingBottom();

		if (dwidth <= vwidth && dheight <= vheight) {
			//Modified original: scale = 1.0f;
			scale = (float) vwidth / (float) dwidth * 0.75f;
		} else {
			scale = Math.min((float) vwidth / (float) dwidth,
							 (float) vheight / (float) dheight);
		}

		dx = Math.round((vwidth - dwidth * scale) * 0.5f);
		dy = Math.round((vheight - dheight * scale) * 0.5f);

		mDrawMatrix.setScale(scale, scale);
		mDrawMatrix.postTranslate(dx, dy);

		setImageMatrix(mDrawMatrix);

		//New code
		oldScale = scale;
		oldX = dx;
		oldY = dy;
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom)
	{
		super.onLayout(changed, left, top, right, bottom);
		centerToFit();
	}

	@Override
	public void setImageDrawable(Drawable drawable)
	{
		if (getDrawable() instanceof BitmapDrawable) previousBitmap = ((BitmapDrawable) getDrawable()).getBitmap();
		super.setImageDrawable(drawable);
	}

	public Bitmap getPreviousBitmap()
	{
		return previousBitmap;
	}
}
