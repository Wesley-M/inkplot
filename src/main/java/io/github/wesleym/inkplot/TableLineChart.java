package io.github.wesleym.inkplot;

import io.github.wesleym.inkplot.data.ChartBuilder;
import io.github.wesleym.inkplot.data.Table;
import io.github.wesleym.inkplot.spec.Aggregate;
import io.github.wesleym.inkplot.spec.ChartSpec;

/**
 * A line chart over a table (X may be numbers or timestamps — timestamps get calendar ticks). Values
 * sharing an X bucket are summed by default; {@link #points()} plots the raw rows instead, and
 * {@link #by} splits into one line per value of another column.
 */
public final class TableLineChart extends Chart {

	private final Table table;
	private final int x;
	private final int y;
	private Integer series;
	private Aggregate agg = Aggregate.SUM;

	TableLineChart(Table table, int x, int y) {
		super(null);
		this.table = table;
		this.x = x;
		this.y = y;
		rebuild();
	}

	/** One line per value of the named column. */
	public TableLineChart by(String seriesColumn) {
		this.series = table.column(seriesColumn);
		rebuild();
		return this;
	}

	/** Plots the raw rows instead of summing values that share an X bucket. */
	public TableLineChart points() {
		this.agg = null;
		rebuild();
		return this;
	}

	private void rebuild() {
		setData(ChartBuilder.build(new ChartSpec.Line(x, y, series, agg), table, 0));
	}
}
