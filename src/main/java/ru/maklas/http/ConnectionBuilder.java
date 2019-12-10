package ru.maklas.http;

import com.badlogic.gdx.utils.Array;
import org.apache.commons.lang3.StringUtils;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

public class ConnectionBuilder {

	public static final Pattern PROTOCOL_PATTERN = Pattern.compile("^[.\\-+a-zA-Z0-9]+://.+");

	private final String method;
	private String stringUrl; //Either of url or stringUrl must be set
	private URL url;
	private HeaderList headers = new HeaderList(); //Headers that will be send
	private Array<Cookie> cookies = new Array<>(); //Cookie store that will be used to produce Cookie header
	private ProxyData proxy;
	private Boolean followRedirect;
	private Boolean useCache;
	private byte[] output = null;
	private boolean built = false;
	private CookieStore assignedCookieStore; //Cookie database that will be changed according to set-cookie header

	public ConnectionBuilder(@NotNull @MagicConstant(valuesFromClass = Http.class) String method) {
		this.method = method;
	}

	/** Creates copy with the same data, assigned cookies and everything **/
	public ConnectionBuilder cpy() {
		ConnectionBuilder cb = new ConnectionBuilder(method);
		cpy(cb);
		return cb;
	}

	private void cpy(ConnectionBuilder cb) {
		cb.stringUrl = stringUrl;
		cb.url = url;
		cb.headers.addAll(headers);
		cb.cookies.addAll(cookies);
		cb.proxy = proxy;
		cb.followRedirect = followRedirect;
		cb.useCache = useCache;
		cb.built = built;
		if (output != null) {
			cb.output = new byte[output.length];
			System.arraycopy(output, 0, cb.output, 0, output.length);
		}
		cb.assignedCookieStore = assignedCookieStore;
	}

	/** new ConnectionBuilder starting with get method request **/
	public static ConnectionBuilder get() {
		return new ConnectionBuilder(Http.GET);
	}

	/** new ConnectionBuilder starting with POST method request and defaultHeaders included **/
	public static ConnectionBuilder post() {
		return new ConnectionBuilder(Http.POST);
	}

	/** new ConnectionBuilder starting with get method request **/
	public static ConnectionBuilder get(URL url) {
		return new ConnectionBuilder(Http.GET).url(url);
	}

	/** new ConnectionBuilder starting with get method request **/
	public static ConnectionBuilder get(String url) {
		return new ConnectionBuilder(Http.GET).url(url);
	}

	/** new ConnectionBuilder starting with pot method request **/
	public static ConnectionBuilder post(String url) {
		return new ConnectionBuilder(Http.POST).url(url);
	}

	/** new ConnectionBuilder starting with get method request **/
	public static ConnectionBuilder get(String base, @Nullable String path) {
		return get(combineUrl(base, path));
	}


	/** new ConnectionBuilder starting with get method request **/
	public static ConnectionBuilder get(String base, @Nullable String path, @Nullable UrlEncoder query) {
		return get(combineUrl(base, path)).write(query == null ? null : query.encode(HttpUtils.utf_8));
	}

	/** new ConnectionBuilder starting with pot method request **/
	public static ConnectionBuilder post(String base, @Nullable String path) {
		return post(combineUrl(base, path));
	}

	/**
	 * Specify connection address.
	 * format <b>"example.com"</b> is supported.
	 * If protocol is not specified,
	 * <b>http://</b> is automatically used
	 */
	public ConnectionBuilder url(String url) {
		this.stringUrl = url;
		return this;
	}

	/** Specify connection address. **/
	public ConnectionBuilder url(URL url) {
		this.url = url;
		return this;
	}

	/** Specify connection address. **/
	public ConnectionBuilder url(String base, String path, @Nullable UrlEncoder query) {
		url(combineUrl(base, path));
		if (query != null) write(query.encode().getBytes(HttpUtils.utf_8));
		return this;
	}

	/** Specify connection address. **/
	public ConnectionBuilder url(String base, String path) {
		return url(combineUrl(base, path));
	}

	/**
	 * Add a header to request
		 * @see Header
	 */
	public ConnectionBuilder header(Header header) {
		this.headers.addUnique(header);
		return this;
	}

	/**
	 * Add a header to request
		 * @see Header
	 */
	public ConnectionBuilder h(Header header) {
		return this.header(header);
	}

	/**
	 * Add a header to request
		 * @see Header
	 */
	public ConnectionBuilder header(String key, String value) {
		this.headers.addUnique(new Header(key, value));
		return this;
	}

	/**
	 * Add a header to request
		 * @see Header
	 */
	public ConnectionBuilder h(String key, String value) {
		return header(key, value);
	}

	/**
	 * Adds headers to request
		 * @see Header
	 */
	public ConnectionBuilder headers(Array<Header> headers) {
		for (Header header : headers) {
			this.headers.addUnique(header);
		}
		return this;
	}

	/**
	 * Adds headers to request
		 * @see Header
	 */
	public ConnectionBuilder headers(Header... headers) {
		for (Header header : headers) {
			this.headers.addUnique(header);
		}
		return this;
	}

	/**
	 * <p>
	 * Only fetches cookies from the store into Cookie header of the request.
	 * Doesn't update this CookieStore after getting response.
	 * Doesn't change cookies at all.
	 * <b>Cookie with non-empty and non-url-matching domain won't be added to the request.</b>
	 * </p>
	 * <p>
	 * <b>Use when:</b>
	 * <li>You have specific set of cookies and don't want them to be altered in any way by the request</li>
	 * </p>
	 */
	public ConnectionBuilder addCookies(CookieStore cookies) {
		for (Cookie cookie : cookies) {
			addCookie(cookie);
		}
		return this;
	}

	/**
	 * <p>
	 * Adds a cookie to Cookie header of request. Will replace old cookie with the same key.
	 * <b>Will have empty domain specified, meaning that it will definitely be added to the request.</b>
	 * </p>
	 * <p>
	 * <b>Use when:</b>
	 * <li>You have specific set of cookies and don't want them to be altered in any way by the request</li>
	 * <li>You need to build a fast request without hassle of creating CookieStore</li>
	 * </p>
	 */
	public ConnectionBuilder addCookie(String key, String value) {
		return addCookie(new Cookie(key, value));
	}

	/**
	 * <p>
	 * Adds a cookie to Cookie header of request. Will replace old cookie with the same key.
	 * <b>Cookie with non-empty and non-url-matching domain won't be added to the request.</b>
	 * </p>
	 */
	public ConnectionBuilder addCookie(Cookie cookie) {
		Array<Cookie> cookies = this.cookies;
		for (int i = 0; i < cookies.size; i++) {
			if (cookies.get(i).key.equals(cookie.key)) {
				cookies.set(i, cookie);
				return this;
			}
		}

		cookies.add(cookie);
		return this;
	}

	/**
	 * <p>
	 * Assigns cookies from the store and uses it in Cookie header.
	 * If this request succeed, cookies will be affected by Set-Cookie header.
	 * <b>Cookie with non-empty and non-url-matching domain won't be added to the request.</b>
	 * </p>
	 */
	public ConnectionBuilder assignCookieStore(CookieStore cookieStore) {
		for (Cookie cookie : cookieStore) {
			addCookie(cookie);
		}
		this.assignedCookieStore = cookieStore;
		return this;
	}

	/** Do this request using proxy **/
	public ConnectionBuilder proxy(@Nullable ProxyData data) {
		this.proxy = data;
		return this;
	}

	/** Do this request using proxy **/
	public ConnectionBuilder proxy(String address, int port) {
		this.proxy = new ProxyData(address, port);
		return this;
	}

	/** Whether or not to allow redirect **/
	public ConnectionBuilder allowRedirect(boolean allow) {
		this.followRedirect = allow;
		return this;
	}

	/** @see URLConnection#setUseCaches(boolean) **/
	public ConnectionBuilder cache(boolean enabled) {
		this.useCache = enabled;
		return this;
	}

	/** Appends Content-Type header and writes data to the output **/
	public ConnectionBuilder write(Header.ContentType contentType, String data) {
		headers.addUnique(contentType);
		return write(data);
	}

	/** writes data to the output **/
	public ConnectionBuilder write(Header.ContentType contentType, byte[] data) {
		headers.addUnique(contentType);
		return write(data);
	}

	/** Appends Content-Type header and writes data to the output using UTF-8 encoding **/
	public ConnectionBuilder write(String data, String mimeType, Charset encoding) {
		headers.addUnique(Header.ContentType.with(mimeType, encoding));
		return write(data.getBytes(encoding));
	}

	/** Writes data to the output using UTF-8 encoding **/
	public ConnectionBuilder write(@NotNull String data) {
		return write(data.getBytes(HttpUtils.utf_8));
	}

	/**
	 * If this request is POST, url encoded value will be put in the body of request.
	 * If method is GET, this url encoded value will be appended to URL.
	 * <p>ex:
	 * <br>
	 * url: www.google.com/ajax_test
	 * <br>
	 * UrlEncodedValues: key1=value1&key2=value2
	 * <br>
	 * result:
	 * www.google.com/ajax_test?key1=value1&key2=value2
	 * </p>
	 */
	public ConnectionBuilder writeUrlEncoded(UrlEncoder encoder) {
		return write(Header.ContentType.form_urlencoded, encoder.encode());
	}

	/** Specifies Content-Type as 'application/javascript; charset=UTF-8' and writes jsonString to the output **/
	public ConnectionBuilder writeJson(String jsonString) {
		return write(Header.ContentType.appJson, jsonString);
	}

	/** Specifies Content-Type as 'application/xml; charset=UTF-8' and writes xml to the output **/
	public ConnectionBuilder writeXml(String jsonString) {
		return write(Header.ContentType.appXml, jsonString);
	}

	/** Specifies Content-Type as 'text/plain; charset=UTF-8' and writes xml to the output **/
	public ConnectionBuilder writePlainText(String text) {
		return write(Header.ContentType.textPlain, text);
	}

	/** Specifies Content-Type as 'text/plain; charset=UTF-8' and writes xml to the output **/
	public ConnectionBuilder writeHtml(String html) {
		return write(Header.ContentType.textHtml, html);
	}

	/** Specifies Content-Type as 'application/octet-stream' and writes xml to the output **/
	public ConnectionBuilder writeBinary(byte[] data) {
		return write(Header.ContentType.octetStream, data);
	}

	/**
	 * <p><b>Output must always be encoded with UTF-8, if it's a string</b></p>
	 * If this request is POST, url encoded value will be put in the body of request.
	 * If method is GET, this url encoded value will be appended to URL.
	 * <p>ex:
	 * <br>
	 * url: www.google.com/ajax_test
	 * <br>
	 * UrlEncodedValues: key1=value1&key2=value2
	 * <br>
	 * result:
	 * www.google.com/ajax_test?key1=value1&key2=value2
	 * </p>
	 */
	public ConnectionBuilder write(byte[] data) {
		this.output = data;
		return this;
	}

	/** Whether this builder was already used. Warning! No builder should be reused **/
	public boolean isBuilt() {
		return built;
	}

	@SuppressWarnings("all")
	public Request build() throws ConnectionException {
		if (built)
			throw new ConnectionException(ConnectionException.Type.USED, "This builder has already been used", null, this, null);
		built = true;
		this.buildStarted = System.currentTimeMillis();
		URL url = null;
		try {
			url = buildUrl();
		} catch (MalformedURLException e) {
			throw new ConnectionException(ConnectionException.Type.BAD_URL, e, this, null);
		}

		Header cookieHeader = buildCookieHeader(url);
		if (cookieHeader != null) {
			headers.add(cookieHeader);
		}

		HttpURLConnection javaCon = null;
		try {
			javaCon = openConnection(url);
		} catch (IOException e) {
			throw new ConnectionException(ConnectionException.Type.IO, e, this, null);
		}
		try {
			javaCon.setRequestMethod(method);
		} catch (ProtocolException e) {
			throw new ConnectionException(ConnectionException.Type.BAD_PROTOCOL, e, this, null);
		}
		if (followRedirect != null) javaCon.setInstanceFollowRedirects(followRedirect);
		if (useCache != null) javaCon.setUseCaches(useCache);

		preprocessHeaders(url);
		for (Header header : headers) {
			javaCon.addRequestProperty(header.key, header.value);
		}
		if (output != null && !Http.GET.equals(method)) {
			javaCon.setDoOutput(true);
		}

		return new Request(javaCon, url, method, output, headers, this);
	}

	public FullResponse send() throws ConnectionException {
		Request request = build();
		return request.send();
	}

	//************//
	//* PRIVATES *//
	//************//

	private URL buildUrl() throws MalformedURLException {
		URL url;

		if (Http.GET.equals(method) && output != null) {
			String query = new String(output);
			if (this.url != null) {
				url = parseUrl(construct(this.url.toString(), query));
			} else {
				url = parseUrl(construct(stringUrl, query));
			}
		} else {
			if (this.url != null) {
				url = this.url;
			} else {
				url = parseUrl(stringUrl);
			}
		}
		return url;
	}

	/** appends query string to the url **/
	private static String construct(String url, @NotNull String query) {
		if (url == null) url = "";

		if (url.endsWith("?")) {
			return url + query;
		} else {
			if (url.endsWith("/")) {
				url = url.substring(0, url.length() - 1);
			}
			return url + "?" + query;
		}
	}

	private static URL parseUrl(String fullQuery) throws MalformedURLException {
		if (!PROTOCOL_PATTERN.matcher(fullQuery).matches()) {
			return new URL("http://" + fullQuery);
		}
		return new URL(fullQuery);
	}

	private static String combineUrl(String baseUrl, @Nullable String path) {
		if (StringUtils.isEmpty(baseUrl)) throw new RuntimeException("Base url must not be empty");
		if (StringUtils.isEmpty(path)) return baseUrl;
		StringBuilder sb = new StringBuilder();
		sb.append(baseUrl);
		if (sb.charAt(sb.length() - 1) == '/') {
			sb.setLength(sb.length() - 1);
		}
		if (path.charAt(0) == '/') {
			sb.append(path);
		} else {
			sb.append('/').append(path);
		}
		return sb.toString();
	}

	private void preprocessHeaders(URL url) {
		if (Http.autoAddHostHeader) {
			headers.addIfNotPresent(Header.Host.fromUrl(url));
		}
		if (Http.GET.equalsIgnoreCase(method)) {
			headers.remove(Header.ContentType.key);
		}
	}

	private Header buildCookieHeader(URL url) {
		if (cookies.size == 0) return null;
		StringBuilder builder = new StringBuilder();
		for (Cookie cookie : cookies) {
			if (StringUtils.isEmpty(cookie.getDomain()) || cookie.appliesToDomain(url.getHost())) {
				builder
						.append(cookie.getKey())
						.append("=")
						.append(cookie.getValue())
						.append("; ");
			}
		}

		if (builder.length() <= 2) return null;
		builder.setLength(builder.length() - 2);
		return new Header(Cookie.headerKey, builder.toString());
	}

	private HttpURLConnection openConnection(URL url) throws IOException {
		HttpURLConnection con;
		if (proxy == null) {
			con = (HttpURLConnection) url.openConnection();
		} else {
			con = (HttpURLConnection) url.openConnection(proxy.getJavaProxy());
		}
		return con;
	}

	private long buildStarted;

	long getBuildStarted() {
		return buildStarted;
	}

	HeaderList getHeaders() {
		return headers;
	}

	ProxyData getProxy() {
		return proxy;
	}

	URL getUrl() {
		return url;
	}

	String getStringUrl() {
		return stringUrl;
	}

	@Nullable
	byte[] getOutput() {
		return output;
	}

	String getMethod() {
		return method;
	}

	CookieStore getAssignedCookieStore() {
		return assignedCookieStore;
	}
}
