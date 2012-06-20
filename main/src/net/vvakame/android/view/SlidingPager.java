// forked by here. under Apache license.
// https://github.com/tarotaro/TubeDroidPlayer/blob/master/src/com/freeworks/android/tubedroidplayer/widget/FacebookLikePager.java

package net.vvakame.android.view;

import net.vvakame.android.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.Scroller;

public class SlidingPager extends FrameLayout {

	static final String TAG = SlidingPager.class.getSimpleName();

	public static final int DURATION = 250;

	enum Moving {
		OPEN, CLOSE, NONE
	}

	public int mRightSpaceWidth;

	private Scroller mMainScroller;
	private Scroller mSubScroller;
	private int mCurrentPos = 0;
	private Moving mMoving = Moving.NONE;
	private boolean isInitialized = false;

	public SlidingPager(Context context) {
		this(context, null);

	}

	public SlidingPager(Context context, AttributeSet attrs) {
		super(context, attrs);

		TypedArray tArray = context.obtainStyledAttributes(attrs,
				R.styleable.SlidingView);

		mRightSpaceWidth = tArray.getDimensionPixelSize(
				R.styleable.SlidingView_rightspace, 80);
		mMainScroller = new Scroller(context,
				new AccelerateDecelerateInterpolator());
		mSubScroller = new Scroller(context,
				new AccelerateDecelerateInterpolator());
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		if (!isInitialized) {
			adjust();
			viewScrollTo(0, 0);
			viewScrollTo(1, -getMeasuredWidth() + mRightSpaceWidth);
			isInitialized = true;
		}
	}

	@Override
	public void computeScroll() {
		if (mMainScroller.computeScrollOffset()) {
			viewScrollTo(mCurrentPos, mMainScroller.getCurrX());
			postInvalidate();
		}
		if (mSubScroller.computeScrollOffset()) {
			viewScrollTo(mCurrentPos + 1, mSubScroller.getCurrX());
			postInvalidate();
		}
		if (mMainScroller.isFinished()) {
			if (mMoving == Moving.CLOSE) {
				mCurrentPos--;
				mCurrentPos = Math.max(mCurrentPos, 0);
				mMoving = Moving.NONE;
			}
		}
	}

	/**
	 * 開く動作. 現在はじっこに見えているViewが大きく出てくる.
	 */
	public void open() {
		if (!hasNext()) {
			return;
		}
		adjust();
		mMoving = Moving.OPEN;
		viewScrollTo(mCurrentPos, 0);
		mCurrentPos++;
		final int visibleWidth = (getMeasuredWidth() - mRightSpaceWidth);
		mMainScroller.startScroll(-visibleWidth, 0, visibleWidth, 0, DURATION);
		mSubScroller.startScroll(-getMeasuredWidth(), 0, mRightSpaceWidth, 0,
				DURATION);
		invalidate();
	}

	/**
	 * 閉じる動作. 現在大きく見えているViewが脇に引っ込む.
	 */
	public void close() {
		if (!hasPrevious()) {
			return;
		}
		adjust();
		mMoving = Moving.CLOSE;
		viewScrollTo(mCurrentPos - 1, 0);
		final int visibleWidth = (getMeasuredWidth() - mRightSpaceWidth);
		mMainScroller.startScroll(0, 0, -visibleWidth, 0, DURATION);
		mSubScroller.startScroll(-visibleWidth, 0, -getMeasuredWidth(), 0,
				DURATION);
		invalidate();
	}

	public boolean hasNext() {
		return mCurrentPos + 1 < getChildCount();
	}

	public boolean hasPrevious() {
		if (mMoving == Moving.CLOSE) {
			return mCurrentPos > 1;
		} else {
			return mCurrentPos > 0;
		}
	}

	public boolean isOpen(View v) {
		if (mMoving == Moving.CLOSE) {
			return getChildAt(mCurrentPos - 1) == v;
		} else {
			return getChildAt(mCurrentPos) == v;
		}
	}

	public void adjust() {

		for (int i = 0; i < getChildCount(); i++) {
			viewScrollTo(i, getMeasuredWidth());
		}
		viewScrollTo(mCurrentPos - 1, 0);
		if (!mMainScroller.isFinished()) {
			mMainScroller.abortAnimation();
			viewScrollTo(mCurrentPos, mMainScroller.getFinalX());
		}
		if (!mSubScroller.isFinished()) {
			mSubScroller.abortAnimation();
			viewScrollTo(mCurrentPos + 1, mSubScroller.getFinalX());
		}
		if (mMoving == Moving.CLOSE) {
			mCurrentPos--;
			mCurrentPos = Math.max(mCurrentPos, 0);
		}
		mMoving = Moving.NONE;
		invalidate();
	}

	private void viewScrollTo(int pos, int x) {
		View view = getChildAt(pos);
		if (view != null) {
			view.scrollTo(x, 0);
		}
	}
}
