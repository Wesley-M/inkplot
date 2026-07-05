package io.github.wesleym.inkplot.render;

import io.github.wesleym.inkplot.ChartFormat;
import io.github.wesleym.inkplot.data.ChartData;
import io.github.wesleym.inkplot.ChartStyle;
import io.github.wesleym.inkplot.ChartTheme;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Draws points over two continuous (or time-X) axes, coloured by series. Overplotting is handled by scaling each
 * point's opacity to how crowded its neighbourhood is — dense clusters stay legible as tone instead of a solid blob —
 * and a 2px surface ring is added only where points are sparse enough for it to help rather than add noise. The point
 * count is already capped upstream, so this always draws a bounded number of marks.
 */
public final class ScatterRenderer implements MarkRenderer {

	private static final int HIT_RADIUS = 24;

	private final ChartData.Scatter data;

	public ScatterRenderer(ChartData.Scatter data) {
		this.data = data;
	}

	@Override
	public XAxisModel xModel() {
		double[] range = range(data.x());
		return data.timeAxisX()
				? new XAxisModel.Time((long) range[0], (long) range[1])
				: new XAxisModel.Continuous(range[0], range[1]);
	}

	@Override
	public YAxisModel yModel() {
		double[] range = range(data.y());
		double positiveMin = Double.POSITIVE_INFINITY;
		boolean hasNegative = false;
		boolean hasPositive = false;
		for (double y : data.y()) {
			hasNegative |= y < 0;
			hasPositive |= y > 0;
			if (y > 0) {
				positiveMin = Math.min(positiveMin, y);
			}
		}
		return YAxisModel.loggable(range[0], range[1], !hasNegative && hasPositive, positiveMin);
	}

	@Override
	public void paintMarks(Graphics2D g, PlotContext ctx) {
		int n = data.x().length;
		if (n == 0) {
			return;
		}
		double r = Math.max(3, ChartStyle.px(4));
		double ring = Math.max(1, ChartStyle.px(2));
		int cell = ChartStyle.px(10);
		int cols = Math.max(1, ctx.plot().width / cell);
		int rows = Math.max(1, ctx.plot().height / cell);
		int[] density = new int[cols * rows];
		double[] px = new double[n];
		double[] py = new double[n];
		for (int i = 0; i < n; i++) {
			px[i] = ctx.xScale().map(data.x()[i]);
			py[i] = ctx.yScale().map(data.y()[i]);
			int c = clamp((int) ((px[i] - ctx.plot().x) / cell), cols);
			int rw = clamp((int) ((py[i] - ctx.plot().y) / cell), rows);
			density[rw * cols + c]++;
		}

		for (int i = 0; i < n; i++) {
			int c = clamp((int) ((px[i] - ctx.plot().x) / cell), cols);
			int rw = clamp((int) ((py[i] - ctx.plot().y) / cell), rows);
			int crowd = density[rw * cols + c];
			double opacity = Math.max(0.15, Math.min(0.85, 0.9 / (1 + Math.log(crowd) / Math.log(2))));
			Color colour = ChartInk.series(ctx.theme(), seriesOf(i));
			if (crowd <= 3) {
				g.setColor(ChartInk.separator(ctx.theme()));
				g.fill(new Ellipse2D.Double(px[i] - r - ring, py[i] - r - ring, (r + ring) * 2, (r + ring) * 2));
			}
			g.setColor(ChartInk.alpha(colour, opacity));
			g.fill(new Ellipse2D.Double(px[i] - r, py[i] - r, r * 2, r * 2));
		}
	}

	@Override
	public List<LegendEntry> legend(ChartTheme theme) {
		if (data.seriesCount() <= 1) {
			return List.of();
		}
		List<LegendEntry> entries = new ArrayList<>();
		for (int s = 0; s < data.seriesCount(); s++) {
			entries.add(new LegendEntry(ChartInk.series(theme, s), data.seriesNames()[s], false));
		}
		return entries;
	}

	@Override
	public Optional<MarkHit> hitTest(PlotContext ctx, Point p) {
		double best = HIT_RADIUS * (double) HIT_RADIUS;
		int found = -1;
		for (int i = 0; i < data.x().length; i++) {
			double dx = ctx.xScale().map(data.x()[i]) - p.x;
			double dy = ctx.yScale().map(data.y()[i]) - p.y;
			double d2 = dx * dx + dy * dy;
			if (d2 < best) {
				best = d2;
				found = i;
			}
		}
		if (found < 0) {
			return Optional.empty();
		}
		double r = ChartStyle.px(4);
		double cx = ctx.xScale().map(data.x()[found]);
		double cy = ctx.yScale().map(data.y()[found]);
		return Optional.of(new MarkHit(found, seriesOf(found), new Rectangle2D.Double(cx - r, cy - r, r * 2, r * 2)));
	}

	@Override
	public void highlight(Graphics2D g, PlotContext ctx, MarkHit hit) {
		double cx = ctx.xScale().map(data.x()[hit.a()]);
		double cy = ctx.yScale().map(data.y()[hit.a()]);
		double r = Math.max(4, ChartStyle.px(5));
		double ring = Math.max(1, ChartStyle.px(2));
		g.setColor(ChartInk.separator(ctx.theme()));
		g.fill(new Ellipse2D.Double(cx - r - ring, cy - r - ring, (r + ring) * 2, (r + ring) * 2));
		g.setColor(ChartInk.series(ctx.theme(), seriesOf(hit.a())));
		g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
	}

	@Override
	public TooltipContent tooltip(PlotContext ctx, MarkHit hit) {
		int i = hit.a();
		String value = "(" + ChartFormat.groupedDecimal(data.x()[i], 2) + ", "
				+ ChartFormat.groupedDecimal(data.y()[i], 2) + ")";
		String label = data.seriesCount() > 1 ? data.seriesNames()[seriesOf(i)] : "Point";
		return TooltipContent.single(label, ChartInk.series(ctx.theme(), seriesOf(i)), label, value);
	}

	private int seriesOf(int i) {
		return data.series() == null || data.series().length == 0 ? 0 : data.series()[i];
	}

	private static int clamp(int v, int size) {
		return Math.max(0, Math.min(size - 1, v));
	}

	private static double[] range(double[] values) {
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		for (double v : values) {
			min = Math.min(min, v);
			max = Math.max(max, v);
		}
		return min > max ? new double[] { 0, 1 } : new double[] { min, max };
	}
}
