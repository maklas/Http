package ru.maklas.http;

public class CookieChange {

	public final String key;
	public final String oldValue;
	public final String newValue;

	public CookieChange(String key, String oldValue, String newValue) {
		this.key = key;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}

	public boolean newCookie() {
		return oldValue == null;
	}

	@Override
	public String toString() {
		return "CookieReplacement{" +
				"key='" + key + '\'' +
				", oldValue='" + oldValue + '\'' +
				", newValue='" + newValue + '\'' +
				'}';
	}
}
