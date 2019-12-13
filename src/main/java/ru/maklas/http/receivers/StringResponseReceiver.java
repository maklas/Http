package ru.maklas.http.receivers;

import org.jetbrains.annotations.NotNull;
import ru.maklas.http.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * Accumulates response body into a string. Obtain it by calling {@link #getResponse()} or {@link #getResponseUnescaped()}
 * If charset is not specified in the constructor, it will be obtained from the header. If header is not present utf-8 is used.
 */
public class StringResponseReceiver implements ResponseReceiver {

	private String response = "";
	private String unescapedResponse;
	private final Charset charset;

	/** Uses charset from ContentType header or UTF-8 if not present **/
	public StringResponseReceiver() {
		this(null);
	}

	/** Uses specified charset to decode data **/
	public StringResponseReceiver(Charset charset) {
		this.charset = charset;
	}

	@Override
	public void receive(Response response, long contentLength, InputStream is, Counter counter, boolean isError) throws Exception {
		InputStreamReader isr = new InputStreamReader(is, charset != null ? charset : response.getCharset());
		int bufferSize = HttpUtils.bufferSize(contentLength);
		char[] buffer = new char[bufferSize];

		StringBuilder sb = new StringBuilder(contentLength > 0 ? (contentLength > Integer.MAX_VALUE ? Integer.MAX_VALUE : ((int) contentLength)) : 1024);
		int read = isr.read(buffer, 0, bufferSize);
		while (read != -1) {
			sb.append(buffer, 0, read);
			read = isr.read(buffer, 0, bufferSize);
		}
		this.response = sb.toString();
	}

	@NotNull
	public String getResponse() {
		return response;
	}

	@NotNull
	public String getResponseUnescaped() {
		if (unescapedResponse == null) {
			unescapedResponse = Unescaper.translateSafely(response);
		}
		return unescapedResponse;
	}
}
