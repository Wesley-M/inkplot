package io.github.wesleym.inkplot;

import io.github.wesleym.inkplot.data.ChartData;
import io.github.wesleym.inkplot.data.Table;
import io.github.wesleym.inkplot.spec.Aggregate;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The name-addressed table factories — the surface a data-analysis-literate developer reaches for first.
 * Columns resolve case-insensitively, refinements ({@code by}/{@code stacked}/{@code points}) rebuild in
 * place, and a typo fails with the table's real names, never a blank chart.
 */
class TableFactoriesTest {

	private static Table table() {
		List<List<String>> rows = new ArrayList<>();
		for (int i = 0; i < 120; i++) {
			rows.add(List.of("region-" + (i % 4), String.valueOf(10 + i % 50),
					"2026-03-" + String.format("%02d", 1 + i % 28), "channel-" + (i % 3)));
		}
		return Table.of(List.of("Region", "Amount", "Created", "Channel"), rows);
	}

	@Test
	void everyFormBuildsFromColumnNames() {
		Table t = table();
		assertInstanceOf(ChartData.Bar.class, Charts.bar(t, "region").data());
		assertInstanceOf(ChartData.Bar.class, Charts.bar(t, "region", "amount").data());
		assertInstanceOf(ChartData.Bar.class, Charts.bar(t, "region", "amount", Aggregate.AVG).data());
		assertInstanceOf(ChartData.Line.class, Charts.line(t, "created", "amount").data());
		assertInstanceOf(ChartData.Scatter.class, Charts.scatter(t, "amount", "amount").data());
		assertInstanceOf(ChartData.Doughnut.class, Charts.doughnut(t, "region").data());
		assertInstanceOf(ChartData.Treemap.class, Charts.treemap(t, "region", "amount").data());
		assertInstanceOf(ChartData.Waffle.class, Charts.waffle(t, "region").data());
		assertInstanceOf(ChartData.Histogram.class, Charts.histogram(t, "amount").data());
		assertInstanceOf(ChartData.Density.class, Charts.density(t, "amount").data());
		assertInstanceOf(ChartData.Box.class, Charts.box(t, "amount", "region").data());
		assertInstanceOf(ChartData.Line.class, Charts.auto(t).data(), "a timestamp + a measure suggests a trend");
	}

	@Test
	void refinementsRebuildInPlace() {
		ChartData.Bar grouped = (ChartData.Bar) Charts.bar(table(), "region", "amount").by("channel").data();
		assertEquals(3, grouped.seriesCount(), "one series per channel");
		ChartData.Bar stacked = (ChartData.Bar) Charts.bar(table(), "region", "amount").by("channel").stacked().data();
		assertTrue(stacked.stacked());
		ChartData.Line lines = (ChartData.Line) Charts.line(table(), "created", "amount").by("region").data();
		assertEquals(4, lines.series().length, "one line per region");
		ChartData.Scatter cohorts = (ChartData.Scatter) Charts.scatter(table(), "amount", "amount").by("channel").data();
		assertEquals(3, cohorts.seriesCount());
	}

	@Test
	void namesResolveCaseInsensitivelyAndTrimmed() {
		ChartData.Bar bar = (ChartData.Bar) Charts.bar(table(), "  REGION  ").data();
		assertEquals(4, bar.categories().length);
	}

	@Test
	void aTypoFailsWithTheTablesRealNames() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> Charts.bar(table(), "reigon"));
		assertTrue(e.getMessage().contains("reigon"), e.getMessage());
		assertTrue(e.getMessage().contains("Region, Amount, Created, Channel"), e.getMessage());
	}

	@Test
	void theNamedChartRendersInk() {
		var img = Charts.bar(table(), "region", "amount").by("channel").stacked()
				.title("Revenue by region").image(320, 200);
		int first = img.getRGB(0, 0);
		boolean ink = false;
		for (int y = 0; y < img.getHeight() && !ink; y += 2) {
			for (int x = 0; x < img.getWidth() && !ink; x += 2) {
				ink = img.getRGB(x, y) != first;
			}
		}
		assertTrue(ink, "named-column chart drew marks");
	}
}
