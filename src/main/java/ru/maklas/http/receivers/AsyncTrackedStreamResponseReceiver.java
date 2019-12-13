package ru.maklas.http.receivers;

import org.jetbrains.annotations.NotNull;
import ru.maklas.http.Counter;
import ru.maklas.http.HttpUtils;
import ru.maklas.http.Response;
import ru.maklas.http.ResponseReceiver;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicLong;

/** Writes data to the OutputStream. Allows to check progress by calling {@link #poll()} from another thread**/
public class AsyncTrackedStreamResponseReceiver implements ResponseReceiver {

	public static final int NOT_STARTED = -2;
	public static final int NO_CONT_LENGTH = -1;
	public static final int MAX = 10000;

	private final OutputStream os;
	private volatile long cl;
	private final AtomicLong progress = new AtomicLong(NOT_STARTED);
	private volatile boolean finished = false;

	public AsyncTrackedStreamResponseReceiver(@NotNull OutputStream os) {
		this.os = os;
	}

	@Override
	public void receive(Response response, long contentLength, InputStream is, Counter counter, boolean isError) throws Exception {
		OutputStream os = this.os;
		int bufferSize = HttpUtils.bufferSize(contentLength);
		try {
			if (contentLength <= 0) {
				progress.set(NO_CONT_LENGTH);
				HttpUtils.copy(is, os, new byte[bufferSize]);
				os.flush();
			} else {
				AtomicLong progress = this.progress;
				progress.set(0);
				cl = contentLength;

				byte[] buffer = new byte[bufferSize];
				int n;
				while (-1 != (n = is.read(buffer, 0, bufferSize))) {
					os.write(buffer, 0, n);
					progress.set(counter.getCount());
				}
				progress.set(contentLength);
				os.flush();
			}
		} finally {
			finished = true;
		}

	}

	/**
	 * @return -1 if contentLength was not specified and it's unknown how much was downloaded, value from 0 to 10_000,
	 * where 10_000 means 100%
	 */
	public int poll() {
		long cl = this.cl;
		long progress = this.progress.get();
		if (progress <= 0) return (int) progress;
		return progress < cl ? (int) ((((double) progress) / cl) * MAX) : MAX;
	}

	/** Returns true after download is finished **/
	public boolean finished() {
		return finished;
	}

	/** Whether or not download started **/
	public boolean started() {
		return progress.get() != NOT_STARTED;
	}
}
