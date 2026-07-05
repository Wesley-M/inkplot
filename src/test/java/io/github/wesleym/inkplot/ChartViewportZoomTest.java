package io.github.wesleym.inkplot;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The screen-only viewport zoom/pan: magnifying the on-screen chart changes what the widget paints, but must NOT
 * change what an export renders (exports go through renderTo, which the viewport transform never touches).
 */
class ChartViewportZoomTest {

	private static final int W = 640;
	private static final int H = 400;

	@Test
	void zoomMagnifiesTheWidgetButNeverTheExport() throws Exception {
		ChartTheme theme = ChartTheme.LIGHT;
		ChartCanvas canvas = new ChartCanvas(theme);
		canvas.setData(ChartFixtures.barSingle());
		canvas.setSize(W, H);

		BufferedImage exportBefore = export(canvas);   // the export path is canvas.renderTo — viewport-transform-free
		BufferedImage screenBefore = paint(canvas);

		canvas.zoomIn();
		canvas.zoomIn();
		canvas.zoomIn();
		assertTrue(canvas.isZoomed(), "three zoom-ins magnify the viewport");

		BufferedImage screenAfter = paint(canvas);
		BufferedImage exportAfter = export(canvas);

		assertFalse(imagesEqual(screenBefore, screenAfter), "the on-screen widget magnifies");
		assertTrue(imagesEqual(exportBefore, exportAfter), "the export stays at 1:1 fit regardless of viewport zoom");

		canvas.fitView();
		assertFalse(canvas.isZoomed(), "fit resets the viewport");
		assertTrue(imagesEqual(screenBefore, paint(canvas)), "fitting returns the widget to the unzoomed view");

		// A zoomed render for eyeballing the settled crisp bake + centre-pivot magnification.
		ImageIO.write(screenAfter, "png", new File("build/chart-viewport-zoom.png"));
	}

	@Test
	void zoomOutFromFitStepsBackAndFitRestores() throws Exception {
		ChartTheme theme = ChartTheme.LIGHT;
		ChartCanvas canvas = new ChartCanvas(theme);
		canvas.setData(ChartFixtures.barSingle());
		canvas.setSize(W, H);

		BufferedImage fit = paint(canvas);
		canvas.zoomOut();
		assertTrue(canvas.isZoomed(), "zoom-out is allowed below fit, not just back toward it");
		assertFalse(imagesEqual(fit, paint(canvas)), "the widget visibly steps back");

		canvas.fitView();
		assertFalse(canvas.isZoomed());
		assertTrue(imagesEqual(fit, paint(canvas)), "fitting restores the 1:1 view");
	}

	private static BufferedImage paint(ChartCanvas canvas) {
		BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		canvas.paint(g);
		g.dispose();
		return img;
	}

	// The export path: renderTo straight onto a buffer, exactly as ChartPngExport does (minus the 2x scale).
	private static BufferedImage export(ChartCanvas canvas) {
		BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		canvas.renderTo(g, W, H);
		g.dispose();
		return img;
	}

	private static boolean imagesEqual(BufferedImage a, BufferedImage b) {
		if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
			return false;
		}
		for (int y = 0; y < a.getHeight(); y++) {
			for (int x = 0; x < a.getWidth(); x++) {
				if (a.getRGB(x, y) != b.getRGB(x, y)) {
					return false;
				}
			}
		}
		return true;
	}
}
