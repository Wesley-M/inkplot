package io.github.wesleym.inkplot.render;

import io.github.wesleym.inkplot.ChartFormat;

import io.github.wesleym.inkplot.data.ChartData;
import io.github.wesleym.inkplot.data.Downsample;
import io.github.wesleym.inkplot.scale.Scale;
import io.github.wesleym.inkplot.ChartStyle;
import io.github.wesleym.inkplot.ChartTheme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws one or more line series over a shared X: a 2px stroke with round joins and caps, a single series carrying a
 * ~10% area wash beneath it, and an end marker with a 2px surface ring so it reads where it crosses a gridline. Dense
 * series are decimated to roughly one point per pixel (shape-preserving), so a line over a million rows still draws in
 * pixel-time.
 */
public final class LineRenderer implements MarkRenderer {

	private final ChartData.Line data;

	public LineRenderer(ChartData.Line data) {
		this.data = data;
	}

	@Override
	public XAxisModel xModel() {
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		for (ChartData.Line.Series s : data.series()) {
			for (double x : s.x()) {
				min = Math.min(min, x);
				max = Math.max(max, x);
			}
		}
		if (min > max) {
			min = 0;
			max = 1;
		}
		return data.timeAxis()
				? new XAxisModel.Time((long) min, (long) max)
				: new XAxisModel.Continuous(min, max);
	}

	@Override
	public YAxisModel yModel() {
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		double positiveMin = Double.POSITIVE_INFINITY;
		boolean hasNegative = false;
		boolean hasPositive = false;
		for (ChartData.Line.Series s : data.series()) {
			for (double y : s.y()) {
				min = Math.min(min, y);
				max = Math.max(max, y);
				hasNegative |= y < 0;
				hasPositive |= y > 0;
				if (y > 0) {
					positiveMin = Math.min(positiveMin, y);
				}
			}
		}
		if (min > max) {
			min = 0;
			max = 1;
		}
		// A line needs no forced zero baseline, but touching zero when the data is close to it reads more honestly.
		if (min > 0 && min < max * 0.35) {
			min = 0;
		}
		return YAxisModel.loggable(min, max, !hasNegative && hasPositive, positiveMin);
	}

	@Override
	public void paintMarks(Graphics2D g, PlotContext ctx) {
		g.setStroke(new BasicStroke(Math.max(1, ChartStyle.px(2)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		boolean single = data.series().length == 1;
		for (int s = 0; s < data.series().length; s++) {
			ChartData.Line.Series series = data.series()[s];
			if (series.x().length == 0) {
				continue;
			}
			Color colour = ChartInk.series(ctx.theme(), s);
			int[] keep = Downsample.lttb(series.x(), series.y(), Math.max(8, ctx.plot().width));
			Path2D line = new Path2D.Double();
			for (int k = 0; k < keep.length; k++) {
				double px = ctx.xScale().map(series.x()[keep[k]]);
				double py = ctx.yScale().map(series.y()[keep[k]]);
				if (k == 0) {
					line.moveTo(px, py);
				}
				else {
					line.lineTo(px, py);
				}
			}
			if (single) {
				paintArea(g, ctx, line, colour);
			}
			g.setColor(colour);
			g.draw(line);
			paintEndMarker(g, ctx, series, colour);
		}
	}

	@Override
	public List<LegendEntry> legend(ChartTheme theme) {
		if (data.series().length <= 1) {
			return List.of();
		}
		List<LegendEntry> entries = new ArrayList<>();
		for (int s = 0; s < data.series().length; s++) {
			entries.add(new LegendEntry(ChartInk.series(theme, s), data.series()[s].name(), true));
		}
		return entries;
	}

	@Override
	public boolean usesCrosshair() {
		return true;
	}

	@Override
	public java.util.Optional<CrosshairReadout> crosshairAt(PlotContext ctx, int mouseX) {
		if (data.series().length == 0 || data.series()[0].x().length == 0) {
			return java.util.Optional.empty();
		}
		double xValue = CrosshairMath.invertX(ctx.xScale(), mouseX);
		ChartData.Line.Series primary = data.series()[0];
		int anchor = CrosshairMath.nearest(primary.x(), xValue);
		double snapX = ctx.xScale().map(primary.x()[anchor]);

		List<TooltipContent.Row> rows = new ArrayList<>();
		for (int s = 0; s < data.series().length; s++) {
			ChartData.Line.Series series = data.series()[s];
			int idx = CrosshairMath.nearest(series.x(), xValue);
			rows.add(new TooltipContent.Row(ChartInk.series(ctx.theme(), s), series.name(),
					ChartFormat.groupedDecimal(series.y()[idx], 2)));
		}
		String title = CrosshairMath.formatX(data.timeAxis(), primary.x()[anchor]);
		return java.util.Optional.of(new CrosshairReadout(snapX, new TooltipContent(title, rows)));
	}

	private void paintArea(Graphics2D g, PlotContext ctx, Path2D line, Color colour) {
		double baseline = ctx.yScale() instanceof Scale.Log
				? ctx.plot().y + ctx.plot().height
				: Math.max(ctx.plot().y, Math.min(ctx.plot().y + ctx.plot().height, ctx.yScale().map(0)));
		Path2D area = new Path2D.Double(line);
		java.awt.geom.Point2D current = line.getCurrentPoint();
		java.awt.geom.Rectangle2D b = line.getBounds2D();
		area.lineTo(current.getX(), baseline);
		area.lineTo(b.getMinX(), baseline);
		area.closePath();
		g.setColor(ChartInk.areaWash(colour));
		g.fill(area);
	}

	private void paintEndMarker(Graphics2D g, PlotContext ctx, ChartData.Line.Series series, Color colour) {
		int last = series.x().length - 1;
		double px = ctx.xScale().map(series.x()[last]);
		double py = ctx.yScale().map(series.y()[last]);
		double r = Math.max(3, ChartStyle.px(4));
		double ring = Math.max(1, ChartStyle.px(2));
		g.setColor(ChartInk.separator(ctx.theme()));
		g.fill(new Ellipse2D.Double(px - r - ring, py - r - ring, (r + ring) * 2, (r + ring) * 2));
		g.setColor(colour);
		g.fill(new Ellipse2D.Double(px - r, py - r, r * 2, r * 2));
	}
}
