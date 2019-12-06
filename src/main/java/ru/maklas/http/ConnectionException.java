package ru.maklas.http;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Date;

public class ConnectionException extends Exception {

	public enum Type {

		/** Thrown after attempt to reuse builder and call build() twice. Contains no cause. **/
		USED,

		/** Exception caused by using wrong http method or not using one **/
		BAD_PROTOCOL,

		/** MalformedURLException wrong link that doesn't include http:// or https:// **/
		BAD_URL,

		/** Something wrong with access controll. Should not happen **/
		IO,

		/** Nobody anwered for too long **/
		TIME_OUT,

		/** Guess that target was not found. dns didn't resolve | no internet | bad address **/
		UNKNOWN_ADDRESS,

		/** Attempted to connect via HTTPS, but site only accept http **/
		NOT_SSL,

		/**
		 * Something wrong while sending or receiving data. Very bad type, since
		 * Your request could already reach server, but you won't get response.
		 * May be not
		 */
		CONNECTION_ERROR,

	}

	private final Type type;
	private final ConnectionBuilder builder;
	private final Request request;
	private final long created;

	public ConnectionException(IOException cause, ConnectionBuilder builder, Request request) {
		this(getExceptionType(cause), cause, builder, request);
	}

	public ConnectionException(Type type, IOException cause, ConnectionBuilder builder, Request request) {
		super(cause.getMessage(), cause);
		this.type = type;
		this.builder = builder;
		this.request = request;
		this.created = System.currentTimeMillis();
	}

	public ConnectionException(Type type, String message, IOException cause, ConnectionBuilder builder, Request request) {
		super(message, cause);
		this.type = type;
		this.builder = builder;
		this.request = request;
		this.created = System.currentTimeMillis();
	}

	public Type getType() {
		return type;
	}

	@NotNull
	public ConnectionBuilder getBuilder() {
		return builder;
	}

	@Nullable
	public Request getRequest() {
		return request;
	}

	@Override
	public String toString() {
		String s = getClass().getName();
		String message = getLocalizedMessage();
		return (message != null) ? (s + ": " + message) : s;
	}


	@Override
	public String getMessage() {
		return "(" + type.name() + ") " + super.getMessage();
	}

	@Override
	public void printStackTrace(PrintStream s) {
		printStackTrace(new PrintWriter(s));
	}

	@Override
	public void printStackTrace(PrintWriter w) {
		try {
			@Nullable String requestUrl = builder.getUrl() != null ? builder.getUrl().toString() : builder.getStringUrl();
			@Nullable IOException exception = (IOException) getCause();

			w.println("--------------------START--------------------");
			w.println("Request ->");
			w.println("Date: " + new Date(created).toString());
			w.println("URL: " + (requestUrl == null ? "NULL" : requestUrl));
			w.println("Method: " + builder.getMethod());
			if (builder.getBuildStarted() != 0) w.println("Build time: " + (created - builder.getBuildStarted()));
			if (getRequest() != null) w.println("TimeOut: " + (getRequest().getJavaCon().getConnectTimeout() <= 0 ? "infinity" : getRequest().getJavaCon().getConnectTimeout()));
			if (builder.getProxy() != null) w.println("Proxy: " + builder.getProxy());
			w.println("Headers:");
			for (Header requestHeader : builder.getHeaders()) {
				w.print("  ");w.println(requestHeader);
			}
			if (builder.getOutput() != null) {
				w.println("HTTP Body:");
				w.println(new String(builder.getOutput(), HttpUtils.utf_8));
			}
			w.println();
			w.println("Exception Type: " + type + ". Trace:");
			if (exception != null) exception.printStackTrace(w);
			w.println("--------------------END--------------------");
		} finally {
			w.flush();
		}
	}

	public void printStackTrace(OutputStream out) {
		printStackTrace(new PrintWriter(out));
	}

	static ConnectionException.Type getExceptionType(IOException e) {
		if (e instanceof SocketTimeoutException) {
			return ConnectionException.Type.TIME_OUT;
		} else if (e instanceof UnknownHostException) {
			return ConnectionException.Type.UNKNOWN_ADDRESS;
		} else if (e instanceof SSLHandshakeException) {
			return ConnectionException.Type.NOT_SSL;
		} else {
			return ConnectionException.Type.CONNECTION_ERROR;
		}
	}
}
