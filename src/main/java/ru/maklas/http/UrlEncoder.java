package ru.maklas.http;

import com.badlogic.gdx.utils.Array;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.function.Function;

/** Encoder used to encode query parameters for GET in address bar and POST in request body **/
public class UrlEncoder {

	public static final Function<String, String> javaUrl = UrlEncoder::encodeJavaUrl;
	public static final Function<String, String> jsUri = UrlEncoder::encodeURIComponent;
	public static final Function<String, String> jsUriAndPlus = UrlEncoder::encodeURIComponentPlus;
	public static Function<String, String> defaultEncoding = javaUrl;

	private Array<KeyValuePair> pairs = new Array<>();
	private Function<String, String> encodingFunction = defaultEncoding;

	public UrlEncoder add(String key, Object value) {
		pairs.add(new KeyValuePair(key, value != null ? String.valueOf(value) : ""));
		return this;
	}

	public Array<KeyValuePair> getPairs() {
		return pairs;
	}

	public UrlEncoder usingJavaEncoding() {
		this.encodingFunction = javaUrl;
		return this;
	}

	public UrlEncoder usingJsEncoding() {
		this.encodingFunction = jsUri;
		return this;
	}

	public UrlEncoder usingJsAndPlus() {
		this.encodingFunction = jsUriAndPlus;
		return this;
	}

	public String encode() {
		if (pairs.size == 0) return "";
		Function<String, String> encodingFunction = this.encodingFunction;
		StringBuilder builder = new StringBuilder();

		for (KeyValuePair pair : pairs) {
			builder.append(encodingFunction.apply(pair.key))
					.append("=")
					.append(encodingFunction.apply(pair.value))
					.append("&");
		}
		builder.setLength(builder.length() - 1);
		return builder.toString();
	}

	/** performs encoding in specified charset **/
	public byte[] encode(Charset charset) {
		return encode().getBytes(charset);
	}

	/**
	 * Encodes as per x-www-form-urlencoded documentation {@link URLEncoder}.
	 * Same as browser does when doing GET request
	 */
	public static String encodeJavaUrl(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (UnsupportedEncodingException ignore) {
		}
		return s;
	}

	/** equivalent to js' encodeURIComponent() **/
	public static String encodeURIComponent(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8")
					.replaceAll("\\+", "%20")
					.replaceAll("%21", "!")
					.replaceAll("%7E", "~")
					.replaceAll("%27", "'")
					.replaceAll("%28", "(")
					.replaceAll("%29", ")");
		} catch (UnsupportedEncodingException ignore) {
		}
		return s;
	}

	/** Encodes using Wikipedia's documentation of x-www-form-urlencoded **/
	public static String encodeURIComponentPlus(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8")
					.replaceAll("%21", "!")
					.replaceAll("%7E", "~")
					.replaceAll("%27", "'")
					.replaceAll("%28", "(")
					.replaceAll("%29", ")");
		} catch (UnsupportedEncodingException ignore) {
		}
		return s;
	}
}
