package ru.maklas.http;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.util.zip.Deflater;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;

public class Response {

    private static final Charset utf8 = Charset.forName("UTF-8");

    private final HttpURLConnection javaCon;
    private final URL requestUrl;
    private final int msToConnect;
    private final Request request;
    private ResponseHeaders headerCache;
    private boolean unescaped = false;
    private String responseUnescaped;
    private String responseBodyAsIs;
    private int responseCode;
    private String responseMessage;
    private CookieChangeList cookieChangeList;
    private boolean errorStreamUsed;
    private Exception bodyException;
    private int contentLength; //Content length from the headers. Doesn't actually represent length of the content. Might be 0
    private Charset charset;


    public Response(HttpURLConnection javaCon, URL url, int msToConnect, Request request, HttpCallback callback) throws IOException {
        this.javaCon = javaCon;
        requestUrl = url;
        this.msToConnect = msToConnect;
        this.request = request;
        this.responseCode = javaCon.getResponseCode();
        this.responseMessage = javaCon.getResponseMessage();
        ResponseHeaders headers = getHeaders();
        if (request.getBuilder().getAssignedCookieStore() != null)
            cookieChangeList = headers.updateCookiesIfChanged(request.getBuilder().getAssignedCookieStore());
        _getBody();
        javaCon.disconnect();
        if (callback != null) callback.finished(this);
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
    public URL getResponseUrl(){
        return javaCon.getURL();
    }

    /** URL used to connect to the service **/
    public URL getRequestUrl() {
        return requestUrl;
    }

    /** Checks if request url matches response url. Can indicate redirection **/
    public boolean urlsMatch(){
        return getRequestUrl().toString().equalsIgnoreCase(getResponseUrl().toString());
    }

    /** Response message. Should match HttpResponseCode**/
    public String getResponseMessage() {
        return responseMessage;
    }

    /** Underlying HttpUrlConnection that was used to connect **/
    public HttpURLConnection getJavaCon() {
        return javaCon;
    }

    /** Exception that was thrown while attempting to obtain body of the response **/
    public Exception getBodyException() {
        return bodyException;
    }

    /** Response headers **/
    public ResponseHeaders getHeaders() {
        if (headerCache == null){
            headerCache = new ResponseHeaders();
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
                    } catch (Throwable ignore) { }
                } else if (Header.ContentType.key.equalsIgnoreCase(entry.getKey())){
                    try {
                        String afterCharset = StringUtils.substringAfter(entry.getValue().get(0), "charset=");
                        String responseCharset = StringUtils.substringBefore(afterCharset, ";");
                        charset = Charset.forName(responseCharset);
                    } catch (Exception ignore) {}
                }
            }
        }
        return headerCache;
    }

    /** If true, error stream was used to fetch body **/
    public boolean isError() {
        return errorStreamUsed;
    }

    /** Returns true if this is a redirect page. Use {@link #getRedirectUrl()} to get redirection path; **/
    public boolean isRedirect(){
        if (getResponseCode() == 200){
            return false;
        }
        Header locationHeader = getHeaders().getHeader(Header.Location.key);
        if (locationHeader != null){
            try {
                new URL(locationHeader.value);
            } catch (MalformedURLException e) {
                return false;
            }
            return true;
        }

        int[] redirectionCodes = {301, 302, 303, 307, 308};
        for (int redirectionCode : redirectionCodes) {
            if (getResponseCode() == redirectionCode){
                return true;
            }
        }

        return false;
    }

    /** Redirection URL. Might be null if not found. **/
    @Nullable
    public String getRedirectUrl(){
        Header locationHeader = getHeaders().getHeader(Header.Location.key);
        return locationHeader == null ? null : locationHeader.value;
    }

    /** Changelist of cookies after this response. Which were added or removed **/
    public CookieChangeList getCookieChangeList() {
        return cookieChangeList;
    }

    /**
     * Unescaped body of the response. Changed version, where escaping of characters was removed.
     * Usually produces more human-readable result.
     */
    public String getBodyUnescaped(){
        if (!unescaped){
            if (responseBodyAsIs != null){
                try {
                    responseUnescaped = unescape(responseBodyAsIs);
                } catch (Exception e) {
                    e.printStackTrace();
                    responseUnescaped = responseBodyAsIs;
                }
            }
            unescaped = true;
        }
        return responseUnescaped;
    }

    /** Body of the response as it was received, unchanged **/
    public String getBodyAsIs(){
        return responseBodyAsIs;
    }

    private String _getBody() throws IOException {
        if (responseBodyAsIs == null) {

            Reader reader;
            try {
                reader = _getReader(getCharset());
            } catch (Exception e) {
                errorStreamUsed = true;
                bodyException = e;
                try {
                    reader = _getErrorReader(getCharset());
                } catch (Exception e1) {
                    responseBodyAsIs = "";
                    bodyException = e1;
                    return responseBodyAsIs;
                }
            }

            int bufferSize;
            if (contentLength <= 0) {
                bufferSize = 8192;
            } else if (contentLength < 16384) {
                bufferSize = 4096;
            } else if (contentLength < 131072){
                bufferSize = 8192;
            } else {
                bufferSize = 16384;
            }
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[bufferSize];
            int read = reader.read(buffer);
            while (read != -1) {
                sb.append(buffer, 0, read);
                read = reader.read(buffer);
            }
            responseBodyAsIs = sb.toString();
            reader.close();
        }
        return responseBodyAsIs;
    }

    private static String unescape(String input){
        return StringEscapeUtils.unescapeJava(input.replaceAll("\\\\\\\\u", "\\\\u"));
    }

    private InputStream _getInputStream() throws IOException {
        return wrapStream(javaCon.getInputStream());
    }

    private InputStream _getErrorInputStream() throws IOException {
        return wrapStream(javaCon.getErrorStream());
    }

    private Charset getCharset(){
        return charset == null ? utf8 : charset;
    }

    private InputStream wrapStream(InputStream is) throws IOException {
        Header contentEncodingHeader = getHeaders().getHeader(Header.ContentEncoding.key);
        if (contentEncodingHeader == null) return is;
        String[] encodings = StringUtils.split(contentEncodingHeader.value, ',');
        for (String encoding : encodings) {
            if (StringUtils.containsIgnoreCase(encoding, "gzip")){
                is = new GZIPInputStream(is);
            } else if (StringUtils.containsIgnoreCase(encoding, "deflate")){
                is = new DeflaterInputStream(is, new Deflater());
            } else if (!StringUtils.containsIgnoreCase(encoding, "identity")){
                System.err.println("Unsupported encoding '" + StringUtils.trim(encoding) + "'");
            }
        }
        return is;
    }

    private InputStreamReader _getErrorReader(Charset charset) throws IOException {
        return new InputStreamReader(_getErrorInputStream(), charset);
    }

    private InputStreamReader _getReader(Charset charset) throws IOException {
        return new InputStreamReader(_getInputStream(), charset);
    }

    public String toString() {
        return getTrace();
    }

    /** Information about Http exchange. Full data on request and response. Human-readable **/
    public String getTrace(){
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

    public <T extends ResponseBean> T parseOrFail(T bean){
        try {
            bean.parse(this);
        } catch (Exception e) {
            throw new RuntimeException(new ResponseParseException(this, bean, e));
        }
        return bean;
    }

    /** Information about Http exchange. Full data on request and response. Human-readable **/
    public void printTrace(OutputStream out){
        PrintWriter w = new PrintWriter(out);
        try {
            HeaderList requestHeaders = getRequest().getRequestHeaders();
            URL requestUrl = getRequest().getRequestUrl();
            String fullResponse = getBodyUnescaped();
            int responseCode = getResponseCode();
            String responseMessage = getResponseMessage();

            w.println("------- REQUEST -------");
            w.println("Date: " + (request.timeRequested == 0 ? "-" : new Date().toString()));
            w.println("URL: " + requestUrl);
            w.println("Method: " + getRequest().getBuilder().getMethod());
            ProxyData proxy = getRequest().getBuilder().getProxy();
            if (proxy != null) w.println("Proxy: " + proxy);
            for (Header requestHeader : requestHeaders) {
                w.print("  "); w.println(requestHeader);
            }
            if (!Http.GET.equalsIgnoreCase(getRequest().getBuilder().getMethod()) && getRequest().getSendingBody() != null) {
                w.println("Body:");
                w.println(new String(getRequest().getSendingBody(), Charset.forName("UTF-8")));
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
            w.println();
            w.println("------- BODY -------");
            w.println(fullResponse);
            w.println();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            w.flush();
        }
    }
}
