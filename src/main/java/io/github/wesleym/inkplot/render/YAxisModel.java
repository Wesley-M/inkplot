package io.github.wesleym.inkplot.render;

/**
 * A renderer's Y domain and whether a logarithmic scale is offered for it (only when the data is strictly positive). Y
 * is always continuous — no chart puts categories on the value axis. {@code min}/{@code max} bound the linear domain
 * (bars and histograms report {@code min == 0} for their baseline); {@code positiveMin} is the smallest positive value,
 * which is what a log axis floors to since it cannot represent zero.
 */
public record YAxisModel(double min, double max, boolean allowLog, double positiveMin) {

	/** A linear-only Y domain. */
	public static YAxisModel linear(double min, double max) {
		return new YAxisModel(min, max, false, min);
	}

	/** A domain that may switch to log, given the smallest positive value to floor the log axis to. */
	public static YAxisModel loggable(double min, double max, boolean allowLog, double positiveMin) {
		return new YAxisModel(min, max, allowLog && positiveMin > 0, positiveMin);
	}
}
