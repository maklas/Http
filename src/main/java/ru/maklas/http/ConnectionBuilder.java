package ru.maklas.http;

import com.badlogic.gdx.utils.Array;
import org.apache.commons.lang3.StringUtils;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.*;
import java.util.regex.Pattern;

public class ConnectionBuilder {

    public static final Pattern PROTOCOL_PATTERN = Pattern.compile("^[.\\-+a-zA-Z0-9]+://.+");

    private String stringUrl;
    private URL url;
    private String method = Http.GET;
    private HeaderList headers = new HeaderList();
    private CookieStore cookies = new CookieStore();
    private ProxyData proxy;
    private boolean allowRedirectChanged = false;
    private boolean followRedirect = true;
    private boolean useCacheChanged = false;
    private boolean useCache = false;
    private byte[] output = null;
    private boolean built = false;
    private CookieStore assignedCookieStore;

    /** Use {@link ConnectionBuilder#get(String) .get()} or  {@link ConnectionBuilder#post(String) .post()} instead **/
    public ConnectionBuilder() { }

    public ConnectionBuilder cpy(){
        ConnectionBuilder cb = new ConnectionBuilder();
        cpy(cb);
        return cb;
    }

    private void cpy(ConnectionBuilder cb){
        cb.stringUrl = stringUrl;
        cb.url = url;
        cb.method  = method;
        cb.headers.addAll(headers);
        cb.cookies.addAll(cookies);
        cb.proxy = proxy;
        cb.allowRedirectChanged = allowRedirectChanged;
        cb.followRedirect = followRedirect;
        cb.useCacheChanged = useCacheChanged;
        cb.useCache = useCache;
        cb.built = built;
        if (output != null) {
            cb.output = new byte[output.length];
            System.arraycopy(output, 0, cb.output, 0, output.length);
        }
        cb.assignedCookieStore = assignedCookieStore;
    }

    /** new ConnectionBuilder starting with get method request **/
    public static ConnectionBuilder get(){
        return new ConnectionBuilder().method(Http.GET);
    }

    /** new ConnectionBuilder starting with POST method request and defaultHeaders included **/
    public static ConnectionBuilder post(){
        return new ConnectionBuilder().method(Http.POST);
    }

    /** new ConnectionBuilder starting with get method request **/
    public static ConnectionBuilder get(String url){
        return new ConnectionBuilder()
                .method(Http.GET).url(url);
    }

    /** new ConnectionBuilder starting with pot method request **/
    public static ConnectionBuilder post(String url){
        return new ConnectionBuilder()
                .method(Http.POST).url(url);
    }

    /** new ConnectionBuilder starting with get method request **/
    public static ConnectionBuilder get(String base, @Nullable String path){
        return get(combineUrl(base, path));
    }


    /** new ConnectionBuilder starting with get method request **/
    public static ConnectionBuilder get(String base, @Nullable String path, @Nullable UrlEncoder query){
        return get(combineUrl(base, path)).write(query == null ? null : query.encode().getBytes(Charsets.utf_8));
    }

    /** new ConnectionBuilder starting with pot method request **/
    public static ConnectionBuilder post(String base, @Nullable String path){
        return post(combineUrl(base, path));
    }

    /**
     * Specify connection address.
     * format <b>"example.com"</b> is supported.
     * If protocol is not specified,
     * <b>http://</b> is automatically used
     */
    public ConnectionBuilder url(String url){
        this.stringUrl = url;
        return this;
    }

    /** Specify connection address. **/
    public ConnectionBuilder url(URL url){
        this.url = url;
        return this;
    }

    /** @see Http **/
    public ConnectionBuilder method(@MagicConstant(valuesFromClass = Http.class) String httpMethod){
        this.method = httpMethod;
        return this;
    }

    /**
     * Add a header to request
     * @see Header
     */
    public ConnectionBuilder header(Header header){
        this.headers.addUnique(header);
        return this;
    }
    /**
     * Add a header to request
     * @see Header
     */
    public ConnectionBuilder h(Header header){
        return this.header(header);
    }

    /**
     * Add a header to request
     * @see Header
     */
    public ConnectionBuilder header(String key, String value){
        this.headers.addUnique(new Header(key, value));
        return this;
    }

    /**
     * Add a header to request
     * @see Header
     */
    public ConnectionBuilder h(String key, String value){
        return header(key, value);
    }

    /**
     * Adds headers to request
     * @see Header
     */
    public ConnectionBuilder headers(Array<Header> headers){
        for (Header header : headers) {
            this.headers.addUnique(header);
        }
        return this;
    }

    /**
     * Adds headers to request
     * @see Header
     */
    public ConnectionBuilder headers(Header... headers){
        for (Header header : headers) {
            this.headers.addUnique(header);
        }
        return this;
    }

    /**
     * Only fetches cookies from the store into Cookie header of the request.
     * Doesn't update this cookiestore after getting response.
     */
    public ConnectionBuilder addCookies(CookieStore cookies){
        this.cookies.addAll(cookies);
        return this;
    }

    /** Adds a cookie to Cookie header of request. **/
    public ConnectionBuilder addCookie(String key, String value){
        this.cookies.setCookie(key, value);
        return this;
    }

    /** Do this requesst using proxy **/
    public ConnectionBuilder proxy(@Nullable ProxyData data){
        this.proxy = data;
        return this;
    }

    /** Do this requesst using proxy **/
    public ConnectionBuilder proxy(String address, int port){
        this.proxy = new ProxyData(address, port);
        return this;
    }

    /** Whether or not to allow redirect **/
    public ConnectionBuilder allowRedirect(boolean allow){
        this.allowRedirectChanged = true;
        this.followRedirect = allow;
        return this;
    }

    /** @see URLConnection#setUseCaches(boolean) **/
    public ConnectionBuilder cache(boolean enabled){
        this.useCacheChanged = true;
        this.useCache = enabled;
        return this;
    }

    /**
     * Assigns this cookie store and use it in Cookie header.
     * If this request succeed, Cookies will be updated according to their predicate,
     */
    public ConnectionBuilder assignCookieStore(CookieStore cookieStore) {
        if (cookieStore == null) return this;
        cookies.addAll(cookieStore);
        this.assignedCookieStore = cookieStore;
        return this;
    }

    /**
     * If this request is POST, url encoded value will be put in the body of request.
     * If method is GET, this url encoded value will be appended to URL.
     * <p>ex:
     * <br>
     * url: www.google.com/ajax_test
     * <br>
     * UrlEncodedValues: key1=value1&key2=value2
     *<br>
     * result:
     * www.google.com/ajax_test?key1=value1&key2=value2
     * </p>
     */
    public ConnectionBuilder writeUrlEncoded(UrlEncoder encoder){
        return write(Header.ContentType.form_urlencoded, encoder.encode());
    }

    /** Specifies Content-Type asapplication/json and writes jsonString to the output **/
    public ConnectionBuilder writeJson(String jsonString){
        return write(Header.ContentType.appJson, jsonString);
    }

    /** Specifies Content-Type as application/xml and writes xml to the output **/
    public ConnectionBuilder writeXml(String jsonString){
        return write(Header.ContentType.appXml, jsonString);
    }

    /** Appends Content-Type header and writes data to the output **/
    public ConnectionBuilder write(Header.ContentType contentType, String data){
        headers.addUnique(contentType);
        return write(data);
    }

    /** Appends Content-Type header and writes data to the output **/
    public ConnectionBuilder write(String contentType, String data){
        headers.addUnique(Header.ContentType.with(contentType));
        return write(data);
    }

    /** writes data to the output **/
    public ConnectionBuilder write(@NotNull String data){
        return write(data.getBytes(Charsets.utf_8));
    }


    /** writes data to the output **/
    public ConnectionBuilder write(String contentType, byte[] data){
        headers.addUnique(Header.ContentType.with(contentType));
        return write(data);
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
     *<br>
     * result:
     * www.google.com/ajax_test?key1=value1&key2=value2
     * </p>
     */
    public ConnectionBuilder write(byte[] data){
        this.output = data;
        return this;
    }

    /** Whether this builder was already used. Warning! No builder should be reused **/
    public boolean isBuilt() {
        return built;
    }

    @SuppressWarnings("all")
    public Request build() throws ConnectionException {
        if (built) throw new ConnectionException(ConnectionException.Type.USED, "This builder has already been used", null, this, null);
        built = true;
        this.buildStarted = System.currentTimeMillis();
        if (cookies.size() > 0) headers.addUnique(cookies.toHeader());
        URL url = null;
        try {
            url = buildUrl();
        } catch (MalformedURLException e) {
            throw new ConnectionException(ConnectionException.Type.BAD_URL, e, this, null);
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
        if (allowRedirectChanged) javaCon.setInstanceFollowRedirects(followRedirect);
        if (useCacheChanged) javaCon.setUseCaches(useCache);

        preprocessHeaders(url);
        for (Header header : headers) {
            javaCon.addRequestProperty(header.key, header.value);
        }
        if (output != null && !Http.GET.equals(method)){
            javaCon.setDoOutput(true);
        }

        return new Request(javaCon, url, method, output, headers, this);
    }

    private void preprocessHeaders(URL url) {
        if (Http.autoAddHostHeader){
            headers.replaceIfNotPresent(Header.Host.fromUrl(url));
        }
        if (Http.GET.equalsIgnoreCase(method)) {
            headers.remove(Header.ContentType.key);
        }
    }

    public Response send() throws ConnectionException {
        Request request = build();
        Response response = request.send();
        return response;
    }



    //************//
    //* PRIVATES *//
    //************//

    private URL buildUrl() throws MalformedURLException {
        URL url;

        if (Http.GET.equals(method) && output != null){
            String query = new String(output);
            if (this.url != null){
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

    /** appends query string to the url  **/
    private static String construct(String url, @NotNull String query){
        if (url == null) url = "";

        if (url.endsWith("?")){
            return url + query;
        } else {
            if (url.endsWith("/")){
                url = url.substring(0, url.length() - 1);
            }
            return url + "?" + query;
        }
    }

    private static URL parseUrl(String fullQuery) throws MalformedURLException {
        if (!PROTOCOL_PATTERN.matcher(fullQuery).matches()){
            return new URL("http://" + fullQuery);
        }
        return new URL(fullQuery);
    }

    private static String combineUrl(String baseUrl, @Nullable String path) {
        if (StringUtils.isEmpty(baseUrl)) throw new RuntimeException("Base url must not be empty");
        if (StringUtils.isEmpty(path)) return baseUrl;
        baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        path = path.startsWith("/") ? path : "/" + path;
        return baseUrl + path;
    }

    private static String combineUrl(String baseUrl, String path, String query) {
        String url = combineUrl(baseUrl, path);
        if (StringUtils.isEmpty(query)) return url;
        url = url.endsWith("?") ? url.substring(0, url.length() - 1) : url;
        query = query.startsWith("?") ? query : "?" + query;
        return url + query;
    }

    private HttpURLConnection openConnection(URL url) throws IOException {
        HttpURLConnection con;
        if (proxy == null) {
            con = (HttpURLConnection) url.openConnection();
        } else {
            Proxy p = new Proxy(proxy.getType(), new InetSocketAddress(proxy.getAddress(), proxy.getPort()));
            con = (HttpURLConnection) url.openConnection(p);
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

    String getStringUrl(){
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
