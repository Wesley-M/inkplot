package io.github.wesleym.inkplot;

import io.github.wesleym.inkplot.data.ChartData;
import io.github.wesleym.inkplot.render.AxisRenderer;
import io.github.wesleym.inkplot.render.ChartHoverState;
import io.github.wesleym.inkplot.render.ChartInk;
import io.github.wesleym.inkplot.render.ChartTooltipPainter;
import io.github.wesleym.inkplot.render.CrosshairReadout;
import io.github.wesleym.inkplot.render.LegendEntry;
import io.github.wesleym.inkplot.render.MarkHit;
import io.github.wesleym.inkplot.render.MarkRenderer;
import io.github.wesleym.inkplot.render.PlotContext;
import io.github.wesleym.inkplot.render.Renderers;
import io.github.wesleym.inkplot.render.SeriesEmphasis;
import io.github.wesleym.inkplot.render.XAxisModel;
import io.github.wesleym.inkplot.render.YAxisModel;
import io.github.wesleym.inkplot.scale.LogTicks;
import io.github.wesleym.inkplot.scale.NiceTicks;
import io.github.wesleym.inkplot.scale.Scale;

import javax.swing.JComponent;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The embeddable chart surface: given a prepared {@link ChartData} it lays out one plot — margins measured from the
 * real label widths, one X and one Y axis, recessive gridlines behind the marks, a legend for multi-series data — and
 * paints it. It is deliberately decoupled from the query-tab shell (it depends only on the theme and the chart data),
 * so it drops into a dashboard tile or a full-screen frame unchanged.
 *
 * <p>The static chart is rendered once into a cached image and redrawn from it on a bare repaint; only a size, data,
 * theme, or scale change rebuilds it, and the hover overlay paints on top without touching the cache. The same
 * {@link #renderTo} path backs PNG export.
 */
public final class ChartCanvas extends JComponent {

	/** Whether the value axis is linear or logarithmic (log offered only for strictly-positive data). */
	public enum YScale { LINEAR, LOG }

	/**
	 * Where the legend block sits. {@code BOTTOM} also asks slice-labelled charts (the doughnut's callouts)
	 * to emit legend entries instead — the compact, cohesive treatment for embedded charts.
	 */
	public enum LegendPlacement { TOP, BOTTOM }

	private static final double BAND_INNER_PAD = 0.34;
	private static final int OUTER_PAD = ChartStyle.SPACE_L;
	private static final int AXIS_GAP = 6;

	private ChartTheme theme;
	private MarkRenderer renderer;
	private YScale yScaleMode = YScale.LINEAR;
	private LegendPlacement legendPlacement = LegendPlacement.TOP;
	private List<String> notes = List.of();
	private String title;      // optional headline drawn into the surface (and so into every export)
	private String subtitle;   // muted context beside it, e.g. the rows covered

	private ChartHoverState hover = ChartHoverState.NONE;
	private PlotContext lastContext;

	private double[] xZoom;         // view-only X-domain override from a brush, or null for the data's full range
	private int brushStart = -1;    // drag anchor in pixels while a brush is being drawn (-1 = not brushing)
	private int brushEnd;

	// Screen-only viewport camera: a zoom + pan applied at paint time (never in renderTo), so PNG exports
	// stay at 1:1 fit. Pointer coordinates are inverse-transformed back into base-image space before
	// hover/brush/hit-testing. While a wheel/pan gesture is live, paint magnifies the cached bitmap (cheap
	// enough to track the pointer); once the gesture settles, the view re-bakes as vectors under the camera
	// transform, so a zoomed chart is as crisp as a fit one — the diagram canvas's settle-and-rebake pattern.
	private static final double VIEW_ZOOM_STEP = 1.15;
	private static final int VIEW_SETTLE_MS = 150;
	private final ChartViewport viewport = new ChartViewport();
	private final javax.swing.Timer viewSettle;
	private boolean gestureActive;
	private int panAnchorX = -1;   // pan-drag anchor in screen px while panning (-1 = not panning)
	private int panAnchorY;

	private BufferedImage baseLayer;
	private boolean baseDirty = true;

	// Entry animation: on first appearance the marks reveal (type-appropriate) over a short beat, re-baking
	// the base image each frame. It plays once per canvas, never on later data updates (so a streaming feed
	// doesn't re-animate), and never in exports — renderTo/image/toSvg always draw the full chart.
	private static final int INTRO_MS = 560;
	private final javax.swing.Timer introTimer;
	private double introT;            // 0..1 linear progress
	private double introReveal = 1.0; // eased reveal the base bake uses (1.0 = full)
	private boolean introPending;
	private boolean introPlayed;

	// Interactive legend, multi-series charts only: click an entry to toggle its series off/on, hover to focus
	// it and dim the rest. State lives here and feeds the renderer through the plot context; exports never see
	// it. The hit rects are captured (in base coordinates) each time the base image is baked.
	private boolean[] hidden = new boolean[0];   // toggle targets
	private int focusSeries = -1;                // hover-focus target
	private double[] seriesAlpha = new double[0];   // current per-series draw alpha, eased toward the targets
	private final javax.swing.Timer legendTimer;
	private static final double DIM_FOCUS = 0.22;   // a non-focused series eases down to this while another is focused
	private final List<Rectangle> legendHitRects = new ArrayList<>();
	private final List<Integer> legendHitSeries = new ArrayList<>();
	private boolean capturingLegendHits;
	private BufferedImage viewLayer;   // the settled camera view, baked crisp at device resolution
	private boolean viewDirty = true;

	public ChartCanvas(ChartTheme theme) {
		this.theme = theme;
		setOpaque(true);
		viewSettle = new javax.swing.Timer(VIEW_SETTLE_MS, e -> settleView());
		viewSettle.setRepeats(false);
		introTimer = new javax.swing.Timer(16, e -> stepIntro());
		legendTimer = new javax.swing.Timer(16, e -> stepLegend());
		MouseAdapter tracker = new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				Point p = toBase(e.getPoint());
				int s = legendSeriesAt(p);
				if (s >= 0) {
					setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
					setFocus(s);
					setHover(ChartHoverState.NONE);   // no data tooltip while pointing at the legend
					return;
				}
				setCursor(java.awt.Cursor.getDefaultCursor());
				setFocus(-1);
				setHover(ChartHoverState.at(p.x, p.y));
			}

			@Override
			public void mouseExited(MouseEvent e) {
				setFocus(-1);
				setHover(ChartHoverState.NONE);
			}

			@Override
			public void mousePressed(MouseEvent e) {
				if (!javax.swing.SwingUtilities.isLeftMouseButton(e)) {
					return;
				}
				int series = legendSeriesAt(toBase(e.getPoint()));
				if (series >= 0) {
					toggleSeries(series);   // click a legend entry to toggle its series
					return;
				}
				// Once zoomed in, left-drag pans; at fit it starts the X-brush (data zoom) on brushable charts.
				if (viewport.scale() > 1.0) {
					panAnchorX = e.getX();
					panAnchorY = e.getY();
					return;
				}
				Point p = toBase(e.getPoint());
				if (brushable() && lastContext != null && lastContext.plot().contains(p)) {
					brushStart = p.x;
					brushEnd = p.x;
				}
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				if (panAnchorX >= 0) {
					viewport.panBy(e.getX() - panAnchorX, e.getY() - panAnchorY, getWidth(), getHeight());
					panAnchorX = e.getX();
					panAnchorY = e.getY();
					gesture();
				}
				else if (brushStart >= 0) {
					Rectangle plot = lastContext.plot();
					brushEnd = Math.max(plot.x, Math.min(plot.x + plot.width, toBase(e.getPoint()).x));
					repaint();
				}
				else {
					Point p = toBase(e.getPoint());
					setHover(ChartHoverState.at(p.x, p.y));
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (panAnchorX >= 0) {
					panAnchorX = -1;
					return;
				}
				if (brushStart < 0) {
					return;
				}
				int a = Math.min(brushStart, brushEnd);
				int b = Math.max(brushStart, brushEnd);
				brushStart = -1;
				// A sub-threshold drag is a click, not a zoom — never trap the reader in an accidental sliver.
				if (b - a >= ChartStyle.px(8) && lastContext != null) {
					setXZoom(invertX(lastContext.xScale(), a), invertX(lastContext.xScale(), b));
				}
				else {
					repaint();
				}
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					fitView();       // reset the viewport magnification…
					clearXZoom();    // …and the brush data-zoom, so a double-click always returns to the full view
				}
			}
		};
		addMouseMotionListener(tracker);
		addMouseListener(tracker);
		addMouseWheelListener(e -> {
			double rotation = e.getPreciseWheelRotation();
			if (rotation != 0) {
				zoomAt(e.getX(), e.getY(), Math.pow(VIEW_ZOOM_STEP, -rotation));
			}
		});
	}

	// A screen point mapped back into base-image (unzoomed) coordinates. All hover/brush/hit-testing runs in
	// base-image space (renderTo draws there), so pointer input must land there too.
	private Point toBase(Point screen) {
		return viewport.toBase(screen);
	}

	// A wheel/drag gesture step: show it through the cheap bitmap blit now, and re-bake crisp vectors once
	// the stream goes quiet.
	private void gesture() {
		gestureActive = true;
		viewDirty = true;
		viewSettle.restart();
		repaint();
	}

	private void settleView() {
		gestureActive = false;
		repaint();
	}

	/** Zooms the viewport by {@code factor} about a screen pivot, keeping the point under the cursor fixed. */
	private void zoomAt(int pivotX, int pivotY, double factor) {
		if (viewport.zoomAt(pivotX, pivotY, factor, getWidth(), getHeight())) {
			gesture();
		}
	}

	/**
	 * Zoom in / out about the canvas centre — the pill buttons. Discrete clicks re-bake crisp immediately;
	 * only the high-frequency wheel/pan streams go through the gesture blit.
	 */
	public void zoomIn() {
		zoomStep(VIEW_ZOOM_STEP);
	}

	public void zoomOut() {
		zoomStep(1 / VIEW_ZOOM_STEP);
	}

	private void zoomStep(double factor) {
		if (viewport.zoomAt(getWidth() / 2, getHeight() / 2, factor, getWidth(), getHeight())) {
			viewDirty = true;
			repaint();
		}
	}

	/** Resets the viewport camera to fit (1:1). Does not touch the brush X-zoom. */
	public void fitView() {
		if (!viewport.atFit()) {
			viewport.fit();
			gestureActive = false;
			viewSettle.stop();
			viewLayer = null;   // fit blits the base layer; don't hold a stale camera bake
			repaint();
		}
	}

	/** True when the chart is magnified away from fit in either direction. */
	public boolean isZoomed() {
		return viewport.isZoomed();
	}

	/** Sets the data to draw (null clears it), rebuilding the renderer and invalidating the cached image. */
	public void setData(ChartData data) {
		this.renderer = data == null ? null : Renderers.of(data);
		if (this.renderer != null) {
			this.renderer.setPreferLegend(legendPlacement == LegendPlacement.BOTTOM);
		}
		this.xZoom = null;   // a new dataset means a new domain — never inherit the old brush
		this.hidden = new boolean[renderer == null ? 0 : renderer.legend(theme).size()];   // reset toggles
		this.focusSeries = -1;
		this.seriesAlpha = new double[hidden.length];
		java.util.Arrays.fill(seriesAlpha, 1.0);
		legendTimer.stop();
		fitView();           // …nor the old viewport magnification
		if (renderer != null && !introPlayed) {
			introPending = true;   // play the entry animation once, on first appearance
		}
		invalidateBase();
	}

	/**
	 * Book-style figure notes: centred caption lines seated at the chart's bottom edge (under a bottom
	 * legend), part of the rendered surface — so they travel with the chart into PNG exports too.
	 */
	public void setNotes(List<String> notes) {
		this.notes = notes == null ? List.of() : List.copyOf(notes);
		invalidateBase();
		repaint();
	}

	/** Seats the legend at the top (default) or as a block under the plot; see {@link LegendPlacement}. */
	public void setLegendPlacement(LegendPlacement placement) {
		if (this.legendPlacement != placement) {
			this.legendPlacement = placement;
			if (renderer != null) {
				renderer.setPreferLegend(placement == LegendPlacement.BOTTOM);
			}
			invalidateBase();
			repaint();
		}
	}

	/**
	 * Zooms the X axis to {@code [min, max]} (data units; epoch-millis on a time axis) — a view-only override the
	 * brush drives, applying to on-screen rendering and exports alike. Double-click (or {@link #clearXZoom}) resets.
	 */
	public void setXZoom(double min, double max) {
		this.xZoom = new double[] { Math.min(min, max), Math.max(min, max) };
		invalidateBase();
	}

	public void clearXZoom() {
		if (xZoom != null) {
			xZoom = null;
			invalidateBase();
		}
	}

	/** Whether the current chart admits a brush zoom: a continuous or time X, never a band or an axis-free chart. */
	public boolean brushable() {
		return renderer != null && !renderer.axisFree() && !(renderer.xModel() instanceof XAxisModel.Band);
	}

	private static double invertX(Scale scale, int px) {
		java.util.Objects.requireNonNull(scale, "scale");
		if (scale instanceof Scale.Linear linear) {
			return linear.invert(px);
		}
		if (scale instanceof Scale.Log log) {
			return log.invert(px);
		}
		if (scale instanceof Scale.Time time) {
			return time.invert(px);
		}
		if (scale instanceof Scale.Band) {
			return Double.NaN;   // band axes are never brushable
		}
		throw new AssertionError("Unknown scale: " + scale.getClass().getName());
	}

	/**
	 * Sets the headline drawn inside the chart surface — "Sum of amount by region" over a muted rows subtitle —
	 * so on-screen charts and PNG/clipboard exports alike say what they show. Null clears it (embedded
	 * thumbnails, e.g. the insight cards, stay chrome-free).
	 */
	public void setTitle(String title, String subtitle) {
		this.title = title;
		this.subtitle = subtitle;
		invalidateBase();
	}

	/** Whether the canvas currently holds prepared data (false while empty or still being prepared). */
	public boolean hasData() {
		return renderer != null;
	}

	/** Whether the current chart carries a legend — for hosts that show legend controls conditionally. */
	public boolean hasLegend() {
		return renderer != null && !renderer.legend(theme).isEmpty();
	}

	/** The renderer currently driving the canvas, or null when empty — the hover layer reads it. */
	MarkRenderer renderer() {
		return renderer;
	}

	/** The last laid-out plot context (scales + plot rect), for the hover layer's hit-testing. */
	PlotContext plotContext() {
		return lastContext;
	}

	public void setYScale(YScale mode) {
		if (this.yScaleMode != mode) {
			this.yScaleMode = mode;
			invalidateBase();
		}
	}

	public YScale yScale() {
		return yScaleMode;
	}

	/** Whether the current data admits a log value axis (all-non-negative with some positive value). */
	public boolean supportsLog() {
		return renderer != null && renderer.yModel().allowLog();
	}

	/** Adopts the pointer state for the hover overlay; a bare repaint (no cache rebuild) reflects it. */
	void setHover(ChartHoverState state) {
		this.hover = state == null ? ChartHoverState.NONE : state;
		repaint();
	}

	ChartHoverState hover() {
		return hover;
	}

	public void restyle(ChartTheme theme) {
		this.theme = theme;
		invalidateBase();
	}

	private void invalidateBase() {
		baseDirty = true;
		viewDirty = true;
		repaint();
	}

	private SeriesEmphasis emphasis() {
		for (double a : seriesAlpha) {
			if (a < 0.999) {
				return new SeriesEmphasis(seriesAlpha);
			}
		}
		return SeriesEmphasis.NONE;   // every series fully shown — the common case and every export
	}

	// The alpha a series should settle at: 0 when toggled off, dimmed when another is focused, else full.
	private double targetAlpha(int s) {
		if (s < hidden.length && hidden[s]) {
			return 0;
		}
		return focusSeries < 0 || s == focusSeries ? 1.0 : DIM_FOCUS;
	}

	// One frame of the legend transition: ease each series' alpha toward its target and re-bake, until settled.
	private void stepLegend() {
		boolean settled = true;
		for (int s = 0; s < seriesAlpha.length; s++) {
			double target = targetAlpha(s);
			double d = target - seriesAlpha[s];
			if (Math.abs(d) > 0.004) {
				seriesAlpha[s] += d * 0.28;
				settled = false;
			}
			else {
				seriesAlpha[s] = target;
			}
		}
		invalidateBase();
		if (settled) {
			legendTimer.stop();
		}
	}

	// The series index of the legend entry under a base-space point, or -1.
	private int legendSeriesAt(Point base) {
		for (int i = 0; i < legendHitRects.size(); i++) {
			if (legendHitRects.get(i).contains(base)) {
				return legendHitSeries.get(i);
			}
		}
		return -1;
	}

	private void toggleSeries(int s) {
		if (s < 0 || s >= hidden.length) {
			return;
		}
		if (!hidden[s]) {   // never hide the last visible series
			int visible = 0;
			for (boolean b : hidden) {
				if (!b) {
					visible++;
				}
			}
			if (visible <= 1) {
				return;
			}
		}
		hidden[s] = !hidden[s];
		startLegend();
	}

	private void setFocus(int s) {
		if (focusSeries != s) {
			focusSeries = s;
			startLegend();
		}
	}

	// Animate the series alphas toward their new targets (idempotent — retargets a running transition).
	private void startLegend() {
		if (seriesAlpha.length > 0 && !legendTimer.isRunning()) {
			legendTimer.restart();
		}
	}

	// One frame of the entry animation: advance the eased reveal and re-bake the base at it. Ends at full.
	private void stepIntro() {
		introT = Math.min(1.0, introT + 16.0 / INTRO_MS);
		double t = introT;
		introReveal = 1 - (1 - t) * (1 - t) * (1 - t);   // easeOutCubic — quick then gentle
		baseDirty = true;
		repaint();
		if (introT >= 1.0) {
			introReveal = 1.0;
			introPlayed = true;
			introTimer.stop();
		}
	}

	@Override
	public void removeNotify() {
		introTimer.stop();
		legendTimer.stop();
		super.removeNotify();
	}

	@Override
	public Dimension getPreferredSize() {
		// Honour an explicit setPreferredSize (e.g. a small chart embedded in a host panel); otherwise a
		// comfortable default for a standalone chart.
		return isPreferredSizeSet() ? super.getPreferredSize() : new Dimension(ChartStyle.px(480), ChartStyle.px(300));
	}

	@Override
	protected void paintComponent(Graphics g) {
		int w = getWidth();
		int h = getHeight();
		if (w <= 0 || h <= 0) {
			return;
		}
		// The entry animation is on-screen-only: it plays on the first paint of a canvas the reader can actually
		// see. An offscreen or not-yet-realized paint (headless render, a bake before the component is shown)
		// draws the settled chart at full reveal, exactly like the export path — no reader is there to watch it grow.
		if (introPending && renderer != null && isShowing()) {   // first on-screen paint with data — kick off the entry animation
			introPending = false;
			introT = 0;
			introReveal = 0;
			baseDirty = true;
			introTimer.restart();
		}
		// The base image is baked at DEVICE resolution (like the settled camera view), not logical, so the fit
		// blit below lands on real pixels — otherwise on a HiDPI display the first (unzoomed) paint upscales a
		// logical-size bitmap and reads soft until a zoom re-bakes it crisp. paintChart still draws in logical
		// coordinates (the buffer's Graphics is pre-scaled), so hit rects and hover geometry stay logical.
		double dpr = deviceScale((Graphics2D) g);
		int bpw = Math.max(1, (int) Math.round(w * dpr));
		int bph = Math.max(1, (int) Math.round(h * dpr));
		if (baseDirty || baseLayer == null || baseLayer.getWidth() != bpw || baseLayer.getHeight() != bph) {
			baseLayer = new BufferedImage(bpw, bph, BufferedImage.TYPE_INT_ARGB);
			Graphics2D bg = baseLayer.createGraphics();
			applyHints(bg);
			bg.scale(dpr, dpr);
			capturingLegendHits = true;          // capture legend hit rects from this on-screen bake only
			paintChart(bg, w, h, introReveal);   // reveal < 1 only mid-entry-animation
			capturingLegendHits = false;
			bg.dispose();
			baseDirty = false;
			viewDirty = true;
		}
		// The camera never touches renderTo, so the cached base image — and every PNG/clipboard export —
		// stays at 1:1 fit. At fit the base blits straight through; mid-gesture it blits magnified (smoothed,
		// but a bitmap); settled it swaps for the crisp vector bake of the camera view.
		viewport.clamp(w, h);
		Graphics2D g2 = (Graphics2D) g.create();
		if (viewport.atFit()) {
			g2.drawImage(baseLayer, 0, 0, w, h, null);   // device-res buffer down to logical size — crisp on HiDPI
		}
		else if (gestureActive) {
			g2.setColor(theme.surface());
			g2.fillRect(0, 0, w, h);   // a below-fit blit leaves margins around the shrunk chart
			Graphics2D blit = (Graphics2D) g2.create();
			blit.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			blit.translate(viewport.offsetX(), viewport.offsetY());
			blit.scale(viewport.scale(), viewport.scale());
			blit.drawImage(baseLayer, 0, 0, w, h, null);
			blit.dispose();
		}
		else {
			ensureViewLayer(w, h, deviceScale(g2));
			g2.drawImage(viewLayer, 0, 0, w, h, null);
		}
		// The overlays draw in base coordinates through the camera transform, in every branch.
		if (!viewport.atFit()) {
			g2.translate(viewport.offsetX(), viewport.offsetY());
			g2.scale(viewport.scale(), viewport.scale());
		}
		paintBrushOverlay(g2);
		if (brushStart < 0) {
			paintHoverOverlay(g2);
		}
		g2.dispose();
	}

	// The display's device-pixel ratio (>1 under HiDPI scaling), so the settled bake lands on real pixels.
	private static double deviceScale(Graphics2D g) {
		return Math.max(1.0, g.getTransform().getScaleX());
	}

	// Re-renders the camera view as vectors into a device-resolution buffer — the crisp image a settled zoom
	// blits on every subsequent repaint (hover moves must not re-render the whole chart).
	private void ensureViewLayer(int w, int h, double dpr) {
		int pw = Math.max(1, (int) Math.round(w * dpr));
		int ph = Math.max(1, (int) Math.round(h * dpr));
		if (!viewDirty && viewLayer != null && viewLayer.getWidth() == pw && viewLayer.getHeight() == ph) {
			return;
		}
		viewLayer = new BufferedImage(pw, ph, BufferedImage.TYPE_INT_ARGB);
		Graphics2D vg = viewLayer.createGraphics();
		applyHints(vg);
		vg.scale(dpr, dpr);
		vg.setColor(theme.surface());
		vg.fillRect(0, 0, w, h);   // below fit, the chart covers only the centred part of the viewport
		vg.translate(viewport.offsetX(), viewport.offsetY());
		vg.scale(viewport.scale(), viewport.scale());
		renderTo(vg, w, h);
		vg.dispose();
		viewDirty = false;
	}

	// The in-progress brush band and, once zoomed, a quiet reset hint — screen chrome only, never in an export
	// (exports go through renderTo, which draws neither).
	private void paintBrushOverlay(Graphics2D g) {
		if (lastContext == null) {
			return;
		}
		Rectangle plot = lastContext.plot();
		Graphics2D g2 = (Graphics2D) g.create();
		applyHints(g2);
		if (brushStart >= 0) {
			int x0 = Math.min(brushStart, brushEnd);
			int x1 = Math.max(brushStart, brushEnd);
			g2.setColor(ChartInk.alpha(theme.accent(), 0.12));
			g2.fillRect(x0, plot.y, x1 - x0, plot.height);
			g2.setColor(ChartInk.alpha(theme.accent(), 0.55));
			g2.drawLine(x0, plot.y, x0, plot.y + plot.height);
			g2.drawLine(x1, plot.y, x1, plot.y + plot.height);
		}
		else if (xZoom != null) {
			g2.setFont(ChartStyle.caption());
			FontMetrics fm = g2.getFontMetrics();
			String hint = "zoomed — double-click to reset";
			g2.setColor(theme.muted());
			g2.drawString(hint, plot.x + plot.width - fm.stringWidth(hint) - ChartStyle.px(4),
					plot.y + fm.getAscent() + ChartStyle.px(2));
		}
		g2.dispose();
	}

	// Draws the hover feedback on top of the cached static chart, so a pointer move never rebuilds the base image:
	// a snapping crosshair with an all-series read-out on line/density charts, or the lifted mark plus its tooltip on
	// bar/scatter/box/histogram charts.
	private void paintHoverOverlay(Graphics2D g) {
		if (!hover.active() || renderer == null || lastContext == null) {
			return;
		}
		Rectangle plot = lastContext.plot();
		if (!plot.contains(hover.x(), hover.y())) {
			return;
		}
		Graphics2D g2 = (Graphics2D) g.create();
		applyHints(g2);
		Rectangle bounds = new Rectangle(0, 0, getWidth(), getHeight());
		if (renderer.usesCrosshair()) {
			Optional<CrosshairReadout> readout = renderer.crosshairAt(lastContext, hover.x());
			if (readout.isPresent()) {
				int sx = (int) Math.round(readout.get().snapX());
				g2.setColor(ChartInk.alpha(theme.muted(), 0.55));
				g2.drawLine(sx, plot.y, sx, plot.y + plot.height);
				ChartTooltipPainter.paint(g2, bounds, sx, hover.y(), readout.get().tooltip(), theme);
			}
		}
		else {
			Optional<MarkHit> hit = renderer.hitTest(lastContext, new Point(hover.x(), hover.y()));
			if (hit.isPresent()) {
				renderer.highlight(g2, lastContext, hit.get());
				ChartTooltipPainter.paint(g2, bounds, hover.x(), hover.y(),
						renderer.tooltip(lastContext, hit.get()), theme);
			}
		}
		g2.dispose();
	}

	/**
	 * Renders the whole chart into {@code g} at size {@code w×h} — the single drawing path shared by the on-screen
	 * cache and PNG export. Fills the surface, measures margins from the label metrics, builds the scales, then draws
	 * gridlines, marks, axes, and the legend.
	 */
	public void renderTo(Graphics2D g, int w, int h) {
		paintChart(g, w, h, 1.0);   // exports and the settled view always draw the full chart
	}

	// Renders one entry-animation frame at {@code reveal} — for tests of the reveal geometry (package-private,
	// never public API).
	void renderRevealFrame(Graphics2D g, int w, int h, double reveal) {
		paintChart(g, w, h, reveal);
	}

	// Legend-interaction hooks for tests (package-private, never public API). They settle the transition
	// immediately, since no EDT timer runs the animation in a headless test.
	void toggleSeriesForTest(int s) {
		toggleSeries(s);
		settleLegend();
	}

	void focusSeriesForTest(int s) {
		focusSeries = s;
		settleLegend();
	}

	boolean hasInteractiveLegendForTest() {
		return renderer != null && renderer.interactiveSeries();
	}

	int baseLayerWidthForTest() {
		return baseLayer == null ? 0 : baseLayer.getWidth();
	}

	private void settleLegend() {
		for (int i = 0; i < seriesAlpha.length; i++) {
			seriesAlpha[i] = targetAlpha(i);
		}
	}

	// The chart at a given entry-reveal fraction (1.0 = full). Only the live base bake passes < 1 mid-animation;
	// axes, gridlines, labels and the legend always draw fully — just the marks reveal.
	private void paintChart(Graphics2D g, int w, int h, double reveal) {
		g.setColor(theme.surface());
		g.fillRect(0, 0, w, h);
		if (renderer == null) {
			return;
		}

		int outer = ChartStyle.px(OUTER_PAD);
		int gap = ChartStyle.px(AXIS_GAP);
		int titleH = drawTitle(g, w, outer);

		Font caption = ChartStyle.caption();
		g.setFont(caption);
		FontMetrics fm = g.getFontMetrics();

		List<LegendEntry> legend = renderer.legend(theme);
		int legendAvail = w - 2 * outer;
		int legendRows = legend.isEmpty() ? 0 : legendRows(fm, legend, legendAvail);
		// The legend may claim at most a third of the surface: past that, entries fold into a "+ N more"
		// tail instead of squeezing the plot below readability or clipping off the edge.
		int maxLegendRows = Math.max(1, h / 3 / (fm.getHeight() + ChartStyle.px(2)));
		if (legendRows > maxLegendRows) {
			legend = capLegend(fm, legend, legendAvail, maxLegendRows);
			legendRows = legendRows(fm, legend, legendAvail);
		}
		int legendH = legendRows == 0 ? 0
				: legendRows * fm.getHeight() + (legendRows - 1) * ChartStyle.px(2) + ChartStyle.px(ChartStyle.SPACE_S);
		boolean legendBelow = legendPlacement == LegendPlacement.BOTTOM;
		int topMargin = outer + titleH + (legendBelow ? 0 : legendH);
		// The figure notes claim the very bottom edge; a bottom legend sits directly above them.
		int notesH = notes.isEmpty() ? 0 : notes.size() * fm.getHeight() + ChartStyle.px(ChartStyle.SPACE_XS);
		int notesBaseline = h - notes.size() * fm.getHeight() - ChartStyle.px(OUTER_PAD) / 2 + fm.getAscent();
		// The first legend baseline, in either placement: under the title, or in the block reserved above the
		// bottom edge (the block height counts full line steps minus the trailing inter-line gap).
		int legendBaseline = legendBelow
				? h - notesH - (legendRows * (fm.getHeight() + ChartStyle.px(2)) - ChartStyle.px(2))
						- ChartStyle.px(OUTER_PAD) / 2 + fm.getAscent()
				: ChartStyle.px(OUTER_PAD) / 2 + titleH + fm.getAscent();

		// An axis-free chart (doughnut, waffle) gets the whole padded surface as its plot, scale-less, and draws
		// its own labelling inside it.
		if (renderer.axisFree()) {
			Rectangle plot = new Rectangle(outer, topMargin, w - 2 * outer,
					h - topMargin - outer - (legendBelow ? legendH : 0) - notesH);
			if (plot.width < ChartStyle.px(48) || plot.height < ChartStyle.px(32)) {
				this.lastContext = null;
				return;
			}
			PlotContext ctx = new PlotContext(plot, null, null, theme, hover, emphasis());
			this.lastContext = ctx;
			paintMarksRevealed(g, ctx, reveal, false);
			if (!legend.isEmpty()) {
				drawLegend(g, fm, legend, outer, legendAvail, legendBaseline, legendBelow);
			}
			drawNotes(g, outer, legendAvail, notesBaseline);
			return;
		}

		YAxisModel ym = renderer.yModel();
		String[] yBands = renderer.yBands();
		boolean log = yBands == null && yScaleMode == YScale.LOG && ym.allowLog();
		YTicks yticks = yBands == null ? yTicks(ym, log, Math.max(2, h / ChartStyle.px(44))) : null;
		// A band Y (horizontal bars) sizes its gutter from the real category names — the point of the orientation —
		// capped so a monster label can't push the plot off the surface.
		int maxYLabel = yBands != null ? Math.min(maxWidth(fm, yBands), Math.max(ChartStyle.px(80), w / 3))
				: maxWidth(fm, yticks.labels());

		int leftMargin = outer + maxYLabel + gap;
		int rightMargin = outer + ChartStyle.px(8);

		XAxisModel xm = renderer.xModel();
		int bottomMargin = bottomMargin(fm, xm, w - leftMargin - rightMargin, outer, gap)
				+ (legendBelow ? legendH : 0) + notesH;

		Rectangle plot = new Rectangle(leftMargin, topMargin,
				w - leftMargin - rightMargin, h - topMargin - bottomMargin);
		if (plot.width < ChartStyle.px(48) || plot.height < ChartStyle.px(32)) {
			this.lastContext = null;   // clear stale geometry so the hover overlay can't hit-test the old plot
			return;   // too small to draw a legible plot
		}

		Scale yScale = yBands != null
				? new Scale.Band(yBands.length, plot.y, plot.y + plot.height, BAND_INNER_PAD)
				: log
				? new Scale.Log(yticks.domainMin(), yticks.domainMax(), plot.y + plot.height, plot.y)
				: new Scale.Linear(yticks.domainMin(), yticks.domainMax(), plot.y + plot.height, plot.y);

		XBuild xb = buildX(xm, plot);
		PlotContext ctx = new PlotContext(plot, xb.scale(), yScale, theme, hover, emphasis());
		this.lastContext = ctx;

		if (yBands != null) {
			AxisRenderer.axisYBand(g, theme, plot, (Scale.Band) yScale, yBands, maxYLabel);
		}
		else {
			AxisRenderer.gridlinesY(g, theme, plot, yScale, yticks.values(), yticks.labels());
		}
		// Clip marks to the plot so nothing spills into the axis gutters — e.g. a log-axis zero bar (whose value
		// clamps to a pixel below the baseline) or an out-of-domain point.
		paintMarksRevealed(g, ctx, reveal, true);
		if (xb.scale() instanceof Scale.Band band) {
			AxisRenderer.axisXBand(g, theme, plot, band, band(xm));
		}
		else {
			AxisRenderer.axisXContinuous(g, theme, plot, xb.tickPixels(), xb.tickLabels());
		}
		if (!legend.isEmpty()) {
			drawLegend(g, fm, legend, outer, legendAvail, legendBaseline, legendBelow);
		}
		drawNotes(g, outer, legendAvail, notesBaseline);
	}

	// Draws the marks, clipped to the plot (for axis charts) and, mid entry-animation, to a type-appropriate
	// reveal — bars grow up, lines/scatter wipe in, the doughnut sweeps, others fade. Restores clip + composite.
	private void paintMarksRevealed(Graphics2D g, PlotContext ctx, double reveal, boolean plotClip) {
		Rectangle plot = ctx.plot();
		Shape savedClip = g.getClip();
		Composite savedComposite = g.getComposite();
		if (plotClip) {
			g.clipRect(plot.x, plot.y, plot.width, plot.height);
		}
		if (reveal < 0.999) {
			switch (renderer.revealStyle()) {
				case GROW_UP -> {
					int shown = (int) Math.ceil(reveal * plot.height);
					g.clip(new Rectangle(plot.x, plot.y + plot.height - shown, plot.width, shown));
				}
				case WIPE_RIGHT -> g.clip(new Rectangle(plot.x, plot.y,
						(int) Math.ceil(reveal * plot.width), plot.height));
				case SWEEP -> {
					double r = Math.hypot(plot.width, plot.height);
					g.clip(new Arc2D.Double(plot.getCenterX() - r, plot.getCenterY() - r, 2 * r, 2 * r,
							90, -reveal * 360, Arc2D.PIE));
				}
				case FADE -> g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) reveal));
			}
		}
		renderer.paintMarks(g, ctx);
		g.setClip(savedClip);
		g.setComposite(savedComposite);
	}

	// Figure notes in the book style: centred, muted, italic caption lines at the chart's bottom edge.
	private void drawNotes(Graphics2D g, int left, int available, int firstBaseline) {
		if (notes.isEmpty()) {
			return;
		}
		g.setFont(ChartStyle.caption().deriveFont(Font.ITALIC));
		FontMetrics fm = g.getFontMetrics();
		g.setColor(theme.muted());
		int y = firstBaseline;
		for (String note : notes) {
			String text = ellipsize(note, fm, available);
			g.drawString(text, left + Math.max(0, (available - fm.stringWidth(text)) / 2), y);
			y += fm.getHeight();
		}
	}

	private static String ellipsize(String text, FontMetrics fm, int available) {
		if (fm.stringWidth(text) <= available) {
			return text;
		}
		String suffix = "...";
		int end = text.length();
		while (end > 0 && fm.stringWidth(text.substring(0, end) + suffix) > available) {
			end--;
		}
		return end == 0 ? suffix : text.substring(0, end) + suffix;
	}

	// The headline row: the title in text ink with the muted subtitle beside it. Returns the height it claimed.
	private int drawTitle(Graphics2D g, int w, int outer) {
		if (title == null || title.isBlank()) {
			return 0;
		}
		Font titleFont = ChartStyle.font(ChartStyle.BODY, Font.BOLD);
		g.setFont(titleFont);
		FontMetrics tm = g.getFontMetrics();
		int baseline = ChartStyle.px(OUTER_PAD) / 2 + tm.getAscent();
		g.setColor(theme.text());
		g.drawString(title, outer, baseline);
		if (subtitle != null && !subtitle.isBlank()) {
			int x = outer + tm.stringWidth(title) + ChartStyle.px(ChartStyle.SPACE_S);
			g.setFont(ChartStyle.caption());
			g.setColor(theme.muted());
			g.drawString(subtitle, x, baseline);
		}
		return tm.getHeight() + ChartStyle.px(4);
	}

	// ---- layout helpers ---------------------------------------------------------------------------------

	private record YTicks(double domainMin, double domainMax, double[] values, String[] labels) { }

	private YTicks yTicks(YAxisModel ym, boolean log, int target) {
		if (log) {
			LogTicks.Result r = LogTicks.of(ym.positiveMin(), ym.max());
			return new YTicks(r.niceMin(), r.niceMax(), r.values(), r.labels());
		}
		NiceTicks.Result r = NiceTicks.linear(ym.min(), ym.max(), target);
		return new YTicks(r.niceMin(), r.niceMax(), r.values(), AxisRenderer.labelsFor(r));
	}

	private record XBuild(Scale scale, double[] tickPixels, String[] tickLabels) { }

	private XBuild buildX(XAxisModel xm, Rectangle plot) {
		double px0 = plot.x;
		double px1 = plot.x + plot.width;
		java.util.Objects.requireNonNull(xm, "xm");
		if (xm instanceof XAxisModel.Band b) {
			return new XBuild(new Scale.Band(b.categories().length, px0, px1, BAND_INNER_PAD),
					new double[0], new String[0]);
		}
		if (xm instanceof XAxisModel.Continuous c) {
			double lo = xZoom != null ? xZoom[0] : c.min();
			double hi = xZoom != null ? xZoom[1] : c.max();
			NiceTicks.Result r = NiceTicks.linear(lo, hi, Math.max(2, plot.width / ChartStyle.px(80)));
			Scale.Linear scale = new Scale.Linear(r.niceMin(), r.niceMax(), px0, px1);
			return new XBuild(scale, mapAll(scale, r.values()), AxisRenderer.labelsFor(r));
		}
		if (xm instanceof XAxisModel.Time t) {
			long lo = xZoom != null ? (long) xZoom[0] : t.min();
			long hi = xZoom != null ? (long) xZoom[1] : t.max();
			var r = io.github.wesleym.inkplot.scale.TimeTicks.of(
					lo, hi, Math.max(2, plot.width / ChartStyle.px(90)));
			Scale.Time scale = new Scale.Time(lo, hi, px0, px1);
			double[] px = new double[r.values().length];
			for (int i = 0; i < px.length; i++) {
				px[i] = scale.map(r.values()[i]);
			}
			return new XBuild(scale, px, r.labels());
		}
		throw new AssertionError("Unknown X axis model: " + xm.getClass().getName());
	}

	private static String[] band(XAxisModel xm) {
		return xm instanceof XAxisModel.Band b ? b.categories() : new String[0];
	}

	private static double[] mapAll(Scale scale, double[] values) {
		double[] px = new double[values.length];
		for (int i = 0; i < values.length; i++) {
			px[i] = scale.map(values[i]);
		}
		return px;
	}

	private int bottomMargin(FontMetrics fm, XAxisModel xm, int plotWidthGuess, int outer, int gap) {
		int captionH = fm.getHeight();
		if (xm instanceof XAxisModel.Band b && b.categories().length > 0) {
			double slot = (double) plotWidthGuess / b.categories().length;
			int maxLabel = maxWidth(fm, b.categories());
			if (maxLabel > slot - gap) {
				return outer + Math.min(ChartStyle.px(72), (int) (maxLabel * 0.6) + captionH);
			}
			return outer + captionH;
		}
		return outer + captionH + gap;
	}

	// How many rows the legend wraps into for the available width — so a many-series legend stacks instead of overflowing.
	private int legendRows(FontMetrics fm, List<LegendEntry> entries, int available) {
		int swatch = ChartStyle.px(10);
		int gap = ChartStyle.px(ChartStyle.SPACE_XS);
		int itemGap = ChartStyle.px(ChartStyle.SPACE_M);
		int x = 0;
		int rows = 1;
		for (LegendEntry e : entries) {
			int itemW = swatch + gap + fm.stringWidth(e.label()) + itemGap;
			if (x > 0 && x + itemW > available) {
				rows++;
				x = 0;
			}
			x += itemW;
		}
		return rows;
	}

	// Draws the legend left-aligned in the top band (below the title, when there is one), wrapping as needed.
	// Trims a long legend to the allowed rows, appending a swatch-less muted "+ N more" tail for the rest.
	private List<LegendEntry> capLegend(FontMetrics fm, List<LegendEntry> entries, int available, int maxRows) {
		List<LegendEntry> shown = new ArrayList<>(entries);
		while (shown.size() > 1) {
			shown.remove(shown.size() - 1);
			List<LegendEntry> withTail = new ArrayList<>(shown);
			withTail.add(moreEntry(entries.size() - shown.size()));
			if (legendRows(fm, withTail, available) <= maxRows) {
				return withTail;
			}
		}
		return List.of(moreEntry(entries.size()));
	}

	private static LegendEntry moreEntry(int hidden) {
		return new LegendEntry(null, "+ " + hidden + " more", false);
	}

	// Wraps the entries into rows first, so a bottom-placed legend can centre each row under the plot; a
	// top legend keeps the flush-left alignment it shares with the title.
	private void drawLegend(Graphics2D g, FontMetrics fm, List<LegendEntry> entries, int left, int available,
			int firstBaseline, boolean centred) {
		int swatch = ChartStyle.px(10);
		int gap = ChartStyle.px(ChartStyle.SPACE_XS);
		int itemGap = ChartStyle.px(ChartStyle.SPACE_M);
		int lineH = fm.getHeight() + ChartStyle.px(2);
		List<List<LegendEntry>> rows = new ArrayList<>();
		List<LegendEntry> row = new ArrayList<>();
		int rowW = 0;
		for (LegendEntry e : entries) {
			int itemW = swatch + gap + fm.stringWidth(e.label()) + itemGap;
			if (!row.isEmpty() && rowW + itemW > available) {
				rows.add(row);
				row = new ArrayList<>();
				rowW = 0;
			}
			row.add(e);
			rowW += itemW;
		}
		rows.add(row);

		boolean interactive = renderer != null && renderer.interactiveSeries();
		if (capturingLegendHits) {
			legendHitRects.clear();
			legendHitSeries.clear();
		}
		int series = 0;   // real (keyed) entries are in series order; the "+ N more" tail carries no key
		int y = firstBaseline;
		for (List<LegendEntry> line : rows) {
			int lineW = -itemGap;   // the trailing gap doesn't count against centring
			for (LegendEntry e : line) {
				lineW += swatch + gap + fm.stringWidth(e.label()) + itemGap;
			}
			int x = centred ? left + Math.max(0, (available - lineW) / 2) : left;
			for (LegendEntry e : line) {
				if (e.color() == null) {
					// The swatch-less overflow tail ("+ N more") — muted text, no key.
					g.setColor(theme.muted());
					g.drawString(e.label(), x, y);
					x += swatch + gap + fm.stringWidth(e.label()) + itemGap;
					continue;
				}
				int labelW = fm.stringWidth(e.label());
				boolean off = interactive && series < hidden.length && hidden[series];
				int mid = y - fm.getAscent() / 2 + 1;
				g.setColor(off ? theme.muted() : e.color());
				if (e.line()) {
					g.fillRect(x, mid - ChartStyle.px(1), swatch, Math.max(1, ChartStyle.px(2)));
				}
				else {
					g.fill(new Rectangle2D.Double(x, mid - swatch / 2.0, swatch, swatch));
				}
				g.setColor(off ? theme.muted() : theme.text());
				g.drawString(e.label(), x + swatch + gap, y);
				if (off) {   // a strike through the toggled-off entry
					g.drawLine(x + swatch + gap, mid, x + swatch + gap + labelW, mid);
				}
				if (interactive && capturingLegendHits) {
					legendHitRects.add(new Rectangle(x, y - fm.getAscent(), swatch + gap + labelW, fm.getHeight()));
					legendHitSeries.add(series);
				}
				series++;
				x += swatch + gap + labelW + itemGap;
			}
			y += lineH;
		}
	}

	private static int maxWidth(FontMetrics fm, String[] labels) {
		int max = 0;
		for (String label : labels) {
			max = Math.max(max, fm.stringWidth(label));
		}
		return max;
	}

	private static void applyHints(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
	}
}
