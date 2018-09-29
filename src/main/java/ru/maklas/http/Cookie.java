package ru.maklas.http;

import org.jetbrains.annotations.NotNull;

/**
 * Cookie is considered to be deleted if it's value was set to empty string or null or 'false' or 'deleted'.
 */
public class Cookie {

    public static final String headerKey = "Cookie";
    public static final String DELETED = "deleted";

    private final String key;
    @NotNull
    private String value;
    private boolean deleted;

    public Cookie(Cookie cookie) {
        this.key = cookie.key;
        this.value = cookie.value;
        this.deleted = cookie.deleted;
    }

    public Cookie(String key, String value) {
        if (key == null) throw new RuntimeException();
        this.key = key;
        this.deleted = shouldBeDeleted(value);
        this.value =  deleted ? DELETED : value;
    }

    @NotNull
    public String getKey() {
        return key;
    }

    @NotNull
    public String getValue() {
        return value;
    }

    public boolean isDeleted() {
        return deleted;
    }

    void setValue(String value) {
        if (value == null || value.equals("") || value.equals(DELETED)){
            this.value = DELETED;
            this.deleted = true;
        } else {
            this.value = value;
            this.deleted = false;
        }
    }

    public static Cookie fromResponseHeader(Header h){
        if (!h.key.equalsIgnoreCase(Header.SetCookie.key)) throw new RuntimeException("Header's key != " + Header.SetCookie.key);
        String value = h.value;
        String keyValuePair = value.split(";")[0];
        String[] split = keyValuePair.split("=");
        if (split.length == 1) return new Cookie(split[0], "");

        String key = split[0];
        String val = split[1];
        return new Cookie(key.replaceAll(" ", ""), val.replaceAll(" ", ""));
    }

    public static boolean shouldBeDeleted(String newValue) {
        return newValue == null || newValue.equals(DELETED) || newValue.equals("") || newValue.equals("false");
    }
}
