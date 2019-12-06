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
		if (!(obj instanceof KeyValuePair)) return false;
		KeyValuePair other = (KeyValuePair) obj;
		return (key == other.key || key != null && key.equalsIgnoreCase(other.key)) && HttpUtils.equals(value, other.value);
	}
}
