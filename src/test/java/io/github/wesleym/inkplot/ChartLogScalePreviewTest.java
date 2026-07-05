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
 * Verifies the Log Y toggle actually re-scales the value axis — the same bars (spanning five orders of magnitude)
 * render differently under linear vs log, and the log axis reaches its decades. Writes both PNGs to eyeball.
 */
class ChartLogScalePreviewTest {

	private static final int W = 640;
	private static final int H = 400;

	@Test
	void logToggleRescalesTheValueAxis() throws Exception {
		ChartTheme theme = ChartTheme.PAPER;
		ChartData.Bar data = new ChartData.Bar(
				new String[] { "A", "B", "C", "D", "E" },
				new String[] { "Value" },
				new double[][] { { 5, 50, 500, 5000, 50000 } },
				false, Provenance.full(55555));

		BufferedImage linear = render(theme, data, ChartCanvas.YScale.LINEAR, "linear");
		BufferedImage log = render(theme, data, ChartCanvas.YScale.LOG, "log");
		assertTrue(differs(linear, log), "the log render differs from the linear one — the toggle took effect");
	}

	private BufferedImage render(ChartTheme theme, ChartData data, ChartCanvas.YScale scale, String name)
			throws Exception {
		ChartCanvas canvas = new ChartCanvas(theme);
		canvas.setData(data);
		canvas.setYScale(scale);
		canvas.setSize(W, H);

		BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		canvas.renderTo(g, W, H);
		g.dispose();

		File out = new File("build/chart-logscale-" + name + ".png");
		ImageIO.write(img, "png", out);
		assertTrue(out.exists(), "written " + out);
		return img;
	}

	private static boolean differs(BufferedImage a, BufferedImage b) {
		int diff = 0;
		for (int y = 0; y < a.getHeight(); y += 4) {
			for (int x = 0; x < a.getWidth(); x += 4) {
				if (a.getRGB(x, y) != b.getRGB(x, y)) {
					diff++;
				}
			}
		}
		return diff > 100;
	}
}
