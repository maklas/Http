package ru.maklas.http;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;

public class CountingInputStream extends InputStream implements Counter {

	private final InputStream delegate;
	private long count;

	public CountingInputStream(InputStream is) {
		this.delegate = is;
	}

	@Override
	public int read() throws IOException {
		int read = delegate.read();
		if (read != -1) {
			count++;
		}
		return read;
	}

	@Override
	public int read(@NotNull byte[] b) throws IOException {
		int read = delegate.read(b);
		if (read != -1) {
			count += read;
		}
		return read;
	}

	@Override
	public int read(@NotNull byte[] b, int off, int len) throws IOException {
		int read = delegate.read(b, off, len);
		if (read != -1) {
			count += read;
		}
		return read;
	}

	@Override
	public long skip(long n) throws IOException {
		long skip = super.skip(n);
		if (skip > 0) {
			count += skip;
		}
		return skip;
	}

	@Override
	public synchronized void reset() throws IOException {
		super.reset();
		count = 0;
	}

	public long getCount() {
		return count;
	}
}
