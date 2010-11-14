package net.vvakame.android.helper;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;

/**
 * EventDrivenぽい型のHandlerです。
 * 
 * @author vvakame
 */
public class DrivenHandler extends Handler {
	private Activity mActivity = null;
	private int mDialogId = -1;
	private Map<Integer, Closure> mCloMap = new HashMap<Integer, Closure>();

	public DrivenHandler() {
	}

	public DrivenHandler(Activity activity, int dialogId) {
		mActivity = activity;
		mDialogId = dialogId;
	}

	public boolean pushEvent(int id, Closure clo) {
		boolean exists = mCloMap.containsKey(id);
		mCloMap.put(id, clo);

		return exists;
	}

	public boolean pushEventWithShowDialog(int id, Closure clo) {
		if (mActivity == null) {
			throw new IllegalStateException("Activity not regited");
		}

		Closure wraped = new Closure(clo) {
			@Override
			public void exec() {
				super.exec();
				mActivity.showDialog(mDialogId);
			}
		};

		boolean exists = mCloMap.containsKey(id);
		mCloMap.put(id, wraped);

		return exists;
	}

	public boolean pushEventWithDissmiss(int id, Closure clo) {
		if (mActivity == null) {
			throw new IllegalStateException("Activity not regited");
		}

		Closure wraped = new Closure(clo) {
			@Override
			public void exec() {
				super.exec();
				mActivity.dismissDialog(mDialogId);
			}
		};

		boolean exists = mCloMap.containsKey(id);
		mCloMap.put(id, wraped);

		return exists;
	}

	@Override
	public void handleMessage(Message msg) {
		if (mCloMap.containsKey(msg.what)) {
			mCloMap.get(msg.what).exec();
		} else {
			throw new NotHandlerRegitedException();
		}
	}

	public static class NotHandlerRegitedException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}
}
