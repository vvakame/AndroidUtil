package net.vvakame.android.view;

import net.vvakame.android.R;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

public class DragnDropListView extends ListView {
	private static final String TAG = DragnDropListView.class.getSimpleName();
	private static final boolean DEBUG = false;

	private static final int SCROLL_SPEED_FAST = 25;
	private static final int SCROLL_SPEED_SLOW = 8;

	private boolean sortMode = false;
	private OnDragListener mDrag = new OnDragListenerImpl();
	private OnDropListener mDrop = new OnDropListenerImpl();
	private OnRemoveListener mRemove = new OnRemoveListenerImpl();

	public DragnDropListView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public DragnDropListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		TypedArray tArray = context.obtainStyledAttributes(attrs,
				R.styleable.DragnDropListView);

		mFixedX = tArray
				.getBoolean(R.styleable.DragnDropListView_fixedX, false);
		mFixedY = tArray
				.getBoolean(R.styleable.DragnDropListView_fixedY, false);
		mOnHoverColorStart = tArray.getColor(
				R.styleable.DragnDropListView_onHoverColorStart, 0x40ff0000);
		mOnHoverColorEnd = tArray.getColor(
				R.styleable.DragnDropListView_onHoverColorEnd, 0x00ff0000);
		mOnHoverDuration = tArray.getColor(
				R.styleable.DragnDropListView_onHoverDuration, 500);
	}

	private Bitmap mDragBitmap = null;
	private ImageView mDragView = null;
	private WindowManager.LayoutParams mWindowParams = null;
	private int mFrom = -1;

	private View mRemoveTile = null;
	private Rect mRemoveHit = null;

	private boolean mFixedX;
	private boolean mFixedY;
	private int mOnHoverColorStart;
	private int mOnHoverColorEnd;
	private int mOnHoverDuration;

	private View mCurrentBling;

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (!sortMode) {
			return super.onTouchEvent(ev);
		}

		WindowManager wm = null;
		int index = -1;
		final int x = (int) ev.getX();
		final int y = (int) ev.getY();

		switch (ev.getAction()) {

		case MotionEvent.ACTION_DOWN:
			index = pointToIndex(ev);

			if (index < 0) {
				return false;
			}

			mFrom = index;
			startDrag(ev);

			// fall through

		case MotionEvent.ACTION_MOVE:

			final int height = getHeight();
			final int fastBound = height / 9;
			final int slowBound = height / 4;
			final int center = height / 2;

			int speed = 0;
			if (ev.getEventTime() - ev.getDownTime() < 500) {
				// 500ミリ秒間はスクロールなし
			} else if (y < slowBound) {
				speed = y < fastBound ? -SCROLL_SPEED_FAST : -SCROLL_SPEED_SLOW;
			} else if (y > height - slowBound) {
				speed = y > height - fastBound ? SCROLL_SPEED_FAST
						: SCROLL_SPEED_SLOW;
			}

			if (DEBUG) {
				Log.d(TAG, "ACTION_MOVE y=" + y + ", height=" + height
						+ ", fastBound=" + fastBound + ", slowBound="
						+ slowBound + ", center=" + center + ", speed=" + speed);
			}

			View v = null;
			if (speed != 0) {
				// 横方向はとりあえず考えない
				int centerPosition = pointToPosition(0, center);
				if (centerPosition == AdapterView.INVALID_POSITION) {
					centerPosition = pointToPosition(0, center
							+ getDividerHeight() + 64);
				}
				v = getChildByIndex(centerPosition);
				if (v != null) {
					int pos = v.getTop();
					setSelectionFromTop(centerPosition, pos - speed);
				}
			}

			if (mDragView != null) {
				if (mDragView.getHeight() < 0) {
					mDragView.setVisibility(View.INVISIBLE);
				} else {
					mDragView.setVisibility(View.VISIBLE);
				}
				if (!mFixedX) {
					mWindowParams.x = getLeft() + x - 130;
				}
				if (!mFixedY) {
					mWindowParams.y = getTop() + y - 32;
				}

				wm = (WindowManager) getContext().getSystemService(
						Context.WINDOW_SERVICE);
				wm.updateViewLayout(mDragView, mWindowParams);

				index = pointToIndex(ev);
				blingView(getChildByIndex(index));

				Rect rect = new Rect();
				mRemoveTile.getGlobalVisibleRect(rect);
				if (rect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
					blingView(mRemoveTile);
				}
				if (mDrag != null) {
					mDrag.drag(mFrom, index);
				}

				return true;
			}

			break;

		case MotionEvent.ACTION_UP:

			if (mRemove != null && mRemoveTile != null
					|| mRemoveTile.getVisibility() == View.VISIBLE) {
				if (mRemoveHit == null) {
					mRemoveHit = new Rect();
				}
				mRemoveTile.getHitRect(mRemoveHit);
				if (mRemoveHit.contains(x + getLeft(), y + getTop())) {
					mRemove.remove(mFrom);
				}
			}
			if (mDrop != null) {
				index = pointToIndex(ev);
				mDrop.drop(mFrom, index);
			}

			// fall through

		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_OUTSIDE:

			if (mDragView != null) {
				wm = (WindowManager) getContext().getSystemService("window");
				wm.removeView(mDragView);
				mDragView = null;
				// リサイクルするとたまに死ぬけどタイミング分からない
				// mDragBitmap.recycle();
				mDragBitmap = null;

				return true;
			}

			break;

		default:
			Log.d(TAG, "Unknown event action=" + ev.getAction());

			break;
		}

		return super.onTouchEvent(ev);
	}

	void blingView(final View view) {
		if (view != null && mCurrentBling != view) {
			ValueAnimator animator = ObjectAnimator.ofObject(view,
					"backgroundColor", new ArgbEvaluator(), mOnHoverColorStart,
					mOnHoverColorEnd);
			animator.setDuration(mOnHoverDuration);
			animator.start();
			mCurrentBling = view;
		}
	}

	private void startDrag(MotionEvent ev) {
		WindowManager wm;
		View view = getChildByIndex(mFrom);

		final Bitmap.Config c = Bitmap.Config.ARGB_8888;
		mDragBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), c);
		Canvas canvas = new Canvas();
		canvas.setBitmap(mDragBitmap);
		view.draw(canvas);

		if (mWindowParams == null) {
			mWindowParams = new WindowManager.LayoutParams();
			mWindowParams.gravity = Gravity.TOP | Gravity.LEFT;

			mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
			mWindowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
			mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
					| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
					| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
					| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
			mWindowParams.format = PixelFormat.TRANSLUCENT;
			mWindowParams.windowAnimations = 0;
			mWindowParams.x = 0;
			mWindowParams.y = 0;
		}

		ImageView v = new ImageView(getContext());
		v.setBackgroundColor(Color.argb(128, 0, 0, 0));
		v.setImageBitmap(mDragBitmap);

		wm = (WindowManager) getContext().getSystemService("window");
		if (mDragView != null) {
			wm.removeView(mDragView);
		}
		wm.addView(v, mWindowParams);
		mDragView = v;
	}

	private View getChildByIndex(int index) {
		return getChildAt(index - getFirstVisiblePosition());
	}

	private int pointToIndex(MotionEvent ev) {
		return pointToIndex((int) ev.getX(), (int) ev.getY());
	}

	private int pointToIndex(int x, int y) {
		return (int) pointToPosition(x, y);
	}

	public void setRemoveTile(View v) {
		mRemoveTile = v;
	}

	public void setOnDragListener(OnDragListener listener) {
		mDrag = listener;
	}

	public void setOnDropListener(OnDropListener listener) {
		mDrop = listener;
	}

	public void setOnRemoveListener(OnRemoveListener listener) {
		mRemove = listener;
	}

	public interface OnDragListener {
		public void drag(int from, int to);
	}

	public interface OnDropListener {
		public void drop(int from, int to);
	}

	public interface OnRemoveListener {
		public void remove(int which);
	}

	class OnDragListenerImpl implements OnDragListener {
		public void drag(int from, int to) {
			if (DEBUG) {
				Log.d(TAG, "DragListenerImpl drag event. from=" + from
						+ ", to=" + to);
			}
		}
	}

	public class OnDropListenerImpl implements OnDropListener {
		@SuppressWarnings("unchecked")
		public void drop(int from, int to) {
			if (DEBUG) {
				Log.d(TAG, "DropListenerImpl drop event. from=" + from
						+ ", to=" + to);
			}

			if (from == to || from < 0 || to < 0) {
				return;
			}

			Adapter adapter = getAdapter();
			if (adapter != null && adapter instanceof ArrayAdapter) {
				ArrayAdapter<Object> arrayAdapter = (ArrayAdapter<Object>) adapter;
				Object item = adapter.getItem(from);

				arrayAdapter.remove(item);
				arrayAdapter.insert(item, to);
			}
		}
	}

	public class OnRemoveListenerImpl implements OnRemoveListener {
		@SuppressWarnings("unchecked")
		public void remove(int which) {
			if (DEBUG) {
				Log.d(TAG, "RemoveListenerImpl remove event. which=" + which);
			}

			if (which < 0) {
				return;
			}

			Adapter adapter = getAdapter();
			if (adapter != null && adapter instanceof ArrayAdapter) {
				ArrayAdapter<Object> arrayAdapter = (ArrayAdapter<Object>) adapter;
				Object item = adapter.getItem(which);

				arrayAdapter.remove(item);
			}
		}
	}

	public void setSortMode(boolean sortMode) {
		this.sortMode = sortMode;
	}

	public boolean isSortMode() {
		return sortMode;
	}
}