package net.vvakame.util.shorten;

public interface ShortenAgent {
	public String getShorten(String uri) throws ShortenFailedException;
}
