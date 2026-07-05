package io.github.wesleym.inkplot;

import io.github.wesleym.inkplot.render.ChartInk;

import javax.swing.JComponent;
import javax.swing.Timer;
import javax.swing.ToolTipManager;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * A slim, chart-styled proportion strip: shares of a whole in a few pixels of height, for the tight spots (an
 * insight card header, a stat row) where a full {@link ChartCanvas} has no room. Drawn in the charting design
 * language — a rounded pill, neighbouring segments split by the 2px surface gap, a hover lift with a per-segment
 * tooltip — and the fill eases in only when the strip is on screen, so an offscreen render or export never catches
 * a half-filled bar.
 */
public final class ProportionStrip extends JComponent {

	/** One share of the whole: its hover label, its 0..1 share, its row count ({@code -1} unknown), its fill. */
	public record Segment(String label, double share, long count, Color color) { }

	private static final int REMAINDER = -1;
	private static final int NOTHING = Integer.MIN_VALUE;

	private ChartTheme theme;
	private List<Segment> segments = List.of();
	private String remainderLabel;
	private long remainderCount = -1;
	private float progress = 1f;
	private int hovered = NOTHING;

	private final Timer animation = new Timer(16, e -> {
		progress = Math.min(1f, progress + 0.08f);
		if (progress >= 1f) {
			((Timer) e.getSource()).stop();
		}
		repaint();
	});

	public ProportionStrip(ChartTheme theme) {
		this.theme = theme;
		setOpaque(false);
		ToolTipManager.sharedInstance().registerComponent(this);   // per-segment tips via getToolTipText
		MouseAdapter mouse = new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				setHovered(at(e.getX()));
			}

			@Override
			public void mouseExited(MouseEvent e) {
				setHovered(NOTHING);
			}
		};
		addMouseMotionListener(mouse);
		addMouseListener(mouse);
	}

	/** Replaces the segments (shares of 1); anything short of the whole reads as the recessive remainder track. */
	public void setSegments(List<Segment> segments) {
		this.segments = List.copyOf(segments);
		if (isShowing()) {
			progress = 0f;
			animation.restart();
		}
		else {
			progress = 1f;
			animation.stop();
		}
		repaint();
	}

	/** Names the remainder track for its hover tooltip (e.g. "Null" with its row count), or null to leave it mute. */
	public void setRemainder(String label, long count) {
		this.remainderLabel = label;
		this.remainderCount = count;
	}

	public void restyle(ChartTheme theme) {
		this.theme = theme;
		repaint();
	}

	@Override
	public String getToolTipText(MouseEvent e) {
		int at = at(e.getX());
		if (at >= 0) {
			Segment s = segments.get(at);
			return tooltip(s.label(), s.count(), s.share());
		}
		if (at == REMAINDER && remainderLabel != null) {
			return tooltip(remainderLabel, remainderCount, remainderShare());
		}
		return null;
	}

	// "Filled · 42,371 · 88%" — the real count beside the share, when the caller knows it.
	private static String tooltip(String label, long count, double share) {
		String middle = count >= 0 ? " · " + ChartFormat.grouped(count) : "";
		return label + middle + " · " + ChartFormat.percent(share);
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(ChartStyle.px(120), ChartStyle.px(8));
	}

	private double remainderShare() {
		double sum = 0;
		for (Segment s : segments) {
			sum += s.share();
		}
		return Math.max(0, 1 - sum);
	}

	private void setHovered(int at) {
		if (at != hovered) {
			hovered = at;
			repaint();
		}
	}

	// What sits under x — a segment index, the remainder track, or nothing — from the widths the paint uses.
	private int at(int x) {
		double left = 0;
		for (int i = 0; i < segments.size(); i++) {
			double w = segments.get(i).share() * getWidth() * progress;
			if (x >= left && x < left + w) {
				return i;
			}
			left += w;
		}
		return x >= left && x < getWidth() ? REMAINDER : NOTHING;
	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		int w = getWidth();
		int h = getHeight();
		g2.clip(new RoundRectangle2D.Double(0, 0, w, h, h, h));
		Color track = theme.dark() ? new Color(0x3A, 0x42, 0x4F) : new Color(0xDD, 0xE1, 0xE9);
		g2.setColor(hovered == REMAINDER && remainderLabel != null ? ChartInk.lift(theme, track) : track);
		g2.fillRect(0, 0, w, h);

		double left = 0;
		for (int i = 0; i < segments.size(); i++) {
			double segW = segments.get(i).share() * w * progress;
			Color fill = segments.get(i).color();
			g2.setColor(i == hovered ? ChartInk.lift(theme, fill) : fill);
			g2.fill(new Rectangle2D.Double(left, 0, segW, h));
			left += segW;
		}

		// The 2px surface gap between visible neighbours (the remainder track counts as one) — the surface does
		// the separating, as in the stacked bars.
		g2.setColor(ChartInk.separator(theme));
		double gap = ChartStyle.px(2);
		double x = 0;
		for (int i = 0; i < segments.size(); i++) {
			double segW = segments.get(i).share() * w * progress;
			x += segW;
			double after = i < segments.size() - 1 ? segments.get(i + 1).share() * w * progress
					: remainderShare() * w;
			if (segW > 0 && after > 0) {
				g2.fill(new Rectangle2D.Double(x - gap / 2, 0, gap, h));
			}
		}
		g2.dispose();
	}
}
