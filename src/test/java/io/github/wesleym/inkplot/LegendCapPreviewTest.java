package io.github.wesleym.inkplot;

import io.github.wesleym.inkplot.data.ChartBuilder;
import io.github.wesleym.inkplot.data.ChartData;
import io.github.wesleym.inkplot.data.Table;
import io.github.wesleym.inkplot.spec.ChartSpec;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dev visual harness for the bottom legend at scale: a doughnut built from ascending-ordered rows (the
 * fold must keep the giants), with enough slices that the legend caps into a "+ N more" tail.
 */
class LegendCapPreviewTest {

	@Test
	void rendersLargestSlicesWithACappedBottomLegend() throws Exception {
		ChartTheme theme = ChartTheme.PAPER;

		// Pre-aggregated, ordered smallest-first — like SELECT v, count(*) ... ORDER BY count(*).
		List<List<String>> rows = new ArrayList<>();
		for (int i = 1; i <= 24; i++) {
			rows.add(List.of("5.4." + i, String.valueOf(i * i)));
		}
		Table s = new Table(List.of("clientVersion", "count"),
				List.of("varchar", "int"), rows, false);
		ChartData.Doughnut d = (ChartData.Doughnut) ChartBuilder.build(new ChartSpec.Doughnut(0, 1), s, 640);
		assertEquals("5.4.24", d.categories()[0], "the biggest share leads, whatever the row order");
		assertEquals("Other", d.categories()[d.categories().length - 1]);

		ChartCanvas canvas = new ChartCanvas(theme);
		canvas.setData(d);
		canvas.setLegendPlacement(ChartCanvas.LegendPlacement.BOTTOM);
		canvas.setSize(560, 360);

		BufferedImage image = new BufferedImage(560, 360, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		canvas.renderTo(g, 560, 360);
		g.dispose();

		File out = new File("build/legend-cap-doughnut.png");
		ImageIO.write(image, "png", out);
		assertTrue(out.exists() && out.length() > 0);

		// A cramped surface: the legend must cap into "+ N more" instead of eating the ring or clipping.
		BufferedImage small = new BufferedImage(240, 240, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = small.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		canvas.renderTo(g2, 240, 240);
		g2.dispose();
		File outSmall = new File("build/legend-cap-doughnut-small.png");
		ImageIO.write(small, "png", outSmall);
		assertTrue(outSmall.exists() && outSmall.length() > 0);
	}
}
