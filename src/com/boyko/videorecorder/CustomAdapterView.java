/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.boyko.videorecorder;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;

public class CustomAdapterView extends ViewGroup {

	private static final int BIG_MOVE_DISTANCE = 125;
	/**
	 * Indicates that we are not in the middle of a touch gesture
	 */
	static final int TOUCH_MODE_REST = -1;

	/**
	 * Indicates we just received the touch event and we are waiting to see if
	 * the it is a tap or a scroll gesture.
	 */
	static final int TOUCH_MODE_DOWN = 0;

	/**
	 * Indicates the touch has been recognized as a tap and we are now waiting
	 * to see if the touch is a longpress
	 */
	static final int TOUCH_MODE_TAP = 1;

	/**
	 * Indicates we have waited for everything we can wait for, but the user's
	 * finger is still down
	 */
	static final int TOUCH_MODE_DONE_WAITING = 2;

	/**
	 * Represents an invalid position. All valid positions are in the range 0 to
	 * 1 less than the number of items in the current adapter.
	 */
	public static final int INVALID_POSITION = -1;

	public interface OnItemTouchListener {
		boolean onItemClick(CustomAdapterView parent, View view, int position, long id);
		boolean onItemLongClick(CustomAdapterView parent, View view, int position, long id);
		boolean onItemStopTouch();
		boolean onCancelTouch();
	}

	private BaseAdapter adapter;
	private OnItemTouchListener itemClickListener;

	/**
	 * One of TOUCH_MODE_REST, TOUCH_MODE_DOWN, TOUCH_MODE_TAP,
	 * TOUCH_MODE_SCROLL, or TOUCH_MODE_DONE_WAITING
	 */
	private int mTouchMode = TOUCH_MODE_REST;
	/**
	 * The X value associated with the the down motion event
	 */
	private int mMotionPosition;
	/**
	 * Rectangle used for hit testing children
	 */
	private Rect mTouchFrame;
	/**
	 * The last CheckForLongPress runnable we posted, if any
	 */
	private CheckForLongPress mPendingCheckForLongPress;

	/**
	 * The last CheckForTap runnable we posted, if any
	 */
	private Runnable mPendingCheckForTap;
	/**
	 * Delayed action for touch mode.
	 */
	private Runnable mTouchModeReset;
	/**
	 * Acts upon click
	 */
	private PerformClick mPerformClick;
	/**
	 * Last touched position
	 */
	private int mLastX;
	/**
	 * Last touched position
	 */
	private int mLastY;

	private boolean isAttach;

	private DataSetObserver dataSetObserver;

	private boolean isDirty;

	public CustomAdapterView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public CustomAdapterView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CustomAdapterView(Context context) {
		super(context);
	}

	public void setAdapter(BaseAdapter adapter) {
		if (this.adapter != null) {
			this.adapter.unregisterDataSetObserver(dataSetObserver);
		}

		dataSetObserver = new AdapterDataSetObserver();
		adapter.registerDataSetObserver(dataSetObserver);

		this.adapter = adapter;
	}

	public BaseAdapter getAdapter() {
		return adapter;
	}

	public OnItemTouchListener getItemClickListener() {
		return itemClickListener;
	}

	public void setItemClickListener(OnItemTouchListener itemClickListener) {
		this.itemClickListener = itemClickListener;
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if (adapter == null)
			return;
		if (isDirty == true && getChildCount() > 0) {
			removeAllViews();
		} else if (getChildCount() < adapter.getCount()) {
			int position = 0;
			int bottomEdge = 0;
			while (bottomEdge < getHeight() && position < adapter.getCount()) {
				int rightEdge = 0;
				int measuredHeight = 0;
				while (rightEdge < getWidth() && position < adapter.getCount()) {
					View newChild = adapter.getView(position, null, this);
					addAndMeasureChild(newChild, position);
					rightEdge += newChild.getMeasuredWidth();
					measuredHeight = newChild.getMeasuredHeight();
					position++;
				}
				bottomEdge += measuredHeight;
			}
		}
		layoutChildren();
		isDirty = false;
	}

	/**
	 * Adds a view as a child view and takes care of measuring it
	 * 
	 * @param child
	 *            The view to add
	 */
	private void addAndMeasureChild(View child, int position) {
		LayoutParams params = child.getLayoutParams();
		if (params == null) {
			params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		}
		addViewInLayout(child, position, params, true);

		int itemWidth = getWidth() / 3;
		int itemHeight = getHeight() / 3;
		child.measure(MeasureSpec.EXACTLY | itemWidth, MeasureSpec.EXACTLY | itemHeight);
	}

	/**
	 * Positions the children at the "correct" positions
	 */
	private void layoutChildren() {
		View child = getChildAt(0);
		int numCol = getWidth() / child.getMeasuredWidth();

		for (int index = 0; index < getChildCount(); index++) {
			child = getChildAt(index);
			int width = child.getMeasuredWidth();
			int height = child.getMeasuredHeight();
			int mod = index / numCol;
			int left = (index - mod * numCol) * width;
			int top = mod * height;

			child.layout(left, top, left + width, top + height);
		}
		
		Log.d(VIEW_LOG_TAG, "layoutChildren");
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		isAttach = true;
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		isAttach = false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (!isEnabled()) {
			// A disabled view that is clickable still consumes the touch
			// events, it just doesn't respond to them.
			return isClickable() || isLongClickable();
		}

		if (!isAttach)
			return false;

		final int actionMasked = ev.getActionMasked();
		switch (actionMasked) {
		case MotionEvent.ACTION_DOWN: {
			Log.w(VIEW_LOG_TAG, "ACTION_DOWN");
			onTouchDown(ev);
			break;
		}

		case MotionEvent.ACTION_UP: {
			Log.w(VIEW_LOG_TAG, "ACTION_UP");
			onTouchUp(ev);
			break;
		}

		case MotionEvent.ACTION_CANCEL: {
			Log.w(VIEW_LOG_TAG, "ACTION_CANCEL");
			onTouchCancel();
			break;
		}
		case MotionEvent.ACTION_MOVE: {
			Log.w(VIEW_LOG_TAG, "ACTION_MOVE");
			onTouchMove(ev);
			break;
		}
		}

		return true;
	}

	private void onTouchDown(MotionEvent ev) {
		final int x = (int) ev.getX();
		final int y = (int) ev.getY();
		int motionPosition = pointToPosition(x, y);

		if ((motionPosition >= 0) && getAdapter().isEnabled(motionPosition)) {
			// User clicked on an actual view (and was not stopping a
			// fling). It might be a click or a scroll. Assume it is a
			// click until proven otherwise.
			mTouchMode = TOUCH_MODE_DOWN;

			// FIXME Debounce
			if (mPendingCheckForTap == null) {
				mPendingCheckForTap = new CheckForTap();
			}

			postDelayed(mPendingCheckForTap, ViewConfiguration.getTapTimeout());

			mMotionPosition = motionPosition;
		}

		mLastX = x;
		mLastY = y;

	}

	private void onTouchUp(MotionEvent ev) {

		if (itemClickListener != null)
			itemClickListener.onItemStopTouch();

		switch (mTouchMode) {
		case TOUCH_MODE_DOWN:
		case TOUCH_MODE_TAP:
		case TOUCH_MODE_DONE_WAITING: {
			final int motionPosition = mMotionPosition;
			final View child = getChildAt(motionPosition);
			if (child != null) {
				if (mTouchMode != TOUCH_MODE_DOWN) {
					child.setPressed(false);
				}

				child.dispatchTouchEvent(ev);

				if (mPerformClick == null) {
					mPerformClick = new PerformClick();
				}

				final PerformClick performClick = mPerformClick;
				performClick.mClickMotionPosition = motionPosition;
				performClick.rememberWindowAttachCount();
				if (mTouchMode == TOUCH_MODE_DOWN || mTouchMode == TOUCH_MODE_TAP) {
					removeCallbacks(mTouchMode == TOUCH_MODE_DOWN ? mPendingCheckForTap : mPendingCheckForLongPress);

					if (adapter.isEnabled(motionPosition)) {
						mTouchMode = TOUCH_MODE_TAP;
						layoutChildren();
						child.setPressed(true);
						setPressed(true);
						if (mTouchModeReset != null) {
							removeCallbacks(mTouchModeReset);
						}
						mTouchModeReset = new Runnable() {
							@Override
							public void run() {
								mTouchModeReset = null;
								mTouchMode = TOUCH_MODE_REST;
								child.setPressed(false);
								setPressed(false);
								if (isAttach) {
									performClick.run();
								}
							}
						};
						postDelayed(mTouchModeReset, ViewConfiguration.getPressedStateDuration());
					} else {
						mTouchMode = TOUCH_MODE_REST;
					}
					return;
				} else if (adapter.isEnabled(motionPosition)) {
					performClick.run();
				}

			}
			mTouchMode = TOUCH_MODE_REST;
			break;
		}
		}
		setPressed(false);

		// Need to redraw since we probably aren't drawing the selector anymore
		invalidate();
		removeCallbacks(mPendingCheckForLongPress);
	}

	private void onTouchCancel() {
		switch (mTouchMode) {
		default:
			mTouchMode = TOUCH_MODE_REST;
			setPressed(false);
			final View motionView = this.getChildAt(mMotionPosition);
			if (motionView != null) {
				motionView.setPressed(false);
			}
			removeCallbacks(mPendingCheckForLongPress);
		}
	}

	private void onTouchMove(MotionEvent ev) {
		if (isBigMove(ev)) {
			mTouchMode = TOUCH_MODE_REST;
			setPressed(false);
			final View motionView = this.getChildAt(mMotionPosition);
			if (motionView != null) {
				motionView.setPressed(false);
			}
			removeCallbacks(mPendingCheckForLongPress);
			if (itemClickListener != null)
				itemClickListener.onCancelTouch();
		}
	}

	private boolean isBigMove(MotionEvent event) {
		Double a2 = Math.pow(mLastX - event.getRawX(), 2D);
		Double b2 = Math.pow(mLastY - event.getRawY(), 2D);
		Double limit = (double) Convenience.dpToPx(getContext(), (int) Math.pow(BIG_MOVE_DISTANCE, 2D));
		
		Log.d(VIEW_LOG_TAG, "isBigMove " + mLastX + ", " + mLastY + " | " + event.getRawX() + "," + event.getRawY()
				+ " || " + (a2 + b2) + " ? "+ limit);
		
		if (a2 + b2 > limit) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Maps a point to a position in the list.
	 * 
	 * @param x
	 *            X in local coordinate
	 * @param y
	 *            Y in local coordinate
	 * @return The position of the item which contains the specified point, or
	 *         {@link #INVALID_POSITION} if the point does not intersect an
	 *         item.
	 */
	public int pointToPosition(int x, int y) {
		Rect frame = mTouchFrame;
		if (frame == null) {
			mTouchFrame = new Rect();
			frame = mTouchFrame;
		}

		final int count = getChildCount();
		for (int i = count - 1; i >= 0; i--) {
			final View child = getChildAt(i);
			if (child.getVisibility() == View.VISIBLE) {
				child.getHitRect(frame);
				if (frame.contains(x, y)) {
					return i;
				}
			}
		}
		return INVALID_POSITION;
	}

	final class CheckForTap implements Runnable {
		@Override
		public void run() {
			if (mTouchMode == TOUCH_MODE_DOWN) {
				mTouchMode = TOUCH_MODE_TAP;
				final View child = getChildAt(mMotionPosition);
				if (child != null) {// && !child.hasFocusable()) {

					child.setPressed(true);
					setPressed(true);
					layoutChildren();

					final int longPressTimeout = ViewConfiguration.getLongPressTimeout();

					if (mPendingCheckForLongPress == null) {
						mPendingCheckForLongPress = new CheckForLongPress();
					}
					mPendingCheckForLongPress.rememberWindowAttachCount();
					postDelayed(mPendingCheckForLongPress, longPressTimeout);
				}
			}
		}
	}

	/**
	 * A base class for Runnables that will check that their view is still
	 * attached to the original window as when the Runnable was created.
	 * 
	 */
	private class WindowRunnnable {
		private int mOriginalAttachCount;

		public void rememberWindowAttachCount() {
			mOriginalAttachCount = getWindowAttachCount();
		}

		public boolean sameWindow() {
			return getWindowAttachCount() == mOriginalAttachCount;
		}
	}

	private class PerformClick extends WindowRunnnable implements Runnable {
		int mClickMotionPosition;

		@Override
		public void run() {

			final ListAdapter _adapter = adapter;
			final int motionPosition = mClickMotionPosition;
			if (_adapter != null && _adapter.getCount() > 0 && motionPosition != INVALID_POSITION
					&& motionPosition < _adapter.getCount() && sameWindow()) {
				final View view = getChildAt(motionPosition);
				// If there is no view, something bad happened (the view
				// scrolled off the
				// screen, etc.) and we should cancel the click
				if (view != null) {
					performItemClick(view, motionPosition, _adapter.getItemId(motionPosition));
				}
			}
		}
	}

	private class CheckForLongPress extends WindowRunnnable implements Runnable {
		@Override
		public void run() {
			final int motionPosition = mMotionPosition;
			final View child = getChildAt(motionPosition);
			if (child != null) {
				final int longPressPosition = mMotionPosition;
				final long longPressId = adapter.getItemId(mMotionPosition);

				boolean handled = false;
				if (sameWindow()) {
					handled = performLongPress(child, longPressPosition, longPressId);
				}
				if (handled) {
					mTouchMode = TOUCH_MODE_REST;
					setPressed(false);
					child.setPressed(false);
				} else {
					mTouchMode = TOUCH_MODE_DONE_WAITING;
				}
			}
		}
	}

	private class AdapterDataSetObserver extends DataSetObserver {
		@Override
		public void onChanged() {
			isDirty = true && getChildCount() > 0;
			requestLayout();
		}

		@Override
		public void onInvalidated() {
			requestLayout();
		}
	}

	public boolean performItemClick(View view, int position, long id) {
		Log.d(VIEW_LOG_TAG, "performItemClick");
		boolean handled = false;
		if (itemClickListener != null)
			handled = itemClickListener.onItemClick(this, view, position, id);

		return handled;
	}

	boolean performLongPress(final View child, final int longPressPosition, final long longPressId) {

		boolean handled = false;
		if (itemClickListener != null) {
			handled = itemClickListener.onItemLongClick(this, child, longPressPosition, longPressId);
		}
		return handled;
	}
}
