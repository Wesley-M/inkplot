package io.github.wesleym.inkplot;

import io.github.wesleym.inkplot.data.ChartBuilder;
import io.github.wesleym.inkplot.data.Table;
import io.github.wesleym.inkplot.spec.ChartSpec;

/** A scatter plot over a table; {@link #by} colours the points by another column's values. */
public final class TableScatterChart extends Chart {

	private final Table table;
	private final int x;
	private final int y;
	private Integer series;

	TableScatterChart(Table table, int x, int y) {
		super(null);
		this.table = table;
		this.x = x;
		this.y = y;
		rebuild();
	}

	/** Colours the points by the named column — one palette slot per value. */
	public TableScatterChart by(String seriesColumn) {
		this.series = table.column(seriesColumn);
		rebuild();
		return this;
	}

	private void rebuild() {
		setData(ChartBuilder.build(new ChartSpec.Scatter(x, y, series), table, 0));
	}
}
