package ru.maklas.http;


import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ResponseHeaders extends HeaderList {

	public ResponseHeaders() {
		super();
	}

	public ResponseHeaders(Map<String, List<String>> headerMap) {
		Set<Map.Entry<String, List<String>>> entries = headerMap.entrySet();

		for (Map.Entry<String, List<String>> entry : entries) {
			if (entry.getKey() == null || entry.getValue() == null) continue;
			for (String s : entry.getValue()) {
				if (s != null) {
					headers.add(new Header(entry.getKey(), s));
				}
			}
		}
	}

	/**
	 * @return null if {@link Http#generateCookieChanges} set to false. changes
	 */
	@Nullable
	public CookieChangeList updateCookiesIfChanged(@NotNull URL url, @NotNull CookieStore cookies) {
		return updateCookiesIfChanged(url, cookies, cookies.getCookieChangePredicate());
	}

	@Nullable
	public CookieChangeList updateCookiesIfChanged(@NotNull URL url, @NotNull CookieStore cookies, @NotNull Predicate<Cookie> allowCookiePredicate) {
		Array<Cookie> newCookies = new Array<>(5);
		for (int i = 0; i < headers.size; i++) {
			Header header = headers.get(i);
			if (Header.SetCookie.key.equalsIgnoreCase(header.key)) {
				newCookies.add(Cookie.fromSetCookieValue(url, header.value));
			}
		}

		if (Http.generateCookieChanges) {
			CookieChangeList changeList = new CookieChangeList();
			for (Cookie newCookie : newCookies) {
				if (!allowCookiePredicate.evaluate(newCookie)) {
					changeList.addIgnored(new CookieChange(newCookie.getKey(), cookies.getCookie(newCookie.getKey()), newCookie.getValue()));
				} else {
					String oldValue = cookies.setCookie(newCookie);
					changeList.addChanged(new CookieChange(newCookie.getKey(), oldValue, newCookie.getValue()));
				}
			}
			return changeList;
		} else {
			for (Cookie newCookie : newCookies) {
				if (allowCookiePredicate.evaluate(newCookie)) {
					cookies.setCookie(newCookie);
				}
			}
			return null;
		}
	}

}
