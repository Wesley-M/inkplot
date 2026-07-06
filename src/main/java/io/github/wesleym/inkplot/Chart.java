package io.github.wesleym.inkplot;

import io.github.wesleym.inkplot.data.ChartData;
import io.github.wesleym.inkplot.render.SvgGraphics2D;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * A chart being configured: the fluent options every chart shares — title, theme, legend seat, log scale,
 * figure notes — ending in {@link #component()} for a live Swing widget or {@link #image} for a rendered
 * frame. Obtained from the {@link Charts} factories; the type-specific builders ({@link BarChart},
 * {@link LineChart}) layer their options on top.
 */
public class Chart {

	private ChartData data;
	private ChartTheme theme = ChartTheme.PAPER;
	private String title;
	private String subtitle;
	private boolean legendBelow;
	private boolean logScale;
	private List<String> notes = List.of();

	Chart(ChartData data) {
		this.data = data;
	}

	/** Replaces the prepared data — used by the type-specific builders as series accumulate. */
	final void setData(ChartData data) {
		this.data = data;
	}

	final ChartData data() {
		return data;
	}

	/** The headline drawn into the chart surface (and so into every export). */
	public Chart title(String title) {
		this.title = title;
		return this;
	}

	/** A headline plus the muted context beside it, e.g. the rows covered. */
	public Chart title(String title, String subtitle) {
		this.title = title;
		this.subtitle = subtitle;
		return this;
	}

	/** The colour theme; defaults to {@link ChartTheme#PAPER}. */
	public Chart theme(ChartTheme theme) {
		this.theme = theme;
		return this;
	}

	/** Seats the legend under the plot instead of above it — the compact treatment for embedded charts. */
	public Chart legendBelow() {
		this.legendBelow = true;
		return this;
	}

	/** A logarithmic value axis, so small marks stay visible next to dominant ones (positive data only). */
	public Chart logScale() {
		this.logScale = true;
		return this;
	}

	/** Book-style figure notes: muted caption lines at the chart's bottom edge. */
	public Chart notes(String... notes) {
		this.notes = List.of(notes);
		return this;
	}

	/**
	 * The live Swing component: interactive hover tooltips, wheel zoom + pan, brush X-zoom on continuous
	 * axes, and a double-click reset. Size it like any other widget.
	 */
	public ChartCanvas component() {
		ChartCanvas canvas = new ChartCanvas(theme);
		if (legendBelow) {
			canvas.setLegendPlacement(ChartCanvas.LegendPlacement.BOTTOM);
		}
		canvas.setData(data);
		if (logScale && canvas.supportsLog()) {
			canvas.setYScale(ChartCanvas.YScale.LOG);
		}
		canvas.setTitle(title, subtitle);
		canvas.setNotes(notes);
		return canvas;
	}

	/** The chart rendered to an image at {@code width × height} — for files, reports, or tests. */
	public BufferedImage image(int width, int height) {
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		component().renderTo(g, width, height);
		g.dispose();
		return img;
	}

	/**
	 * The chart rendered to a standalone SVG document at {@code width × height} — true vector, crisp at any
	 * size. Text is outlined so the file needs no fonts to display identically anywhere. Pure JDK: no
	 * dependency.
	 */
	public String toSvg(int width, int height) {
		SvgGraphics2D svg = new SvgGraphics2D(width, height);
		component().renderTo(svg, width, height);
		return svg.document();
	}
}
