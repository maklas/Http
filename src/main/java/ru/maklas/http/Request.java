package ru.maklas.http;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sun.net.www.MessageHeader;
import sun.net.www.protocol.https.DelegateHttpsURLConnection;

import java.io.*;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Request {

	private HttpURLConnection javaCon;
	private final URL url;
	private final String method;
	private byte[] output;
	private String multipartBoundary;
	private final MultipartWriter multipartWriter;
	private final HeaderList reqHeaders;
	private final ConnectionBuilder builder;
	/** Time when the Request was sent and HttpResponseCode received **/
	long timeRequested;

	/** Already connected! **/
	Request(HttpURLConnection javaCon, URL url, String method, byte[] output, String multipartBoundary, MultipartWriter multipartWriter, HeaderList reqHeaders, ConnectionBuilder builder) {
		this.javaCon = javaCon;
		this.url = url;
		this.method = method;
		this.output = output;
		this.multipartBoundary = multipartBoundary;
		this.multipartWriter = multipartWriter;
		this.reqHeaders = reqHeaders;
		this.builder = builder;
	}

	public ConnectionBuilder getBuilder() {
		return builder;
	}

	/** Data written to output **/
	@Nullable
	public byte[] getOutput() {
		return output;
	}

	public String getMultipartBoundary() {
		return multipartBoundary;
	}

	public MultipartWriter getMultipartWriter() {
		return multipartWriter;
	}

	/** Data written to output, unless written with inputStream of MultipartWriter **/
	@Nullable
	public String getOutputAsString() {
		if (output != null) {
			return new String(output, HttpUtils.utf_8);
		}
		if (multipartWriter != null && !multipartWriter.hasStream()) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			try {
				multipartWriter.encode(bos, getMultipartBoundary());
				return new String(bos.toByteArray(), HttpUtils.utf_8);
			} catch (IOException ignore) {}
		}
		return null;
	}

	public URL getRequestUrl() {
		return url;
	}

	public HeaderList getRequestHeaders() {
		return reqHeaders;
	}

	/** Use it to send data yourself. Throws exception if data was specified in ConnectionBuilder **/
	public OutputStream getOutputStream() throws IOException {
		if (output != null || multipartWriter != null) throw new IOException("Output data was already send!!!");
		return javaCon.getOutputStream();
	}

	/** Use it to send data yourself. Throws exception if data was specified in ConnectionBuilder **/
	public OutputStreamWriter getWriter() throws IOException {
		if (output != null || multipartWriter != null) throw new IOException("Output data was already send!!!");
		return new OutputStreamWriter(getOutputStream());
	}

	/** Underlying HttpUrlConnection that is used to connect **/
	public HttpURLConnection getJavaCon() {
		return javaCon;
	}


	/** Sends request **/
	public FullResponse send() throws ConnectionException {
		javaCon.setConnectTimeout(Http.defaultConnectTimeOut);
		javaCon.setReadTimeout(Http.defaultReadTimeOut);
		return _send(null);
	}

	/** Sends request **/
	public FullResponse send(HttpCallback callback) throws ConnectionException {
		javaCon.setConnectTimeout(Http.defaultConnectTimeOut);
		javaCon.setReadTimeout(Http.defaultReadTimeOut);
		return _send(callback);
	}

	/**
	 * Sends request. Note that timeOut will be for both - the connection and read time, so in theory,
	 * specified time can be doubled. For much stricter timeing, use {@link #send(int, int)}
	 */
	public FullResponse send(int timeOut) throws ConnectionException {
		javaCon.setConnectTimeout(timeOut);
		javaCon.setReadTimeout(timeOut);
		return _send(null);
	}

	/** Sends request **/
	public FullResponse send(int connectTimeOut, int readTimeOut) throws ConnectionException {
		javaCon.setConnectTimeout(connectTimeOut);
		javaCon.setReadTimeout(readTimeOut);
		return _send(null);
	}

	/** Sends request **/
	public FullResponse send(int connectTimeOut, int readTimeOut, HttpCallback callback) throws ConnectionException {
		javaCon.setConnectTimeout(connectTimeOut);
		javaCon.setReadTimeout(readTimeOut);
		return _send(callback);
	}

	/** Sends request. You can try parsing response yourself **/
	public ConsumedResponse send(@NotNull ResponseReceiver responseReceiver) throws ConnectionException {
		javaCon.setConnectTimeout(Http.defaultConnectTimeOut);
		javaCon.setReadTimeout(Http.defaultReadTimeOut);
		return _send(null, responseReceiver);
	}

	/** Sends request. You can try parsing response yourself **/
	public ConsumedResponse send(int connectTimeOut, int readTimeOut, ResponseReceiver responseReceiver) throws ConnectionException {
		javaCon.setConnectTimeout(connectTimeOut);
		javaCon.setReadTimeout(readTimeOut);
		return _send(null, responseReceiver);
	}

	private FullResponse _send(HttpCallback callback) throws ConnectionException {
		int ttc = (int) connect(callback);
		FullResponse response = new FullResponse(javaCon, url, ttc, this);
		javaCon.disconnect();
		if (callback != null) callback.finished(response);
		return response;
	}

	private ConsumedResponse _send(HttpCallback callback, @NotNull ResponseReceiver receiver) throws ConnectionException {
		int ttc = (int) connect(callback);
		try {
			ConsumedResponse response = new ConsumedResponse(javaCon, url, ttc, this, receiver);
			if (callback != null) callback.finished(response);
			return response;
		} finally {
			javaCon.disconnect();
		}
	}

	/** Returns time taken to connect **/
	private long connect(HttpCallback callback) throws ConnectionException {
		if (callback != null) callback.start(this);

		if ((output != null || multipartWriter != null) && !Http.GET.equals(method)) {
			try {
				OutputStream os = javaCon.getOutputStream();
				if (output != null) {
					os.write(output);
				} else {
					multipartWriter.encode(os, getMultipartBoundary());
				}
				os.flush();
				os.close();
				if (callback != null) callback.wroteBody();
			} catch (IOException e) {
				ConnectionException ce = new ConnectionException(e, getBuilder(), this);
				if (callback != null) callback.interrupted(ce);
				throw ce;
			}
		}
		if (callback != null) callback.connecting();
		long before = System.currentTimeMillis();
		try {
			javaCon.connect();
		} catch (IOException e) {
			ConnectionException ce = new ConnectionException(e, getBuilder(), this);
			if (callback != null) callback.interrupted(ce);
			throw ce;
		}
		try {
			int responseCode = javaCon.getResponseCode();
			if (Http.fetchJavaHeaders) {
				appendJavaHeaders();
			}
			timeRequested = System.currentTimeMillis();
			long ttc = timeRequested - before;
			if (callback != null) callback.connected(responseCode);
			return ttc;
		} catch (IOException e) {
			ConnectionException ce = new ConnectionException(ConnectionException.Type.CONNECTION_ERROR, e, getBuilder(), this);
			if (callback != null) callback.interrupted(ce);
			throw ce;
		}
	}

	private void appendJavaHeaders() {
		Map<String, List<String>> javaRequests = getJavaRequests();
		for (Map.Entry<String, List<String>> e : javaRequests.entrySet()) {
			if (e != null && e.getKey() != null) {
				if (reqHeaders.getHeader(e.getKey()) == null) {
					if (e.getValue() != null && e.getValue().size() > 0 && e.getValue().get(0) != null) {
						reqHeaders.add(new Header(e.getKey(), e.getValue().get(0)));
					}
				}
			}
		}
	}

	private Map<String, List<String>> getJavaRequests() {
		MessageHeader requests = null;

		try {
			if (javaCon instanceof sun.net.www.protocol.http.HttpURLConnection) {
				Field requestsField = sun.net.www.protocol.http.HttpURLConnection.class.getDeclaredField("requests");
				requestsField.setAccessible(true);
				requests = (MessageHeader) requestsField.get(javaCon);
			} else if (javaCon instanceof sun.net.www.protocol.https.HttpsURLConnectionImpl) {
				DelegateHttpsURLConnection delegate = (DelegateHttpsURLConnection) getFieldValue(javaCon, "delegate");
				requests = (MessageHeader) getFieldValue(delegate, sun.net.www.protocol.http.HttpURLConnection.class, "requests");
			} else if ("com.android.okhttp.internal.huc.HttpURLConnectionImpl".equals(javaCon.getClass().getName())) {

				String[] headerArray = (String[]) getFieldValueDeep(javaCon, "httpEngine", "networkRequest", "headers", "namesAndValues");
				if (headerArray != null) {
					requests = new MessageHeader();
					for (int i = 0; i < headerArray.length; i += 2) {
						String key = headerArray[i];
						String val = headerArray[i + 1];
						requests.add(key, val);
					}
				}
			} else if ("com.android.okhttp.internal.huc.HttpsURLConnectionImpl".equals(javaCon.getClass().getName())) {
				String[] headerArray = (String[]) getFieldValueDeep(javaCon, "delegate", "httpEngine", "networkRequest", "headers", "namesAndValues");
				if (headerArray != null) {
					requests = new MessageHeader();
					for (int i = 0; i < headerArray.length; i += 2) {
						String key = headerArray[i];
						String val = headerArray[i + 1];
						requests.add(key, val);
					}
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}

		Map<String, List<String>> headers;

		if (requests != null) {
			headers = requests.getHeaders();
		} else {
			headers = new HashMap<>();
		}

		return headers;
	}

	private static Object getFieldValue(Object o, String fieldName) throws Exception {
		return getFieldValue(o, o.getClass(), fieldName);
	}

	private static Object getFieldValue(Object o, Class clazz, String fieldName) throws Exception {
		Field field = clazz.getDeclaredField(fieldName);
		field.setAccessible(true);
		Object val = field.get(o);
		return val;
	}

	private static Object getFieldValueDeep(Object o, String... path) throws Exception {
		Object val = o;
		for (String s : path) {
			val = getFieldValue(val, s);
			if (val == null) {
				return null;
			}
		}

		return val;
	}
}
