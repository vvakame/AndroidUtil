package net.vvakame.util.shorten;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * URIのgoo.gl短縮をやってくれるライブラリ
 * 
 * @author vvakame
 */
public class GoogleShorten implements ShortenAgent {

	private int c(int... args) {
		int l = 0;
		for (int m = 0; m < args.length; m++) {
			l += args[m] & 4294967295L;
		}
		return l;
	}

	private String d(long l) {
		l = l > 0 ? l : l + 4294967296L;
		String m = String.valueOf(l);
		int o = 0;
		boolean n = false;
		for (int p = m.length() - 1; p >= 0; --p) {
			int q = m.charAt(p) - '0';
			if (n) {
				q *= 2;
				o += q / 10 + q % 10;
			} else {
				o += q;
			}
			n = !n;
		}
		int m1 = o % 10;
		o = 0;
		if (m1 != 0) {
			o = 10 - m1;
			if ((String.valueOf(l).length() % 2) == 1) {
				if ((o % 2) == 1) {
					o += 9;
				}
				o /= 2;
			}
		}
		return String.valueOf(o) + String.valueOf(l);
	}

	private int e(String l) {
		int m = 5381;
		for (int o = 0; o < l.length(); o++) {
			m = c(m << 5, m, l.charAt(o));
		}
		return m;
	}

	private int f(String l) {
		int m = 0;
		for (int o = 0; o < l.length(); o++) {
			m = c(l.charAt(o), m << 6, m << 16, -m);
		}
		return m;
	}

	private String getRequestParam(String uri)
			throws UnsupportedEncodingException {
		int i = e(uri);
		i = i >> 2 & 1073741823;
		i = i >> 4 & 67108800 | i & 63;
		i = i >> 4 & 4193280 | i & 1023;
		i = i >> 4 & 245760 | i & 16383;

		int h = f(uri);
		int k = (i >> 2 & 15) << 4 | h & 15;
		k |= (i >> 6 & 15) << 12 | (h >> 8 & 15) << 8;
		k |= (i >> 10 & 15) << 20 | (h >> 16 & 15) << 16;
		k |= (i >> 14 & 15) << 28 | (h >> 24 & 15) << 24;
		String j = "7" + d(k);

		String retUriPath = "user=toolbar@google.com&url=";
		retUriPath += URLEncoder.encode(uri, "UTF-8");
		retUriPath += "&auth_token=";
		retUriPath += j;
		return retUriPath;
	}

	public String getShorten(String uri) throws ShortenFailedException {
		String req;
		String shorten = null;
		try {
			req = getRequestParam(uri);

			URL url = new URL("http://goo.gl/api/url");
			HttpURLConnection con = (HttpURLConnection) url.openConnection();

			con.setDoOutput(true);

			PrintWriter out = new PrintWriter(con.getOutputStream());
			out.print(req);
			out.close();

			int status = con.getResponseCode();
			if (HttpURLConnection.HTTP_CREATED != status) {
				throw new ShortenFailedException();
			}

			InputStreamReader isr = new InputStreamReader(con.getInputStream());
			BufferedReader br = new BufferedReader(isr);

			String line = br.readLine(); // 1行しか取れない前提

			br.close();
			isr.close();

			// JSONから引っこ抜くのめんどいので正規表現でひっこぬく
			Pattern httpPattern = Pattern
					.compile("(http://[-_.!~*'()a-zA-Z0-9;/?:@&=+$,%#]+)");
			Matcher matcher = httpPattern.matcher(line);

			while (matcher.find()) {
				shorten = matcher.group();
				break;
			}

		} catch (IOException e) {
			throw new ShortenFailedException(e);
		}

		return shorten;
	}
}
