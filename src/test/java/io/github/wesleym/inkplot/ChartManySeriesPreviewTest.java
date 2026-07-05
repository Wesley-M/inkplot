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
 * Dev-only harness for the shade-generated series colours and the wrapping legend — a chart with many more series than
 * the eight base palette slots, so the generated shades and multi-row legend can be eyeballed.
 */
class ChartManySeriesPreviewTest {

	private static final int W = 720;
	private static final int H = 440;

	@Test
	void manySeriesGetDistinctShadesAndAWrappingLegend() throws Exception {
		render("dark", ChartTheme.INKWELL);
		render("light", ChartTheme.PAPER);
	}

	private void render(String themeId, ChartTheme theme) throws Exception {
		int series = 18;
		String[] names = new String[series];
		double[][] values = new double[series][3];
		for (int s = 0; s < series; s++) {
			names[s] = "v" + (s + 1);
			for (int c = 0; c < 3; c++) {
				values[s][c] = 20 + (s * 7 + c * 13) % 90;
			}
		}
		ChartData.Bar data = new ChartData.Bar(new String[] { "A", "B", "C" }, names, values, false,
				Provenance.full(1000));

		ChartCanvas canvas = new ChartCanvas(theme);
		canvas.setData(data);
		canvas.setSize(W, H);
		BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		canvas.renderTo(g, W, H);
		g.dispose();
		File out = new File("build/chart-many-series-" + themeId + ".png");
		ImageIO.write(img, "png", out);
		assertTrue(out.exists() && out.length() > 0, "written " + out);
	}
}
