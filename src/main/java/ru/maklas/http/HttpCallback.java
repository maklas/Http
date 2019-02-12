package ru.maklas.http;

/**
 * A callback interface for {@link Request#send()} method.
 * Used to provide asynchronous notification about Http request execution.
 * Do not block or do any time consuming calculations in these methods, as it might affect
 * request speed.
 */
public interface HttpCallback {

    /**
     * Always called. Right after {@link Request#send()}
     * At this point you can save request to use in other methods.
     * If you use single Http callback for multiple requests,
     * don't forget to remove reference to Request in {@link #finished(Response)} and {@link #interrupted(ConnectionException)}
     * as these are the methods where request is terminated and won't be used any more.
     */
    void start(Request request);

    /** Called after request's body was written to the Http request and only if it's POST method with body. **/
    void wroteBody();

    /** Called before attempting to execute HTTP request. **/
    void connecting();

    /** Called after establishing connection with server and receiving Http response code. **/
    void connected(int responseCode);

    /**
     * Indicates that HTTP request was successfully finished.
     * At the end either this method or {@link #interrupted(ConnectionException)} is called to indicate finality of request.
     */
    void finished(Response response);

    /**
     * Called just before throwing an Exception. Don't do anything with Request at this point, it might be broken.
     * HTTP request is terminated if this method is called.
     */
    void interrupted(ConnectionException ce);
}
