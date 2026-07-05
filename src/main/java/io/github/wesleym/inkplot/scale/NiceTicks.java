package io.github.wesleym.inkplot.scale;

import io.github.wesleym.inkplot.ChartFormat;

/**
 * "Nice number" tick generation for a linear axis (Heckbert's algorithm): rounds the domain out to clean bounds and
 * spaces ticks at 1/2/5 × 10ᵏ so the labels read as {@code 0 · 2,000 · 4,000}, never {@code 1,734 · 3,468}. The
 * result also carries the fractional-digit count the spacing implies, so labels show just enough precision.
 */
public final class NiceTicks {

	private NiceTicks() { }

	/** A nicened domain plus the tick values that fall on it, with the precision the tick spacing needs. */
	public record Result(double niceMin, double niceMax, double[] values, int fractionDigits) { }

	/**
	 * Ticks for {@code [min, max]} aiming for about {@code target} of them. A degenerate span (min == max, or
	 * non-finite input) is expanded to a readable unit range so the axis still draws.
	 */
	public static Result linear(double min, double max, int target) {
		int want = Math.max(2, target);
		if (!Double.isFinite(min) || !Double.isFinite(max) || min == max) {
			double centre = Double.isFinite(min) ? min : 0;
			double pad = centre == 0 ? 1 : Math.abs(centre) * 0.5;
			min = centre - pad;
			max = centre + pad;
		}
		if (min > max) {
			double t = min;
			min = max;
			max = t;
		}
		double range = niceNum(max - min, false);
		double spacing = niceNum(range / (want - 1), true);
		double niceMin = Math.floor(min / spacing) * spacing;
		double niceMax = Math.ceil(max / spacing) * spacing;
		int count = (int) Math.round((niceMax - niceMin) / spacing) + 1;
		double[] values = new double[count];
		for (int i = 0; i < count; i++) {
			// Re-round each step so accumulated floating error never shows up as 1999.9999 in a label.
			values[i] = roundToSpacing(niceMin + i * spacing, spacing);
		}
		int fractionDigits = (int) Math.max(0, -Math.floor(Math.log10(spacing)));
		return new Result(niceMin, niceMax, values, fractionDigits);
	}

	/** Labels a tick value at the precision the axis chose, grouped with thousands separators. */
	public static String label(double value, int fractionDigits) {
		return ChartFormat.groupedDecimal(value, fractionDigits);
	}

	// The nearest "nice" number to `value`: rounds it (or, when `round` is false, takes the ceiling) to 1/2/5/10 × 10ᵏ.
	private static double niceNum(double value, boolean round) {
		if (value <= 0) {
			return 1;
		}
		double exponent = Math.floor(Math.log10(value));
		double fraction = value / Math.pow(10, exponent);
		double niceFraction;
		if (round) {
			niceFraction = fraction < 1.5 ? 1 : fraction < 3 ? 2 : fraction < 7 ? 5 : 10;
		}
		else {
			niceFraction = fraction <= 1 ? 1 : fraction <= 2 ? 2 : fraction <= 5 ? 5 : 10;
		}
		return niceFraction * Math.pow(10, exponent);
	}

	private static double roundToSpacing(double value, double spacing) {
		double snapped = Math.round(value / spacing) * spacing;
		return snapped == 0 ? 0 : snapped;   // fold -0.0 to 0
	}
}
