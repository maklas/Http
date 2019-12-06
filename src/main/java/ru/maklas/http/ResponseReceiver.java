package ru.maklas.http;

import java.io.InputStream;

public interface ResponseReceiver {

	/**
	 * @param response      First part of the response, containgin headers and other handy data
	 * @param contentLength Value from the Content-Length header. Might not be indicated and have value of 0.
	 * @param is            InputStream of the response.
	 *                      No need to decode gzip or delfate as it's already done if header present. No need to close the stream
	 * @param isError       Whether InputStream is error stream or not.
	 */
	void receive(Response response, long contentLength, InputStream is, boolean isError) throws Exception;

}
