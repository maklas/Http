package ru.maklas.http;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import ru.maklas.http.utils.ResponseBean;
import ru.maklas.http.utils.ResponseParseException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Date;
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
    private String fullResponse;
    private String fullResponseUnescaped;
    private int responseCode;
    private String responseMessage;
    private CookieChangeList cookieChangeList;
    private boolean errorStreamUsed;
    private Exception bodyException;
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

    public int getResponseTime() {
        return msToConnect;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public Request getRequest() {
        return request;
    }

    public URL getResponseUrl(){
        return javaCon.getURL();
    }

    public URL getRequestUrl() {
        return requestUrl;
    }

    public boolean urlsMatch(){
        return getRequestUrl().toString().equalsIgnoreCase(getResponseUrl().toString());
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public HttpURLConnection getJavaCon() {
        return javaCon;
    }

    public Exception getBodyException() {
        return bodyException;
    }

    public ResponseHeaders getHeaders() {
        if (headerCache == null){
            headerCache = new ResponseHeaders(javaCon.getHeaderFields());
        }
        return headerCache;
    }

    /**
     * If true, error stream was used to fetch responseData
     */
    public boolean isError() {
        return errorStreamUsed;
    }

    public CookieChangeList getCookieChangeList() {
        return cookieChangeList;
    }

    public String getBody(){
        return fullResponse;
    }

    public String getEscapedBody(){
        return fullResponseUnescaped;
    }

    private String _getBody() throws IOException {
        if (fullResponse == null) {
            BufferedReader reader;
            try {
                reader = new BufferedReader(_getReader(getCharset()));
            } catch (Exception e) {
                errorStreamUsed = true;
                bodyException = e;
                try {
                    reader = new BufferedReader(_getErrorReader(getCharset()));
                } catch (Exception e1) {
                    fullResponseUnescaped = "";
                    fullResponse = "";
                    bodyException = e1;
                    return fullResponse;
                }
            }

            StringBuilder builder = new StringBuilder();
            String s = reader.readLine();
            while (s != null) {
                builder.append(s);
                builder.append("\n");
                s = reader.readLine();
            }
            fullResponseUnescaped = builder.toString();
            try {
                fullResponse = unescape(fullResponseUnescaped);
            } catch (Exception e) {
                e.printStackTrace();
                fullResponse = fullResponseUnescaped;
            }
            reader.close();
        }
        return fullResponse;
    }

    public static String unescape(String input){
        return StringEscapeUtils.unescapeJava(input.replaceAll("\\\\\\\\u", "\\\\u"));
    }

    private InputStream _getInputStream() throws IOException {
        return wrapStream(javaCon.getInputStream());
    }

    private InputStream _getErrorInputStream() throws IOException {
        return wrapStream(javaCon.getErrorStream());
    }

    private Charset getCharset(){
        if (charset == null) {
            charset = utf8;
            Header contentType = getHeaders().getHeader(Header.ContentType.key);
            if (contentType != null) {
                try {
                    String afterCharset = StringUtils.substringAfter(contentType.value, "charset=");
                    String responseCharset = StringUtils.substringBefore(afterCharset, ";");
                    charset = Charset.forName(responseCharset);
                } catch (Exception ignore) {}
            }
        }
        return charset;
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


    public void printTrace(OutputStream out){
        PrintWriter w = new PrintWriter(out);
        try {
            HeaderList requestHeaders = getRequest().getRequestHeaders();
            URL requestUrl = getRequest().getRequestUrl();
            String fullResponse = getBody();
            int responseCode = getResponseCode();
            String responseMessage = getResponseMessage();

            w.println("------- REQUEST -------");
            w.println("Date: " + new Date().toString());
            w.println("URL: " + requestUrl);
            w.println("Method: " + getRequest().getBuilder().getMethod());
            ProxyData proxy = getRequest().getBuilder().getProxy();
            if (proxy != null) w.println("Proxy: " + proxy);
            for (Header requestHeader : requestHeaders) {
                w.print("  "); w.println(requestHeader);
            }
            if (getRequest().getSendingBody() != null) {
                w.println("HTTP Body:");
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
