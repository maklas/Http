package ru.maklas.http;

import com.badlogic.gdx.utils.Array;

import java.io.*;

public class MultipartWriter {

	private final Array<Data> sources = new Array<>();
	private boolean hasStream = false;

	public MultipartWriter() {

	}

	public MultipartWriter add(String key, String value) {
		sources.add(new StringMultipartData(key, value, null, null));
		return this;
	}

	public MultipartWriter add(String key, String value, String fileName) {
		sources.add(new StringMultipartData(key, value, fileName, null));
		return this;
	}

	public MultipartWriter add(String key, String value, String fileName, String contentType) {
		sources.add(new StringMultipartData(key, value, fileName, contentType));
		return this;
	}

	public MultipartWriter add(String key, InputStream is) {
		sources.add(new StreamMultipartData(key, is, null, null));
		hasStream = true;
		return this;
	}

	public MultipartWriter add(String key, InputStream is, String fileName) {
		sources.add(new StreamMultipartData(key, is, fileName, null));
		hasStream = true;
		return this;
	}

	public MultipartWriter add(String key, InputStream is, String fileName, String contentType) {
		sources.add(new StreamMultipartData(key, is, fileName, contentType));
		hasStream = true;
		return this;
	}

	public boolean hasStream() {
		return hasStream;
	}

	public void encode(OutputStream os, String boundary) throws IOException {
		if (sources.size == 0) return;
		byte[] encodedBoundary = boundary.getBytes(HttpUtils.ascii);
		byte[] splitter = new byte[4 + encodedBoundary.length];
		splitter[0] = '-';
		splitter[1] = '-';
		splitter[splitter.length - 2] = '\r';
		splitter[splitter.length - 1] = '\n';
		System.arraycopy(encodedBoundary, 0, splitter, 2, encodedBoundary.length);

		byte[] buffer = hasStream ? new byte[8192] : null;


		for (Data data : sources) {
			os.write(splitter);
			os.write("Content-Disposition: form-data; name=\"".getBytes(HttpUtils.utf_8));
			os.write(encodeString(data.key));
			os.write("\"".getBytes(HttpUtils.utf_8));
			if (data.fileName != null) {
				os.write("; filename=\"".getBytes(HttpUtils.utf_8));
				os.write(encodeString(data.fileName));
				os.write('\"');
			}
			if (data.contentType != null) {
				os.write("\r\nContent-Type: ".getBytes(HttpUtils.utf_8));
				os.write(encodeString(data.contentType));
			}
			os.write("\r\n\r\n".getBytes(HttpUtils.utf_8));

			if (data instanceof StringMultipartData) {
				os.write(((StringMultipartData) data).value.getBytes(HttpUtils.utf_8));
			} else if (data instanceof ByteArrayMultipartData) {
				os.write(((ByteArrayMultipartData) data).value);
			} else if (data instanceof StreamMultipartData) {
				StreamMultipartData streamData = (StreamMultipartData) data;
				if (!streamData.used || streamData.is.markSupported()) {
					if (streamData.used) {
						streamData.is.reset();
					}
					HttpUtils.copy(streamData.is, os, buffer);
					streamData.used = true;
				}
			}
			os.write("\r\n".getBytes(HttpUtils.utf_8));
		}
		os.write(splitter, 0, splitter.length - 2);
		os.write("--\r\n".getBytes(HttpUtils.utf_8));
		os.flush();
	}


	private static byte[] encodeString(String s){
		return HttpUtils.escapeFormData(s).getBytes(HttpUtils.ascii);
	}

	/** Representation of Multipart encoded data **/
	String getTraceRepresentation(String boundary) {
		if (sources.size == 0) return "null";

		StringBuilder sb = new StringBuilder();
		for (Data source : sources) {
			sb.append("--").append(boundary).append("\n");
			sb.append("Content-Disposition: form-data; name=\"").append(source.key).append("\"");
			if (source.fileName != null) {
				sb.append("; filename=\"").append(new String(encodeString(source.fileName), HttpUtils.ascii)).append("\"");
			}
			sb.append("\n");
			if (source.contentType != null) {
				sb.append("Content-Type: ").append(source.contentType);
				sb.append("\n");
			}
			sb.append("\n");
			if (source instanceof StringMultipartData) {
				sb.append(HttpUtils.trim(((StringMultipartData) source).value, 1024));
			} else if (source instanceof ByteArrayMultipartData) {
				sb.append(HttpUtils.trim(new String(((ByteArrayMultipartData) source).value, HttpUtils.utf_8), 1024));
			} else if (source instanceof StreamMultipartData) {
				sb.append("***Data from Stream***");
			}
			sb.append("\n");
		}
		sb.append("--").append(boundary).append("--").append("\n");

		return sb.toString();
	}

	private abstract static class Data {
		String key;
		String fileName;
		String contentType;
	}

	private static class ByteArrayMultipartData extends Data {
		final byte[] value;

		public ByteArrayMultipartData(String key, byte[] value) {
			this.key = key;
			this.value = value;
		}
	}

	private static class StringMultipartData extends Data {
		final String value;

		public StringMultipartData(String key, String value, String fileName, String contentType) {
			this.key = key;
			this.value = value;
			this.fileName = fileName;
			this.contentType = contentType;
		}

	}

	private static class StreamMultipartData extends Data {
		private final InputStream is;
		private boolean used;

		public StreamMultipartData(String key, InputStream is, String fileName, String contentType) {
			this.key = key;
			this.is = is;
			this.fileName = fileName;
			this.contentType = contentType;
		}
	}

}
