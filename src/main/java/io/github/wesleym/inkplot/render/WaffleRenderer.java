package io.github.wesleym.inkplot.render;

import io.github.wesleym.inkplot.ChartFormat;
import io.github.wesleym.inkplot.data.ChartData;
import io.github.wesleym.inkplot.ChartStyle;
import io.github.wesleym.inkplot.ChartTheme;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Draws shares of a whole as a waffle: a {@value #SIDE}×{@value #SIDE} unit grid (each cell {@code 1/}{@value
 * #CELLS} of the whole) filled largest-category-first in reading order, with a swatch-labelled list beside it and
 * the total beneath the list. The fine grid is why the waffle admits more categories than the doughnut — a share
 * far under a ring slice's legibility still gets visible cells. Cells are allocated by largest remainder with a
 * one-cell floor, so every category in the data is somewhere on the grid. Hover works per category: any of its
 * cells (or its label) merges them all into one solid, lifted silhouette — the share momentarily reads as a
 * single mark while the tooltip carries the exact count.
 */
public final class WaffleRenderer implements MarkRenderer {

	/** The grid is {@code SIDE × SIDE}; a finer grid resolves smaller shares at the cost of busier cells. */
	public static final int SIDE = 20;
	public static final int CELLS = SIDE * SIDE;

	/** Deterministic text measurement without a Graphics, so hit-testing lays out exactly like painting. */
	private static final FontRenderContext FRC = new FontRenderContext(null, true, true);

	private final ChartData.Waffle data;

	public WaffleRenderer(ChartData.Waffle data) {
		this.data = data;
	}

	@Override
	public boolean axisFree() {
		return true;
	}

	// No axes; degenerate models keep the canvas's log-scale probe trivially false.
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
		return List.of();   // the swatch list beside the grid is the legend, drawn by this renderer
	}

	/**
	 * Cells per category over {@code cells}, by largest remainder, with a one-cell floor for every positive
	 * share (stolen from the largest category) so no category vanishes from the grid.
	 */
	public static int[] quotas(double[] values, int cells) {
		double total = 0;
		for (double v : values) {
			total += v;
		}
		int[] quota = new int[values.length];
		double[] remainder = new double[values.length];
		int assigned = 0;
		for (int i = 0; i < values.length; i++) {
			double exact = values[i] / total * cells;
			quota[i] = (int) Math.floor(exact);
			remainder[i] = exact - quota[i];
			assigned += quota[i];
		}
		while (assigned < cells) {
			int best = 0;
			for (int i = 1; i < values.length; i++) {
				if (remainder[i] > remainder[best]) {
					best = i;
				}
			}
			quota[best]++;
			remainder[best] = -1;
			assigned++;
		}
		int largest = 0;
		for (int i = 1; i < values.length; i++) {
			if (quota[i] > quota[largest]) {
				largest = i;
			}
		}
		for (int i = 0; i < values.length; i++) {
			if (quota[i] == 0 && values[i] > 0) {
				quota[i] = 1;
				quota[largest]--;
			}
		}
		return quota;
	}

	@Override
	public void paintMarks(Graphics2D g, PlotContext ctx) {
		Layout layout = layout(ctx);
		double radius = ChartStyle.px(2);
		for (Cell cell : layout.cells()) {
			g.setColor(fillFor(ctx.theme(), cell.category()));
			g.fill(rounded(cell.rect(), radius));
		}
		paintLabels(g, ctx.theme(), layout);
	}

	@Override
	public void highlight(Graphics2D g, PlotContext ctx, MarkHit hit) {
		Layout layout = layout(ctx);
		Color lifted = ChartInk.lift(ctx.theme(), fillFor(ctx.theme(), hit.a()));
		// The hovered category's cells merge into one solid silhouette: each cell grows by half the grid gap, so
		// the gaps INSIDE the category close exactly (the reading-order fill keeps its region contiguous) while
		// half a gap survives at the border with its neighbours — the share reads as one mark, like a slice,
		// and the tooltip carries the unit count the merge hides.
		double grow = ChartStyle.px(2) / 2.0;
		Area silhouette = new Area();
		for (Cell cell : layout.cells()) {
			if (cell.category() == hit.a()) {
				Rectangle2D r = cell.rect();
				silhouette.add(new Area(new Rectangle2D.Double(r.getX() - grow, r.getY() - grow,
						r.getWidth() + 2 * grow, r.getHeight() + 2 * grow)));
			}
		}
		g.setColor(lifted);
		g.fill(silhouette);
		for (Label label : layout.labels()) {
			if (label.index() == hit.a()) {
				g.fill(label.swatch());
			}
		}
	}

	@Override
	public Optional<MarkHit> hitTest(PlotContext ctx, Point p) {
		Layout layout = layout(ctx);
		// Hit-test the cells grown by the same half-gap the hover silhouette uses, so the grid is contiguous to
		// the pointer: sweeping within one category must hold the highlight steady, never blink it off while the
		// pointer crosses a 2px gap that the silhouette is painting as solid.
		double grow = ChartStyle.px(2) / 2.0;
		for (Cell cell : layout.cells()) {
			Rectangle2D r = cell.rect();
			if (p.x >= r.getX() - grow && p.x < r.getMaxX() + grow
					&& p.y >= r.getY() - grow && p.y < r.getMaxY() + grow) {
				return Optional.of(new MarkHit(cell.category(), 0, cell.rect()));
			}
		}
		// The label row stands in for its category — same lift and tooltip as its cells.
		for (Label label : layout.labels()) {
			if (label.bounds().contains(p.x, p.y)) {
				return Optional.of(new MarkHit(label.index(), 0, label.bounds()));
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
		Font caption = ChartStyle.caption();
		double total = data.total();
		LineMetrics lm = caption.getLineMetrics("Ag", FRC);
		int gap = ChartStyle.px(2);
		int pad = ChartStyle.px(ChartStyle.SPACE_M);
		int swatch = ChartStyle.px(10);
		int swatchGap = ChartStyle.px(6);

		String[] texts = new String[data.values().length];
		double maxLabelW = 0;
		for (int i = 0; i < texts.length; i++) {
			texts[i] = ShareText.label(data.categories()[i], data.values()[i] / total);
			maxLabelW = Math.max(maxLabelW, caption.getStringBounds(texts[i], FRC).getWidth());
		}
		double labelColW = swatch + swatchGap + maxLabelW;

		double gridSide = Math.min(plot.height, plot.width - labelColW - pad);
		boolean labelled = gridSide >= ChartStyle.px(80);
		if (!labelled) {
			gridSide = Math.min(plot.width, plot.height);   // too tight for the list: bare grid, tooltips still tell
			labelColW = 0;
			pad = 0;
		}
		double cell = (gridSide - (SIDE - 1) * gap) / SIDE;
		double blockW = gridSide + (labelled ? pad + labelColW : 0);
		double gridX = plot.getCenterX() - blockW / 2;
		double gridY = plot.getCenterY() - gridSide / 2;

		int[] quota = quotas(data.values(), CELLS);
		List<Cell> cells = new ArrayList<>(CELLS);
		int category = 0;
		int used = 0;
		for (int i = 0; i < CELLS; i++) {
			while (category < quota.length && used >= quota[category]) {
				category++;
				used = 0;
			}
			if (category >= quota.length) {
				break;
			}
			double x = gridX + (i % SIDE) * (cell + gap);
			double y = gridY + (i / SIDE) * (cell + gap);
			cells.add(new Cell(category, new Rectangle2D.Double(x, y, cell, cell)));
			used++;
		}

		List<Label> labels = new ArrayList<>();
		if (labelled) {
			// The swatch list sits beside the grid, vertically centred, the total in muted ink beneath it.
			double rowH = Math.max(lm.getHeight() + ChartStyle.px(4), swatch + ChartStyle.px(4));
			double listH = texts.length * rowH;
			double listX = gridX + gridSide + pad;
			double listY = plot.getCenterY() - (listH + rowH) / 2;   // + one row for the total line
			for (int i = 0; i < texts.length; i++) {
				double y = listY + i * rowH;
				Rectangle2D swatchRect = new Rectangle2D.Double(listX, y + (rowH - swatch) / 2, swatch, swatch);
				Rectangle2D bounds = new Rectangle2D.Double(listX, y, labelColW, rowH);
				labels.add(new Label(i, texts[i], swatchRect, bounds,
						listX + swatch + swatchGap, y + (rowH + lm.getAscent()) / 2 - ChartStyle.px(2)));
			}
			labels.add(new Label(-1, ChartFormat.groupedDecimal(total) + " " + data.valueLabel(), null,
					new Rectangle2D.Double(0, 0, 0, 0),
					listX, listY + texts.length * rowH + (rowH + lm.getAscent()) / 2 - ChartStyle.px(2)));
		}
		return new Layout(cells, labels);
	}

	private void paintLabels(Graphics2D g, ChartTheme theme, Layout layout) {
		g.setFont(ChartStyle.caption());
		for (Label label : layout.labels()) {
			if (label.swatch() != null) {
				g.setColor(fillFor(theme, label.index()));
				g.fill(label.swatch());
				g.setColor(theme.text());
			}
			else {
				g.setColor(theme.muted());   // the total line
			}
			g.drawString(label.text(), (float) label.textX(), (float) label.baseline());
		}
	}

	private static RoundRectangle2D rounded(Rectangle2D r, double radius) {
		return new RoundRectangle2D.Double(r.getX(), r.getY(), r.getWidth(), r.getHeight(), radius, radius);
	}

	private Color fillFor(ChartTheme theme, int category) {
		return "Other".equals(data.categories()[category]) ? ChartInk.otherGray(theme)
				: ChartInk.series(theme, category);
	}

	/** One grid cell: the category it counts toward and its rectangle. */
	private record Cell(int category, Rectangle2D rect) { }

	/** One list row: its category ({@code -1} for the total line), swatch, hover bounds, and text position. */
	private record Label(int index, String text, Rectangle2D swatch, Rectangle2D bounds, double textX,
			double baseline) { }

	private record Layout(List<Cell> cells, List<Label> labels) { }
}
