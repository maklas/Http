package ru.maklas.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

/** Utils for ru.maklas.http **/
public class HttpUtils {

	public static final Charset utf_8 = Charset.forName("UTF-8");

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

}
