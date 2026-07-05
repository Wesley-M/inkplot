package io.github.wesleym.inkplot.render;

import io.github.wesleym.inkplot.scale.Scale;
import io.github.wesleym.inkplot.ChartTheme;

import java.awt.Rectangle;

/**
 * Everything a renderer needs to draw or hit-test within a laid-out plot: the plot rectangle, the built X and Y
 * scales the canvas derived from the renderer's axis models (null for a radial renderer, which has no axes), the live
 * theme, and the current hover state. Immutable — the canvas rebuilds it whenever the size, data, or theme changes.
 */
public record PlotContext(Rectangle plot, Scale xScale, Scale yScale, ChartTheme theme, ChartHoverState hover) {

	public PlotContext withHover(ChartHoverState next) {
		return new PlotContext(plot, xScale, yScale, theme, next);
	}
}
