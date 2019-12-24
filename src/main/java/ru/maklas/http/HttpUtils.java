package ru.maklas.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Utils for ru.maklas.http **/
public class HttpUtils {

	public static final Charset utf_8 = Charset.forName("UTF-8");
	public static final Charset ascii = Charset.forName("US-ASCII");

	//Java 6 compitablity
	static boolean equals(Object a, Object b) {
		return (a == b) || (a != null && a.equals(b));
	}

	public static int bufferSize(long contentLength) {
		int bufferSize;
		if (contentLength <= 0) { //Unknown
			bufferSize = 8192;
		} else if (contentLength < 16384) {
			bufferSize = 4096;
		} else if (contentLength < 131072) {
			bufferSize = 8192;
		} else {
			bufferSize = 16384;
		}
		return bufferSize;
	}

	public static long copy(InputStream is, OutputStream os, byte[] buffer) throws IOException {
		int len = buffer.length;
		long count = 0;
		int n;
		while (-1 != (n = is.read(buffer, 0, len))) {
			os.write(buffer, 0, n);
			count += n;
		}
		return count;
	}

	private final static char[] MULTIPART_CHARS = "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

	public static String generateMultipartBoundary() {
		return generateMultipartBoundary(35);
	}

	private static String generateMultipartBoundary(int length) {
	     final Random rand = new Random();
		final StringBuilder sb = new StringBuilder();
		if (length > 20) {
			 sb.append("------");
		 }
	     length -= sb.length();
	     for (int i = 0; i < length; i++) {
	             sb.append(MULTIPART_CHARS[rand.nextInt(MULTIPART_CHARS.length)]);
	         }
	     return sb.toString();
	}

	static String trim(String value, int maxLen) {
		return value.length() <= maxLen ? value : value.substring(0, maxLen - 3).concat("...");
	}

	static String escapeFormData(String s) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c < 256) {
				sb.append(c);
			} else {
				sb.append("&#").append(((int) c));
			}
		}
		return sb.toString();
	}

	static String unescapeFormData(String s) {
		StringBuffer sb = new StringBuffer();
		Matcher matcher = Pattern.compile("&#(\\d{3,4})").matcher(s);
		while (matcher.find()) {
			int code = Integer.valueOf(matcher.group(1));
			matcher.appendReplacement(sb, String.valueOf((char) code));
		}
		matcher.appendTail(sb);
		return sb.toString();
	}
}
