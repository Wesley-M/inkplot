package io.github.wesleym.inkplot.render;

/**
 * How a chart's marks reveal during the live component's entry animation — an internal hint per renderer.
 * Exports ({@code renderTo}, {@code image}, {@code toSvg}) are always drawn fully, never animated.
 */
public enum RevealStyle {

	/** Grow up from the baseline — vertical bars, histogram, box. */
	GROW_UP,

	/** Wipe on left to right — line, density, horizontal bars. */
	WIPE_RIGHT,

	/** Sweep clockwise from the top — the doughnut. */
	SWEEP,

	/** Fade in — treemap, waffle, scatter. */
	FADE
}
