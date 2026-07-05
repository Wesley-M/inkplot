package io.github.wesleym.inkplot.render;

/**
 * What a renderer's X axis is, so the canvas can build the right scale and ticks without knowing the chart type:
 * discrete categories (bars, boxes), a continuous number range (scatter, histogram, density), or a time range
 * (time-series lines and scatter).
 */
public sealed interface XAxisModel permits XAxisModel.Band, XAxisModel.Continuous, XAxisModel.Time {

	/** Discrete categories, one band each. */
	record Band(String[] categories) implements XAxisModel { }

	/** A continuous number range. */
	record Continuous(double min, double max) implements XAxisModel { }

	/** A time range in epoch-millis. */
	record Time(long min, long max) implements XAxisModel { }
}
