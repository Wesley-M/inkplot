package io.github.wesleym.inkplot.render;

import java.awt.AlphaComposite;
import java.awt.Composite;

/**
 * Per-series draw alpha driven by the interactive legend on the live component: a fully shown series is 1.0, a
 * toggled-off one eases to 0, and when one series is focused the rest ease down to a dim fraction. The canvas
 * animates these values between states; renderers just draw each series at its current alpha. Exports pass
 * {@link #NONE}.
 */
public record SeriesEmphasis(double[] alpha) {

	public static final SeriesEmphasis NONE = new SeriesEmphasis(new double[0]);

	public double alpha(int series) {
		return series >= 0 && series < alpha.length ? alpha[series] : 1.0;
	}

	/** Whether the series is worth drawing at all (skipped once it has all but faded out). */
	public boolean visible(int series) {
		return alpha(series) > 0.02;
	}

	/** The composite to draw {@code series} with: the base at full alpha, or a faded one otherwise. */
	public Composite composite(int series, Composite base) {
		double a = alpha(series);
		return a >= 0.999 ? base : AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.max(0, a));
	}

	/** The series alpha as a multiplier for renderers that already blend per mark (scatter). */
	public double dim(int series) {
		return alpha(series);
	}
}
