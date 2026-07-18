package io.github.wesleym.inkplot.render;

import io.github.wesleym.inkplot.scale.NiceTicks;
import io.github.wesleym.inkplot.scale.Scale;
import io.github.wesleym.inkplot.ChartStyle;
import io.github.wesleym.inkplot.ChartTheme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

/**
 * Draws the two axes of a plot — one X, one Y, never a second scale. Horizontal gridlines and their value labels sit
 * behind the marks in recessive hairline ink; X labels ride below the plot, elided or gently rotated when categories
 * crowd, and dropped rather than overlapped when continuous ticks collide. All metrics route through
 * {@link ChartStyle#px} so the axes scale with the app zoom.
 */
public final class AxisRenderer {

	private AxisRenderer() { }

	private static final int LABEL_GAP = 6;   // gap between a tick and its label

	/** Horizontal gridlines at each Y tick with right-aligned labels — for a linear axis (labels from its ticks). */
	public static void gridlinesY(Graphics2D g, ChartTheme theme, Rectangle plot, Scale.Linear yScale,
			NiceTicks.Result ticks) {
		gridlinesY(g, theme, plot, yScale, ticks.values(), labelsFor(ticks));
	}

	/**
	 * Horizontal gridlines across the plot at each Y {@code value}, with right-aligned labels in the left gutter —
	 * works for any continuous Y scale (linear or log), the caller supplying the pre-formatted labels.
	 */
	public static void gridlinesY(Graphics2D g, ChartTheme theme, Rectangle plot, Scale yScale, double[] values,
			String[] labels) {
		g.setFont(ChartStyle.caption());
		FontMetrics fm = g.getFontMetrics();
		int gap = ChartStyle.px(LABEL_GAP);
		g.setStroke(hairline());
		for (int i = 0; i < values.length; i++) {
			int y = (int) Math.round(yScale.map(values[i]));
			if (y < plot.y - 1 || y > plot.y + plot.height + 1) {
				continue;
			}
			g.setColor(values[i] == 0 ? zeroLine(theme) : ChartInk.grid(theme));
			g.drawLine(plot.x, y, plot.x + plot.width, y);
			g.setColor(ChartInk.axisText(theme));
			int lx = plot.x - gap - fm.stringWidth(labels[i]);
			int ly = y + fm.getAscent() / 2 - 1;
			g.drawString(labels[i], lx, ly);
		}
	}

	/** Pre-formats the labels for a linear tick set at the precision the spacing implies. */
	public static String[] labelsFor(NiceTicks.Result ticks) {
		String[] labels = new String[ticks.values().length];
		for (int i = 0; i < labels.length; i++) {
			labels[i] = NiceTicks.label(ticks.values()[i], ticks.fractionDigits());
		}
		return labels;
	}

	/** Faint vertical gridlines and centred labels for a continuous X axis; colliding labels are dropped, not stacked. */
	public static void axisXContinuous(Graphics2D g, ChartTheme theme, Rectangle plot, double[] tickPixels,
			String[] labels) {
		Font font = ChartStyle.caption();
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();
		int baseline = plot.y + plot.height + ChartStyle.px(LABEL_GAP) + fm.getAscent();
		g.setStroke(hairline());
		int lastRight = Integer.MIN_VALUE;
		int minGap = ChartStyle.px(LABEL_GAP);
		for (int i = 0; i < tickPixels.length; i++) {
			int x = (int) Math.round(tickPixels[i]);
			if (x < plot.x - 1 || x > plot.x + plot.width + 1) {
				continue;
			}
			g.setColor(ChartInk.grid(theme));
			g.drawLine(x, plot.y, x, plot.y + plot.height);
			int half = fm.stringWidth(labels[i]) / 2;
			if (x - half <= lastRight + minGap) {
				continue;   // would touch the previous label — let the gridline carry this tick
			}
			g.setColor(ChartInk.axisText(theme));
			g.drawString(labels[i], x - half, baseline);
			lastRight = x + half;
		}
	}

	/** Category labels right-aligned in the left gutter, one per band — the Y axis of a horizontal bar chart. */
	public static void axisYBand(Graphics2D g, ChartTheme theme, Rectangle plot, Scale.Band band, String[] labels,
			int gutterWidth) {
		Font font = ChartStyle.caption();
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();
		g.setColor(ChartInk.axisText(theme));
		int gap = ChartStyle.px(LABEL_GAP);
		for (int i = 0; i < labels.length; i++) {
			String elided = elide(fm, labels[i], gutterWidth);
			int y = (int) Math.round(band.center(i)) + fm.getAscent() / 2 - 1;
			g.drawString(elided, plot.x - gap - fm.stringWidth(elided), y);
		}
	}

	/** Category labels centred under each band; elided to fit, then rotated when even that would overlap. */
	public static void axisXBand(Graphics2D g, ChartTheme theme, Rectangle plot, Scale.Band band, String[] labels) {
		Font font = ChartStyle.caption();
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();
		g.setColor(ChartInk.axisText(theme));
		double slot = band.slot();
		boolean rotate = false;
		for (String label : labels) {
			if (fm.stringWidth(label) > slot - ChartStyle.px(LABEL_GAP)) {
				rotate = true;
				break;
			}
		}
		// When the labels rotate they can still pile up in a narrow plot; draw an evenly-spaced subset so the axis
		// reads cleanly and let hover reveal every bar (the chart is interactive). Non-rotated labels already fit
		// their slot, so nothing is dropped there. First bar is value-largest, so a stride from 0 keeps it labelled.
		int stride = 1;
		if (rotate) {
			int minSpacing = ChartStyle.px(22);
			int fit = Math.max(1, plot.width / minSpacing);
			stride = Math.max(1, (int) Math.ceil(labels.length / (double) fit));
		}
		int top = plot.y + plot.height + ChartStyle.px(LABEL_GAP);
		for (int i = 0; i < labels.length; i++) {
			if (i % stride != 0) {
				continue;
			}
			int cx = (int) Math.round(band.center(i));
			if (rotate) {
				String elided = elide(fm, labels[i], plot.height / 2 + ChartStyle.px(24));
				AffineTransform saved = g.getTransform();
				g.translate(cx, top + fm.getAscent());
				g.rotate(Math.toRadians(-35));
				g.drawString(elided, -fm.stringWidth(elided), 0);
				g.setTransform(saved);
			}
			else {
				String elided = elide(fm, labels[i], (int) slot - ChartStyle.px(LABEL_GAP));
				g.drawString(elided, cx - fm.stringWidth(elided) / 2, top + fm.getAscent());
			}
		}
	}

	private static String elide(FontMetrics fm, String text, int maxWidth) {
		if (fm.stringWidth(text) <= maxWidth || text.length() <= 1) {
			return text;
		}
		String ellipsis = "…";
		int end = text.length();
		while (end > 1 && fm.stringWidth(text.substring(0, end) + ellipsis) > maxWidth) {
			end--;
		}
		return text.substring(0, end) + ellipsis;
	}

	private static BasicStroke hairline() {
		return new BasicStroke(Math.max(1, ChartStyle.px(1)));
	}

	// The zero baseline reads a touch stronger than an ordinary gridline, so above/below is legible at a glance.
	private static Color zeroLine(ChartTheme theme) {
		return ChartInk.mix(theme.hairline(), theme.muted(), 0.5);
	}
}
