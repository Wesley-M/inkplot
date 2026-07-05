package io.github.wesleym.inkplot;

import io.github.wesleym.inkplot.data.ChartBuilder;
import io.github.wesleym.inkplot.data.Table;
import io.github.wesleym.inkplot.spec.Aggregate;
import io.github.wesleym.inkplot.spec.CategoryOrder;
import io.github.wesleym.inkplot.spec.ChartSpec;

/**
 * A bar chart over a table, refining fluently: {@link #by} splits each category into one bar per value
 * of another column, {@link #stacked()} piles the split into one bar, {@link #horizontal()} lays long
 * category names sideways.
 */
public final class TableBarChart extends Chart {

	private final Table table;
	private final int category;
	private final Integer value;
	private final Aggregate agg;
	private Integer series;
	private boolean stacked;
	private boolean horizontal;

	TableBarChart(Table table, int category, Integer value, Aggregate agg) {
		super(null);
		this.table = table;
		this.category = category;
		this.value = value;
		this.agg = agg;
		rebuild();
	}

	/** Splits each category by the named column — one coloured bar (or stack segment) per value. */
	public TableBarChart by(String seriesColumn) {
		this.series = table.column(seriesColumn);
		rebuild();
		return this;
	}

	/** One stacked bar per category instead of grouped bars. */
	public TableBarChart stacked() {
		this.stacked = true;
		rebuild();
		return this;
	}

	/** Horizontal bars: category labels run down the left gutter at full width. */
	public TableBarChart horizontal() {
		this.horizontal = true;
		rebuild();
		return this;
	}

	private void rebuild() {
		setData(ChartBuilder.build(
				new ChartSpec.Bar(category, value, agg, series, stacked, CategoryOrder.VALUE_DESC, horizontal),
				table, 0));
	}
}
