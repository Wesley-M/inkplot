package io.github.wesleym.inkplot.data;

/** Lenient numeric parsing for string cells — the one rule for "does this value chart as a number". */
final class Numbers {

	private Numbers() { }

	/** Parses a cell as a number, or null if it isn't one. */
	public static Double parse(String s) {
		if (s == null) {
			return null;
		}
		try {
			return Double.valueOf(s.trim());
		}
		catch (NumberFormatException e) {
			return null;
		}
	}
}
