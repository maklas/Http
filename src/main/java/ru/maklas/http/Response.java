package ru.maklas.http;

import org.apache.commons.lang3.StringUtils;
import org.brotli.dec.BrotliInputStream;
import org.jetbrains.annotations.Nullable;
import ru.maklas.http.utils.ResponseBean;
import ru.maklas.http.utils.ResponseParseException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/** Response for {@link Request} **/
public abstract class Response {

	private final HttpURLConnection javaCon;
	private final URL requestUrl;
	private final int msToConnect;
	private final Request request;
	private ResponseHeaders headerCache;
	private int responseCode;
	private String responseMessage;
	private CookieChangeList cookieChangeList;
	protected int contentLength; //Content length from the headers. Doesn't actually represent length of the content. Might be 0
	private Charset charset;

	public Response(HttpURLConnection javaCon, URL url, int msToConnect, Request request) {
		this.javaCon = javaCon;
		requestUrl = url;
		this.msToConnect = msToConnect;
		this.request = request;
		try {
			this.responseCode = javaCon.getResponseCode();
			this.responseMessage = javaCon.getResponseMessage();
		} catch (Exception ignore) {
		}
		ResponseHeaders headers = getHeaders();
		if (request.getBuilder().getAssignedCookieStore() != null) {
			cookieChangeList = headers.updateCookiesIfChanged(javaCon.getURL(), request.getBuilder().getAssignedCookieStore());
		}
	}

	/** Time took to connect to the service **/
	public int getResponseTime() {
		return msToConnect;
	}

	/** Http code of the response. Can be translated with {@link Http#getResponseCodeMeaning(int, String)} **/
	public int getResponseCode() {
		return responseCode;
	}

	/** The request **/
	public Request getRequest() {
		return request;
	}

	/**
	 * Response URL can vary from the {@link #getRequestUrl()} if redirection is allowed and it happened automatically
	 * You can check if redirection took place with {@link #isRedirect()} or {@link #urlsMatch()}
	 */
	public URL getResponseUrl() {
		return javaCon.getURL();
	}

	/** URL used to connect to the service **/
	public URL getRequestUrl() {
		return requestUrl;
	}

	/** Checks if request url matches response url. Can indicate redirection **/
	public boolean urlsMatch() {
		return getRequestUrl().toString().equalsIgnoreCase(getResponseUrl().toString());
	}

	/** Response message. Should match HttpResponseCode **/
	public String getResponseMessage() {
		return responseMessage;
	}

	/** Underlying HttpUrlConnection that was used to connect **/
	public HttpURLConnection getJavaCon() {
		return javaCon;
	}

	/** Response headers **/
	public ResponseHeaders getHeaders() {
		if (headerCache == null) {
			headerCache = new ResponseHeaders();
			try {
				Set<Map.Entry<String, List<String>>> entries = javaCon.getHeaderFields().entrySet();
				for (Map.Entry<String, List<String>> entry : entries) {
					if (entry.getKey() == null || entry.getValue() == null) continue;
					for (String s : entry.getValue()) {
						if (s != null) {
							headerCache.add(new Header(entry.getKey(), s));
						}
					}
					if ("content-length".equalsIgnoreCase(entry.getKey())) {
						try {
							this.contentLength = Integer.parseInt(entry.getValue().get(0));
						} catch (Throwable ignore) {
						}
					} else if (Header.ContentType.key.equalsIgnoreCase(entry.getKey())) {
						try {
							String afterCharset = StringUtils.substringAfter(entry.getValue().get(0), "charset=");
							String responseCharset = StringUtils.substringBefore(afterCharset, ";");
							charset = Charset.forName(responseCharset);
						} catch (Exception ignore) {
						}
					}
				}
			} catch (Throwable ignore) {
			}
		}
		return headerCache;
	}

	/** Returns true if this is a redirect page. Use {@link #getRedirectUrl()} to get redirection path; **/
	public boolean isRedirect() {
		if (getResponseCode() == 200) {
			return false;
		}

		int[] redirectionCodes = {301, 302, 303, 307, 308};
		for (int redirectionCode : redirectionCodes) {
			if (getResponseCode() == redirectionCode) {
				return true;
			}
		}

		return StringUtils.isNotEmpty(getHeaders().getHeaderValue(Header.Location.key));
	}

	/** Redirection URL. Might be null if not found. **/
	@Nullable
	public String getRedirectUrl() {
		Header locationHeader = getHeaders().getHeader(Header.Location.key);
		return locationHeader == null ? null : locationHeader.value;
	}

	/**
	 * Changelist of cookies after this response. Which were added or removed.
	 * Is <b>null</b> if no cookieStore was assigned in ConnectionBuilder
	 */
	@Nullable
	public CookieChangeList getCookieChangeList() {
		return cookieChangeList;
	}

	/** Charset used to decode response **/
	public Charset getCharset() {
		return charset != null ? charset : HttpUtils.utf_8;
	}

	protected InputStream _getInputStream() throws IOException {
		return wrapStream(javaCon.getInputStream());
	}

	protected InputStream _getErrorInputStream() throws IOException {
		return wrapStream(javaCon.getErrorStream());
	}

	InputStream wrapStream(InputStream is) throws IOException {
		if (is == null) return new ByteArrayInputStream(new byte[0]);
		Header contentEncodingHeader = getHeaders().getHeader(Header.ContentEncoding.key);
		if (contentEncodingHeader == null) return is;
		String[] encodings = StringUtils.split(contentEncodingHeader.value, ',');
		for (String encoding : encodings) {
			if (StringUtils.containsIgnoreCase(encoding, "gzip")) {
				is = new GZIPInputStream(is);
			} else if (StringUtils.containsIgnoreCase(encoding, "deflate")) {
				is = new InflaterInputStream(is);
			} else if (StringUtils.containsIgnoreCase(encoding, "br")) {
				is = new BrotliInputStream(is);
			} else if (!StringUtils.containsIgnoreCase(encoding, "identity")) {
				System.err.println("Unsupported encoding '" + StringUtils.trim(encoding) + "'");
			}
		}
		return is;
	}

	public String toString() {
		return getTrace();
	}

	/** Information about Http exchange. Full data on request and response. Human-readable **/
	public String getTrace() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		printTrace(bos);
		return new String(bos.toByteArray());
	}

	public <T extends ResponseBean> T parse(T bean) throws ResponseParseException {
		try {
			bean.parse(this);
		} catch (Exception e) {
			throw new ResponseParseException(this, bean, e);
		}
		return bean;
	}

	public <T extends ResponseBean> T parseOrFail(T bean) {
		try {
			bean.parse(this);
		} catch (Exception e) {
			throw new RuntimeException(new ResponseParseException(this, bean, e));
		}
		return bean;
	}

	/** Information about Http exchange. Full data on request and response. Human-readable **/
	public void printTrace(OutputStream out) {
		PrintWriter w = new PrintWriter(out);
		try {
			HeaderList requestHeaders = getRequest().getRequestHeaders();
			URL requestUrl = getRequest().getRequestUrl();
			int responseCode = getResponseCode();
			String responseMessage = getResponseMessage();

			w.println("------- REQUEST -------");
			w.println("Date: " + (request.timeRequested == 0 ? "-" : new Date(request.timeRequested).toString()));
			w.print(getRequest().getBuilder().getMethod()); w.print(" "); w.print(requestUrl.toExternalForm()); w.println(" HTTP/1.1");
			ProxyData proxy = getRequest().getBuilder().getProxy();
			if (proxy != null) w.println("Proxy: " + proxy);
			for (Header requestHeader : requestHeaders) {
				w.print("  ");
				w.println(requestHeader);
			}
			String outputAsString = getRequest().getOutputAsString();
			boolean isGetRequest = Http.GET.equalsIgnoreCase(getRequest().getBuilder().getMethod());
			if (!isGetRequest && outputAsString != null) {
				w.println("Body:");
				w.println(outputAsString);
			} else if (!isGetRequest && getRequest().getMultipartWriter() != null) {
				w.println("Body:");
				w.println(getRequest().getMultipartWriter().getTraceRepresentation(getRequest().getMultipartBoundary()));
			}
			w.println();
			w.println("------- RESPONSE -------");
			w.println("URL: " + getResponseUrl());
			w.println("Time: " + getResponseTime() + " ms");
			w.println("Code: " + responseCode + " (" + Http.getResponseCodeMeaning(responseCode, "???") + ")");
			w.println("Message: " + responseMessage);
			for (Header header : getHeaders()) {
				w.print("  ");
				w.println(header);
			}
			if (hasBody()) {
				w.println();
				w.println("------- BODY -------");
				printBodyTrace(w);
			}
			w.println("------- END -------");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			w.flush();
		}
	}

	abstract protected boolean hasBody();

	abstract protected void printBodyTrace(PrintWriter writer);
}
