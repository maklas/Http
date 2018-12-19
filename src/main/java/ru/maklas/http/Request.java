package ru.maklas.http;

import javax.net.ssl.SSLHandshakeException;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;

public class Request {

    public static int defaultConnectTimeOut = 10_000;
    public static int defaultReadTimeOut = 20_000;
    private HttpURLConnection javaCon;
    private final URL url;
    private final String method;
    private byte[] output;
    private final HeaderList reqHeaders;
    private final ConnectionBuilder builder;

    /**
     * Already connected!
     */
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

    public byte[] getSendingBody() {
        return output;
    }

    public String getSendingBodyAsString() {
        if (output == null) return "";
        return new String(output);
    }

    /**
     * Only before connecting!!!
     */
    public Request enableOutput(){
        javaCon.setDoOutput(true);
        return this;
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
                    ConnectionException ce = new ConnectionException(ConnectionException.Type.IO, e, getBuilder(), this);
                    if (callback != null) callback.interrupted(ce);
                    throw ce;
                }
            }
            if (callback != null) callback.connecting();
        long before = System.currentTimeMillis();
        try {
            javaCon.connect();
        } catch (IOException e) {
            if (e instanceof SocketTimeoutException){
                ConnectionException ce = new ConnectionException(ConnectionException.Type.TIME_OUT, e, getBuilder(), this);
                if (callback != null) callback.interrupted(ce);
                throw ce;
            } else if (e instanceof UnknownHostException){
                ConnectionException ce = new ConnectionException(ConnectionException.Type.UNKNOWN_ADDRESS, e, getBuilder(), this);
                if (callback != null) callback.interrupted(ce);
                throw ce;
            } else if (e instanceof SSLHandshakeException){
                ConnectionException ce = new ConnectionException(ConnectionException.Type.NOT_SSL, e, getBuilder(), this);
                if (callback != null) callback.interrupted(ce);
                throw ce;
            } else {
                ConnectionException ce = new ConnectionException(ConnectionException.Type.CONNECTION_ERROR, e, getBuilder(), this);
                if (callback != null) callback.interrupted(ce);
                throw ce;
            }
        }
        Response response;
        try {
            int responseCode = javaCon.getResponseCode();
            long ttc = System.currentTimeMillis() - before;
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
        javaCon.setConnectTimeout(defaultConnectTimeOut);
        javaCon.setReadTimeout(defaultReadTimeOut);
        return _send(null);
    }

    public Response send(HttpCallback callback) throws ConnectionException {
        javaCon.setConnectTimeout(defaultConnectTimeOut);
        javaCon.setReadTimeout(defaultReadTimeOut);
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

    public OutputStream getOutputStream() throws IOException {
        if (output != null) throw new IOException("Output data was already send!!!");
        return javaCon.getOutputStream();
    }

    public OutputStreamWriter getWriter() throws IOException {
        if (output != null) throw new IOException("Output data was already send!!!");
        return new OutputStreamWriter(getOutputStream());
    }

    public HttpURLConnection getJavaCon() {
        return javaCon;
    }
}
