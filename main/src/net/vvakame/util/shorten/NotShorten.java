package net.vvakame.util.shorten;

public class NotShorten implements ShortenAgent {

	@Override
	public String getShorten(String uri) throws ShortenFailedException {
		return uri;
	}
}
