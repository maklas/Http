package ru.maklas.http;

import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Header extends KeyValuePair {

	public Header(String key, String value) {
		super(key, value);
	}

	/** @return new Header instance with the same key, but changed value **/
	public Header withValue(String value) {
		return new Header(key, value);
	}

	@Override
	public String toString() {
		return key + ": " + value;
	}














	/** Request header. Specifies browser version of the client and machine it runs on **/
	public static class UserAgent {

		public static final String key = "User-Agent";

		public static final Header windows_10_chrome = new Header(key, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36");
		public static final Header linux_mozila = new Header(key, "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:15.0) Gecko/20100101 Firefox/15.0.1");
		public static final Header samsung_galaxy_s9 = new Header(key, "Mozilla/5.0 (Linux; Android 8.0.0; SM-G960F Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.84 Mobile Safari/537.36");
		public static final Header samsung_galaxy_tab_s3 = new Header(key, "Mozilla/5.0 (Linux; Android 7.0; SM-T827R4 Build/NRD90M) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.116 Safari/537.36");
		public static final Header mac_osx_safari = new Header(key, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_2) AppleWebKit/601.3.9 (KHTML, like Gecko) Version/9.0.2 Safari/601.3.9");
		public static final Header iphone_x_safari = new Header(key, "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1");
		public static Header def = windows_10_chrome;

		public static Header with(String val) {
			return new Header(key, val);
		}
	}

	/** Request header. The domain name of the server. **/
	public static class Host {
		public static final String key = "Host";

		public static Header of(String s) {
			return new Header(key, s);
		}

		public static Header fromUrl(URL url) {
			return new Header(key, url.getHost());
		}

		public static Header fromUrl(String url) {
			URL javaUrl = null;
			try {
				javaUrl = new URL(url);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			return new Header(key, javaUrl != null ? javaUrl.getHost() : "");
		}
	}

	/** Request header. Type of connection (close/keep-alive). Doesn't have to be appended to every request **/
	public static class Connection {
		public static final String key = "Connection";
		public static final Header keepAlive = new Header(key, "keep-alive");
		public static final Header close = new Header(key, "close");

		public static Header with(String val) {
			return new Header(key, val);
		}
	}

	/** Request header. What type of response we expect **/
	public static class Accept {
		public static final String key = "Accept";
		public static final Header all = new Header(key, "*/*");
		public static final Header html = new Header(key, "text/html");
		public static final Header appJson = new Header(key, "application/json");
		public static final Header appJsonOrJS = new Header(key, "application/json, text/javascript, */*; q=0.01");
		public static final Header textAppImageOrAny = new Header(key, "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");

		public static Header with(String val) {
			return new Header(key, val);
		}

		public static Header multi(String... vals) {
			return multiComma(key, vals);
		}
	}

	/** Request header. Initiates a request for cross-origin resource sharing **/
	public static class Origin {
		public static final String key = "Origin";

		public static Header with(String val) {
			return new Header(key, val);
		}
	}

	/** Request/Response header. Tells caching mechanisms from server to client whether they may cache this object **/
	public static class CacheControl {
		public static final String key = "Cache-Control";
		public static final Header noStore_noCache_revalidate = new Header(key, "no-store, no-cache, must-revalidate");
		public static final Header noCache = new Header(key, "no-cache");
		public static final Header pragmaNoCache = new Header("Pragma", "no-cache"); //HTTP 1.0
		public static final Header maxAge0 = new Header(key, "max-age=0");
		public static final Header privat = new Header(key, "private");

		public static Header with(String val) {
			return new Header(key, val);
		}
	}

	/** Request header. Mainly used to identify Ajax requests. Most JavaScript frameworks send this field with value of XMLHttpRequest **/
	public static class RequestedWith {
		public static final String key = "X-Requested-With";
		public static final Header ajax = new Header(key, "XMLHttpRequest");

		public static Header with(String val) {
			return new Header(key, val);
		}
	}

	/**
	 * Request header. Lists acceptable encodings for compressing response.
	 * identity, gzip, deflate and br are supported by the library and input streams will be wrapped accordingly
	 */
	public static class AcceptEncoding {
		public static final String key = "Accept-Encoding";
		public static final Header gzip = new Header(key, "gzip");
		public static final Header br = new Header(key, "br");
		public static final Header deflate = new Header(key, "deflate");
		public static final Header identity = new Header(key, "identity");
		public static final Header gzipDeflate = new Header(key, "gzip, deflate");
		public static final Header gzipDeflateBr = new Header(key, "gzip, deflate, br");
		public static final Header any = new Header(key, "*");

		public static Header with(String val) {
			return new Header(key, val);
		}

		public static Header multi(String... values) {
			return Header.multiComma(key, values);
		}
	}

	/** Response header. Specifies which encoding was used to compress Http body **/
	public static class ContentEncoding {
		public static final String key = "Content-Encoding";
		public static final Header gzip = new Header(key, "gzip");
		public static final Header compress = new Header(key, "compress");
		public static final Header deflate = new Header(key, "deflate");
		public static final Header identity = new Header(key, "identity");
		public static final Header br = new Header(key, "br");
	}

	/** Request header. Lists preferable languages in which response should be in. Ex: 'da, en-gb;q=0.8, en;q=0.7' **/
	public static class AcceptLanguage {
		public static final String key = "Accept-Language";
		public static final Header en = new Header(key, "en"); //english
		public static final Header ru = new Header(key, "ru"); //russian
		public static final Header de = new Header(key, "de"); //german
		public static final Header fr = new Header(key, "fr"); //french
		public static final Header zh = new Header(key, "zh"); //chinese
		public static final Header es = new Header(key, "es"); //spanish
		public static final Header ja = new Header(key, "ja"); //japanese
		public static Header defaultLang = en;

		public static Header with(String val) {
			return new Header(key, val);
		}
	}

	/** Response header. Must not be used with HTTP/2 **/
	public static class TransferEncoding {
		public static final String key = "Transfer-Encoding";
		public static final Header chunked = new Header(key, "chunked");
		public static final Header compress = new Header(key, "compress");
		public static final Header deflate = new Header(key, "deflate");
		public static final Header gzip = new Header(key, "gzip");
		public static final Header identity = new Header(key, "identity");
	}

	/** Request header. The address of the previous web page from which a link to the currently requested page was followed **/
	public static class Referer {
		public static final String key = "Referer";

		public static Header with(String val) {
			return new Header(key, val);
		}

		public static Header with(URL url) {
			return with(url.toExternalForm());
		}
	}

	//В ответе от сервера. Указывает какой вид контента содержится в теле. (MIME-type)

	/** Request/Response header. Specifies mime type of the content **/
	public static class ContentType extends Header {
		public static final String key = "Content-Type";
		public static final ContentType textPlain = new ContentType("text/plain; charset=UTF-8");
		public static final ContentType textHtml = new ContentType("text/html; charset=UTF-8");
		public static final ContentType textXml = new ContentType("text/xml; charset=UTF-8");
		public static final ContentType appXml = new ContentType("application/xml; charset=UTF-8");
		public static final ContentType appJson = new ContentType("application/json; charset=UTF-8");
		public static final ContentType appJS = new ContentType("application/javascript; charset=UTF-8");
		public static final ContentType form_urlencoded = new ContentType("application/x-www-form-urlencoded; charset=UTF-8");
		public static final ContentType formData = new ContentType("multipart/form-data; charset=UTF-8");
		public static final ContentType octetStream = new ContentType("application/octet-stream");

		ContentType(String value) {
			super(key, value);
		}

		public static Header with(String val) {
			return new Header(key, val);
		}

		public static Header with(String mimeType, @Nullable String encoding) {
			StringBuilder sb = new StringBuilder(mimeType);
			if (encoding != null) {
				sb.append("; charset=").append(encoding);
			}
			return with(sb.toString());
		}

		public static Header with(String mimeType, @Nullable Charset encoding) {
			return with(mimeType, encoding == null ? null : encoding.displayName(Locale.US));
		}
	}

	public static class ContentLength {
		public static final String key = "Content-Length";

		public static Header with(long len) {
			return new Header(key, String.valueOf(len));
		}

	}

	/** Response header. Sets cookie for the client **/
	public static class SetCookie {
		public static final String key = "Set-Cookie";

		public static Header with(String val) {
			return new Header(key, val);
		}
	}

	/** Request header. Whether or not upgrade to https is allowed **/
	public static class UpgradeInsecure {
		public static final String key = "Upgrade-Insecure-Requests";
		public static final Header one = new Header(key, "1");

		public static Header with(String val) {
			return new Header(key, val);
		}
	}

	/** Response header. Specifies content location of a file. For example '/documents/foo.json' **/
	public static class ContentLocation {
		public static final String key = "Content-Location";
	}

	/** Request/Response header. The date and time at which the message was originated **/
	public static class DateHeader {
		public static final String key = "Date";
		public static SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

		public static Header fromDate(Date date) {
			return new Header(key, format.format(date));
		}

		public static Date fromHeader(Header header) throws ParseException {
			return format.parse(header.value);
		}

		public static Header now() {
			return fromDate(new Date());
		}
	}

	/** Response header. Specifies new location (redirect) **/
	public static class Location {
		public static final String key = "Location";
	}

	private static Header multiComma(String key, String... encodings) {
		if (encodings == null || encodings.length == 0) return new Header(key, "");
		else if (encodings.length == 1) return new Header(key, encodings[0]);

		StringBuilder sb = new StringBuilder(encodings[0]);
		for (int i = 1; i < encodings.length; i++) {
			sb.append(", ").append(encodings[i]);
		}
		return new Header(key, sb.toString());
	}

}
