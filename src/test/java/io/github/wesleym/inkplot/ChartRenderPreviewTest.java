package io.github.wesleym.inkplot;

import io.github.wesleym.inkplot.data.ChartData;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dev-only visual harness: renders each chart type in every palette to {@code build/chart-<type>-<theme>.png} so the
 * marks, spacing, axes, and legend can be eyeballed — the render-and-look gate for "does it look premium". Also asserts
 * each render is non-blank (the plot actually drew), guarding against a silent layout regression.
 */
class ChartRenderPreviewTest {

	private static final int W = 640;
	private static final int H = 400;

	record NamedTheme(String id, ChartTheme theme) { }

	static final java.util.List<NamedTheme> THEMES = java.util.List.of(
			new NamedTheme("light", ChartTheme.LIGHT),
			new NamedTheme("dark", ChartTheme.DARK));

	@Test
	void rendersEveryChartTypeAcrossEveryPalette() throws Exception {
		for (NamedTheme def : THEMES) {
			render(def, "bar-single", ChartFixtures.barSingle());
			render(def, "bar-grouped", ChartFixtures.barGrouped());
			render(def, "bar-stacked", ChartFixtures.barStacked());
			render(def, "bar-horizontal", ChartFixtures.barHorizontal());
			render(def, "doughnut", ChartFixtures.doughnut());
			render(def, "doughnut-tail", ChartFixtures.doughnutTail());
			render(def, "waffle", ChartFixtures.waffle());
			render(def, "treemap", ChartFixtures.treemap());
			render(def, "line-time", ChartFixtures.lineTime());
			render(def, "line-multi", ChartFixtures.lineMulti());
			render(def, "histogram", ChartFixtures.histogram());
			render(def, "density", ChartFixtures.density());
			render(def, "scatter", ChartFixtures.scatter());
			render(def, "scatter-series", ChartFixtures.scatterSeries());
			render(def, "box", ChartFixtures.box());
		}
	}

	// The headline + rows subtitle render inside the surface, so PNG/clipboard exports carry their own context.
	@Test
	void rendersTheTitledChartExportsShip() throws Exception {
		ChartTheme theme = ChartTheme.LIGHT;
		ChartCanvas canvas = new ChartCanvas(theme);
		canvas.setData(ChartFixtures.barSingle());
		canvas.setTitle("Count by status", "21,830 rows");
		canvas.setSize(W, H);
		BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		canvas.renderTo(g, W, H);
		g.dispose();
		File out = new File("build/chart-titled-bar-light.png");
		ImageIO.write(img, "png", out);
		assertTrue(out.exists() && out.length() > 0, "titled preview written: " + out);
	}

	// The insights card embeds charts at strip heights (160px doughnut, 220px top-value bars); they must stay
	// legible (or degrade their labels gracefully) there, not just at the standalone canvas size.
	@Test
	void rendersEmbeddedChartsAtInsightsStripSizes() throws Exception {
		for (NamedTheme def : THEMES) {
			render(def, "doughnut-strip", ChartFixtures.doughnutBoolean(), W, 160);
			render(def, "bar-topvalues-strip", ChartFixtures.barTopValues(), W, 220);
		}
	}

	static BufferedImage render(NamedTheme def, String name, ChartData data) throws Exception {
		return render(def, name, data, W, H);
	}

	static BufferedImage render(NamedTheme def, String name, ChartData data, int w, int h) throws Exception {
		ChartTheme theme = def.theme();
		ChartCanvas canvas = new ChartCanvas(theme);
		canvas.setData(data);
		canvas.setSize(w, h);

		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		canvas.renderTo(g, w, h);
		g.dispose();

		File out = new File("build/chart-" + name + "-" + def.id() + ".png");
		ImageIO.write(img, "png", out);
		assertTrue(out.exists() && out.length() > 0, "preview written: " + out);
		assertTrue(hasInk(img, theme), name + "/" + def.id() + " drew marks, not a blank surface");
		return img;
	}

	// At least some pixels must differ from the surface colour — i.e. the plot actually rendered something.
	private static boolean hasInk(BufferedImage img, ChartTheme theme) {
		int surface = theme.surface().getRGB() & 0xFFFFFF;
		int differing = 0;
		for (int y = 0; y < img.getHeight(); y += 3) {
			for (int x = 0; x < img.getWidth(); x += 3) {
				if ((img.getRGB(x, y) & 0xFFFFFF) != surface) {
					differing++;
				}
			}
		}
		return differing > 200;
	}
}
