package ru.maklas.http;

public class KeyValuePair {

    public final String key;
    public final String value;

    public KeyValuePair(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return "KeyValuePair{" +
                "key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
