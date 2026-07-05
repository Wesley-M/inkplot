package io.github.wesleym.inkplot.scale;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ticks for a time axis: picks a calendar unit (second → year) whose step yields about the requested number of
 * ticks, aligns the first tick to a natural boundary, and formats each label at the granularity of that unit
 * (a time-of-day for sub-day steps, a day for daily, a month for monthly, a year above). All arithmetic is in UTC so
 * a day step is exactly one day — the axis never gains or drops a tick across a daylight-saving change.
 */
public final class TimeTicks {

	private TimeTicks() { }

	/** Tick timestamps (epoch millis, ascending) and the labels to draw under them, aligned one-to-one. */
	public record Result(long[] values, String[] labels) { }

	private enum Unit { SECOND, MINUTE, HOUR, DAY, MONTH, YEAR }

	// The candidate step ladder: (unit, multiple, approximate span in millis for picking). Ordered coarsest-first
	// picking walks it to the first step at least as large as the desired per-tick span.
	private record Step(Unit unit, int multiple, long approxMillis, DateTimeFormatter format) { }

	private static final long SEC = 1000L;
	private static final long MIN = 60 * SEC;
	private static final long HOUR = 60 * MIN;
	private static final long DAY = 24 * HOUR;

	private static final DateTimeFormatter TIME_HMS = fmt("HH:mm:ss");
	private static final DateTimeFormatter TIME_HM = fmt("HH:mm");
	private static final DateTimeFormatter DATE_MD = fmt("MMM d");
	private static final DateTimeFormatter DATE_MY = fmt("MMM yyyy");
	private static final DateTimeFormatter DATE_Y = fmt("yyyy");

	private static final List<Step> LADDER = List.of(
			new Step(Unit.SECOND, 1, SEC, TIME_HMS),
			new Step(Unit.SECOND, 5, 5 * SEC, TIME_HMS),
			new Step(Unit.SECOND, 15, 15 * SEC, TIME_HMS),
			new Step(Unit.SECOND, 30, 30 * SEC, TIME_HMS),
			new Step(Unit.MINUTE, 1, MIN, TIME_HM),
			new Step(Unit.MINUTE, 5, 5 * MIN, TIME_HM),
			new Step(Unit.MINUTE, 15, 15 * MIN, TIME_HM),
			new Step(Unit.MINUTE, 30, 30 * MIN, TIME_HM),
			new Step(Unit.HOUR, 1, HOUR, TIME_HM),
			new Step(Unit.HOUR, 3, 3 * HOUR, TIME_HM),
			new Step(Unit.HOUR, 6, 6 * HOUR, TIME_HM),
			new Step(Unit.HOUR, 12, 12 * HOUR, TIME_HM),
			new Step(Unit.DAY, 1, DAY, DATE_MD),
			new Step(Unit.DAY, 2, 2 * DAY, DATE_MD),
			new Step(Unit.DAY, 7, 7 * DAY, DATE_MD),
			new Step(Unit.MONTH, 1, 30 * DAY, DATE_MY),
			new Step(Unit.MONTH, 3, 91 * DAY, DATE_MY),
			new Step(Unit.MONTH, 6, 182 * DAY, DATE_MY),
			new Step(Unit.YEAR, 1, 365 * DAY, DATE_Y),
			new Step(Unit.YEAR, 2, 730 * DAY, DATE_Y),
			new Step(Unit.YEAR, 5, 1826 * DAY, DATE_Y),
			new Step(Unit.YEAR, 10, 3652 * DAY, DATE_Y));

	/** Ticks for the epoch-millis span {@code [min, max]} aiming for about {@code target} of them. */
	public static Result of(long min, long max, int target) {
		if (max <= min) {
			max = min >= Long.MAX_VALUE - SEC ? Long.MAX_VALUE : min + SEC;
			min = max == Long.MAX_VALUE ? max - SEC : min;
		}
		int want = Math.max(2, target);
		long desired = Math.max(1, saturatedSpan(min, max) / want);
		Step step = stepFor(desired);

		List<Long> ticks = new ArrayList<>();
		ZonedDateTime cursor = align(min, step);
		ZonedDateTime end = Instant.ofEpochMilli(max).atZone(ZoneOffset.UTC);
		// The size guard caps the loop even if a pathological span slips past unit selection.
		while (!cursor.isAfter(end) && ticks.size() < 2048) {
			Long ms = epochMillis(cursor);
			if (ms != null && ms >= min) {
				ticks.add(ms);
			}
			cursor = advance(cursor, step);
		}
		if (ticks.isEmpty()) {
			ticks.add(min);
		}

		long[] values = new long[ticks.size()];
		String[] labels = new String[ticks.size()];
		for (int i = 0; i < ticks.size(); i++) {
			values[i] = ticks.get(i);
			labels[i] = step.format().format(Instant.ofEpochMilli(ticks.get(i)).atZone(ZoneOffset.UTC));
		}
		return new Result(values, labels);
	}

	// The first tick at or before `min`, snapped to a natural boundary for the unit (a minute mark, midnight,
	// the first of the month, Jan 1) then stepped forward into range by the caller's loop.
	private static ZonedDateTime align(long min, Step step) {
		ZonedDateTime t = Instant.ofEpochMilli(min).atZone(ZoneOffset.UTC);
		return switch (step.unit()) {
			case SECOND -> floorField(t.withNano(0), t.getSecond(), step.multiple())
					.withNano(0);
			case MINUTE -> floorMinute(t.withSecond(0).withNano(0), t.getMinute(), step.multiple());
			case HOUR -> floorHour(t.withMinute(0).withSecond(0).withNano(0), t.getHour(), step.multiple());
			case DAY -> t.toLocalDate().atStartOfDay(ZoneOffset.UTC);
			case MONTH -> t.toLocalDate().withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC)
					.withMonth(floorMonth(t.getMonthValue(), step.multiple()));
			case YEAR -> t.toLocalDate().withDayOfYear(1).atStartOfDay(ZoneOffset.UTC)
					.withYear(floorYear(t.getYear(), step.multiple()));
		};
	}

	private static ZonedDateTime advance(ZonedDateTime t, Step step) {
		return switch (step.unit()) {
			case SECOND -> t.plusSeconds(step.multiple());
			case MINUTE -> t.plusMinutes(step.multiple());
			case HOUR -> t.plusHours(step.multiple());
			case DAY -> t.plusDays(step.multiple());
			case MONTH -> t.plusMonths(step.multiple());
			case YEAR -> t.plusYears(step.multiple());
		};
	}

	private static ZonedDateTime floorField(ZonedDateTime t, int value, int multiple) {
		return t.withSecond(value - value % multiple);
	}

	private static ZonedDateTime floorMinute(ZonedDateTime t, int value, int multiple) {
		return t.withMinute(value - value % multiple);
	}

	private static ZonedDateTime floorHour(ZonedDateTime t, int value, int multiple) {
		return t.withHour(value - value % multiple);
	}

	private static int floorMonth(int month, int multiple) {
		int zero = month - 1;
		return zero - zero % multiple + 1;
	}

	private static int floorYear(int year, int multiple) {
		return year - Math.floorMod(year, multiple);
	}

	private static Long epochMillis(ZonedDateTime time) {
		try {
			return time.toInstant().toEpochMilli();
		}
		catch (ArithmeticException outsideLongMillis) {
			return null;
		}
	}

	private static long saturatedSpan(long min, long max) {
		try {
			return Math.subtractExact(max, min);
		}
		catch (ArithmeticException overflow) {
			return Long.MAX_VALUE;
		}
	}

	private static Step stepFor(long desiredMillis) {
		for (Step candidate : LADDER) {
			if (candidate.approxMillis() >= desiredMillis) {
				return candidate;
			}
		}
		return new Step(Unit.YEAR, coarseYearMultiple(desiredMillis), desiredMillis, DATE_Y);
	}

	private static int coarseYearMultiple(long desiredMillis) {
		long years = Math.max(10, desiredMillis / (365 * DAY));
		double exponent = Math.pow(10, Math.floor(Math.log10(years)));
		double fraction = years / exponent;
		double nice = fraction <= 1 ? 1 : fraction <= 2 ? 2 : fraction <= 5 ? 5 : 10;
		return (int) Math.min(Integer.MAX_VALUE, Math.max(10, Math.round(nice * exponent)));
	}

	private static DateTimeFormatter fmt(String pattern) {
		return DateTimeFormatter.ofPattern(pattern, Locale.US);
	}
}
