package ru.maklas.http;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Consumer;
import com.badlogic.gdx.utils.Predicate;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public class CookieStore implements Iterable<Cookie>{

    private Array<Cookie> cookies;
    private Predicate<Cookie> cookieChangePredicate = (c) -> true;

    public CookieStore() {
        cookies = new Array<>();
    }

    /**
     * @return null if there was no cookie before, otherwise returns old cookie value
     * which is never null or empty string.
     */
    public String setCookie(String key, String value){
        if (key == null) throw new RuntimeException("Never null");
        if (Cookie.shouldBeDeleted(value))
            return remove(key);


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

    @NotNull
    @Override
    public Iterator<Cookie> iterator() {
        return cookies.iterator();
    }

    /** @return null if there is no cookie with this name **/
    public String getCookie(String key){
        return getCookie(key, null);
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

    public void setCookieChangePredicate(Predicate<Cookie> cookieChangePredicate) {
        if (cookieChangePredicate == null){
            cookieChangePredicate = (c) -> true;
        }
        this.cookieChangePredicate = cookieChangePredicate;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Cookie cookie : cookies) {
            builder.append(cookie.getKey())
                    .append(" = ")
                    .append(cookie.getValue()).append('\n');
        }
        return builder.toString();
    }

    public int size() {
        return cookies.size;
    }

    public boolean contains(String key) {
        return getCookie(key) != null;
    }
}
