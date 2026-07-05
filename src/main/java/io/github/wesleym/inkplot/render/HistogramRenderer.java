package io.github.wesleym.inkplot.render;

import io.github.wesleym.inkplot.ChartFormat;
import io.github.wesleym.inkplot.data.ChartData;
import io.github.wesleym.inkplot.scale.Scale;
import io.github.wesleym.inkplot.ChartStyle;
import io.github.wesleym.inkplot.ChartTheme;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Draws a binned distribution of one numeric column: contiguous columns in a single sequential hue (no identity job to
 * do), each separated from its neighbour by a 2px surface gap rather than a border. Counts read against a zero
 * baseline; the value axis may be switched to log to reveal a long tail.
 */
public final class HistogramRenderer implements MarkRenderer {

	private final ChartData.Histogram data;

	public HistogramRenderer(ChartData.Histogram data) {
		this.data = data;
	}

	@Override
	public XAxisModel xModel() {
		double[] edges = data.edges();
		return new XAxisModel.Continuous(edges[0], edges[edges.length - 1]);
	}

	@Override
	public YAxisModel yModel() {
		long max = 0;
		long positiveMin = Long.MAX_VALUE;
		for (long c : data.counts()) {
			max = Math.max(max, c);
			if (c > 0) {
				positiveMin = Math.min(positiveMin, c);
			}
		}
		// Log counts help reveal a long tail; the axis floors to the smallest non-empty bin (empty bins sit at the base).
		return YAxisModel.loggable(0, max, max > 0, positiveMin == Long.MAX_VALUE ? 1 : positiveMin);
	}

	@Override
	public void paintMarks(Graphics2D g, PlotContext ctx) {
		for (Bin bin : bins(ctx)) {
			g.setColor(ChartInk.distributionFill(ctx.theme()));
			g.fill(bin.rect());
			// The 2px gap is surface showing through — paint a thin surface sliver on the right edge of each bin.
			g.setColor(ChartInk.separator(ctx.theme()));
			double gap = ChartStyle.px(2);
			g.fill(new Rectangle2D.Double(bin.rect().getMaxX(), bin.rect().getY(), gap, bin.rect().getHeight()));
		}
	}

	@Override
	public void highlight(Graphics2D g, PlotContext ctx, MarkHit hit) {
		for (Bin bin : bins(ctx)) {
			if (bin.index() == hit.a()) {
				g.setColor(ChartInk.lift(ctx.theme(), ChartInk.distributionFill(ctx.theme())));
				g.fill(bin.rect());
				return;
			}
		}
	}

	@Override
	public List<LegendEntry> legend(ChartTheme theme) {
		return List.of();
	}

	@Override
	public Optional<MarkHit> hitTest(PlotContext ctx, Point p) {
		for (Bin bin : bins(ctx)) {
			if (bin.rect().contains(p.x, p.y)) {
				return Optional.of(new MarkHit(bin.index(), -1, bin.rect()));
			}
		}
		return Optional.empty();
	}

	@Override
	public TooltipContent tooltip(PlotContext ctx, MarkHit hit) {
		int i = hit.a();
		String range = ChartFormat.groupedDecimal(data.edges()[i], 1) + " – "
				+ ChartFormat.groupedDecimal(data.edges()[i + 1], 1);
		return TooltipContent.single(range, ChartInk.distributionFill(ctx.theme()), "Count",
				ChartFormat.grouped(data.counts()[i]));
	}

	private List<Bin> bins(PlotContext ctx) {
		List<Bin> out = new ArrayList<>();
		double baseline = baselineY(ctx);
		double gap = ChartStyle.px(2);
		for (int i = 0; i < data.counts().length; i++) {
			double x0 = ctx.xScale().map(data.edges()[i]);
			double x1 = ctx.xScale().map(data.edges()[i + 1]);
			double w = Math.max(1, x1 - x0 - gap);
			double top = ctx.yScale().map(data.counts()[i]);
			out.add(new Bin(i, new Rectangle2D.Double(x0, Math.min(top, baseline), w, Math.abs(baseline - top))));
		}
		return out;
	}

	private double baselineY(PlotContext ctx) {
		if (ctx.yScale() instanceof Scale.Log) {
			return ctx.plot().y + ctx.plot().height;
		}
		double zero = ctx.yScale().map(0);
		return Math.max(ctx.plot().y, Math.min(ctx.plot().y + ctx.plot().height, zero));
	}

	private record Bin(int index, Rectangle2D rect) { }
}
