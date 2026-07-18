package io.github.wesleym.inkplot.data;

import io.github.wesleym.inkplot.spec.Aggregate;
import io.github.wesleym.inkplot.spec.CategoryOrder;
import io.github.wesleym.inkplot.spec.ChartSpec;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartBuilderTest {

	private static Table snapshot(List<String> columns, List<String> types, List<List<String>> rows) {
		return new Table(columns, types, rows, false);
	}

	@Test
	void barCountsRowsPerCategory() {
		List<List<String>> rows = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			rows.add(List.of("A", "10"));
		}
		for (int i = 0; i < 3; i++) {
			rows.add(List.of("B", "20"));
		}
		Table s = snapshot(List.of("status", "amount"), List.of("varchar", "int"), rows);
		ChartData.Bar bar = (ChartData.Bar) ChartBuilder.build(
				new ChartSpec.Bar(0, null, Aggregate.COUNT, null, false), s, 400);
		assertEquals(2, bar.categories().length);
		assertEquals(5.0, bar.values()[0][index(bar, "A")], 1e-9);
		assertEquals(3.0, bar.values()[0][index(bar, "B")], 1e-9);
	}

	@Test
	void barSumsValuePerCategory() {
		List<List<String>> rows = List.of(
				List.of("A", "10"), List.of("A", "5"), List.of("B", "7"));
		Table s = snapshot(List.of("k", "v"), List.of("varchar", "int"), rows);
		ChartData.Bar bar = (ChartData.Bar) ChartBuilder.build(
				new ChartSpec.Bar(0, 1, Aggregate.SUM, null, false), s, 400);
		assertEquals(15.0, bar.values()[0][index(bar, "A")], 1e-9);
		assertEquals(7.0, bar.values()[0][index(bar, "B")], 1e-9);
	}

	@Test
	void barOrdersCategoriesLargestFirstByDefault() {
		List<List<String>> rows = new ArrayList<>();
		rows.add(List.of("C"));
		for (int i = 0; i < 3; i++) {
			rows.add(List.of("A"));
		}
		for (int i = 0; i < 5; i++) {
			rows.add(List.of("B"));
		}
		Table s = snapshot(List.of("k"), List.of("varchar"), rows);
		ChartData.Bar bar = (ChartData.Bar) ChartBuilder.build(
				new ChartSpec.Bar(0, null, Aggregate.COUNT, null, false), s, 400);
		assertEquals(List.of("B", "A", "C"), List.of(bar.categories()));
	}

	@Test
	void barHonoursTheRequestedCategoryOrder() {
		List<List<String>> rows = List.of(
				List.of("beta", "5"), List.of("alpha", "2"), List.of("gamma", "9"));
		Table s = snapshot(List.of("k", "v"), List.of("varchar", "int"), rows);

		ChartData.Bar asc = (ChartData.Bar) ChartBuilder.build(
				new ChartSpec.Bar(0, 1, Aggregate.SUM, null, false, CategoryOrder.VALUE_ASC), s, 400);
		assertEquals(List.of("alpha", "beta", "gamma"), List.of(asc.categories()), "smallest first");

		ChartData.Bar byLabel = (ChartData.Bar) ChartBuilder.build(
				new ChartSpec.Bar(0, 1, Aggregate.SUM, null, false, CategoryOrder.LABEL), s, 400);
		assertEquals(List.of("alpha", "beta", "gamma"), List.of(byLabel.categories()), "A to Z");

		ChartData.Bar source = (ChartData.Bar) ChartBuilder.build(
				new ChartSpec.Bar(0, 1, Aggregate.SUM, null, false, CategoryOrder.SOURCE), s, 400);
		assertEquals(List.of("beta", "alpha", "gamma"), List.of(source.categories()), "as the data arrived");
		assertEquals(5.0, source.values()[0][0], 1e-9, "values travel with their reordered categories");
	}

	@Test
	void barCarriesTheHorizontalOrientationThrough() {
		List<List<String>> rows = List.of(List.of("A"), List.of("B"));
		Table s = snapshot(List.of("k"), List.of("varchar"), rows);
		ChartData.Bar bar = (ChartData.Bar) ChartBuilder.build(
				new ChartSpec.Bar(0, null, Aggregate.COUNT, null, false, CategoryOrder.VALUE_DESC, true), s, 400);
		assertTrue(bar.horizontal(), "the orientation travels from spec to data");
	}

	@Test
	void barFoldsTailIntoOther() {
		List<List<String>> rows = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			rows.add(List.of("cat" + i));   // 100 distinct categories, far over MAX_CATEGORIES
		}
		Table s = snapshot(List.of("k"), List.of("varchar"), rows);
		ChartData.Bar bar = (ChartData.Bar) ChartBuilder.build(
				new ChartSpec.Bar(0, null, Aggregate.COUNT, null, false), s, 400);
		assertTrue(bar.categories().length <= ChartLimits.MAX_CATEGORIES, "categories are capped");
		assertEquals("Other", bar.categories()[bar.categories().length - 1], "the tail folds into Other");
	}

	@Test
	void barFoldKeepsTheBiggestCategoryEvenWhenItIsRare() {
		// The reported bug: "Sum of count" where a category appears in ONE row with a huge value was folded into
		// "Other" for being infrequent. 20 small categories (5 rows each) + one huge (a single row, value 1000):
		// the fold must rank by VALUE, so "huge" survives and leads, never dropped into "Other".
		List<List<String>> rows = new ArrayList<>();
		rows.add(List.of("huge", "1000"));
		for (int i = 0; i < 20; i++) {
			for (int j = 0; j < 5; j++) {
				rows.add(List.of("small" + i, "1"));
			}
		}
		Table s = snapshot(List.of("k", "v"), List.of("varchar", "int"), rows);
		ChartData.Bar bar = (ChartData.Bar) ChartBuilder.build(
				new ChartSpec.Bar(0, 1, Aggregate.SUM, null, false, CategoryOrder.VALUE_DESC), s, 4000);
		assertTrue(List.of(bar.categories()).contains("huge"), "the rare-but-biggest category must survive the fold");
		assertEquals("huge", bar.categories()[0], "and rank first under largest-first");
		assertEquals(1000.0, bar.values()[0][0], 1e-9);
	}

	@Test
	void barOtherBucketAveragesFoldedRowsNotPerCategoryAverages() {
		// AVG over "Other" must average the FOLDED ROWS, not the folded categories' averages. 15 big categories are
		// kept; "lowA"=[2,4] and "lowB"=[6] fold — Other = avg{2,4,6} = 4, not avg{avg(2,4)=3, avg(6)=6} = 4.5.
		List<List<String>> rows = new ArrayList<>();
		for (int i = 0; i < 15; i++) {
			rows.add(List.of("hi" + i, "100"));
		}
		rows.add(List.of("lowA", "2"));
		rows.add(List.of("lowA", "4"));
		rows.add(List.of("lowB", "6"));
		Table s = snapshot(List.of("k", "v"), List.of("varchar", "int"), rows);
		ChartData.Bar bar = (ChartData.Bar) ChartBuilder.build(
				new ChartSpec.Bar(0, 1, Aggregate.AVG, null, false, CategoryOrder.VALUE_DESC), s, 400);
		assertEquals(4.0, bar.values()[0][index(bar, "Other")], 1e-9,
				"Other averages the folded rows, combining raw accumulators");
	}

	@Test
	void statCountOverATruncatedResultIsFlagged() {
		// A COUNT over a source that was already capped upstream must NOT present itself as the complete total.
		Table truncated = new Table(List.of("k"), List.of("varchar"),
				List.of(List.of("a"), List.of("b"), List.of("c")), true);
		ChartData.Stat stat = (ChartData.Stat) ChartBuilder.build(new ChartSpec.Stat(null, Aggregate.COUNT),
				truncated, 400);
		assertEquals(3.0, stat.value(), 1e-9);
		assertTrue(stat.provenance().hasNotice(), "a COUNT of a truncated sample must be flagged, not shown as total");
	}

	@Test
	void stackedBarsRejectNegativeValues() {
		// Stacking grows from zero, so a negative segment would silently corrupt the stack — reject it instead.
		List<List<String>> rows = List.of(
				List.of("a", "x", "10"), List.of("a", "y", "-3"), List.of("b", "x", "5"));
		Table s = snapshot(List.of("cat", "ser", "v"), List.of("varchar", "varchar", "int"), rows);
		assertThrows(ChartDataException.class, () -> ChartBuilder.build(
				new ChartSpec.Bar(0, 2, Aggregate.SUM, 1, true), s, 400), "stacked + negative must be rejected");
	}

	@Test
	void barRefusesRunawayCardinality() {
		List<List<String>> rows = new ArrayList<>();
		for (int i = 0; i < ChartLimits.MAX_CATEGORY_SCAN + 50; i++) {
			rows.add(List.of("id" + i));
		}
		Table s = snapshot(List.of("id"), List.of("varchar"), rows);
		assertThrows(ChartDataException.class, () -> ChartBuilder.build(
				new ChartSpec.Bar(0, null, Aggregate.COUNT, null, false), s, 400));
	}

	@Test
	void doughnutCountsRowsAndOrdersSlicesLargestFirst() {
		List<List<String>> rows = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			rows.add(List.of("A"));
		}
		for (int i = 0; i < 5; i++) {
			rows.add(List.of("B"));
		}
		rows.add(List.of("C"));
		Table s = snapshot(List.of("status"), List.of("varchar"), rows);
		ChartData.Doughnut d = (ChartData.Doughnut) ChartBuilder.build(new ChartSpec.Doughnut(0, null), s, 400);
		assertEquals(List.of("B", "A", "C"), List.of(d.categories()), "slices scan largest-first");
		assertEquals(5.0, d.values()[0], 1e-9);
		assertEquals(9.0, d.total(), 1e-9);
		assertEquals("Count", d.valueLabel());
	}

	@Test
	void doughnutSumsMeasureAndDropsZeroSlices() {
		List<List<String>> rows = List.of(
				List.of("A", "10"), List.of("A", "5"), List.of("B", "7"), List.of("C", "0"));
		Table s = snapshot(List.of("k", "v"), List.of("varchar", "int"), rows);
		ChartData.Doughnut d = (ChartData.Doughnut) ChartBuilder.build(new ChartSpec.Doughnut(0, 1), s, 400);
		assertEquals(List.of("A", "B"), List.of(d.categories()), "the zero slice carries no share and drops");
		assertEquals(15.0, d.values()[0], 1e-9);
		assertEquals("v", d.valueLabel());
	}

	@Test
	void doughnutFoldsTailIntoOtherPinnedLast() {
		List<List<String>> rows = new ArrayList<>();
		for (int cat = 0; cat < ChartLimits.MAX_DOUGHNUT_SLICES + 5; cat++) {
			for (int i = 0; i <= cat; i++) {
				rows.add(List.of("cat" + cat));   // cat0 appears once, cat14 fifteen times
			}
		}
		Table s = snapshot(List.of("k"), List.of("varchar"), rows);
		ChartData.Doughnut d = (ChartData.Doughnut) ChartBuilder.build(new ChartSpec.Doughnut(0, null), s, 400);
		assertEquals(ChartLimits.MAX_DOUGHNUT_SLICES, d.categories().length, "slices are capped");
		assertEquals("Other", d.categories()[d.categories().length - 1], "the folded tail sits last");
		assertEquals(rows.size(), d.total(), 1e-9, "every row lands in some slice");
	}

	@Test
	void doughnutFoldKeepsTheLargestSharesWhateverTheRowOrder() {
		// Pre-aggregated rows ordered smallest-first (ORDER BY count ASC): every category appears once, so a
		// frequency-based fold would keep the tiny leading slices and bury the giants in "Other".
		List<List<String>> rows = new ArrayList<>();
		int categories = ChartLimits.MAX_DOUGHNUT_SLICES + 5;
		for (int cat = 0; cat < categories; cat++) {
			rows.add(List.of("cat" + cat, String.valueOf(cat + 1)));   // cat0 smallest, last cat biggest
		}
		Table s = snapshot(List.of("k", "n"), List.of("varchar", "int"), rows);
		ChartData.Doughnut d = (ChartData.Doughnut) ChartBuilder.build(new ChartSpec.Doughnut(0, 1), s, 400);
		assertEquals("cat" + (categories - 1), d.categories()[0], "the biggest share leads");
		assertEquals("Other", d.categories()[d.categories().length - 1]);
		double other = d.values()[d.values().length - 1];
		double biggest = d.values()[0];
		assertEquals(categories, biggest, 1e-9, "the giants are kept as their own slices, not folded");
		assertEquals(1 + 2 + 3 + 4 + 5 + 6, other, 1e-9, "only the smallest tail folds into Other");
	}

	@Test
	void treemapSharesTheDoughnutPipelineWithItsOwnTileCap() {
		List<List<String>> rows = new ArrayList<>();
		for (int cat = 0; cat < ChartLimits.MAX_TREEMAP_TILES + 4; cat++) {
			rows.add(List.of("cat" + cat, String.valueOf(cat + 1)));
		}
		Table s = snapshot(List.of("k", "n"), List.of("varchar", "int"), rows);
		ChartData.Treemap t = (ChartData.Treemap) ChartBuilder.build(new ChartSpec.Treemap(0, 1), s, 400);
		assertEquals(ChartLimits.MAX_TREEMAP_TILES, t.categories().length, "tiles are capped");
		assertEquals("cat" + (ChartLimits.MAX_TREEMAP_TILES + 3), t.categories()[0], "the biggest share leads");
		assertEquals("Other", t.categories()[t.categories().length - 1], "the folded tail sits last");
	}

	@Test
	void waffleSharesTheDoughnutPipelineButAdmitsMoreCategories() {
		List<List<String>> rows = new ArrayList<>();
		for (int cat = 0; cat < 13; cat++) {
			for (int i = 0; i <= cat; i++) {
				rows.add(List.of("cat" + cat));
			}
		}
		Table s = snapshot(List.of("k"), List.of("varchar"), rows);
		ChartData.Waffle waffle = (ChartData.Waffle) ChartBuilder.build(new ChartSpec.Waffle(0, null), s, 400);
		assertEquals(13, waffle.categories().length, "thirteen groups fit under the waffle cap unfolded");
		assertEquals("cat12", waffle.categories()[0], "slices scan largest-first");
		assertEquals(rows.size(), waffle.total(), 1e-9);

		ChartData.Doughnut doughnut = (ChartData.Doughnut) ChartBuilder.build(new ChartSpec.Doughnut(0, null), s, 400);
		assertEquals(ChartLimits.MAX_DOUGHNUT_SLICES, doughnut.categories().length,
				"the same data folds at the doughnut's tighter cap");
	}

	@Test
	void shareChartsHonourTheRequestedFoldPoint() {
		List<List<String>> rows = new ArrayList<>();
		for (int cat = 0; cat < 12; cat++) {
			for (int i = 0; i <= cat; i++) {
				rows.add(List.of("cat" + cat));
			}
		}
		Table s = snapshot(List.of("k"), List.of("varchar"), rows);

		ChartData.Doughnut five = (ChartData.Doughnut) ChartBuilder.build(new ChartSpec.Doughnut(0, null, 5), s, 400);
		assertEquals(5, five.categories().length, "the requested fold point wins over the default");
		assertEquals("Other", five.categories()[4], "the tail still folds last");
		assertEquals(rows.size(), five.total(), 1e-9, "every row lands in some slice");

		ChartData.Waffle floor = (ChartData.Waffle) ChartBuilder.build(new ChartSpec.Waffle(0, null, 1), s, 400);
		assertEquals(2, floor.categories().length, "a degenerate request floors at two categories");

		ChartData.Doughnut auto = (ChartData.Doughnut) ChartBuilder.build(new ChartSpec.Doughnut(0, null, 0), s, 400);
		assertEquals(ChartLimits.MAX_DOUGHNUT_SLICES, auto.categories().length, "0 keeps the chart's default cap");
	}

	@Test
	void doughnutRefusesNegativeTotals() {
		List<List<String>> rows = List.of(List.of("A", "10"), List.of("B", "-4"));
		Table s = snapshot(List.of("k", "v"), List.of("varchar", "int"), rows);
		assertThrows(ChartDataException.class,
				() -> ChartBuilder.build(new ChartSpec.Doughnut(0, 1), s, 400),
				"negative slice totals cannot be shares of a whole");
	}

	@Test
	void scatterCapsPointCountAndReportsIt() {
		List<List<String>> rows = new ArrayList<>();
		for (int i = 0; i < ChartLimits.MAX_SCATTER_POINTS + 5000; i++) {
			rows.add(List.of(String.valueOf(i), String.valueOf(i * 2)));
		}
		Table s = snapshot(List.of("x", "y"), List.of("int", "int"), rows);
		ChartData.Scatter scatter = (ChartData.Scatter) ChartBuilder.build(
				new ChartSpec.Scatter(0, 1, null), s, 400);
		assertTrue(scatter.x().length <= ChartLimits.MAX_SCATTER_POINTS, "points are capped");
		assertTrue(scatter.provenance().capNote() != null, "the cap is reported in the provenance");
	}

	@Test
	void scatterSeriesComeOnlyFromPlottedRows() {
		List<List<String>> rows = List.of(
				List.of("no-x", "1", "Ghost"),
				List.of("1", "10", "A"),
				List.of("2", "no-y", "Skipped"),
				List.of("3", "30", "A"),
				List.of("4", "40", "C"));
		Table s = snapshot(List.of("x", "y", "cohort"), List.of("int", "int", "varchar"), rows);

		ChartData.Scatter scatter = (ChartData.Scatter) ChartBuilder.build(new ChartSpec.Scatter(0, 1, 2), s, 400);

		assertEquals(List.of("A", "C"), List.of(scatter.seriesNames()),
				"invalid x/y rows must not create legend entries or shift colours");
		assertArrayEquals(new int[] { 0, 0, 1 }, scatter.series(), "series ids stay aligned with plotted points");
		assertEquals(2, scatter.provenance().skippedCells(), "the rejected coordinate pairs are still reported");
	}

	@Test
	void lineRawSortsPointsByX() {
		List<List<String>> rows = List.of(
				List.of("3", "30"), List.of("1", "10"), List.of("2", "20"));
		Table s = snapshot(List.of("x", "y"), List.of("int", "int"), rows);
		ChartData.Line line = (ChartData.Line) ChartBuilder.build(new ChartSpec.Line(0, 1, null, null), s, 400);
		double[] x = line.series()[0].x();
		assertEquals(1.0, x[0], 1e-9);
		assertEquals(2.0, x[1], 1e-9);
		assertEquals(3.0, x[2], 1e-9);
	}

	@Test
	void boxGroupsByCategory() {
		List<List<String>> rows = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			rows.add(List.of("A", String.valueOf(i)));
			rows.add(List.of("B", String.valueOf(i + 100)));
		}
		Table s = snapshot(List.of("grp", "v"), List.of("varchar", "int"), rows);
		ChartData.Box box = (ChartData.Box) ChartBuilder.build(new ChartSpec.Box(1, 0), s, 400);
		assertEquals(2, box.groups().length);
	}

	@Test
	void skippedCellsAreCounted() {
		List<List<String>> rows = List.of(
				List.of("A", "10"), List.of("A", "not-a-number"), List.of("B", "5"));
		Table s = snapshot(List.of("k", "v"), List.of("varchar", "varchar"), rows);
		ChartData.Bar bar = (ChartData.Bar) ChartBuilder.build(
				new ChartSpec.Bar(0, 1, Aggregate.SUM, null, false), s, 400);
		assertEquals(1, bar.provenance().skippedCells(), "the unparseable value is counted as skipped");
	}

	private static int index(ChartData.Bar bar, String category) {
		for (int i = 0; i < bar.categories().length; i++) {
			if (bar.categories()[i].equals(category)) {
				return i;
			}
		}
		throw new AssertionError("category not found: " + category);
	}
}
