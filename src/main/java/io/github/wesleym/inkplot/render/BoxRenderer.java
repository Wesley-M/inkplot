package io.github.wesleym.inkplot.render;

import io.github.wesleym.inkplot.ChartFormat;
import io.github.wesleym.inkplot.data.ChartData;
import io.github.wesleym.inkplot.scale.Scale;
import io.github.wesleym.inkplot.ChartStyle;
import io.github.wesleym.inkplot.ChartTheme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Optional;

/**
 * Draws a box-and-whisker summary per category: hairline whiskers to the fences, a ≤24px box spanning the quartiles
 * over a light fill of the sequential hue, a 2px median rule, and capped outlier dots with a surface ring. One hue
 * throughout — the box is a distribution, not a set of distinct series.
 */
public final class BoxRenderer implements MarkRenderer {

	private static final int MAX_BOX_WIDTH = 24;

	private final ChartData.Box data;

	public BoxRenderer(ChartData.Box data) {
		this.data = data;
	}

	@Override
	public XAxisModel xModel() {
		String[] labels = new String[data.groups().length];
		for (int i = 0; i < labels.length; i++) {
			labels[i] = data.groups()[i].label();
		}
		return new XAxisModel.Band(labels);
	}

	@Override
	public YAxisModel yModel() {
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		boolean allPositive = true;
		for (ChartData.Box.Group grp : data.groups()) {
			min = Math.min(min, grp.lo());
			max = Math.max(max, grp.hi());
			allPositive &= grp.lo() > 0;
			for (double o : grp.outliers()) {
				min = Math.min(min, o);
				max = Math.max(max, o);
				allPositive &= o > 0;
			}
		}
		if (min > max) {
			min = 0;
			max = 1;
		}
		return YAxisModel.loggable(min, max, allPositive, min);
	}

	@Override
	public RevealStyle revealStyle() {
		return RevealStyle.GROW_UP;
	}

	@Override
	public void paintMarks(Graphics2D g, PlotContext ctx) {
		Scale.Band band = (Scale.Band) ctx.xScale();
		double boxW = Math.min(ChartStyle.px(MAX_BOX_WIDTH), band.bandWidth());
		for (int i = 0; i < data.groups().length; i++) {
			paintGroup(g, ctx, band, boxW, i, false);
		}
	}

	@Override
	public void highlight(Graphics2D g, PlotContext ctx, MarkHit hit) {
		Scale.Band band = (Scale.Band) ctx.xScale();
		double boxW = Math.min(ChartStyle.px(MAX_BOX_WIDTH), band.bandWidth());
		paintGroup(g, ctx, band, boxW, hit.a(), true);
	}

	private void paintGroup(Graphics2D g, PlotContext ctx, Scale.Band band, double boxW, int i, boolean hot) {
		ChartData.Box.Group grp = data.groups()[i];
		Color hue = ChartInk.distributionFill(ctx.theme());
		double cx = band.center(i);
		double x0 = cx - boxW / 2;
		double yLo = ctx.yScale().map(grp.lo());
		double yHi = ctx.yScale().map(grp.hi());
		double yQ1 = ctx.yScale().map(grp.q1());
		double yQ3 = ctx.yScale().map(grp.q3());
		double yMed = ctx.yScale().map(grp.median());

		// Whiskers and their end caps in the recessive hairline tone.
		g.setStroke(new BasicStroke(Math.max(1, ChartStyle.px(1))));
		g.setColor(ChartInk.axisText(ctx.theme()));
		g.drawLine((int) cx, (int) yHi, (int) cx, (int) yQ3);
		g.drawLine((int) cx, (int) yQ1, (int) cx, (int) yLo);
		double capW = boxW * 0.5;
		g.drawLine((int) (cx - capW / 2), (int) yHi, (int) (cx + capW / 2), (int) yHi);
		g.drawLine((int) (cx - capW / 2), (int) yLo, (int) (cx + capW / 2), (int) yLo);

		// The interquartile box: a light fill under a thin hue outline; lifts on hover.
		Rectangle2D box = new Rectangle2D.Double(x0, Math.min(yQ1, yQ3), boxW, Math.abs(yQ3 - yQ1));
		g.setColor(ChartInk.alpha(hue, hot ? 0.34 : ctx.theme().dark() ? 0.26 : 0.16));
		g.fill(box);
		g.setStroke(new BasicStroke(Math.max(1, ChartStyle.px(1))));
		g.setColor(hue);
		g.draw(box);
		g.setStroke(new BasicStroke(Math.max(1, ChartStyle.px(2))));
		g.drawLine((int) x0, (int) yMed, (int) (x0 + boxW), (int) yMed);

		paintOutliers(g, ctx, grp, cx, hue);
	}

	@Override
	public List<LegendEntry> legend(ChartTheme theme) {
		return List.of();
	}

	@Override
	public Optional<MarkHit> hitTest(PlotContext ctx, Point p) {
		Scale.Band band = (Scale.Band) ctx.xScale();
		int i = band.indexAt(p.x);
		if (i < 0 || i >= data.groups().length || p.y < ctx.plot().y || p.y > ctx.plot().y + ctx.plot().height) {
			return Optional.empty();
		}
		double boxW = Math.min(ChartStyle.px(MAX_BOX_WIDTH), band.bandWidth());
		ChartData.Box.Group grp = data.groups()[i];
		Rectangle2D bounds = new Rectangle2D.Double(band.center(i) - boxW / 2, ctx.yScale().map(grp.hi()), boxW,
				Math.abs(ctx.yScale().map(grp.lo()) - ctx.yScale().map(grp.hi())));
		return Optional.of(new MarkHit(i, -1, bounds));
	}

	@Override
	public TooltipContent tooltip(PlotContext ctx, MarkHit hit) {
		ChartData.Box.Group grp = data.groups()[hit.a()];
		Color hue = ChartInk.distributionFill(ctx.theme());
		return new TooltipContent(grp.label(), List.of(
				new TooltipContent.Row(hue, "Max", ChartFormat.groupedDecimal(grp.hi(), 2)),
				new TooltipContent.Row(hue, "Q3", ChartFormat.groupedDecimal(grp.q3(), 2)),
				new TooltipContent.Row(hue, "Median", ChartFormat.groupedDecimal(grp.median(), 2)),
				new TooltipContent.Row(hue, "Q1", ChartFormat.groupedDecimal(grp.q1(), 2)),
				new TooltipContent.Row(hue, "Min", ChartFormat.groupedDecimal(grp.lo(), 2))));
	}

	private void paintOutliers(Graphics2D g, PlotContext ctx, ChartData.Box.Group grp, double cx, Color hue) {
		double r = Math.max(2, ChartStyle.px(3));
		double ring = Math.max(1, ChartStyle.px(2));
		for (double o : grp.outliers()) {
			double y = ctx.yScale().map(o);
			g.setColor(ChartInk.separator(ctx.theme()));
			g.fill(new Ellipse2D.Double(cx - r - ring, y - r - ring, (r + ring) * 2, (r + ring) * 2));
			g.setColor(ChartInk.alpha(hue, 0.8));
			g.fill(new Ellipse2D.Double(cx - r, y - r, r * 2, r * 2));
		}
	}
}
