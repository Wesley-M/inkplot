package io.github.wesleym.inkplot;

import io.github.wesleym.inkplot.data.Provenance;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/**
 * A quiet caption under a chart that states exactly what the chart does and doesn't cover — a truncated result, a
 * chart-side point cap, dropped non-numeric cells — so a partial picture is never presented as the whole. Hidden
 * entirely when the chart covers everything cleanly.
 */
public final class ChartNotice extends JLabel {

	private ChartTheme theme;

	public ChartNotice(ChartTheme theme) {
		this.theme = theme;
		setBorder(new EmptyBorder(ChartStyle.SPACE_XS, ChartStyle.SPACE_S, ChartStyle.SPACE_XS, ChartStyle.SPACE_S));
		setFont(ChartStyle.caption());
		setIconTextGap(ChartStyle.SPACE_XS);
		setVisible(false);
	}

	/** Hides the notice — nothing to say about coverage. */
	public void clear() {
		setVisible(false);
	}

	public void update(Provenance provenance) {
		if (provenance == null || !provenance.hasNotice()) {
			setVisible(false);
			return;
		}
		List<String> parts = new ArrayList<>();
		if (provenance.resultTruncated()) {
			parts.add("Charting the first " + ChartFormat.grouped(provenance.totalRows())
					+ " rows — a sample of a larger result");
		}
		else if (provenance.rowsCapped()) {
			parts.add("Showing " + ChartFormat.grouped(provenance.shownRows()) + " of "
					+ ChartFormat.grouped(provenance.totalRows()) + " rows");
		}
		if (provenance.capNote() != null) {
			parts.add(provenance.capNote());
		}
		if (provenance.skippedCells() > 0) {
			parts.add(ChartFormat.grouped(provenance.skippedCells()) + " non-numeric cell"
					+ (provenance.skippedCells() == 1 ? "" : "s") + " skipped");
		}
		setText(String.join("   ·   ", parts));
		restyle(theme);
		setVisible(true);
	}

	public void restyle(ChartTheme theme) {
		this.theme = theme;
		setForeground(theme.muted());
		setIcon(dot(theme.muted(), ChartStyle.px(5)));
	}

	// A small filled dot, sized with the style scale — the notice's only glyph.
	private static Icon dot(Color color, int size) {
		return new Icon() {
			@Override
			public void paintIcon(Component c, Graphics g, int x, int y) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(color);
				int d = Math.max(4, Math.round(size * 0.6f));
				int o = (size - d) / 2;
				g2.fillOval(x + o, y + o, d, d);
				g2.dispose();
			}

			@Override
			public int getIconWidth() {
				return size;
			}

			@Override
			public int getIconHeight() {
				return size;
			}
		};
	}
}