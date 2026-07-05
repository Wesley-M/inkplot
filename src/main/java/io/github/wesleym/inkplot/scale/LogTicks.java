package io.github.wesleym.inkplot.scale;

import io.github.wesleym.inkplot.ChartFormat;

import java.util.ArrayList;
import java.util.List;

/**
 * Ticks for a base-10 logarithmic axis: a mark at every power of ten spanning the domain, plus the 2× and 5× minor
 * marks inside each decade when the range is short enough for them to read. The domain is rounded out to whole
 * decades so the axis begins and ends on a clean power of ten.
 */
public final class LogTicks {

	private static final double FLOOR = 1e-12;

	private LogTicks() { }

	/** The decade-rounded domain, the tick values on it, and their labels (aligned one-to-one). */
	public record Result(double niceMin, double niceMax, double[] values, String[] labels) { }

	/** Log ticks spanning {@code [min, max]} (both must be positive; a non-positive min is lifted to a tiny floor). */
	public static Result of(double min, double max) {
		double a = positiveFinite(min, 1);
		double b = positiveFinite(max, 10);
		double lo = Math.min(a, b);
		double hi = Math.max(lo * 10, Math.max(a, b));
		int loExp = (int) Math.floor(Math.log10(lo));
		int hiExp = (int) Math.ceil(Math.log10(hi));
		double niceMin = Math.pow(10, loExp);
		double niceMax = Math.pow(10, hiExp);
		int decades = hiExp - loExp;
		// Only show 2×/5× minor marks when the axis spans few decades; over a wide range they crowd into noise.
		int[] mantissas = decades <= 4 ? new int[] { 1, 2, 5 } : new int[] { 1 };

		List<Double> values = new ArrayList<>();
		for (int exp = loExp; exp <= hiExp; exp++) {
			double decade = Math.pow(10, exp);
			for (int m : mantissas) {
				double v = m * decade;
				if (v >= niceMin - 1e-9 && v <= niceMax + 1e-9) {
					values.add(v);
				}
			}
		}

		double[] out = new double[values.size()];
		String[] labels = new String[values.size()];
		for (int i = 0; i < values.size(); i++) {
			out[i] = values.get(i);
			labels[i] = label(values.get(i));
		}
		return new Result(niceMin, niceMax, out, labels);
	}

	private static String label(double v) {
		int digits = (int) Math.max(0, -Math.floor(Math.log10(v)));
		return ChartFormat.groupedDecimal(v, digits);
	}

	private static double positiveFinite(double value, double fallback) {
		return Double.isFinite(value) ? Math.max(FLOOR, value) : fallback;
	}
}
