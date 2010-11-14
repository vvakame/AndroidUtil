package net.vvakame.android.helper;

/**
 * AsyncTaskで後処理用にClosureっぽいもの作成用IF
 * 
 * @author vvakame
 */
public interface Func<T> {
	public void func(T arg);
}
