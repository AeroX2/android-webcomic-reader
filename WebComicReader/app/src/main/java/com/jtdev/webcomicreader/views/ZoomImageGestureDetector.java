package com.jtdev.webcomicreader.views;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import java.util.EnumSet;

/**
 * Copyright (c) 2016 James Ridey <james@snoopyaustralia.com>
 *
 * All rights reserved. No warranty, explicit or implicit, provided.
 *
 * File created on 1/24/16
 */
public class ZoomImageGestureDetector implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, ScaleGestureDetector.OnScaleGestureListener
{
	public enum Events
	{
		DOUBLE_TAP,
		PINCHING,
		SCROLL,
		FLING
	}

	private final EnumSet<Events> currentEvents = EnumSet.noneOf(Events.class);
	private float scale = 1f;
	private float flingVelocity = 0.0f;

	private float scrollDistanceX;
	private float scrollDistanceY;

	private float currentFocusX;
	private float currentFocusY;

	private float doubleTapX;
	private float doubleTapY;

	@Override
	public boolean onDown(MotionEvent e)
	{
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e)
	{

	}

	@Override
	public boolean onSingleTapUp(MotionEvent e)
	{
		return false;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
	{
		currentEvents.add(Events.SCROLL);

		scrollDistanceX = distanceX;
		scrollDistanceY = distanceY;
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e)
	{

	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
	{
		if (Math.abs(velocityX) > Math.abs(velocityY))
		{
			flingVelocity = velocityX;
			currentEvents.add(Events.FLING);
		}
		return false;
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e)
	{
		return false;
	}

	@Override
	public boolean onDoubleTap(MotionEvent e)
	{
		currentEvents.add(Events.DOUBLE_TAP);
		doubleTapX = e.getX();
		doubleTapY = e.getY();
		return false;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent e)
	{
		return false;
	}

	@Override
	public boolean onScale(ScaleGestureDetector detector)
	{
		currentEvents.add(Events.PINCHING);
		scale = detector.getScaleFactor();

		currentFocusX = detector.getFocusX();
		currentFocusY = detector.getFocusY();

		return true;
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector detector)
	{
		return true;
	}

	@Override
	public void onScaleEnd(ScaleGestureDetector detector)
	{

	}

	public EnumSet<Events> getCurrentEvents()
	{
		return currentEvents;
	}

	public void clearCurrentEvents()
	{
		currentEvents.clear();
	}

	public float getScale()
	{
		return scale;
	}

	public float getFlingVelocity()
	{
		return flingVelocity;
	}

	public float getScrollDistanceX()
	{
		return scrollDistanceX;
	}

	public float getScrollDistanceY()
	{
		return scrollDistanceY;
	}

	public float getFocusX()
	{
		return currentFocusX;
	}

	public float getFocusY()
	{
		return currentFocusY;
	}

	public float getDoubleTapX()
	{
		return doubleTapX;
	}

	public float getDoubleTapY()
	{
		return doubleTapY;
	}
}
