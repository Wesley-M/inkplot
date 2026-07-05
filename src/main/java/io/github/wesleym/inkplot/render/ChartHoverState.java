package io.github.wesleym.inkplot.render;

/**
 * The pointer state a renderer paints against: whether the pointer is currently over the plot and where. Renderers
 * decide what to highlight from it (a lifted bar, a crosshair snapped to the nearest X). {@link #NONE} is the resting
 * state with no hover.
 */
public record ChartHoverState(boolean active, int x, int y) {

	public static final ChartHoverState NONE = new ChartHoverState(false, -1, -1);

	public static ChartHoverState at(int x, int y) {
		return new ChartHoverState(true, x, y);
	}
}
