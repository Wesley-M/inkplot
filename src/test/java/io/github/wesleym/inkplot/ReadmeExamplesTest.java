package io.github.wesleym.inkplot;

import io.github.wesleym.inkplot.data.ResultSnapshot;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Renders every example in the README, code-for-code — each test body is the snippet the README shows, so
 * the published code and the published chart can never drift apart. Writes {@code build/readme-*.png}.
 */
class ReadmeExamplesTest {

	@Test
	void quickstartBars() throws Exception {
		Chart chart = Charts.bar("Mon", "Tue", "Wed", "Thu", "Fri")
				.series("Espresso", 132, 148, 156, 161, 202)
				.series("Filter", 98, 91, 104, 112, 151)
				.title("Cups sold", "one café, one week");
		write("readme-quickstart", chart, 760, 420);
	}

	@Test
	void lineWithTimeAxis() throws Exception {
		long day = 24L * 60 * 60 * 1000;
		long start = Instant.parse("2026-04-01T00:00:00Z").toEpochMilli();
		double[] when = new double[90];
		double[] users = new double[90];
		for (int i = 0; i < 90; i++) {
			when[i] = start + i * day;
			users[i] = 1180 + i * 9 + (i % 7 >= 5 ? -260 : 0) + 70 * Math.sin(i / 5.0);
		}
		Chart chart = Charts.line().timeAxis()
				.series("Daily active users", when, users)
				.title("Steady growth, quiet weekends");
		write("readme-line-time", chart, 760, 400);
	}

	@Test
	void doughnutShares() throws Exception {
		Chart chart = Charts.doughnut(
				new String[] { "Organic search", "Direct", "Social", "Referral", "Email" },
				new double[] { 18200, 12400, 6100, 2900, 1400 }, "Sessions")
				.title("Sessions by source");
		write("readme-doughnut", chart, 640, 400);
	}

	@Test
	void treemapShares() throws Exception {
		Chart chart = Charts.treemap(
				new String[] { "node_modules", ".git", "build", "src", "assets", "docs" },
				new double[] { 482, 130, 88, 34, 27, 9 })
				.title("Disk usage", "MB per folder");
		write("readme-treemap", chart, 640, 400);
	}

	@Test
	void histogramOfRawValues() throws Exception {
		Random rng = new Random(7);
		double[] responseMs = new double[8000];
		for (int i = 0; i < responseMs.length; i++) {
			responseMs[i] = Math.exp(rng.nextGaussian() * 0.5 + 4.6);
		}
		Chart chart = Charts.histogram(responseMs)
				.title("Response time", "milliseconds, 8,000 requests");
		write("readme-histogram", chart, 640, 400);
	}

	@Test
	void horizontalBarsForLongLabels() throws Exception {
		Chart chart = Charts.bar("Billing and account questions", "Shipping and delivery delays",
						"Product setup and installation", "Returns and refund requests",
						"Technical troubleshooting")
				.series("Tickets", 9400, 7200, 3100, 2400, 1900)
				.horizontal()
				.title("Support volume by queue");
		write("readme-horizontal", chart, 640, 400);
	}

	@Test
	void stackedBars() throws Exception {
		Chart chart = Charts.bar("Q1", "Q2", "Q3", "Q4")
				.series("Solar", 41, 54, 66, 48)
				.series("Wind", 88, 72, 61, 94)
				.series("Hydro", 33, 36, 31, 35)
				.stacked()
				.legendBelow()
				.title("Generation mix", "GWh");
		write("readme-stacked", chart, 640, 400);
	}

	@Test
	void tableInChartOut() throws Exception {
		Random rng = new Random(11);
		String[] regions = { "North", "South", "East", "West" };
		double[] base = { 62, 38, 51, 24 };
		List<List<String>> rows = new ArrayList<>();
		for (int i = 0; i < 4000; i++) {
			int r = rng.nextInt(4);
			rows.add(List.of(regions[r], String.format("%.2f", base[r] + rng.nextGaussian() * 9)));
		}
		ResultSnapshot table = new ResultSnapshot(
				List.of("region", "amount"), List.of("varchar", "numeric"), rows, false);
		Chart chart = Charts.auto(table).title("Revenue by region", "4,000 rows");
		write("readme-auto", chart, 640, 400);
	}

	private static void write(String name, Chart chart, int w, int h) throws Exception {
		BufferedImage img = chart.image(w, h);
		File out = new File("build/" + name + ".png");
		ImageIO.write(img, "png", out);
		assertTrue(out.exists() && out.length() > 0, "readme example written: " + out);
	}
}
