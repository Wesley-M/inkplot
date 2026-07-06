package io.github.wesleym.inkplot.render;

import io.github.wesleym.inkplot.ChartFormat;
import io.github.wesleym.inkplot.data.ChartData;
import io.github.wesleym.inkplot.scale.Scale;
import io.github.wesleym.inkplot.ChartStyle;
import io.github.wesleym.inkplot.ChartTheme;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@link BarRenderer} turned sideways: the categories run down a band Y axis (their full names in the left
 * gutter — the reason this orientation exists) and the bars grow rightward from a zero baseline on a continuous
 * X. Same mark language as its vertical sibling: 24px thickness cap, a 4px-rounded data end, a square foot at the
 * baseline, and the 2px surface gap between touching marks.
 */
public final class HBarRenderer implements MarkRenderer {

	private static final int MAX_BAR_THICK = 24;

	private final ChartData.Bar data;

	public HBarRenderer(ChartData.Bar data) {
		this.data = data;
	}

	@Override
	public String[] yBands() {
		return data.categories();
	}

	// The value axis lives on X here; Y is the band the canvas builds from yBands (yModel is unused filler).
	@Override
	public XAxisModel xModel() {
		double min = 0;
		double max = 0;
		if (data.stacked()) {
			for (int c = 0; c < data.categories().length; c++) {
				double sum = 0;
				for (int s = 0; s < data.seriesCount(); s++) {
					sum += data.values()[s][c];
				}
				max = Math.max(max, sum);
			}
		}
		else {
			for (double[] series : data.values()) {
				for (double v : series) {
					max = Math.max(max, v);
					min = Math.min(min, v);
				}
			}
		}
		return new XAxisModel.Continuous(min, max);
	}

	@Override
	public YAxisModel yModel() {
		return YAxisModel.linear(0, 1);
	}

	@Override
	public RevealStyle revealStyle() {
		return RevealStyle.WIPE_RIGHT;
	}

	@Override
	public void paintMarks(Graphics2D g, PlotContext ctx) {
		double radius = ChartStyle.px(4);
		double baseline = baselineX(ctx);
		SeriesEmphasis em = ctx.emphasis();
		java.awt.Composite base = g.getComposite();
		for (BarRect bar : rects(ctx)) {
			if (!em.visible(bar.series())) {
				continue;   // a legend-toggled-off series draws nothing
			}
			g.setComposite(em.composite(bar.series(), base));   // a focused-away series draws dimmed
			g.setColor(fillFor(ctx.theme(), bar.series(), bar.category()));
			g.fill(barShape(bar, radius, baseline));
		}
		g.setComposite(base);
	}

	@Override
	public boolean interactiveSeries() {
		return data.seriesCount() > 1;
	}

	@Override
	public void highlight(Graphics2D g, PlotContext ctx, MarkHit hit) {
		double radius = ChartStyle.px(4);
		double baseline = baselineX(ctx);
		for (BarRect bar : rects(ctx)) {
			if (bar.series() == hit.a() && bar.category() == hit.b()) {
				g.setColor(ChartInk.lift(ctx.theme(), fillFor(ctx.theme(), bar.series(), bar.category())));
				g.fill(barShape(bar, radius, baseline));
				return;
			}
		}
	}

	// Only the exposed data end rounds: a plain bar's tip, or the outer segment of a stack.
	private java.awt.Shape barShape(BarRect bar, double radius, double baseline) {
		boolean roundEnd = !data.stacked() || bar.series() == data.seriesCount() - 1;
		return roundEnd ? roundedBar(bar.rect(), radius, baseline) : bar.rect();
	}

	@Override
	public List<LegendEntry> legend(ChartTheme theme) {
		if (data.colorByCategory() || data.seriesCount() <= 1) {
			return List.of();
		}
		List<LegendEntry> entries = new ArrayList<>();
		for (int s = 0; s < data.seriesCount(); s++) {
			entries.add(new LegendEntry(fillFor(theme, s, 0), data.seriesNames()[s], false));
		}
		return entries;
	}

	@Override
	public Optional<MarkHit> hitTest(PlotContext ctx, Point p) {
		for (BarRect bar : rects(ctx)) {
			if (bar.rect().contains(p.x, p.y)) {
				return Optional.of(new MarkHit(bar.series(), bar.category(), bar.rect()));
			}
		}
		return Optional.empty();
	}

	@Override
	public TooltipContent tooltip(PlotContext ctx, MarkHit hit) {
		String category = data.categories()[hit.b()];
		String value = ChartFormat.groupedDecimal(data.values()[hit.a()][hit.b()]);
		String label = data.colorByCategory() ? category : data.seriesNames()[hit.a()];
		return TooltipContent.single(category, fillFor(ctx.theme(), hit.a(), hit.b()), label, value);
	}

	// One rectangle per drawn bar; shared by paint and hit-testing so their geometry can never drift apart.
	private List<BarRect> rects(PlotContext ctx) {
		List<BarRect> out = new ArrayList<>();
		Scale.Band band = (Scale.Band) ctx.yScale();
		Scale xScale = ctx.xScale();
		double baseline = baselineX(ctx);
		int series = data.seriesCount();
		double gap = ChartStyle.px(2);
		double cap = ChartStyle.px(MAX_BAR_THICK);

		for (int c = 0; c < data.categories().length; c++) {
			if (data.stacked()) {
				double barH = Math.min(cap, band.bandWidth());
				double y = band.center(c) - barH / 2;
				double cursor = baseline;
				for (int s = 0; s < series; s++) {
					double end = xScale.map(cumulative(c, s + 1));
					double segStart = Math.min(cursor + (s == 0 ? 0 : gap), end);
					out.add(new BarRect(s, c, new Rectangle2D.Double(segStart, y, Math.max(0, end - segStart), barH)));
					cursor = end;
				}
			}
			else {
				double groupTop = band.center(c) - band.bandWidth() / 2;
				double subSlot = band.bandWidth() / series;
				// Never let the gap subtract below a visible sliver (see BarRenderer).
				double barH = Math.max(1, Math.min(cap, subSlot - (series > 1 ? gap : 0)));
				for (int s = 0; s < series; s++) {
					double cy = groupTop + subSlot * (s + 0.5);
					double valueX = xScale.map(data.values()[s][c]);
					double left = Math.min(baseline, valueX);
					double w = Math.abs(valueX - baseline);
					out.add(new BarRect(s, c, new Rectangle2D.Double(left, cy - barH / 2, w, barH)));
				}
			}
		}
		return out;
	}

	private double cumulative(int category, int throughSeries) {
		double sum = 0;
		for (int s = 0; s < throughSeries; s++) {
			sum += data.values()[s][category];
		}
		return sum;
	}

	private double baselineX(PlotContext ctx) {
		double zero = ctx.xScale().map(0);
		return Math.max(ctx.plot().x, Math.min(ctx.plot().x + ctx.plot().width, zero));
	}

	private Color fillFor(ChartTheme theme, int series, int category) {
		int slot = data.colorByCategory() ? category : series;
		if (!data.colorByCategory() && "Other".equals(data.seriesNames()[series])) {
			return ChartInk.otherGray(theme);
		}
		if (data.colorByCategory() && "Other".equals(data.categories()[category])) {
			return ChartInk.otherGray(theme);
		}
		return ChartInk.series(theme, slot);
	}

	// A bar with its data end rounded and its baseline foot square — rightward bars round the right end,
	// leftward (negative) bars the left.
	private static Path2D roundedBar(Rectangle2D r, double radius, double baseline) {
		double y = r.getY();
		double h = r.getHeight();
		double left = r.getX();
		double right = r.getX() + r.getWidth();
		boolean rightward = right >= baseline;
		double rad = Math.min(radius, Math.min(h / 2, r.getWidth()));
		Path2D path = new Path2D.Double();
		if (rightward) {
			path.moveTo(left, y);
			path.lineTo(right - rad, y);
			path.quadTo(right, y, right, y + rad);
			path.lineTo(right, y + h - rad);
			path.quadTo(right, y + h, right - rad, y + h);
			path.lineTo(left, y + h);
		}
		else {
			path.moveTo(right, y);
			path.lineTo(left + rad, y);
			path.quadTo(left, y, left, y + rad);
			path.lineTo(left, y + h - rad);
			path.quadTo(left, y + h, left + rad, y + h);
			path.lineTo(right, y + h);
		}
		path.closePath();
		return path;
	}

	private record BarRect(int series, int category, Rectangle2D rect) { }
}
