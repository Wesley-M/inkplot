package io.github.wesleym.inkplot;

import io.github.wesleym.inkplot.data.ChartData;
import io.github.wesleym.inkplot.data.Provenance;

import java.util.ArrayList;
import java.util.List;

/** A bar chart accumulating series — grouped by default, {@link #stacked()} or {@link #horizontal()} on request. */
public final class BarChart extends Chart {

	private final String[] categories;
	private final List<String> names = new ArrayList<>();
	private final List<double[]> values = new ArrayList<>();
	private boolean stacked;
	private boolean horizontal;
	private boolean colorByCategory;

	BarChart(String[] categories) {
		super(null);
		this.categories = categories.clone();
		rebuild();
	}

	/** Adds one named series; {@code values} pair positionally with the categories. */
	public BarChart series(String name, double... values) {
		if (values.length != categories.length) {
			throw new IllegalArgumentException("series '" + name + "' has " + values.length
					+ " values for " + categories.length + " categories");
		}
		names.add(name);
		this.values.add(values.clone());
		rebuild();
		return this;
	}

	/** Stacks the series into one bar per category instead of grouping them side by side. */
	public BarChart stacked() {
		this.stacked = true;
		rebuild();
		return this;
	}

	/** Horizontal bars: category labels run down the left gutter at full width. */
	public BarChart horizontal() {
		this.horizontal = true;
		rebuild();
		return this;
	}

	/** One identity colour per category (for a single series over a small closed set) instead of per series. */
	public BarChart colorByCategory() {
		this.colorByCategory = true;
		rebuild();
		return this;
	}

	private void rebuild() {
		String[] seriesNames = names.isEmpty() ? new String[] { "Value" } : names.toArray(String[]::new);
		double[][] series = values.isEmpty() ? new double[][] { new double[categories.length] }
				: values.toArray(double[][]::new);
		setData(new ChartData.Bar(categories, seriesNames, series, stacked, colorByCategory, horizontal,
				Provenance.full(categories.length)));
	}
}
