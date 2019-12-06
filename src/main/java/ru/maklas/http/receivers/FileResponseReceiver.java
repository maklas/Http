package ru.maklas.http.receivers;

import org.jetbrains.annotations.NotNull;
import ru.maklas.http.HttpUtils;
import ru.maklas.http.Response;
import ru.maklas.http.ResponseReceiver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Simply Writes data to the specified file **/
public class FileResponseReceiver implements ResponseReceiver {

	private final File file;

	public FileResponseReceiver(@NotNull File file) {
		this.file = file;
	}

	@Override
	public void receive(Response response, long contentLength, InputStream is, boolean isError) throws Exception {
		if (!file.exists()) {
			File folder = file.getParentFile();
			if (folder == null) {
				throw new IOException("Target file does not exists and can't be created: " + file.getAbsolutePath());
			}
			if (!folder.exists()) {
				folder.mkdirs();
			}
			file.createNewFile();
		}
		try (FileOutputStream fos = new FileOutputStream(file)) {
			HttpUtils.copy(is, fos, new byte[HttpUtils.bufferSize(contentLength)]);
		}
	}
}
