package ru.maklas.http;

import com.badlogic.gdx.utils.ByteArray;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

/**
 * Response with body stored inside as byte array. Useful when you know you're going to receive small response.
 * If you don't know the size need to use IO, use {@link Request#send(ResponseReceiver)}
 */
public class FullResponse extends Response {

	private boolean unescaped = false;
	private String responseUnescaped;
	private byte[] responseBytes;
	private String responseBodyAsIs;
	private boolean errorStreamUsed;
	private Exception bodyException;

	FullResponse(HttpURLConnection javaCon, URL url, int msToConnect, Request request) {
		super(javaCon, url, msToConnect, request);
		downloadResponse();
	}

	/** Exception that was thrown while attempting to obtain body of the response **/
	public Exception getBodyException() {
		return bodyException;
	}

	/** Response as it was received in byte[] form **/
	public byte[] getResponseBytes() {
		return responseBytes;
	}

	/** Response as it was received in Base64 encoding **/
	public String getResponseBytesBase64() {
		return Base64.getEncoder().encodeToString(responseBytes);
	}

	/** If true, error stream was used to fetch body **/
	public boolean isError() {
		return errorStreamUsed;
	}

	/** Body of the response as it was received, decoded with charset indicated in header or default (utf-8) **/
	public String getBodyAsIs() {
		if (responseBodyAsIs == null) {
			responseBodyAsIs = responseBytes == null || responseBytes.length == 0 ? "" : new String(responseBytes, getCharset());
		}
		return responseBodyAsIs;
	}

	/**
	 * Unescaped body of the response. Changed version, where escaping of characters was removed.
	 * Usually produces more human-readable result.
	 */
	public String getBodyUnescaped() {
		if (!unescaped) {
			if (getBodyAsIs() != null) {
				try {
					responseUnescaped = unescape(getBodyAsIs());
				} catch (Exception e) {
					responseUnescaped = getBodyAsIs();
				}
			}
			unescaped = true;
		}
		return responseUnescaped;
	}

	private void downloadResponse() {
		if (responseBytes != null) return;
		InputStream is;
		try {
			is = _getInputStream();
		} catch (Exception e) {
			errorStreamUsed = true;
			bodyException = e;
			try {
				is = _getErrorInputStream();
			} catch (Exception e1) {
				responseBytes = new byte[0];
				bodyException = e1;
				return;
			}
		}

		int bufferSize = HttpUtils.bufferSize(contentLength);
		byte[] buffer = new byte[bufferSize];

		ByteArray byteArray = new ByteArray(Math.max(contentLength, 512));
		try {
			int read = is.read(buffer, 0, bufferSize);
			while (read != -1) {
				byteArray.addAll(buffer, 0, read);
				read = is.read(buffer, 0, bufferSize);
			}
		} catch (Exception e) {
			bodyException = e;
		} finally {
			responseBytes = byteArray.toArray();
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected boolean hasBody() {
		return responseBytes != null && responseBytes.length > 0;
	}

	static String unescape(String input) {
		//return StringEscapeUtils.unescapeJava(input.replaceAll("\\\\\\\\u", "\\\\u"));
		return Unescaper.translateSafely(input);
	}

	@Override
	protected void printBodyTrace(PrintWriter w) {
		w.println(getBodyAsIs());
	}
}
