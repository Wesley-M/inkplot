package io.github.wesleym.inkplot.render;

import io.github.wesleym.inkplot.ChartFormat;
import io.github.wesleym.inkplot.scale.Scale;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Small shared helpers for the crosshair layer: invert a pixel back to an X value on a continuous or time scale, find
 * the nearest data index to a value in an ascending array (binary search), and format an X value for the tooltip title.
 */
final class CrosshairMath {

	private CrosshairMath() { }

	private static final DateTimeFormatter TIME_LABEL = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);
	private static final DateTimeFormatter TIME_LABEL_INTRADAY = DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.US);

	static double invertX(Scale scale, double px) {
		java.util.Objects.requireNonNull(scale, "scale");
		if (scale instanceof Scale.Linear l) {
			return l.invert(px);
		}
		if (scale instanceof Scale.Time t) {
			return t.invert(px);
		}
		if (scale instanceof Scale.Log log) {
			return log.invert(px);
		}
		if (scale instanceof Scale.Band b) {
			return b.indexAt(px);
		}
		throw new AssertionError("Unknown scale: " + scale.getClass().getName());
	}

	/** The index in ascending {@code values} whose entry is closest to {@code target}. */
	static int nearest(double[] values, double target) {
		if (values.length == 0) {
			return 0;
		}
		int lo = 0;
		int hi = values.length - 1;
		while (lo < hi) {
			int mid = (lo + hi) >>> 1;
			if (values[mid] < target) {
				lo = mid + 1;
			}
			else {
				hi = mid;
			}
		}
		if (lo > 0 && Math.abs(values[lo - 1] - target) <= Math.abs(values[lo] - target)) {
			return lo - 1;
		}
		return lo;
	}

	static String formatX(boolean time, double value) {
		if (!time) {
			return ChartFormat.groupedDecimal(value, 2);
		}
		var when = Instant.ofEpochMilli((long) value).atZone(ZoneOffset.UTC);
		boolean intraday = when.getHour() != 0 || when.getMinute() != 0;
		return (intraday ? TIME_LABEL_INTRADAY : TIME_LABEL).format(when);
	}
}
