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
public record ResultSnapshot(List<String> columns, List<String> types, List<List<String>> rows,
		boolean truncated) {

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
}
