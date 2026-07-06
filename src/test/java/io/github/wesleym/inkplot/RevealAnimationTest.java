package io.github.wesleym.inkplot;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The entry animation is live-component-only: {@code renderTo}/{@code image}/{@code toSvg} always draw the
 * full chart (reveal 1.0). These assert that a partial reveal draws strictly fewer marks than a full one and
 * that reveal is monotonic — so the animation grows the chart on without touching the export path.
 */
class RevealAnimationTest {

	private static final int W = 640;
	private static final int H = 400;

	private long markPixels(Chart chart, double reveal) {
		ChartCanvas canvas = (ChartCanvas) chart.theme(ChartTheme.PAPER).component();
		BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		canvas.renderRevealFrame(g, W, H, reveal);
		g.dispose();
		Color surface = ChartTheme.PAPER.surface();
		long count = 0;
		for (int y = 0; y < H; y++) {
			for (int x = 0; x < W; x++) {
				int rgb = img.getRGB(x, y);
				int dr = ((rgb >> 16) & 0xFF) - surface.getRed();
				int dg = ((rgb >> 8) & 0xFF) - surface.getGreen();
				int db = (rgb & 0xFF) - surface.getBlue();
				if (dr * dr + dg * dg + db * db > 900) {   // meaningfully off the paper surface = ink
					count++;
				}
			}
		}
		return count;
	}

	@Test
	void revealGrowsTheMarksMonotonically() {
		Chart bar = Charts.bar("Mon", "Tue", "Wed", "Thu", "Fri").series("Cups", 132, 148, 156, 161, 202);
		long at15 = markPixels(bar, 0.15);
		long at55 = markPixels(bar, 0.55);
		long at100 = markPixels(bar, 1.0);
		assertTrue(at15 < at55 && at55 < at100, "marks grow with reveal: " + at15 + " < " + at55 + " < " + at100);
	}

	@Test
	void fullRevealMatchesTheStaticExport() {
		Chart line = Charts.line().series("y", new double[] { 0, 1, 2, 3, 4 }, new double[] { 10, 14, 12, 18, 16 });
		long fullReveal = markPixels(line, 1.0);        // an entry frame at 1.0
		long exported = exportInk(line);                // the static export path (renderTo)
		assertTrue(fullReveal > 0);
		assertEquals(exported, fullReveal, "a full-reveal frame draws the same chart the export does");
	}

	private long exportInk(Chart chart) {
		ChartCanvas canvas = (ChartCanvas) chart.theme(ChartTheme.PAPER).component();
		BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		canvas.renderTo(g, W, H);
		g.dispose();
		Color surface = ChartTheme.PAPER.surface();
		long count = 0;
		for (int y = 0; y < H; y++) {
			for (int x = 0; x < W; x++) {
				int rgb = img.getRGB(x, y);
				int dr = ((rgb >> 16) & 0xFF) - surface.getRed();
				int dg = ((rgb >> 8) & 0xFF) - surface.getGreen();
				int db = (rgb & 0xFF) - surface.getBlue();
				if (dr * dr + dg * dg + db * db > 900) {
					count++;
				}
			}
		}
		return count;
	}
}
