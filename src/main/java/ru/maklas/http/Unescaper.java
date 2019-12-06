package ru.maklas.http;

import org.apache.commons.lang3.text.translate.CharSequenceTranslator;

import java.io.IOException;
import java.io.Writer;

/**
 * Unescapes unicode with pattern \\*u+\+*\d{4}
 * \' -> '
 * \" -> "
 * \\ -> \
 */
public class Unescaper extends CharSequenceTranslator {

	public static Unescaper INSTANCE = new Unescaper();

	private Unescaper() {

	}

	public static String translateSafely(String text) {
		if (text == null) return null;
		try {
			return INSTANCE.translate(text);
		} catch (Exception e) {
			return text;
		}
	}

	@Override
	public int translate(final CharSequence input, final int index, final Writer out) throws IOException {
		if (input.charAt(index) == '\\' && index + 1 < input.length()) {

			// consume optional additional '\', 'u' and '+' chars

			int i = 1;
			while (index + i < input.length() && input.charAt(index + i) == '\\') {
				i++;
			}
			boolean hasU = false;
			while (index + i < input.length() && input.charAt(index + i) == 'u') {
				i++;
				hasU = true;
			}
			if (hasU) {
				if (index + i < input.length() && input.charAt(index + i) == '+') {
					i++;
				}

				if (index + i + 4 <= input.length()) {
					// Get 4 hex digits
					final CharSequence unicode = input.subSequence(index + i, index + i + 4);

					try {
						final int value = Integer.parseInt(unicode.toString(), 16);
						out.write((char) value);
					} catch (final NumberFormatException nfe) {
						throw new IllegalArgumentException("Unable to parse unicode value: " + unicode, nfe);
					}
					return i + 4;
				}
				throw new IllegalArgumentException("Less than 4 hex digits in unicode value: '" + input.subSequence(index, input.length())
						+ "' due to end of CharSequence");
			} else {
				char secondChar = input.charAt(index + 1);
				if (secondChar == '\\') {
					out.write('\\');
					return 2;
				} else if (secondChar == '\"') {
					out.write('"');
					return 2;
				} else if (secondChar == '\'') {
					out.write('\'');
					return 2;
				}
			}
		}
		return 0;
	}


}
