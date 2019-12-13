package ru.maklas.http;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/** Response that was processed by ResponseReceiver to obtain Http body **/
public class ConsumedResponse extends Response {

	private boolean errorStreamUsed;
	private Exception bodyException;
	private ResponseReceiver receiver;
	private boolean finished = false;

	public ConsumedResponse(HttpURLConnection javaCon, URL url, int msToConnect, Request request, @NotNull ResponseReceiver receiver) {
		super(javaCon, url, msToConnect, request);
		this.receiver = receiver;
		receive(receiver);
	}

	public boolean isErrorStreamUsed() {
		return errorStreamUsed;
	}

	public Exception getBodyException() {
		return bodyException;
	}

	@SuppressWarnings("unchecked")
	public <T extends ResponseReceiver> T getReceiver() {
		return (T) receiver;
	}

	/** Returns instance of ResponseReceiver if it was successfully used, null otherwise**/
	@SuppressWarnings("unchecked")
	public <T extends ResponseReceiver> T getReceiverIfReceived() {
		return received() ? (T) receiver : null;
	}

	/** receiver was used and no exception was thrown **/
	public boolean received() {
		return finished;
	}

	private void receive(ResponseReceiver receiver) {
		InputStream is;
		CountingInputStream counter;
		try {
			is = getJavaCon().getInputStream();
			if (is == null) {
				is = new ByteArrayInputStream(new byte[0]);
				counter = new CountingInputStream(is);
			} else {
				counter = new CountingInputStream(is);
				is = wrapStream(counter);
			}
		} catch (Exception e) {
			errorStreamUsed = true;
			bodyException = e;
			try {
				is = getJavaCon().getErrorStream();
				counter = new CountingInputStream(is);
				is = wrapStream(counter);
			} catch (Exception e1) {
				bodyException = e1;
				return;
			}
		}
		try {
			receiver.receive(this, contentLength, is, counter, errorStreamUsed);
		} catch (Exception e) {
			e.printStackTrace();
		}
		finished = true;
		try {
			is.close();
		} catch (IOException ignore) { }
	}

	@Override
	protected boolean hasBody() {
		return false;
	}

	@Override
	protected void printBodyTrace(PrintWriter writer) { }
}
