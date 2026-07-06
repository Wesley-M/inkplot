package io.github.wesleym.inkplot;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** The interactive legend: toggling a series off draws fewer marks; focusing one dims the rest. */
class InteractiveLegendTest {

	private static final int W = 720, H = 440;

	private static long ink(ChartCanvas c, String dump) throws Exception {
		BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		c.renderTo(g, W, H);
		g.dispose();
		if (dump != null) {
			ImageIO.write(img, "png", new File("build/legend-" + dump + ".png"));
		}
		Color s = ChartTheme.PAPER.surface();
		long n = 0;
		for (int y = 0; y < H; y++) {
			for (int x = 0; x < W; x++) {
				int rgb = img.getRGB(x, y);
				int dr = ((rgb >> 16) & 0xFF) - s.getRed(), dg = ((rgb >> 8) & 0xFF) - s.getGreen(),
						db = (rgb & 0xFF) - s.getBlue();
				if (dr * dr + dg * dg + db * db > 900) {
					n++;
				}
			}
		}
		return n;
	}

	private static ChartCanvas bars() {
		return (ChartCanvas) Charts.bar("Mon", "Tue", "Wed", "Thu", "Fri")
				.series("Espresso", 132, 148, 156, 161, 202)
				.series("Filter", 98, 91, 104, 112, 151)
				.series("Cold brew", 41, 44, 63, 70, 96)
				.theme(ChartTheme.PAPER).component();
	}

	private static ChartCanvas horizontalBars() {
		return (ChartCanvas) Charts.bar("Mon", "Tue", "Wed", "Thu", "Fri")
				.series("Espresso", 132, 148, 156, 161, 202)
				.series("Filter", 98, 91, 104, 112, 151)
				.series("Cold brew", 41, 44, 63, 70, 96)
				.horizontal()
				.theme(ChartTheme.PAPER).component();
	}

	@Test
	void horizontalBarsHonourTheLegendToggleToo() throws Exception {
		// The horizontal bar has its own renderer (HBarRenderer); it must apply the series emphasis and offer the
		// interactive legend just like the vertical one, not sit inert.
		ChartCanvas c = horizontalBars();
		assertTrue(c.hasInteractiveLegendForTest(), "a multi-series horizontal bar has an interactive legend");
		long all = ink(c, null);
		c.toggleSeriesForTest(1);
		assertTrue(ink(c, null) < all, "hiding a series in a horizontal bar draws fewer marks");
	}

	@Test
	void togglingHidesASeries() throws Exception {
		ChartCanvas c = bars();
		long all = ink(c, null);
		c.toggleSeriesForTest(1);
		long toggled = ink(c, null);
		assertTrue(toggled < all, "hiding a series draws fewer marks: " + toggled + " < " + all);
		c.toggleSeriesForTest(1);   // back on
		assertTrue(Math.abs(ink(c, null) - all) < all * 0.02, "toggling back restores it");
	}

	@Test
	void focusDimsTheOthers() throws Exception {
		ChartCanvas c = bars();
		long all = ink(c, null);
		c.focusSeriesForTest(0);
		long focused = ink(c, null);
		assertTrue(focused < all, "focusing one series dims the rest (less ink): " + focused + " < " + all);
	}
}
