package ru.maklas.http;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Before;
import org.junit.Test;
import ru.maklas.http.receivers.FileResponseReceiver;
import ru.maklas.http.receivers.StreamResponseReceiver;
import ru.maklas.http.receivers.StringResponseReceiver;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;

import static org.junit.Assert.*;

public class TestHttp {



	@Before
	public void setUp() throws Exception {
		Http.setDefaultCookieHandlerByJFX(false);
		Http.setAutoAddHostHeader(true);
		Http.setDefaultKeepAlive(true);
		Http.fetchJavaHeaders(true);
		Http.setSystemUserAgent(Header.UserAgent.mostRecent.value);
	}

	@Test
	public void testGet() throws Exception {
		Request request = ConnectionBuilder
				.get("http://info.cern.ch/hypertext/WWW/TheProject.html")
				.h(Header.UserAgent.mostRecent)
				.h(Header.DateHeader.now())
				.h(Header.CacheControl.noCache)
				.build();
		FullResponse response = request.send();
		System.out.println(response);
		String html = response.getBodyAsIs().toLowerCase();
		assertTrue(html.contains("<body>"));
		assertTrue(html.contains("world wide web"));

		UrlEncoder query = new UrlEncoder().add("key", "val").add("field", "&val").add("emptyKey", null).add("empty2", "").add("long", 50).add("bool", false);
		assertEquals("key=val&field=%26val&emptyKey=&empty2=&long=50&bool=false", query.encode());

		assertEquals(new URL("http://info.cern.ch/hypertext/WWW/TheProject.html?key=val"), ConnectionBuilder.get("http://info.cern.ch/", "/hypertext/WWW/TheProject.html", new UrlEncoder().add("key", "val")).build().getRequestUrl());
	}

	@Test
	public void testPost() throws Exception {
		String body = "Post Data. АБВГД";

		Request request = ConnectionBuilder
				.post("http://httpbin.org/post")
				.h(Header.AcceptEncoding.gzip)
				.h(Header.AcceptLanguage.en)
				.h(Header.CacheControl.noCache)
				.write(body, "text/plain", HttpUtils.utf_8)
				.build();
		FullResponse response = request.send();
		System.out.println(response);

		JsonObject root = new JsonParser().parse(response.getBodyAsIs()).getAsJsonObject();
		assertEquals(body, root.get("data").getAsString());
		assertEquals("text/plain; charset=UTF-8", request.getRequestHeaders().getHeaderValue(Header.ContentType.key));
	}

	@Test
	public void testHead() throws Exception {
		Request request = new ConnectionBuilder(Http.HEAD)
				.url("http://info.cern.ch/hypertext/WWW/TheProject.html")
				.h(Header.UserAgent.mostRecent)
				.h(Header.DateHeader.now())
				.h(Header.CacheControl.noCache)
				.build();
		FullResponse response = request.send();
		System.out.println(response);

		assertEquals("", response.getBodyAsIs());
		assertEquals("", response.getBodyUnescaped());
		assertArrayEquals(new byte[0], response.getResponseBytes());
	}

	@Test
	public void testEncodings() throws Exception {
		FullResponse response = ConnectionBuilder
				.get("https://google.com")
				.h(Header.UserAgent.mostRecent)
				.h(Header.AcceptEncoding.gzip)
				.h(Header.DateHeader.now())
				.h(Header.CacheControl.noCache)
				.send();
		String html = response.getBodyAsIs().toLowerCase();
		assertEquals("gzip", response.getHeaders().getHeaderValue(Header.ContentEncoding.key));
		assertTrue(html.contains("<html"));
		assertTrue(html.contains("<body"));

		response = ConnectionBuilder
				.get("http://httpbin.org/deflate")
				.h(Header.UserAgent.mostRecent)
				.h(Header.AcceptEncoding.deflate)
				.h(Header.DateHeader.now())
				.h(Header.CacheControl.noCache)
				.send();
		System.out.println(response);
		html = response.getBodyAsIs().toLowerCase();
		assertEquals("deflate", response.getHeaders().getHeaderValue(Header.ContentEncoding.key));
		assertTrue(html.contains("{"));
		assertTrue(html.contains("\"deflated\""));

		response = ConnectionBuilder
				.get("http://httpbin.org")
				.h(Header.UserAgent.mostRecent)
				.h(Header.AcceptEncoding.identity)
				.h(Header.DateHeader.now())
				.h(Header.CacheControl.noCache)
				.send();
		System.out.println(response);
		html = response.getBodyAsIs().toLowerCase();
		assertNull(response.getHeaders().getHeaderValue(Header.ContentEncoding.key));
		assertTrue(html.contains("<html"));
		assertTrue(html.contains("<body"));

		response = ConnectionBuilder
				.get("http://httpbin.org/brotli")
				.h(Header.UserAgent.mostRecent)
				.h(Header.AcceptEncoding.br)
				.h(Header.DateHeader.now())
				.h(Header.CacheControl.noCache)
				.send();
		System.out.println(response);
		html = response.getBodyAsIs().toLowerCase();
		assertEquals("br", response.getHeaders().getHeaderValue(Header.ContentEncoding.key));
		assertTrue(html.contains("{"));
		assertTrue(html.contains("\"brotli\""));
	}

	@Test
	public void testUnescape() throws Exception {
		FullResponse response = ConnectionBuilder.get("http://httpbin.org", "get", new UrlEncoder().add("key", "Значение"))
				.send();
		System.out.println(response);
		assertFalse(response.getBodyAsIs().contains("Значение"));
		assertTrue(response.getBodyUnescaped().contains("Значение"));


		assertEquals("ABC \u0410\u0411\u0412\u0413\u0414?", FullResponse.unescape(("ABC \\u0410\\u0411\\u0412\\u0413\\u0414?")));
		assertEquals("ABC \u0410\u0411\u0412\u0413\u0414?", FullResponse.unescape(("ABC \\\\u0410\\\\u0411\\\\u0412\\\\u0413\\\\u0414?")));
		assertEquals("ABC \u0410\u0411\u0412\u0413\u0414?", FullResponse.unescape(("ABC \\\\\\\\u0410\\\\\\\\u0411\\\\\\\\u0412\\\\\\\\u0413\\\\\\\\u0414?")));
		assertEquals("ABC\\\"ABC", FullResponse.unescape("ABC\\\\\"ABC"));//  \\"
		assertEquals("ABC\"ABC", FullResponse.unescape("ABC\\\"ABC"));//  \"
		assertEquals("ABC\'ABC", FullResponse.unescape("ABC\\\'ABC"));//  \'
		assertEquals("ABC\\ABC", FullResponse.unescape("ABC\\\\ABC"));//  \\
	}

	@Test
	public void testCookies() throws Exception {
		CookieStore cs = new CookieStore();
		cs.setCookie(new Cookie("InitialCookie", "initialVal", "httpbin.org"));
		FullResponse response = ConnectionBuilder.get("http://httpbin.org/cookies").assignCookieStore(cs).send();
		JsonObject cookies = new JsonParser().parse(response.getBodyAsIs()).getAsJsonObject().get("cookies").getAsJsonObject();
		assertEquals(cs.getCookie("InitialCookie"), cookies.get("InitialCookie").getAsString());
		assertEquals(1, cookies.entrySet().size());
		assertNotNull(response.getCookieChangeList());
		assertEquals(0, response.getCookieChangeList().size());
	}

	@Test
	public void testFileResponseReceiver() throws Exception {
		Request request = ConnectionBuilder
				.get("http://httpbin.org/image/jpeg")
				.h(Header.AcceptEncoding.gzipDeflateBr)
				.h(Header.UserAgent.mostRecent)
				.build();

		File tempFile = null;
		try {

			tempFile = File.createTempFile("httpTest", null);
			request.send(new FileResponseReceiver(tempFile));
			assertTrue(tempFile.length() > 0);
			BufferedImage image = ImageIO.read(tempFile);
			assertNotNull(image);
			assertTrue(image.getWidth() > 0);
			assertTrue(image.getHeight() > 0);
		} finally {
			tempFile.delete();
		}
	}

	@Test
	public void testOutputStreamReceiver() throws Exception {
		String data = "My String data. Моя строчка";
		Request request = ConnectionBuilder
				.post("http://httpbin.org/post")
				.h(Header.AcceptEncoding.gzipDeflateBr)
				.h(Header.UserAgent.mostRecent)
				.writePlainText(data)
				.build();

		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			Response response = request.send(new StreamResponseReceiver(bos));
			String responseBody = new String(bos.toByteArray(), response.getCharset());
			JsonObject root = new JsonParser().parse(responseBody).getAsJsonObject();
			assertEquals(data, root.get("data").getAsString());
		}
	}

	@Test
	public void testStringReceiver() throws Exception {
		String data = "My String data. Моя строчка";
		Request request = ConnectionBuilder
				.post("http://httpbin.org/post")
				.h(Header.AcceptEncoding.gzipDeflateBr)
				.h(Header.UserAgent.mostRecent)
				.writePlainText(data)
				.build();

		StringResponseReceiver receiver = new StringResponseReceiver();
		ConsumedResponse response = request.send(receiver);
		JsonObject root = new JsonParser().parse(receiver.getResponse()).getAsJsonObject();
		assertEquals(data, root.get("data").getAsString());
	}

	@Test
	public void name() {
		System.out.println("Post Data. \\u0410\\u0411\\u0412\\u0413\\u0414");
	}
}
