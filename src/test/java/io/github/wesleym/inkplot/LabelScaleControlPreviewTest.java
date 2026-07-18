package io.github.wesleym.inkplot;

import io.github.wesleym.inkplot.data.ChartData;
import io.github.wesleym.inkplot.data.Provenance;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Visual check for the new controls: labels OFF (hover only) and a manual value-axis bound. */
class LabelScaleControlPreviewTest {

	private static ChartData.Bar bar() {
		String[] cats = { "5.4.11", "5.4.9", "5.4.8", "5.5.0", "5.5.2-RC1", "3.22.0", "3.23.2", "3.23.1-DEVC5" };
		double[] v = { 300, 265, 240, 210, 180, 150, 120, 90 };
		return new ChartData.Bar(cats, new String[] { "Count" }, new double[][] { v }, false, Provenance.full(1555));
	}

	@Test
	void rendersLabelOffAndBoundedAxis() throws Exception {
		render("build/labels-off.png", c -> c.setLabelMode(LabelMode.OFF));
		render("build/y-bounds.png", c -> c.setYBounds(0.0, 600.0));   // bars scale to a 0..600 axis (look shorter)
	}

	private static void render(String out, Consumer<ChartCanvas> tweak) throws Exception {
		int w = 420;
		int h = 260;
		ChartCanvas canvas = new ChartCanvas(ChartTheme.PAPER);
		canvas.setData(bar());
		tweak.accept(canvas);
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
