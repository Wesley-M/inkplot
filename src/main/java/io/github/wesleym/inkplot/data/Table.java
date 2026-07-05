package io.github.wesleym.inkplot.data;

import java.util.List;

/**
 * A read-only view of the tabular result a chart draws from: column names, declared type names (may be blank
 * — values are sniffed then), the string rows as-fetched, and whether the result was truncated upstream (so
 * the chart can say it shows a sample). It is the single seam between the library and whatever produced the
 * data — a JDBC grid, a CSV, an in-memory table — and holds references, never copies, so building it is cheap.
 *
 * @param columns   the column names, in row order
 * @param types     declared type names parallel to {@code columns} (blank/null entries are sniffed), or null
 * @param rows      the data as fetched: one string list per row
 * @param truncated whether the source was capped upstream, so charts present themselves as a sample
 */
public record Table(List<String> columns, List<String> types, List<List<String>> rows,
		boolean truncated) {

	/** The common case: no declared types (values are sniffed from the rows) and nothing truncated upstream. */
	public static Table of(List<String> columns, List<List<String>> rows) {
		return new Table(columns, null, rows, false);
	}

	public int columnCount() {
		return columns.size();
	}

	public int rowCount() {
		return rows.size();
	}

	public String columnName(int col) {
		return col >= 0 && col < columns.size() ? columns.get(col) : "col" + col;
	}

	public String columnType(int col) {
		return types != null && col >= 0 && col < types.size() ? types.get(col) : "";
	}

	/**
	 * The index of the named column, matched case-insensitively and trimmed. A miss fails fast with the
	 * table's real column names, so a typo reads as a typo — never as a blank chart.
	 */
	public int column(String name) {
		for (int i = 0; i < columns.size(); i++) {
			if (columns.get(i).equalsIgnoreCase(name.trim())) {
				return i;
			}
		}
		throw new IllegalArgumentException(
				"no column named '" + name + "' — this table has: " + String.join(", ", columns));
	}
}
