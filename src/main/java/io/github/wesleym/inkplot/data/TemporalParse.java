package io.github.wesleym.inkplot.data;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Locale;

/**
 * Parses the timestamp shapes the result grid emits into epoch-millis (UTC): ISO-8601 with an offset, ISO local
 * date-times with a {@code T} or a space separator, plain dates, and bare epoch numbers within a plausible range. It
 * tolerates the wildly over-precise fractional seconds some backends return (e.g. SQL Anywhere's
 * {@code 16:29:06.9730000000000000000000000000000}) by keeping at most nanosecond precision and discarding the rest.
 * Anything it doesn't recognise returns null, so a temporal axis degrades to categorical rather than inventing a time.
 */
public final class TemporalParse {

	private TemporalParse() { }

	// Date, then an optional time with a T or space separator, seconds and a fraction of any width (0–9 digits kept).
	private static final DateTimeFormatter FLEXIBLE = new DateTimeFormatterBuilder()
			.append(DateTimeFormatter.ISO_LOCAL_DATE)
			.optionalStart()
			.appendLiteral('T')
			.appendValue(ChronoField.HOUR_OF_DAY, 2)
			.appendLiteral(':')
			.appendValue(ChronoField.MINUTE_OF_HOUR, 2)
			.optionalStart().appendLiteral(':').appendValue(ChronoField.SECOND_OF_MINUTE, 2).optionalEnd()
			.optionalStart().appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true).optionalEnd()
			.optionalEnd()
			.parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
			.parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
			.parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
			.toFormatter(Locale.US);

	/** Epoch-millis for {@code value}, or null if it isn't a timestamp this recognises. */
	public static Long parseMillis(String value) {
		if (value == null) {
			return null;
		}
		String s = value.trim();
		if (s.isEmpty()) {
			return null;
		}
		Long epoch = epochNumber(s);
		if (epoch != null) {
			return epoch;
		}
		String normalized = normalize(s);
		try {
			return OffsetDateTime.parse(normalized).toInstant().toEpochMilli();
		}
		catch (DateTimeParseException ignored) {
			// not an offset date-time; try a local one
		}
		try {
			return LocalDateTime.parse(normalized, FLEXIBLE).toInstant(ZoneOffset.UTC).toEpochMilli();
		}
		catch (DateTimeParseException ignored) {
			return null;
		}
	}

	// The most fractional-second digits we keep — milliseconds. Finer precision never shows on a chart axis and only
	// bloats a backend's padded value.
	private static final int MAX_FRACTION_DIGITS = 3;

	// Normalises a timestamp for parsing: a space separator becomes 'T', and a fractional-seconds run longer than
	// milliseconds is truncated — the extra digits backends pad on are precision no chart would show anyway.
	private static String normalize(String s) {
		String t = s.replace(' ', 'T');
		int dot = t.indexOf('.');
		if (dot < 0) {
			return t;
		}
		int end = dot + 1;
		while (end < t.length() && Character.isDigit(t.charAt(end))) {
			end++;
		}
		int fractionDigits = end - (dot + 1);
		if (fractionDigits <= MAX_FRACTION_DIGITS) {
			return t;
		}
		return t.substring(0, dot + 1) + t.substring(dot + 1, dot + 1 + MAX_FRACTION_DIGITS) + t.substring(end);
	}

	// A bare number is a timestamp only within a plausible window, so a plain integer id isn't mistaken for one:
	// ~1973–2286 in seconds, or the same window in millis.
	private static Long epochNumber(String s) {
		if (s.isEmpty() || !allDigits(s)) {
			return null;
		}
		long n;
		try {
			n = Long.parseLong(s);
		}
		catch (NumberFormatException e) {
			return null;
		}
		if (n >= 100_000_000L && n < 10_000_000_000L) {
			return n * 1000;   // seconds
		}
		if (n >= 100_000_000_000L && n < 10_000_000_000_000L) {
			return n;          // millis
		}
		return null;
	}

	private static boolean allDigits(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (!Character.isDigit(s.charAt(i))) {
				return false;
			}
		}
		return true;
	}
}
