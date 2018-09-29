package ru.maklas.http;


import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Predicate;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ResponseHeaders extends HeaderList {


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

    public CookieChangeList updateCookiesIfChanged(CookieStore cookies){
        return updateCookiesIfChanged(cookies, cookies.getCookieChangePredicate());
    }

    public CookieChangeList updateCookiesIfChanged(CookieStore cookies, Predicate<Cookie> allowCookiePredicate){
        Array<Cookie> newCookies = getHeaders(Header.SetCookie.key, false).map(Cookie::fromResponseHeader);
        CookieChangeList changeList = new CookieChangeList();

        for (Cookie newCookie : newCookies) {
            if (!allowCookiePredicate.evaluate(newCookie)) {
                changeList.addIgnored(new CookieChange(newCookie.getKey(), cookies.getCookie(newCookie.getKey()), newCookie.getValue()));
            } else {
                String oldValue = cookies.setCookie(newCookie.getKey(), newCookie.getValue());
                changeList.addChanged(new CookieChange(newCookie.getKey(), oldValue, newCookie.getValue()));
            }
        }
        return changeList;
    }

}
