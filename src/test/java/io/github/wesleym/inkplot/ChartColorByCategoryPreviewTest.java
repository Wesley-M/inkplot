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

/**
 * Colouring bars by their own category (Series == Category): each category is a single full-width bar in its own
 * colour — not a grouped bar shredded into empty per-series slots. Wide plot, many categories.
 */
class ChartColorByCategoryPreviewTest {

	private static final int W = 1200;
	private static final int H = 440;

	@Test
	void colorsEachCategoryAsAFullWidthBar() throws Exception {
		render("light", ChartTheme.PAPER);
		render("dark", ChartTheme.INKWELL);
	}

	private void render(String themeId, ChartTheme theme) throws Exception {
		int n = 22;
		String[] categories = new String[n];
		double[][] values = new double[1][n];
		for (int c = 0; c < n; c++) {
			categories[c] = "v" + (c + 1);
			values[0][c] = 20 + (c * 13) % 90;
		}
		ChartData.Bar data = new ChartData.Bar(categories, new String[] { "count" }, values, false, true,
				Provenance.full(1500));

		ChartCanvas canvas = new ChartCanvas(theme);
		canvas.setData(data);
		canvas.setSize(W, H);
		BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		canvas.renderTo(g, W, H);
		g.dispose();
		File out = new File("build/chart-color-by-category-" + themeId + ".png");
		ImageIO.write(img, "png", out);
		assertTrue(out.exists() && out.length() > 0, "written " + out);
	}
}
