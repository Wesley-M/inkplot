package io.github.wesleym.inkplot;

import io.github.wesleym.inkplot.data.ChartData;
import io.github.wesleym.inkplot.data.Provenance;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Visual harness for the STAT card — a big single number in a tile-sized surface, light and dark. */
class StatRenderPreviewTest {

	@Test
	void rendersTheStatCard() throws Exception {
		render("paper", ChartTheme.PAPER, new ChartData.Stat(42318, "Total accounts", true, Provenance.full(42318)),
				"build/stat-count-light.png");
		render("nocturne", ChartTheme.NOCTURNE,
				new ChartData.Stat(1284.57, "Average order value", false, Provenance.full(9000)),
				"build/stat-avg-dark.png");
	}

	private static void render(String id, ChartTheme theme, ChartData.Stat data, String out) throws Exception {
		int w = 380;
		int h = 220;
		ChartCanvas canvas = new ChartCanvas(theme);
		canvas.setData(data);
		canvas.setSize(w, h);
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		canvas.renderTo(g, w, h);
		g.dispose();
		File file = new File(out);
		ImageIO.write(img, "png", file);
		assertTrue(file.exists() && file.length() > 0);
	}
}
