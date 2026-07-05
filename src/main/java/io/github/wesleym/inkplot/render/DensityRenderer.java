package io.github.wesleym.inkplot.render;

import io.github.wesleym.inkplot.ChartFormat;

import io.github.wesleym.inkplot.data.ChartData;
import io.github.wesleym.inkplot.scale.Scale;
import io.github.wesleym.inkplot.ChartStyle;
import io.github.wesleym.inkplot.ChartTheme;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.List;

/**
 * Draws a smoothed 1-D density (KDE) of one numeric column: a 2px curve over a ~10% area wash in the sequential hue —
 * the continuous cousin of the histogram, showing the shape of a distribution without imposing bin edges.
 */
public final class DensityRenderer implements MarkRenderer {

	private final ChartData.Density data;

	public DensityRenderer(ChartData.Density data) {
		this.data = data;
	}

	@Override
	public XAxisModel xModel() {
		return new XAxisModel.Continuous(data.min(), data.max());
	}

	@Override
	public YAxisModel yModel() {
		double max = 0;
		for (double d : data.density()) {
			max = Math.max(max, d);
		}
		return YAxisModel.linear(0, max);
	}

	@Override
	public void paintMarks(Graphics2D g, PlotContext ctx) {
		if (data.gridX().length == 0) {
			return;
		}
		Path2D curve = new Path2D.Double();
		for (int i = 0; i < data.gridX().length; i++) {
			double px = ctx.xScale().map(data.gridX()[i]);
			double py = ctx.yScale().map(data.density()[i]);
			if (i == 0) {
				curve.moveTo(px, py);
			}
			else {
				curve.lineTo(px, py);
			}
		}

		double baseline = ctx.yScale() instanceof Scale.Log
				? ctx.plot().y + ctx.plot().height
				: Math.max(ctx.plot().y, Math.min(ctx.plot().y + ctx.plot().height, ctx.yScale().map(0)));
		Path2D area = new Path2D.Double(curve);
		double lastX = ctx.xScale().map(data.gridX()[data.gridX().length - 1]);
		double firstX = ctx.xScale().map(data.gridX()[0]);
		area.lineTo(lastX, baseline);
		area.lineTo(firstX, baseline);
		area.closePath();

		g.setColor(ChartInk.areaWash(ChartInk.distributionFill(ctx.theme())));
		g.fill(area);
		g.setStroke(new BasicStroke(Math.max(1, ChartStyle.px(2)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(ChartInk.distributionFill(ctx.theme()));
		g.draw(curve);
	}

	@Override
	public List<LegendEntry> legend(ChartTheme theme) {
		return List.of();
	}

	@Override
	public boolean usesCrosshair() {
		return true;
	}

	@Override
	public java.util.Optional<CrosshairReadout> crosshairAt(PlotContext ctx, int mouseX) {
		if (data.gridX().length == 0) {
			return java.util.Optional.empty();
		}
		double xValue = CrosshairMath.invertX(ctx.xScale(), mouseX);
		int idx = CrosshairMath.nearest(data.gridX(), xValue);
		double snapX = ctx.xScale().map(data.gridX()[idx]);
		TooltipContent tip = TooltipContent.single(
				ChartFormat.groupedDecimal(data.gridX()[idx], 1),
				ChartInk.distributionFill(ctx.theme()), "Density",
				ChartFormat.groupedDecimal(data.density()[idx], 4));
		return java.util.Optional.of(new CrosshairReadout(snapX, tip));
	}
}
