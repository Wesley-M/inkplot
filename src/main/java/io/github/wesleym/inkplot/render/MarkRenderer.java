package io.github.wesleym.inkplot.render;

import io.github.wesleym.inkplot.ChartTheme;

import java.awt.Graphics2D;
import java.awt.Point;
import java.util.List;
import java.util.Optional;

/**
 * Draws one chart type's marks and answers hover queries about them. A renderer is a thin, immutable wrapper over a
 * prepared {@code ChartData}: it declares its X and Y axis models so the canvas can build scales and axes, paints only
 * the marks (the canvas owns gridlines and axis labels), and hit-tests the pointer for the hover layer. Renderers hold
 * no pixel state — everything derives from the {@link PlotContext} passed in, so the canvas can re-lay-out on resize by
 * simply rebuilding the context.
 */
public interface MarkRenderer {

	XAxisModel xModel();

	YAxisModel yModel();

	/**
	 * Tells the renderer the canvas will seat its legend as a block instead of relying on on-mark labelling.
	 * A renderer that normally labels marks directly (the doughnut's callouts) should switch to emitting
	 * {@link #legend} entries; renderers that already use the legend ignore this.
	 */
	default void setPreferLegend(boolean prefer) {
	}

	/**
	 * Category labels for a band Y axis (horizontal bars), or null for the usual continuous Y. When set, the canvas
	 * puts the categories down the left gutter at full width — the reason horizontal orientation exists — builds a
	 * band Y scale, and ignores {@link #yModel()}.
	 */
	default String[] yBands() {
		return null;
	}

	/**
	 * Whether this chart draws without axes (doughnut, waffle) rather than on a Cartesian plane. An axis-free
	 * renderer gets the whole padded surface as its plot and a {@link PlotContext} with no scales — the canvas
	 * draws no gridlines or axes for it.
	 */
	default boolean axisFree() {
		return false;
	}

	/** Paints the marks inside {@code ctx.plot()} using its scales; the canvas has already drawn the axes/grid. */
	void paintMarks(Graphics2D g, PlotContext ctx);

	/** How the marks reveal during the live component's entry animation (exports are always drawn fully). */
	default RevealStyle revealStyle() {
		return RevealStyle.FADE;
	}

	/** Legend entries in series order, or empty for a single-series chart (whose title already names it). */
	List<LegendEntry> legend(ChartTheme theme);

	/** The mark under the pointer, if any — for the hover lift and tooltip. */
	default Optional<MarkHit> hitTest(PlotContext ctx, Point p) {
		return Optional.empty();
	}

	/** Tooltip content for a hit this renderer produced. */
	default TooltipContent tooltip(PlotContext ctx, MarkHit hit) {
		return null;
	}

	/**
	 * Repaints just the hovered mark in its lifted state, over the cached static chart. Called by the hover overlay
	 * so the base image never has to be re-rendered on a pointer move.
	 */
	default void highlight(Graphics2D g, PlotContext ctx, MarkHit hit) {
	}

	/**
	 * Whether hover should use a vertical crosshair snapped to the nearest X with one all-series tooltip (line and
	 * density), rather than a per-mark tooltip on the mark the pointer is over (bar, scatter, box).
	 */
	default boolean usesCrosshair() {
		return false;
	}

	/** For a crosshair renderer, the snapped position and all-series read-out nearest the pointer's X. */
	default Optional<CrosshairReadout> crosshairAt(PlotContext ctx, int mouseX) {
		return Optional.empty();
	}
}
