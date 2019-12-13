package ru.maklas.http.receivers;

import org.jetbrains.annotations.NotNull;
import ru.maklas.http.Counter;
import ru.maklas.http.HttpUtils;
import ru.maklas.http.Response;
import ru.maklas.http.ResponseReceiver;

import java.io.InputStream;
import java.io.OutputStream;

/** Writes response body to the output stream. Gets notified of progress with listener in the same thread **/
public class TrackedStreamResponseReceiver implements ResponseReceiver {

	private final OutputStream os;
	private final double updatePercentage;
	private final Listener l;

	/**
	 * @param os where to write
	 * @param updatePercentage value from 0 to 1, where 1% is 0.01
	 *                         Indicating how often  should listener be notified of progress.
	 * @param l listener that gets called during receive
	 */
	public TrackedStreamResponseReceiver(@NotNull OutputStream os, double updatePercentage, @NotNull Listener l) {
		this.os = os;
		if (updatePercentage > 1.0 || updatePercentage < 0.0) {
			throw new RuntimeException("updatePercentage must be in range from 0 to 1, where 1% is 0.01");
		}
		this.updatePercentage = updatePercentage;
		this.l = l;
	}

	@Override
	public void receive(Response response, long contentLength, InputStream is, Counter counter, boolean isError) throws Exception {
		int bufferSize = HttpUtils.bufferSize(contentLength);
		OutputStream os = this.os;
		long cl;
		Listener l = this.l;
		if (contentLength <= 0) {
			cl = -1;
			try {
				l.started(response, cl, isError);
				HttpUtils.copy(is, os, new byte[bufferSize]);
				os.flush();
			} finally {
				l.finished();
			}
		} else {
			cl = contentLength;
			long progress;
			double updateStep = (contentLength * updatePercentage);
			double nextGoal = updateStep;
			try {
				l.started(response,  cl, isError);
				byte[] buffer = new byte[bufferSize];
				int n;
				while (-1 != (n = is.read(buffer, 0, bufferSize))) {
					os.write(buffer, 0, n);
					progress = counter.getCount();
					if (progress > nextGoal) {
						l.update(progress, cl);
						while (progress > nextGoal) {
							nextGoal += updateStep;
						}
					}
				}
			} finally {
				l.finished();
			}
		}
	}

	public abstract static class Listener {

		/**
		 * Called before starting download.
		 * If contentLength <= 0, {@link #update(long, long)} will never be called
		 */
		public void started(Response response, long contentLength, boolean isError) { }

		/**
		 * This method is never called if in {@link #started(Response, long, boolean)} contentLength == -1
		 * @param progress bytes downloaded
		 * @param contentLength total bytes
		 */
		public void update(long progress, long contentLength) { }

		public void finished() { }

	}
}
