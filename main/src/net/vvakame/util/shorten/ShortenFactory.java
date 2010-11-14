package net.vvakame.util.shorten;

public class ShortenFactory {
	public static ShortenAgent getShortenAgent(String type) {
		if ("goo.gl".equals(type)) {
			return new GoogleShorten();
		} else if ("bit.ly".equals(type)) {
			return new BitlyShorten();
		}

		return new NotShorten();
	}
}
