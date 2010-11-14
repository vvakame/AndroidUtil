package net.vvakame.android.helper;

/**
 * クロージャっぽいもの
 * 
 * @author vvakame
 */
public abstract class Closure {
	protected Closure mClo = null;

	public Closure() {
	}

	/**
	 * クロージャをクロージャでwrapしたいときに使う
	 * 
	 * @param clo
	 */
	public Closure(Closure clo) {
		mClo = clo;
	}

	/**
	 * wrapしたmCloを実行する。Overrideされて書き潰されるはずなので必ず実行される保証はない。
	 */
	public void exec() {
		if (mClo != null) {
			mClo.exec();
		}
	}
}
