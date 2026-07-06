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
 * Draws category bars — single-series, grouped, or stacked — as thin marks growing from one baseline: capped at 24px
 * thick so a wide band keeps air around the bar, with a 4px-rounded data end, a square foot at the baseline, and a 2px
 * surface gap separating touching marks (grouped neighbours and stacked segments alike) rather than a drawn border.
 */
public final class BarRenderer implements MarkRenderer {

	private static final int MAX_BAR_THICK = 24;

	private final ChartData.Bar data;

	public BarRenderer(ChartData.Bar data) {
		this.data = data;
	}

	@Override
	public XAxisModel xModel() {
		return new XAxisModel.Band(data.categories());
	}

	@Override
	public YAxisModel yModel() {
		double min = 0;
		double max = 0;
		double positiveMin = Double.POSITIVE_INFINITY;
		boolean hasNegative = false;
		boolean hasPositive = false;
		if (data.stacked()) {
			for (int c = 0; c < data.categories().length; c++) {
				double sum = 0;
				for (int s = 0; s < data.seriesCount(); s++) {
					double v = data.values()[s][c];
					sum += v;
					hasNegative |= v < 0;
				}
				max = Math.max(max, sum);
			}
		}
		else {
			for (double[] series : data.values()) {
				for (double v : series) {
					max = Math.max(max, v);
					min = Math.min(min, v);
					hasNegative |= v < 0;
					hasPositive |= v > 0;
					if (v > 0) {
						positiveMin = Math.min(positiveMin, v);
					}
				}
			}
		}
		// Bars read against a zero baseline; a log value axis is offered when there are no negatives (zero bars just
		// sit at the floor) and the bars aren't stacked, flooring at the smallest positive bar.
		boolean allowLog = !hasNegative && hasPositive && !data.stacked();
		return YAxisModel.loggable(min, max, allowLog, positiveMin);
	}

	@Override
	public RevealStyle revealStyle() {
		return RevealStyle.GROW_UP;
	}

	@Override
	public void paintMarks(Graphics2D g, PlotContext ctx) {
		double radius = ChartStyle.px(4);
		double baseline = baselineY(ctx);
		SeriesEmphasis em = ctx.emphasis();
		java.awt.Composite base = g.getComposite();
		for (BarRect bar : rects(ctx)) {
			if (em.isHidden(bar.series())) {
				continue;
			}
			g.setComposite(em.composite(bar.series(), base));
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
		double baseline = baselineY(ctx);
		for (BarRect bar : rects(ctx)) {
			if (bar.series() == hit.a() && bar.category() == hit.b()) {
				g.setColor(ChartInk.lift(ctx.theme(), fillFor(ctx.theme(), bar.series(), bar.category())));
				g.fill(barShape(bar, radius, baseline));
				return;
			}
		}
	}

	// Only the exposed data end rounds: a plain bar's tip, or just the top segment of a stack — interior stacked
	// segments stay square so the rounding never shows inside the column.
	private java.awt.Shape barShape(BarRect bar, double radius, double baseline) {
		boolean roundEnd = !data.stacked() || bar.series() == data.seriesCount() - 1;
		return roundEnd ? roundedBar(bar.rect(), radius, baseline) : bar.rect();
	}

	@Override
	public List<LegendEntry> legend(ChartTheme theme) {
		// Bars coloured by their own category are identified by the x-axis labels — a per-category legend would just
		// restate them (and be enormous), so none is drawn.
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
		Scale.Band band = (Scale.Band) ctx.xScale();
		Scale yScale = ctx.yScale();
		double baseline = baselineY(ctx);
		int series = data.seriesCount();
		double gap = ChartStyle.px(2);
		double cap = ChartStyle.px(MAX_BAR_THICK);

		for (int c = 0; c < data.categories().length; c++) {
			if (data.stacked()) {
				double barW = Math.min(cap, band.bandWidth());
				double x = band.center(c) - barW / 2;
				double cursor = baseline;
				for (int s = 0; s < series; s++) {
					double top = yScale.map(cumulative(c, s + 1));
					double segTop = Math.min(top + (s == 0 ? 0 : gap), cursor);
					out.add(new BarRect(s, c, new Rectangle2D.Double(x, segTop, barW, Math.max(0, cursor - segTop))));
					cursor = top;
				}
			}
			else {
				double groupLeft = band.center(c) - band.bandWidth() / 2;
				double subSlot = band.bandWidth() / series;
				// Never let the gap subtract below a visible sliver — with many series in a narrow band, subSlot - gap
				// goes negative and a negative-width rect paints nothing (and can't be hit-tested).
				double barW = Math.max(1, Math.min(cap, subSlot - (series > 1 ? gap : 0)));
				for (int s = 0; s < series; s++) {
					double cx = groupLeft + subSlot * (s + 0.5);
					double valueY = yScale.map(data.values()[s][c]);
					double top = Math.min(baseline, valueY);
					double h = Math.abs(valueY - baseline);
					out.add(new BarRect(s, c, new Rectangle2D.Double(cx - barW / 2, top, barW, h)));
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

	private double baselineY(PlotContext ctx) {
		if (ctx.yScale() instanceof Scale.Log) {
			return ctx.plot().y + ctx.plot().height;
		}
		double zero = ctx.yScale().map(0);
		return Math.max(ctx.plot().y, Math.min(ctx.plot().y + ctx.plot().height, zero));
	}

	private Color fillFor(ChartTheme theme, int series, int category) {
		// Coloured-by-category bars take the colour of their category slot; otherwise the series slot.
		int slot = data.colorByCategory() ? category : series;
		if (!data.colorByCategory() && "Other".equals(data.seriesNames()[series])) {
			return ChartInk.otherGray(theme);
		}
		if (data.colorByCategory() && "Other".equals(data.categories()[category])) {
			return ChartInk.otherGray(theme);
		}
		return ChartInk.series(theme, slot);
	}

	// A bar with its data end rounded and its baseline foot square — `up` bars round the top, `down` bars the bottom.
	private static Path2D roundedBar(Rectangle2D r, double radius, double baseline) {
		double x = r.getX();
		double w = r.getWidth();
		double top = r.getY();
		double bottom = r.getY() + r.getHeight();
		boolean up = top <= baseline;
		double rad = Math.min(radius, Math.min(w / 2, r.getHeight()));
		Path2D path = new Path2D.Double();
		if (up) {
			path.moveTo(x, bottom);
			path.lineTo(x, top + rad);
			path.quadTo(x, top, x + rad, top);
			path.lineTo(x + w - rad, top);
			path.quadTo(x + w, top, x + w, top + rad);
			path.lineTo(x + w, bottom);
		}
		else {
			path.moveTo(x, top);
			path.lineTo(x + w, top);
			path.lineTo(x + w, bottom - rad);
			path.quadTo(x + w, bottom, x + w - rad, bottom);
			path.lineTo(x + rad, bottom);
			path.quadTo(x, bottom, x, bottom - rad);
		}
		path.closePath();
		return path;
	}

	private record BarRect(int series, int category, Rectangle2D rect) { }
}
