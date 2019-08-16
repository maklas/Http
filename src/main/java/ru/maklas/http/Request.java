package ru.maklas.http;

import org.jetbrains.annotations.Nullable;
import sun.net.www.MessageHeader;
import sun.net.www.protocol.https.DelegateHttpsURLConnection;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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
    private final HeaderList reqHeaders;
    private final ConnectionBuilder builder;
    /** Time when the Request was sent and HttpResponseCode received **/
    long timeRequested;

    /** Already connected! **/
    Request(HttpURLConnection javaCon, URL url, String method, byte[] output, HeaderList reqHeaders, ConnectionBuilder builder) {
        this.javaCon = javaCon;
        this.url = url;
        this.method = method;
        this.output = output;
        this.reqHeaders = reqHeaders;
        this.builder = builder;
    }

    public ConnectionBuilder getBuilder() {
        return builder;
    }

    /** Data written to output **/
    @Nullable
    public byte[] getSendingBody() {
        return output;
    }

    /** Data written to output **/
    @Nullable
    public String getSendingBodyAsString() {
        return output == null ? null : new String(output);
    }

    private Response _send(HttpCallback callback) throws ConnectionException {
        if (callback != null) callback.start(this);
        if (output != null && !Http.GET.equals(method)){
                try {
                    DataOutputStream os = new DataOutputStream(javaCon.getOutputStream());
                    os.write(output);
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
        Response response;
        try {
            int responseCode = javaCon.getResponseCode();
            if (Http.fetchJavaHeaders){
                appendJavaHeaders();
            }
            timeRequested = System.currentTimeMillis();
            long ttc = timeRequested - before;
            if (callback != null) callback.connected(responseCode);
            response = new Response(javaCon, url, (int) ttc, this, callback);
        } catch (IOException e) {
            ConnectionException ce = new ConnectionException(ConnectionException.Type.CONNECTION_ERROR, e, getBuilder(), this);
            if (callback != null) callback.interrupted(ce);
            throw ce;
        }
        return response;
    }

    public Response send() throws ConnectionException {
        javaCon.setConnectTimeout(Http.defaultConnectTimeOut);
        javaCon.setReadTimeout(Http.defaultReadTimeOut);
        return _send(null);
    }

    public Response send(HttpCallback callback) throws ConnectionException {
        javaCon.setConnectTimeout(Http.defaultConnectTimeOut);
        javaCon.setReadTimeout(Http.defaultReadTimeOut);
        return _send(callback);
    }

    public Response send(int timeOut) throws ConnectionException {
        javaCon.setConnectTimeout(timeOut);
        javaCon.setReadTimeout(timeOut);
        return _send(null);
    }

    public Response send(int connectTimeOut, int readTimeOut) throws ConnectionException {
        javaCon.setConnectTimeout(connectTimeOut);
        javaCon.setReadTimeout(readTimeOut);
        return _send(null);
    }

    public Response send(int connectTimeOut, int readTimeOut, HttpCallback callback) throws ConnectionException {
        javaCon.setConnectTimeout(connectTimeOut);
        javaCon.setReadTimeout(readTimeOut);
        return _send(callback);
    }

    public URL getRequestUrl() {
        return url;
    }

    public HeaderList getRequestHeaders(){
        return reqHeaders;
    }

    /** Use it to send data yourself. Throws exception if data was specified in ConnectionBuilder **/
    public OutputStream getOutputStream() throws IOException {
        if (output != null) throw new IOException("Output data was already send!!!");
        return javaCon.getOutputStream();
    }

    /** Use it to send data yourself. Throws exception if data was specified in ConnectionBuilder **/
    public OutputStreamWriter getWriter() throws IOException {
        if (output != null) throw new IOException("Output data was already send!!!");
        return new OutputStreamWriter(getOutputStream());
    }

    /** Underlying HttpUrlConnection that is used to connect **/
    public HttpURLConnection getJavaCon() {
        return javaCon;
    }

    private void appendJavaHeaders(){
        Map<String, List<String>> javaRequests = getJavaRequests();
        for (Map.Entry<String, List<String>> e : javaRequests.entrySet()) {
            if (e != null && e.getKey() != null){
                if (reqHeaders.getHeader(e.getKey()) == null){
                    if (e.getValue() != null && e.getValue().size() > 0 && e.getValue().get(0) != null) {
                        reqHeaders.add(new Header(e.getKey(), e.getValue().get(0)));
                    }
                }
            }
        }
    }

    private Map<String, List<String>> getJavaRequests(){
        MessageHeader requests = null;

        try {
            if (javaCon instanceof sun.net.www.protocol.http.HttpURLConnection){
                Field requestsField = sun.net.www.protocol.http.HttpURLConnection.class.getDeclaredField("requests");
                requestsField.setAccessible(true);
                requests = (MessageHeader) requestsField.get(javaCon);
            } else if (javaCon instanceof sun.net.www.protocol.https.HttpsURLConnectionImpl){
                DelegateHttpsURLConnection delegate = (DelegateHttpsURLConnection) getFieldValue(javaCon, "delegate");
                requests = (MessageHeader) getFieldValue(delegate, "requests");
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

        if (requests != null){
            headers = requests.getHeaders();
        } else {
            headers = new HashMap<>();
        }

        return headers;
    }

    private static Object getFieldValue(Object o, String fieldName) throws Exception {
        Field field = o.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        Object val = field.get(o);
        return val;
    }
    private static Object getFieldValueDeep(Object o, String... path) throws Exception {
        Object val = o;
        for (String s : path) {
            val = getFieldValue(val, s);
            if (val == null){
                return null;
            }
        }

        return val;
    }
}
