package io.github.wesleym.inkplot.render;

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
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Draws shares of a whole as a squarified treemap: near-square tiles whose areas are the shares, separated
 * by the module's 2px surface gap, each labelled inside itself ("name" over "pct") when the tile has room —
 * the third parts-of-a-whole form, best when there are more categories than a ring reads well with. Labels
 * pick a light or dark ink per tile from the tile's own fill. Hover lifts a tile like a slice; the tooltip
 * carries the exact value. With a bottom legend requested, the legend names every tile the labels couldn't.
 */
public final class TreemapRenderer implements MarkRenderer {

	/** Deterministic text measurement without a Graphics, so hit-testing lays out exactly like painting. */
	private static final FontRenderContext FRC = new FontRenderContext(null, true, true);

	private final ChartData.Treemap data;
	private boolean preferLegend;   // legend entries below the chart, on top of the in-tile labels

	public TreemapRenderer(ChartData.Treemap data) {
		this.data = data;
	}

	@Override
	public boolean axisFree() {
		return true;
	}

	// No axes on a tiled chart; degenerate models keep the canvas's log-scale probe trivially false.
	@Override
	public XAxisModel xModel() {
		return new XAxisModel.Continuous(0, 1);
	}

	@Override
	public YAxisModel yModel() {
		return YAxisModel.linear(0, 1);
	}

	@Override
	public void setPreferLegend(boolean prefer) {
		this.preferLegend = prefer;
	}

	@Override
	public List<LegendEntry> legend(ChartTheme theme) {
		if (!preferLegend) {
			return List.of();   // the tiles carry their own labels where they fit
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
		double radius = ChartStyle.px(3);
		for (Tile tile : layout.tiles()) {
			g.setColor(fillFor(ctx.theme(), tile.index()));
			g.fill(rounded(tile.rect(), radius));
		}
		paintLabels(g, ctx.theme(), layout);
	}

	@Override
	public void highlight(Graphics2D g, PlotContext ctx, MarkHit hit) {
		Layout layout = layout(ctx);
		double radius = ChartStyle.px(3);
		for (Tile tile : layout.tiles()) {
			if (tile.index() == hit.a()) {
				g.setColor(ChartInk.lift(ctx.theme(), fillFor(ctx.theme(), tile.index())));
				g.fill(rounded(tile.rect(), radius));
			}
		}
		paintLabels(g, ctx.theme(), layout);   // the lifted fill must not swallow the tile's own label
	}

	@Override
	public Optional<MarkHit> hitTest(PlotContext ctx, Point p) {
		Layout layout = layout(ctx);
		// Grown by half the tile gap, so sweeping across the 2px separators never blinks the hover off.
		double grow = ChartStyle.px(2) / 2.0;
		for (Tile tile : layout.tiles()) {
			Rectangle2D r = tile.rect();
			if (p.x >= r.getX() - grow && p.x < r.getMaxX() + grow
					&& p.y >= r.getY() - grow && p.y < r.getMaxY() + grow) {
				return Optional.of(new MarkHit(tile.index(), 0, r));
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
		double gap = ChartStyle.px(2);
		Rectangle2D[] raw = TreemapSquarify.layout(data.values(),
				new Rectangle2D.Double(plot.x, plot.y, plot.width, plot.height));
		List<Tile> tiles = new ArrayList<>(raw.length);
		for (int i = 0; i < raw.length; i++) {
			Rectangle2D r = raw[i];
			if (r.getWidth() <= gap || r.getHeight() <= gap) {
				continue;   // a sliver below the gap has no drawable body; the legend/tooltip still carries it
			}
			// Inset by half the gap on every side, so neighbouring tiles read separated by one surface gap.
			tiles.add(new Tile(i, new Rectangle2D.Double(r.getX() + gap / 2, r.getY() + gap / 2,
					r.getWidth() - gap, r.getHeight() - gap)));
		}
		return new Layout(tiles);
	}

	// The in-tile labels: the name (ellipsized when the tile is a near-fit), with the share on a second line,
	// in an ink picked per tile — dropped entirely only when not even a stub fits; the tooltips carry the rest.
	private void paintLabels(Graphics2D g, ChartTheme theme, Layout layout) {
		Font caption = ChartStyle.caption();
		g.setFont(caption);
		LineMetrics lm = caption.getLineMetrics("Ag", FRC);
		double pad = ChartStyle.px(6);
		for (Tile tile : layout.tiles()) {
			Rectangle2D r = tile.rect();
			double available = r.getWidth() - 2 * pad;
			if (lm.getHeight() > r.getHeight() - 2 * pad) {
				continue;
			}
			String name = fit(data.categories()[tile.index()], caption, available);
			if (name == null) {
				continue;
			}
			Color ink = ChartTheme.readableOn(fillFor(theme, tile.index()));
			g.setColor(ink);
			float x = (float) (r.getX() + pad);
			float y = (float) (r.getY() + pad + lm.getAscent());
			g.drawString(name, x, y);
			String pct = ShareText.percent(data.values()[tile.index()] / data.total());
			boolean pctFits = 2 * lm.getHeight() <= r.getHeight() - 2 * pad
					&& caption.getStringBounds(pct, FRC).getWidth() <= available;
			if (pctFits) {
				g.setColor(ChartInk.alpha(ink, 0.72));
				g.drawString(pct, x, y + lm.getHeight());
			}
		}
	}

	// The text, whole when it fits, else ellipsized down to a minimum three-character stub; null when even
	// that would overflow the tile.
	private static String fit(String text, Font font, double available) {
		if (font.getStringBounds(text, FRC).getWidth() <= available) {
			return text;
		}
		for (int end = text.length() - 1; end >= 3; end--) {
			String candidate = text.substring(0, end) + "...";
			if (font.getStringBounds(candidate, FRC).getWidth() <= available) {
				return candidate;
			}
		}
		return null;
	}

	private static RoundRectangle2D rounded(Rectangle2D r, double radius) {
		return new RoundRectangle2D.Double(r.getX(), r.getY(), r.getWidth(), r.getHeight(), radius, radius);
	}

	private Color fillFor(ChartTheme theme, int index) {
		return "Other".equals(data.categories()[index]) ? ChartInk.otherGray(theme)
				: ChartInk.series(theme, index);
	}

	/** One drawn tile: its category index and its gap-inset rectangle. */
	private record Tile(int index, Rectangle2D rect) { }

	private record Layout(List<Tile> tiles) { }
}
