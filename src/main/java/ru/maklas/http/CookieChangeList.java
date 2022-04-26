package ru.maklas.http;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Predicate;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/** Change history of cookies after Http response. **/
public class CookieChangeList {

	private Array<CookieChange> changed = new Array<>(5);
	private Array<CookieChange> ignored = new Array<>(1);

	public CookieChangeList() {

	}

	/** Cookies that were changed/deleted **/
	public Array<CookieChange> getChanged() {
		return changed;
	}

	/**
	 * Cookies that were ignored due to cookie filter set by
	 * {@link CookieStore#setCookieChangePredicate(Predicate)}
	 */
	public Array<CookieChange> getIgnored() {
		return ignored;
	}

	public void addChanged(CookieChange change) {
		this.changed.add(change);
	}

	public void addIgnored(CookieChange ignored) {
		this.ignored.add(ignored);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (changed.size > 0) {
			builder.append("Changed: \n");
			for (CookieChange cookieChange : changed) {
				toString(cookieChange, builder, false);
				builder.append("\n");
			}
		}

		if (ignored.size > 0) {

			builder.append("Ignored: \n");
			for (CookieChange ignored : ignored) {
				toString(ignored, builder, true);
				builder.append("\n");
			}
		}

		return builder.toString();
	}

	private static void toString(CookieChange change, StringBuilder builder, boolean ignored) {
		builder.append(change.key);
		builder.append(": ");
		changeValueToString(change.oldValue, builder);

		builder.append(ignored ? " X " : " -> ");

		changeValueToString(change.newValue, builder);
	}

	private static void changeValueToString(String value, StringBuilder builder) {
		if (value == null) {
			builder.append("NULL");
		} else {
			builder.append("'")
					.append(value)
					.append("'");
		}
	}

	public int size() {
		return changed.size + ignored.size;
	}

	public boolean wasChanged(String key) {
		for (CookieChange cookieChange : changed) {
			if (cookieChange.key.equals(key)) {
				return true;
			}
		}

		return false;
	}

	public boolean wasDeleted(String key) {
		for (CookieChange cookieChange : changed) {
			if (cookieChange.key.equals(key) && (Cookie.shouldBeDeleted(cookieChange.newValue))) {
				return true;
			}
		}

		return false;
	}

	public boolean wasAdded(String key) {
		for (CookieChange cookieChange : changed) {
			if (cookieChange.key.equals(key) && !Cookie.shouldBeDeleted(cookieChange.newValue) && cookieChange.oldValue == null) {
				return true;
			}
		}

		return false;
	}

	public boolean ifAddedOrChanged(String key, Consumer<String> valueConsumer) {
		for (CookieChange change : changed) {
			if (change.key.equals(key) && !Cookie.shouldBeDeleted(change.newValue)) {
				valueConsumer.accept(change.newValue);
				return true;
			}
		}
		return false;
	}

	@Nullable
	public CookieChange getChangeFor(String key) {
		for (CookieChange cookieChange : changed) {
			if (cookieChange.key.equalsIgnoreCase(key)) {
				return cookieChange;
			}
		}
		return null;
	}
}
