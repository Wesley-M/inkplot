package io.github.wesleym.inkplot.render;

import io.github.wesleym.inkplot.data.ChartData;

/**
 * Builds the renderer that draws a given prepared {@link ChartData} — the one place chart-data variants map to their
 * mark renderers, so the canvas stays type-agnostic.
 */
public final class Renderers {

	private Renderers() { }

	public static MarkRenderer of(ChartData data) {
		return switch (data) {
			case ChartData.Bar bar -> bar.horizontal() ? new HBarRenderer(bar) : new BarRenderer(bar);
			case ChartData.Doughnut doughnut -> new DoughnutRenderer(doughnut);
			case ChartData.Waffle waffle -> new WaffleRenderer(waffle);
			case ChartData.Treemap treemap -> new TreemapRenderer(treemap);
			case ChartData.Line line -> new LineRenderer(line);
			case ChartData.Histogram histogram -> new HistogramRenderer(histogram);
			case ChartData.Density density -> new DensityRenderer(density);
			case ChartData.Scatter scatter -> new ScatterRenderer(scatter);
			case ChartData.Box box -> new BoxRenderer(box);
		};
	}
}
