package io.github.wesleym.inkplot.data;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartColumnsTest {

	private static Table snapshot(List<String> columns, List<String> types, List<List<String>> rows) {
		return new Table(columns, types, rows, false);
	}

	@Test
	void classifiesFromDeclaredTypes() {
		Table s = snapshot(
				List.of("id", "name", "created", "amount"),
				List.of("int4", "varchar", "timestamp", "numeric"),
				List.of(List.of("1", "Ann", "2026-01-01 00:00:00", "9.5")));
		ChartColumns c = new ChartColumns(s);
		assertTrue(c.isNumeric(0), "int is numeric");
		assertTrue(c.isCategorical(1), "varchar is category-like");
		assertTrue(c.isTemporal(2), "timestamp is temporal");
		assertTrue(c.isNumeric(3), "numeric is numeric");
		assertFalse(c.isTemporal(0), "an int is not temporal");
	}

	@Test
	void sniffsUntypedColumns() {
		List<List<String>> rows = new ArrayList<>();
		for (int i = 0; i < 50; i++) {
			rows.add(List.of(String.valueOf(i * 3), "2026-02-" + String.format("%02d", 1 + i % 27), "label" + i));
		}
		// No declared types (untyped sources return none) — roles must come from value sniffing.
		Table s = snapshot(List.of("n", "d", "t"), List.of("", "", ""), rows);
		ChartColumns c = new ChartColumns(s);
		assertTrue(c.isNumeric(0), "all-numeric column sniffs numeric");
		assertTrue(c.isTemporal(1), "date-shaped column sniffs temporal");
		assertTrue(c.isCategorical(2), "free text sniffs categorical");
	}

	@Test
	void highCardinalityNumericIsNotOfferedAsCategory() {
		List<List<String>> rows = new ArrayList<>();
		for (int i = 0; i < 300; i++) {
			rows.add(List.of(String.valueOf(i)));   // 300 distinct numbers
		}
		Table s = snapshot(List.of("x"), List.of(""), rows);
		ChartColumns c = new ChartColumns(s);
		assertTrue(c.isNumeric(0));
		assertFalse(c.isCategorical(0), "a high-cardinality numeric is not a category axis");
	}
}
