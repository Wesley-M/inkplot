package io.github.wesleym.inkplot;

import java.util.Locale;

/** Human-readable numeric labels for axes, tooltips, and captions — one rule set for the whole library. */
public final class ChartFormat {

	private ChartFormat() { }

	/** Exact count with grouping separators, e.g. {@code 12,345}. */
	public static String grouped(long value) {
		return String.format(Locale.ROOT, "%,d", value);
	}

	/** Grouped decimal label: whole numbers are grouped with no fraction, others use two decimal places. */
	public static String groupedDecimal(double value) {
		return groupedDecimal(value, 2);
	}

	/** Grouped decimal label with explicit fractional precision for compact chart axes. */
	public static String groupedDecimal(double value, int fractionDigits) {
		if (fractionDigits < 0) {
			throw new IllegalArgumentException("fractionDigits must be >= 0");
		}
		if (value == Math.rint(value) && !Double.isInfinite(value)) {
			return grouped((long) value);
		}
		return String.format(Locale.US, "%,." + fractionDigits + "f", value);
	}

	/** Percentage label for ratios: tiny non-zero values show as {@code <1%}, near-100 values keep one decimal. */
	public static String percent(double ratio) {
		double p = ratio * 100;
		if (p > 0 && p < 1) {
			return "<1%";
		}
		if (p > 99 && p < 100) {
			return String.format(Locale.US, "%.1f%%", p);
		}
		return Math.round(p) + "%";
	}
}
