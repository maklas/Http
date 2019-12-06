package ru.maklas.http.receivers;

import org.jetbrains.annotations.NotNull;
import ru.maklas.http.HttpUtils;
import ru.maklas.http.Response;
import ru.maklas.http.ResponseReceiver;

import java.io.InputStream;
import java.io.OutputStream;

/** Simply writes all of the data to the OutputStream **/
public class StreamResponseReceiver implements ResponseReceiver {

	private final OutputStream os;

	public StreamResponseReceiver(@NotNull OutputStream os) {
		this.os = os;
	}

	@Override
	public void receive(Response response, long contentLength, InputStream is, boolean isError) throws Exception {
		HttpUtils.copy(is, os, new byte[HttpUtils.bufferSize(contentLength)]);
	}


}
