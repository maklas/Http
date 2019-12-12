package ru.maklas.http.receivers;

import org.jetbrains.annotations.NotNull;
import ru.maklas.http.HttpUtils;
import ru.maklas.http.Response;
import ru.maklas.http.ResponseReceiver;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicLong;

/** Writes data to the OutputStream. Allows to check progress by calling {@link #poll()} **/
public class TrackedStreamResponseReceiver implements ResponseReceiver {

	public static final int MAX = 10000;

	private final OutputStream os;
	private volatile boolean started = false;
	private volatile boolean finished = false;
	private volatile long cl;
	private final AtomicLong progress = new AtomicLong();

	public TrackedStreamResponseReceiver(@NotNull OutputStream os) {
		this.os = os;
	}

	@Override
	public void receive(Response response, long contentLength, InputStream is, boolean isError) throws Exception {
		started = true;
		int bufferSize = HttpUtils.bufferSize(contentLength);
		OutputStream os = this.os;
		if (contentLength <= 0) {
			cl = -1;
			HttpUtils.copy(is, os, new byte[bufferSize]);
		} else {
			cl = contentLength;
			AtomicLong progress = this.progress;
			progress.set(0);
			byte[] buffer = new byte[bufferSize];

			int n;
			while (-1 != (n = is.read(buffer, 0, bufferSize))) {
				os.write(buffer, 0, n);
				progress.addAndGet(n);
			}
			progress.set(contentLength);
		}
		finished = true;
	}

	/**
	 * @return -1 if contentLength was not specified and it's unknown how much was downloaded, value from 0 to 10_000,
	 * where 10_000 means 100%
	 */
	public int poll() {
		long cl = this.cl;
		if (cl <= 0) return -1;
		long progress = this.progress.get();
		return progress < cl ? (int) ((((double) progress) / cl) * MAX) : MAX;
	}

	/** Returns true after download is finished **/
	public boolean finished() {
		return finished;
	}

	/** Whether or not download started **/
	public boolean started() {
		return started;
	}
}
