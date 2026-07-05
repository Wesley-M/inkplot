package io.github.wesleym.inkplot;

import io.github.wesleym.inkplot.data.ChartBuilder;
import io.github.wesleym.inkplot.data.ChartColumns;
import io.github.wesleym.inkplot.data.ChartData;
import io.github.wesleym.inkplot.data.ResultSnapshot;
import io.github.wesleym.inkplot.spec.Aggregate;
import io.github.wesleym.inkplot.spec.ChartSpec;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end check that real string result rows flow through the data pipeline into a rendered chart: builds a
 * snapshot of untyped string rows, prepares each chart type with {@link ChartBuilder}, and renders it — the seam most
 * likely to break (parsing, aggregation, provenance) exercised for real, not from hand-built data.
 */
class ChartIntegrationPreviewTest {

	@Test
	void realRowsBuildAndRender() throws Exception {
		ChartTheme theme = ChartTheme.PAPER;
		ResultSnapshot orders = orders();

		ChartData bar = ChartBuilder.build(new ChartSpec.Bar(1, 2, Aggregate.SUM, null, false), orders, 640);
		ChartRenderPreviewTest.render(new ChartRenderPreviewTest.NamedTheme("light", theme), "integ-bar", bar);

		ChartData doughnut = ChartBuilder.build(new ChartSpec.Doughnut(1, 2), orders, 640);
		ChartRenderPreviewTest.render(new ChartRenderPreviewTest.NamedTheme("light", theme), "integ-doughnut", doughnut);

		ChartData line = ChartBuilder.build(new ChartSpec.Line(0, 2, null, null), orders, 640);
		ChartRenderPreviewTest.render(new ChartRenderPreviewTest.NamedTheme("light", theme), "integ-line", line);

		ChartData histogram = ChartBuilder.build(new ChartSpec.Histogram(2, 0), orders, 640);
		ChartRenderPreviewTest.render(new ChartRenderPreviewTest.NamedTheme("light", theme), "integ-histogram", histogram);

		// A truncated result should surface a provenance notice rather than pretend to be complete.
		assertTrue(bar.provenance() != null, "provenance present");
	}

	@Test
	void overPreciseTimestampColumnPlotsAsTimeSeries() throws Exception {
		ChartTheme theme = ChartTheme.PAPER;
		String[][] raw = {
				{ "2024-12-14 16:29:06.97300000000000000000000000000000000", "156" },
				{ "2024-12-14 17:03:11.02300000000000000000000000000000000", "39" },
				{ "2024-12-14 17:19:48.61200000000000000000000000000000000", "234" },
				{ "2024-12-14 17:33:33.95800000000000000000000000000000000", "104" },
				{ "2024-12-14 18:04:19.53900000000000000000000000000000000", "397" },
				{ "2024-12-14 18:22:47.89300000000000000000000000000000000", "186" },
				{ "2024-12-14 18:34:34.29300000000000000000000000000000000", "211" },
				{ "2024-12-14 18:45:03.21700000000000000000000000000000000", "48" },
				{ "2024-12-14 18:58:33.91300000000000000000000000000000000", "45" },
				{ "2024-12-14 19:07:00.12700000000000000000000000000000000", "109" },
				{ "2024-12-14 19:51:50.04200000000000000000000000000000000", "86" },
				{ "2024-12-14 20:14:30.42300000000000000000000000000000000", "214" },
				{ "2024-12-14 21:13:24.60800000000000000000000000000000000", "280" },
				{ "2024-12-14 21:50:11.57500000000000000000000000000000000", "48" } };
		List<List<String>> rows = new ArrayList<>();
		for (String[] r : raw) {
			rows.add(List.of(r[0], r[1]));
		}
		// Untyped columns (as string-only sources return them) — the timestamp must sniff as temporal despite the precision.
		ResultSnapshot s = new ResultSnapshot(List.of("ts", "count"), List.of("", ""), rows, false);
		ChartColumns cols = new ChartColumns(s);
		assertTrue(cols.isTemporal(0), "the over-precise timestamp column classifies as temporal");

		ChartData.Line line = (ChartData.Line) ChartBuilder.build(new ChartSpec.Line(0, 1, null, null), s, 640);
		assertTrue(line.timeAxis(), "plotted on a time axis");
		assertEquals(raw.length, line.series()[0].x().length, "every row plotted, none dropped");
		ChartRenderPreviewTest.render(new ChartRenderPreviewTest.NamedTheme("light", theme), "integ-precise-time", line);
	}

	// A realistic untyped result: date, region, amount as plain strings (as CSV or string-only JDBC hands them over).
	private static ResultSnapshot orders() {
		Random rng = new Random(11);
		String[] regions = { "North", "South", "East", "West" };
		List<List<String>> rows = new ArrayList<>();
		long day = 24L * 60 * 60 * 1000;
		long start = java.time.Instant.parse("2026-01-01T00:00:00Z").toEpochMilli();
		for (int i = 0; i < 4000; i++) {
			String date = java.time.Instant.ofEpochMilli(start + (i % 60) * day).toString();
			String region = regions[rng.nextInt(regions.length)];
			String amount = String.format("%.2f", Math.abs(rng.nextGaussian() * 40 + 120));
			rows.add(List.of(date, region, amount));
		}
		return new ResultSnapshot(List.of("created", "region", "amount"),
				List.of("", "", ""), rows, true);
	}
}
