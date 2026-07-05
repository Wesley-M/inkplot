package io.github.wesleym.inkplot;

import io.github.wesleym.inkplot.scale.Scale;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The brush-zoom contract: an X-domain override reshapes the built time scale exactly, resets clear it, new data
 * never inherits it, and band/axis-free charts are not brushable. Also writes a zoomed-line preview (through the
 * on-screen paint path, so the reset hint shows) for the render-and-look gate.
 */
class ChartCanvasZoomTest {

	private static final int W = 640;
	private static final int H = 400;

	@Test
	void zoomOverridesTheTimeDomainExactlyAndResets() throws Exception {
		ChartTheme theme = ChartTheme.LIGHT;
		ChartCanvas canvas = new ChartCanvas(theme);
		canvas.setData(ChartFixtures.lineTime());
		canvas.setSize(W, H);
		paint(canvas, null);
		assertTrue(canvas.brushable(), "a time line admits the brush");

		long t0 = Instant.parse("2026-01-10T00:00:00Z").toEpochMilli();
		long t1 = Instant.parse("2026-01-20T00:00:00Z").toEpochMilli();
		canvas.setXZoom(t0, t1);
		paint(canvas, new File("build/chart-zoom-line-light.png"));
		Scale.Time zoomed = (Scale.Time) canvas.plotContext().xScale();
		assertEquals(t0, zoomed.min(), "the zoomed domain starts at the brush start");
		assertEquals(t1, zoomed.max(), "…and ends at the brush end");

		canvas.clearXZoom();
		paint(canvas, null);
		Scale.Time full = (Scale.Time) canvas.plotContext().xScale();
		assertTrue(full.max() - full.min() > t1 - t0, "reset restores the full domain");
	}

	@Test
	void bandAndAxisFreeChartsAreNotBrushable() {
		ChartTheme theme = ChartTheme.LIGHT;
		ChartCanvas canvas = new ChartCanvas(theme);
		canvas.setData(ChartFixtures.barSingle());
		assertFalse(canvas.brushable(), "a category band has no continuous domain to brush");
		canvas.setData(ChartFixtures.doughnut());
		assertFalse(canvas.brushable(), "an axis-free chart has no X axis at all");
	}

	@Test
	void newDataDropsAnInheritedZoom() throws Exception {
		ChartTheme theme = ChartTheme.LIGHT;
		ChartCanvas canvas = new ChartCanvas(theme);
		canvas.setData(ChartFixtures.lineTime());
		canvas.setSize(W, H);
		canvas.setXZoom(0, 1);
		canvas.setData(ChartFixtures.lineMulti());
		paint(canvas, null);
		Scale.Linear x = (Scale.Linear) canvas.plotContext().xScale();
		assertTrue(x.max() >= 59, "the new dataset renders its own full domain, not the old brush");
	}

	private static void paint(ChartCanvas canvas, File out) throws Exception {
		BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		canvas.paint(g);
		g.dispose();
		if (out != null) {
			ImageIO.write(img, "png", out);
		}
	}
}
