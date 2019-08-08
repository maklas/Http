package ru.maklas.http;

import com.badlogic.gdx.utils.Array;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;
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

    /** Creates new Connection builder without default headers specified.**/
    public ConnectionBuilder() {

    }

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
        return new ConnectionBuilder()
                .headers(defaultHeaders.headers)
                .method(Http.GET);
    }

    /** new ConnectionBuilder starting with POST method request and defaultHeaders included **/
    public static ConnectionBuilder post(){
        return new ConnectionBuilder()
                .headers(defaultHeaders.headers)
                .method(Http.POST);
    }

    /** new ConnectionBuilder starting with get method request **/
    public static ConnectionBuilder get(String url){
        return new ConnectionBuilder()
                .method(Http.GET)
                .headers(defaultHeaders.headers)
                .url(url);
    }

    /** new ConnectionBuilder starting with pot method request **/
    public static ConnectionBuilder post(String url){
        return new ConnectionBuilder()
                .method(Http.POST)
                .headers(defaultHeaders.headers)
                .url(url);
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
    public ConnectionBuilder addCookiesFromStore(CookieStore cookie){
        this.cookies.addAll(cookie);
        return this;
    }

    /** Adds a cookie to Cookie header of request. **/
    public ConnectionBuilder addcookie(String key, String value){
        this.cookies.setCookie(key, value);
        return this;
    }

    /** Do this requesst using proxy **/
    public ConnectionBuilder usingProxy(@Nullable ProxyData data){
        this.proxy = data;
        return this;
    }

    /** Do this requesst using proxy **/
    public ConnectionBuilder usingProxy(String address, int port){
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
     * Writes URLEncoded values in the body of request. Only use for POST requests.
     * @param addContentTypeHeader specifies whether to add ContentType header with value application_x_www_form_url
     */
    public ConnectionBuilder writeUrlEncoded(boolean addContentTypeHeader, UrlEncoder encoder){
        return write(addContentTypeHeader, encoder.encode(Charset.forName("UTF-8")));
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

        return write(headers.getHeader(Header.ContentType.key) == null, encoder.encode(Charset.forName("UTF-8")));
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
     *
     * @param addContentTypeHeader specifies whether to add ContentType header with value application_x_www_form_url
     */
    public ConnectionBuilder writeUrlEncoded(boolean addContentTypeHeader, String key, String value){
        return write(addContentTypeHeader, new UrlEncoder().add(key, value).encode(Charset.forName("UTF-8")));
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
     *
     * @param addContentTypeHeader specifies whether to add ContentType header with value application_x_www_form_url
     */
    public ConnectionBuilder write(boolean addContentTypeHeader, String s){
        return write(addContentTypeHeader, s.getBytes(Charset.forName("UTF-8")));
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
    public ConnectionBuilder write(String s){
        return write(false, s.getBytes(Charset.forName("UTF-8")));
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
     *
     * @param addContentTypeHeader specifies whether to add ContentType header with value application_x_www_form_url
     */
    public ConnectionBuilder write(boolean addContentTypeHeader, byte[] data){
        if (addContentTypeHeader){
            headers.addUnique(Header.ContentType.application_x_www_form_url);
        }
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

        if (output != null && !Http.GET.equals(method)){
            javaCon.setDoOutput(true);
        }

        for (Header header : headers) {
            javaCon.addRequestProperty(header.key, header.value);
        }
        return new Request(javaCon, url, method, output, headers, this);
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

    byte[] getOutput() {
        return output;
    }

    String getMethod() {
        return method;
    }

    CookieStore getAssignedCookieStore() {
        return assignedCookieStore;
    }

    private static final MutableHeaderList defaultHeaders = new MutableHeaderList();
    public static MutableHeaderList getDefaultHeaderList(){
        return defaultHeaders;
    }
}
