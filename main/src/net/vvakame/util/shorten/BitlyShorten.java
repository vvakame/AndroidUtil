package net.vvakame.util.shorten;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;

public class BitlyShorten implements ShortenAgent {

	static final String API_KEY = "R_66cb3939ab9c6e6f923e1f87cafc5f26";
	static final String LOGIN = "droppshare";

	static final String URI_TEMPLATE = "http://api.bit.ly/v3/shorten?"
			+ "login=" + LOGIN + "&apiKey=" + API_KEY + "&uri=%s&"
			+ "format=json";

	@Override
	public String getShorten(String uri) throws ShortenFailedException {
		String reqUri = String.format(URI_TEMPLATE, Uri.encode(uri));

		String shorten = null;
		try {
			URL url = new URL(reqUri);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();

			int status = con.getResponseCode();
			if (HttpURLConnection.HTTP_OK != status) {
				throw new ShortenFailedException();
			}

			InputStreamReader isr = new InputStreamReader(con.getInputStream());
			BufferedReader br = new BufferedReader(isr);

			StringBuilder stb = new StringBuilder();

			String line = null;

			while ((line = br.readLine()) != null) {
				stb.append(line);
			}

			br.close();
			isr.close();

			JSONObject json = new JSONObject(stb.toString());

			shorten = json.getJSONObject("data").getString("url");

		} catch (MalformedURLException e) {
			throw new ShortenFailedException(e);
		} catch (IOException e) {
			throw new ShortenFailedException(e);
		} catch (JSONException e) {
			throw new ShortenFailedException(e);
		}

		return shorten;
	}
}
