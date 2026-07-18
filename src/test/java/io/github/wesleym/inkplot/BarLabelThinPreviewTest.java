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

/** Visual check that a dense category axis thins its labels in a narrow tile but shows them all when wide. */
class BarLabelThinPreviewTest {

	private static ChartData.Bar denseBar() {
		String[] cats = { "5.4.11", "5.4.9", "5.4.8", "5.5.0", "5.5.2-RC1", "3.22.0", "3.23.2", "3.23.1-DEVC5",
				"5.5.2-RC2", "3.22.1", "5.1.0", "5.3.1", "3.22.0-Log", "5.4.4-RC2", "3.23.0-DEV", "5.4.0" };
		double[] v = new double[cats.length];
		for (int i = 0; i < v.length; i++) {
			v[i] = 300 - i * 17;
		}
		return new ChartData.Bar(cats, new String[] { "Count" }, new double[][] { v }, false, Provenance.full(2400));
	}

	@Test
	void thinsWhenNarrowShowsAllWhenWide() throws Exception {
		render(340, 240, "build/bar-thin-tile.png");
		render(820, 320, "build/bar-thin-wide.png");
	}

	private static void render(int w, int h, String out) throws Exception {
		ChartCanvas canvas = new ChartCanvas(ChartTheme.PAPER);
		canvas.setData(denseBar());
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
