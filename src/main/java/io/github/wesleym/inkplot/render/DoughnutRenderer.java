package io.github.wesleym.inkplot.render;

import io.github.wesleym.inkplot.ChartFormat;
import io.github.wesleym.inkplot.data.ChartData;
import io.github.wesleym.inkplot.ChartStyle;
import io.github.wesleym.inkplot.ChartTheme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Draws shares of a whole as a doughnut: slices largest-first clockwise from 12 o'clock, separated by the 2px surface
 * gap, each labelled directly on the chart ("name · pct" with a short leader) so nothing needs a legend to decode, and
 * the total in the hole. The label gutter is measured from the real label widths and the ring takes what's left; when
 * the surface is too small for callouts (an embedded thumbnail) the labels drop and the tooltips still carry them.
 */
public final class DoughnutRenderer implements MarkRenderer {

	private static final double HOLE_FRACTION = 0.62;
	/** The smallest ring worth labelling — under this the callouts drop rather than crowd the slices. */
	private static final int MIN_LABELLED_RADIUS = 40;
	/** Deterministic text measurement without a Graphics, so hit-testing lays out exactly like painting. */
	private static final FontRenderContext FRC = new FontRenderContext(null, true, true);

	private final ChartData.Doughnut data;
	private boolean preferLegend;   // legend entries below the chart instead of per-slice callouts

	public DoughnutRenderer(ChartData.Doughnut data) {
		this.data = data;
	}

	@Override
	public void setPreferLegend(boolean prefer) {
		this.preferLegend = prefer;
	}

	@Override
	public boolean axisFree() {
		return true;
	}

	// No axes on a radial chart; degenerate models keep the canvas's log-scale probe trivially false.
	@Override
	public XAxisModel xModel() {
		return new XAxisModel.Continuous(0, 1);
	}

	@Override
	public YAxisModel yModel() {
		return YAxisModel.linear(0, 1);
	}

	@Override
	public List<LegendEntry> legend(ChartTheme theme) {
		if (!preferLegend) {
			return List.of();   // every slice is labelled on the chart itself
		}
		List<LegendEntry> entries = new ArrayList<>(data.values().length);
		for (int i = 0; i < data.values().length; i++) {
			entries.add(new LegendEntry(fillFor(theme, i),
					ShareText.label(data.categories()[i], data.values()[i] / data.total()), false));
		}
		return entries;
	}

	@Override
	public void paintMarks(Graphics2D g, PlotContext ctx) {
		Layout layout = layout(ctx);
		g.setStroke(new BasicStroke(ChartStyle.px(2)));
		for (Slice slice : layout.slices()) {
			g.setColor(fillFor(ctx.theme(), slice.index()));
			g.fill(slice.shape());
			g.setColor(ChartInk.separator(ctx.theme()));
			g.draw(slice.shape());
		}
		paintLabels(g, ctx.theme(), layout);
		paintCenter(g, ctx.theme(), layout);
	}

	@Override
	public void highlight(Graphics2D g, PlotContext ctx, MarkHit hit) {
		Layout layout = layout(ctx);
		Color fill = fillFor(ctx.theme(), hit.a());
		for (Slice slice : layout.slices()) {
			if (slice.index() == hit.a()) {
				g.setColor(ChartInk.lift(ctx.theme(), fill));
				g.fill(slice.shape());
				g.setStroke(new BasicStroke(ChartStyle.px(2)));
				g.setColor(ChartInk.separator(ctx.theme()));
				g.draw(slice.shape());
			}
		}
		// The leader lights up in the slice's own colour, tying the callout to its slice across the ring.
		for (Label label : layout.labels()) {
			if (label.index() == hit.a()) {
				g.setColor(fill);
				g.setStroke(new BasicStroke(1.6f));
				g.draw(label.leader());
			}
		}
	}

	@Override
	public Optional<MarkHit> hitTest(PlotContext ctx, Point p) {
		Layout layout = layout(ctx);
		for (Slice slice : layout.slices()) {
			if (slice.shape().contains(p.x, p.y)) {
				return Optional.of(new MarkHit(slice.index(), 0, slice.shape().getBounds2D()));
			}
		}
		// A callout is the slice's name plate — hovering it hits its slice (same lift, leader, and tooltip).
		for (Label label : layout.labels()) {
			if (label.bounds().contains(p.x, p.y)) {
				return Optional.of(new MarkHit(label.index(), 0,
						layout.slices().get(label.index()).shape().getBounds2D()));
			}
		}
		return Optional.empty();
	}

	@Override
	public TooltipContent tooltip(PlotContext ctx, MarkHit hit) {
		int i = hit.a();
		String value = ShareText.tooltipValue(data.values()[i], data.values()[i] / data.total());
		return TooltipContent.single(data.categories()[i], fillFor(ctx.theme(), i), data.valueLabel(), value);
	}

	// One geometry pass shared by paint, hit-testing, and highlighting, so they can never drift apart.
	private Layout layout(PlotContext ctx) {
		Rectangle plot = ctx.plot();
		double cx = plot.getCenterX();
		double cy = plot.getCenterY();
		Font caption = ChartStyle.caption();
		double total = data.total();

		String[] texts = new String[data.values().length];
		double maxLabelW = 0;
		for (int i = 0; i < texts.length; i++) {
			texts[i] = ShareText.label(data.categories()[i], data.values()[i] / total);
			maxLabelW = Math.max(maxLabelW, caption.getStringBounds(texts[i], FRC).getWidth());
		}
		LineMetrics lm = caption.getLineMetrics("Ag", FRC);
		int leader = ChartStyle.px(6);
		int pad = ChartStyle.px(4);

		double outerR = Math.min(plot.width / 2.0 - (maxLabelW + leader + pad), plot.height / 2.0 - lm.getHeight() - pad);
		boolean labelled = !preferLegend && outerR >= ChartStyle.px(MIN_LABELLED_RADIUS);
		if (!labelled) {
			outerR = Math.min(plot.width, plot.height) / 2.0 - pad;
		}
		double holeR = outerR * HOLE_FRACTION;

		List<Slice> slices = new ArrayList<>();
		Area hole = new Area(new Ellipse2D.Double(cx - holeR, cy - holeR, holeR * 2, holeR * 2));
		double t = 0;
		for (int i = 0; i < data.values().length; i++) {
			double share = data.values()[i] / total;
			Area shape = new Area(new Arc2D.Double(cx - outerR, cy - outerR, outerR * 2, outerR * 2,
					90 - t * 360, -share * 360, Arc2D.PIE));
			shape.subtract(hole);
			slices.add(new Slice(i, shape, (t + share / 2) * 360));
			t += share;
		}

		List<Label> labels = labelled ? placeLabels(texts, slices, cx, cy, outerR, leader, pad, lm, plot) : List.of();
		return new Layout(slices, labels, cx, cy, holeR);
	}

	// The classic pie-callout layout: labels live in fixed gutter columns at the ring's left and right — outside
	// the ring, so a label can never sit on a slice — at their slice's projected height, swept a line apart so a
	// cluster of tiny slices (which all project near 12 o'clock) stacks neatly instead of overprinting. Each
	// leader runs arc edge → a short radial elbow → the label's spot in the column.
	private List<Label> placeLabels(String[] texts, List<Slice> slices, double cx, double cy, double outerR,
			int leader, int pad, LineMetrics lm, Rectangle plot) {
		Font caption = ChartStyle.caption();
		List<Callout> left = new ArrayList<>();
		List<Callout> right = new ArrayList<>();
		for (Slice slice : slices) {
			double rad = Math.toRadians(slice.midDeg());
			double dx = Math.sin(rad);
			double dy = -Math.cos(rad);
			Point2D from = new Point2D.Double(cx + dx * outerR, cy + dy * outerR);
			Point2D elbow = new Point2D.Double(cx + dx * (outerR + leader * 0.6), cy + dy * (outerR + leader * 0.6));
			double idealY = cy + dy * (outerR + leader);
			(dx >= 0 ? right : left).add(new Callout(slice.index(), texts[slice.index()], from, elbow, idealY));
		}
		List<Label> labels = new ArrayList<>();
		double lineH = lm.getHeight();
		for (List<Callout> side : List.of(right, left)) {
			boolean rightSide = side == right;
			double columnX = rightSide ? cx + outerR + leader : cx - outerR - leader;
			// Projected y is monotone in the slice angle within a side, so sorting by it keeps leaders uncrossed.
			side.sort((a, b) -> Double.compare(a.idealY(), b.idealY()));
			double[] ys = sweep(side, lineH, plot);
			for (int i = 0; i < side.size(); i++) {
				Callout c = side.get(i);
				double textW = caption.getStringBounds(c.text(), FRC).getWidth();
				double x = rightSide ? columnX + pad : columnX - pad - textW;
				double baseline = ys[i] + lm.getAscent() - lineH / 2;
				Path2D lead = new Path2D.Double();
				lead.moveTo(c.from().getX(), c.from().getY());
				lead.lineTo(c.elbow().getX(), c.elbow().getY());
				lead.lineTo(columnX, ys[i]);
				Rectangle2D bounds = new Rectangle2D.Double(x, ys[i] - lineH / 2, textW, lineH);
				labels.add(new Label(c.index(), c.text(), x, baseline, bounds, lead));
			}
		}
		return labels;
	}

	// Y positions for one column: each label at its ideal height, pushed down to stay a line apart, then the tail
	// walked back up if the column ran past the plot — so a dense cluster stays inside and evenly spaced.
	private static double[] sweep(List<Callout> side, double lineH, Rectangle plot) {
		double minY = plot.y + lineH / 2;
		double maxY = plot.y + plot.height - lineH / 2;
		double[] ys = new double[side.size()];
		double y = minY - lineH;
		for (int i = 0; i < side.size(); i++) {
			y = Math.max(side.get(i).idealY(), y + lineH);
			ys[i] = y;
		}
		if (y > maxY) {
			for (int i = side.size() - 1; i >= 0; i--) {
				double limit = i == side.size() - 1 ? maxY : ys[i + 1] - lineH;
				ys[i] = Math.min(ys[i], limit);
			}
		}
		return ys;
	}

	private void paintLabels(Graphics2D g, ChartTheme theme, Layout layout) {
		g.setFont(ChartStyle.caption());
		g.setStroke(new BasicStroke(1f));
		for (Label label : layout.labels()) {
			g.setColor(ChartInk.alpha(theme.muted(), 0.55));
			g.draw(label.leader());
			g.setColor(theme.text());
			g.drawString(label.text(), (float) label.x(), (float) label.baseline());
		}
	}

	// The whole the slices share, stated once in the hole — drawn only when it fits, so a thumbnail ring stays clean.
	private void paintCenter(Graphics2D g, ChartTheme theme, Layout layout) {
		String total = ChartFormat.groupedDecimal(data.total());
		Font totalFont = ChartStyle.title();
		Font labelFont = ChartStyle.caption();
		double totalW = totalFont.getStringBounds(total, FRC).getWidth();
		double labelW = labelFont.getStringBounds(data.valueLabel(), FRC).getWidth();
		if (Math.max(totalW, labelW) > layout.holeR() * 2 - ChartStyle.px(8)) {
			return;
		}
		g.setFont(totalFont);
		g.setColor(theme.text());
		g.drawString(total, (float) (layout.cx() - totalW / 2), (float) (layout.cy() - ChartStyle.px(2)));
		g.setFont(labelFont);
		g.setColor(theme.muted());
		float labelAscent = labelFont.getLineMetrics(data.valueLabel(), FRC).getAscent();
		g.drawString(data.valueLabel(), (float) (layout.cx() - labelW / 2),
				(float) (layout.cy() + labelAscent + ChartStyle.px(2)));
	}

	private Color fillFor(ChartTheme theme, int slice) {
		Color[] semantic = data.sliceColors();
		if (semantic != null && slice < semantic.length && semantic[slice] != null) {
			return semantic[slice];
		}
		return "Other".equals(data.categories()[slice]) ? ChartInk.otherGray(theme) : ChartInk.series(theme, slice);
	}

	/** One drawn slice: its data index, its ring shape, and its mid-angle in degrees clockwise from 12 o'clock. */
	private record Slice(int index, Shape shape, double midDeg) { }

	/** A placed callout: its slice, its text at an exact baseline, its hover bounds, and its elbow leader. */
	private record Label(int index, String text, double x, double baseline, Rectangle2D bounds, Path2D leader) { }

	/** A callout before column placement: its slice, its leader's arc-edge/elbow points, and its ideal height. */
	private record Callout(int index, String text, Point2D from, Point2D elbow, double idealY) { }

	private record Layout(List<Slice> slices, List<Label> labels, double cx, double cy, double holeR) { }
}
