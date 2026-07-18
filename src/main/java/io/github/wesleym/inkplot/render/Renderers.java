package io.github.wesleym.inkplot.render;

import io.github.wesleym.inkplot.data.ChartData;

/**
 * Builds the renderer that draws a given prepared {@link ChartData} — the one place chart-data variants map to their
 * mark renderers, so the canvas stays type-agnostic.
 */
public final class Renderers {

	private Renderers() { }

	public static MarkRenderer of(ChartData data) {
		java.util.Objects.requireNonNull(data, "data");
		if (data instanceof ChartData.Stat stat) {
			return new StatRenderer(stat);
		}
		if (data instanceof ChartData.Bar bar) {
			return bar.horizontal() ? new HBarRenderer(bar) : new BarRenderer(bar);
		}
		if (data instanceof ChartData.Doughnut doughnut) {
			return new DoughnutRenderer(doughnut);
		}
		if (data instanceof ChartData.Waffle waffle) {
			return new WaffleRenderer(waffle);
		}
		if (data instanceof ChartData.Treemap treemap) {
			return new TreemapRenderer(treemap);
		}
		if (data instanceof ChartData.Line line) {
			return new LineRenderer(line);
		}
		if (data instanceof ChartData.Histogram histogram) {
			return new HistogramRenderer(histogram);
		}
		if (data instanceof ChartData.Density density) {
			return new DensityRenderer(density);
		}
		if (data instanceof ChartData.Scatter scatter) {
			return new ScatterRenderer(scatter);
		}
		if (data instanceof ChartData.Box box) {
			return new BoxRenderer(box);
		}
		throw new AssertionError("Unknown chart data: " + data.getClass().getName());
	}
}
