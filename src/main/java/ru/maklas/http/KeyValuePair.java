package ru.maklas.http;

/** Immutable key-value pair **/
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

    @Override
    public boolean equals(Object obj) {
        return obj instanceof KeyValuePair &&
                (key == ((KeyValuePair) obj).key || key != null && key.equals(((KeyValuePair) obj).key)) &&
                (value == ((KeyValuePair) obj).value || value != null && value.equals(((KeyValuePair) obj).value));
    }
}
