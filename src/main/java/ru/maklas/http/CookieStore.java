package ru.maklas.http;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Consumer;
import com.badlogic.gdx.utils.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

/** Storage for cookies. Stores cookies and manages cookie changes **/
public class CookieStore implements Iterable<Cookie>{

    public static final Predicate<Cookie> COOKIE_PREDICATE_ALLOW_ALL = (c) -> true;
    private Array<Cookie> cookies;
    private Predicate<Cookie> cookieChangePredicate = COOKIE_PREDICATE_ALLOW_ALL;

    public CookieStore() {
        cookies = new Array<>(5);
    }

    /**
     * @return null if there was no cookie before, otherwise returns old cookie value
     * which is never null or empty string.
     */
    public String setCookie(String key, String value){
        if (key == null) throw new RuntimeException("Cookie key must not be null");
        if (Cookie.shouldBeDeleted(value)) {
            return remove(key);
        }


        for (Cookie cookie : cookies) {
            if (cookie.getKey().equals(key)){
                String oldValue = cookie.getValue();
                cookie.setValue(value);
                return oldValue;
            }
        }

        cookies.add(new Cookie(key, value));
        return null;
    }


    /**
     * @return null if there was no cookie before, otherwise returns old cookie value
     * which is never null or empty string.
     */
    public String setCookie(Cookie cookie){
        if (Cookie.shouldBeDeleted(cookie.getValue())) {
            return remove(cookie.getKey());
        }

        for (Cookie c : cookies) {
            if (c.getKey().equals(cookie.getKey())){
                String oldValue = c.getValue();
                c.update(cookie);
                return oldValue;
            }
        }

        cookies.add(cookie);
        return null;
    }

    @NotNull
    @Override
    public Iterator<Cookie> iterator() {
        return cookies.iterator();
    }

    /** @return null if there is no cookie with this name **/
    public String getCookie(String key){
        return getCookie(key, null);
    }

    /** @return null if there is no cookie with this name **/
    public Cookie getCookieFull(String key){
        for (Cookie cookie : cookies) {
            if (cookie.getKey().equals(key)){
                return cookie;
            }
        }
        return null;
    }

    /** @return default value if there is no cookie with this name **/
    public String getCookie(String key, String def){
        for (Cookie cookie : cookies) {
            if (cookie.getKey().equals(key)){
                return cookie.getValue();
            }
        }
        return def;
    }

    public void ifCookieExists(String key, Consumer<String> valueConsumer){
        String cookie = getCookie(key);
        if (cookie != null) valueConsumer.accept(cookie);
    }

    public void addAll(CookieStore cookies){
        for (Cookie cookie : cookies.cookies) {
            if (!cookie.isDeleted()){
                this.cookies.add(new Cookie(cookie));
            }
        }
    }

    /** @return null if there was no cookie with the same key. Otherwise returns old cookie value **/
    public String remove(String key){
        Cookie toRemove = null;

        for (Cookie cookie : cookies) {
            if (cookie.getKey().equals(key)) {
                toRemove = cookie;
                break;
            }
        }
        if (toRemove == null) return null;

        cookies.removeValue(toRemove, true);
        return toRemove.getValue();
    }

    /** @return null if there is no cookies!!! **/
    public Header toHeader(){
        if (cookies.size == 0) return null;
        return new Header(Cookie.headerKey, toHeaderString());
    }

    CookieStore removeByHost(String host){
        Array.ArrayIterator<Cookie> it = new Array.ArrayIterator<>(this.cookies);
        while (it.hasNext()){
            Cookie next = it.next();
            if (StringUtils.isNotEmpty(next.getDomain()) && !host.endsWith(next.getDomain())){
                it.remove();
            }
        }
        return this;
    }

    public String toHeaderString() {
        if (cookies.size == 0) return "";
        StringBuilder builder = new StringBuilder();
        for (Cookie cookie : cookies) {
            builder
                    .append(cookie.getKey())
                    .append("=")
                    .append(cookie.getValue())
                    .append("; ");
        }

        builder.setLength(builder.length() - 2);
        return builder.toString();
    }

    public Predicate<Cookie> getCookieChangePredicate() {
        return cookieChangePredicate;
    }

    public void setCookieChangePredicate(@Nullable Predicate<Cookie> cookieChangePredicate) {
        if (cookieChangePredicate == null){
            cookieChangePredicate = (c) -> true;
        }
        this.cookieChangePredicate = cookieChangePredicate;
    }

    @Override
    public String toString() {
        if (cookies.size == 0) return "";
        StringBuilder builder = new StringBuilder();
        for (Cookie cookie : cookies) {
            builder.append(cookie.getKey())
                    .append(" = ")
                    .append(cookie.getValue()).append('\n');
        }
        builder.setLength(builder.length() - 1);
        return builder.toString();
    }

    public String toStringFull() {
        if (cookies.size == 0) return "";
        StringBuilder builder = new StringBuilder();
        for (Cookie cookie : cookies) {
            builder.append(cookie).append('\n');
        }
        builder.setLength(builder.length() - 1);
        return builder.toString();
    }

    public int size() {
        return cookies.size;
    }

    public boolean contains(String key) {
        return getCookie(key) != null;
    }

    public static CookieStore parse(String cookieContainingString){
        CookieStore store = new CookieStore();
        if (StringUtils.isEmpty(cookieContainingString)){
            return store;
        }

        String[] split = cookieContainingString.split(";");
        for (String s : split) {
            String[] keyValue = s.split("=");
            if (keyValue.length == 2){
                store.setCookie(StringUtils.trimToEmpty(keyValue[0]), StringUtils.trimToEmpty(keyValue[1]));
            }
        }

        return store;
    }

}
