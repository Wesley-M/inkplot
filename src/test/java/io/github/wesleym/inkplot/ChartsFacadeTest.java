package io.github.wesleym.inkplot;

import io.github.wesleym.inkplot.data.ChartData;
import io.github.wesleym.inkplot.data.Table;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The front door: every factory yields a chart that renders real ink, and misuse fails fast. */
class ChartsFacadeTest {

	@Test
	void everyFactoryRendersInk() {
		List<Chart> charts = List.of(
				Charts.bar(new String[] { "A", "B", "C" }, new double[] { 3, 1, 2 }),
				Charts.bar("A", "B").series("x", 1, 2).series("y", 3, 4).stacked(),
				Charts.line(new double[] { 1, 2, 3 }, new double[] { 2, 4, 3 }),
				Charts.scatter(new double[] { 1, 2, 3 }, new double[] { 9, 4, 6 }),
				Charts.histogram(1, 2, 2, 3, 3, 3, 4, 4, 5),
				Charts.doughnut(new String[] { "yes", "no" }, new double[] { 7, 3 }),
				Charts.treemap(new String[] { "a", "b", "c" }, new double[] { 5, 3, 2 }),
				Charts.waffle(new String[] { "a", "b" }, new double[] { 6, 4 }));
		for (Chart chart : charts) {
			BufferedImage img = chart.title("t").image(320, 200);
			assertTrue(hasInk(img), chart.data().getClass().getSimpleName() + " drew marks");
		}
	}

	@Test
	void fluentOptionsApplyToTheComponent() {
		ChartCanvas canvas = Charts.bar(new String[] { "A" }, new double[] { 2 })
				.title("Rows", "1 row")
				.theme(ChartTheme.INKWELL)
				.legendBelow()
				.notes("a note")
				.component();
		assertNotNull(canvas);
		assertTrue(hasInk(imageOf(canvas)), "configured chart still renders");
	}

	@Test
	void autoPicksAChartForATabularResult() {
		List<List<String>> rows = new ArrayList<>();
		for (int i = 0; i < 40; i++) {
			rows.add(List.of("region-" + (i % 4), String.valueOf(10 + i)));
		}
		Chart chart = Charts.auto(new Table(
				List.of("region", "amount"), List.of("varchar", "int"), rows, false));
		assertInstanceOf(ChartData.Bar.class, chart.data(), "category + measure suggests bars");
	}

	@Test
	void mismatchedSeriesFailFast() {
		assertThrows(IllegalArgumentException.class,
				() -> Charts.bar("A", "B").series("x", 1, 2, 3));
		assertThrows(IllegalArgumentException.class,
				() -> Charts.scatter(new double[] { 1 }, new double[] { 1, 2 }));
		assertThrows(IllegalArgumentException.class,
				() -> Charts.line().series("x", new double[] { 1 }, new double[] { 1, 2 }));
	}

	@Test
	void histogramAutoBinsCoverEveryValue() {
		ChartData.Histogram data = (ChartData.Histogram) Charts.histogram(1, 1, 2, 8, 9, 9).data();
		long total = 0;
		for (long c : data.counts()) {
			total += c;
		}
		assertEquals(6, total, "every value lands in a bin");
	}

	private static BufferedImage imageOf(ChartCanvas canvas) {
		BufferedImage img = new BufferedImage(320, 200, BufferedImage.TYPE_INT_RGB);
		var g = img.createGraphics();
		canvas.renderTo(g, 320, 200);
		g.dispose();
		return img;
	}

	private static boolean hasInk(BufferedImage img) {
		int first = img.getRGB(0, 0);
		for (int y = 0; y < img.getHeight(); y += 2) {
			for (int x = 0; x < img.getWidth(); x += 2) {
				if (img.getRGB(x, y) != first) {
					return true;
				}
			}
		}
		return false;
	}
}
