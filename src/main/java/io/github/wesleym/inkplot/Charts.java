package io.github.wesleym.inkplot;

import io.github.wesleym.inkplot.data.Binning;
import io.github.wesleym.inkplot.data.ChartAuto;
import io.github.wesleym.inkplot.data.ChartBuilder;
import io.github.wesleym.inkplot.data.ChartColumns;
import io.github.wesleym.inkplot.data.ChartData;
import io.github.wesleym.inkplot.data.Provenance;
import io.github.wesleym.inkplot.data.Table;
import io.github.wesleym.inkplot.spec.Aggregate;
import io.github.wesleym.inkplot.spec.ChartSpec;
import io.github.wesleym.inkplot.spec.ChartSpecs;

/**
 * The front door. Each factory takes plain values and returns a {@link Chart} to configure fluently and
 * turn into a live Swing component (or a rendered image):
 *
 * <pre>{@code
 * panel.add(Charts.bar("Q1", "Q2", "Q3")
 *         .series("2025", 41, 54, 62)
 *         .series("2026", 48, 66, 71)
 *         .title("Bookings by quarter")
 *         .component());
 * }</pre>
 *
 * For tabular data (query results, CSVs), wrap the rows in a {@link Table} and address columns by name,
 * the way a data-analysis library would:
 *
 * <pre>{@code
 * Table table = Table.of(List.of("region", "amount"), rows);
 * panel.add(Charts.bar(table, "region", "amount").title("Revenue by region").component());
 * }</pre>
 *
 * {@link #auto} picks the best form for a table's shape; a {@link ChartSpec} via
 * {@link #of(Table, ChartSpec)} drives the pipeline explicitly (series splits, ordering, bin counts).
 */
public final class Charts {

	private Charts() { }

	/** A bar chart over {@code categories}; add series fluently. One series renders without a legend. */
	public static BarChart bar(String... categories) {
		return new BarChart(categories);
	}

	/** A single-series bar chart in one call: {@code values} pair positionally with {@code categories}. */
	public static Chart bar(String[] categories, double[] values) {
		return bar(categories).series("Value", values);
	}

	/** A line chart; add series of (x, y) pairs fluently, with an optional time axis. */
	public static LineChart line() {
		return new LineChart();
	}

	/** A single-series line chart in one call. */
	public static Chart line(double[] x, double[] y) {
		return line().series("Value", x, y);
	}

	/** A scatter plot of one point series. */
	public static Chart scatter(double[] x, double[] y) {
		if (x.length != y.length) {
			throw new IllegalArgumentException(x.length + " x values for " + y.length + " y values");
		}
		return new Chart(new ChartData.Scatter(x.clone(), y.clone(), new int[x.length],
				new String[] { "Points" }, false, Provenance.full(x.length)));
	}

	/** A histogram of raw values, binned automatically. */
	public static Chart histogram(double... values) {
		Binning.Result bins = Binning.bins(values, 0);
		return new Chart(new ChartData.Histogram(bins.edges(), bins.counts(), Provenance.full(values.length)));
	}

	/** A doughnut of named shares — labelled callouts, or a legend when seated {@link Chart#legendBelow() below}. */
	public static Chart doughnut(String[] categories, double[] values) {
		return doughnut(categories, values, "Value");
	}

	/** A doughnut whose centre total and tooltips carry {@code valueLabel} ("Sessions", "GB", …). */
	public static Chart doughnut(String[] categories, double[] values, String valueLabel) {
		return new Chart(new ChartData.Doughnut(categories.clone(), values.clone(), valueLabel,
				Provenance.full(categories.length), null));
	}

	/** A squarified treemap of named magnitudes. */
	public static Chart treemap(String[] categories, double[] values) {
		return treemap(categories, values, "Value");
	}

	/** A treemap whose tooltips carry {@code valueLabel} ("Sessions", "GB", …). */
	public static Chart treemap(String[] categories, double[] values, String valueLabel) {
		return new Chart(new ChartData.Treemap(categories.clone(), values.clone(), valueLabel,
				Provenance.full(categories.length)));
	}

	/** A waffle of named shares — one square per percentage point. */
	public static Chart waffle(String[] categories, double[] values) {
		return waffle(categories, values, "Value");
	}

	/** A waffle whose tooltips carry {@code valueLabel} ("Sessions", "GB", …). */
	public static Chart waffle(String[] categories, double[] values, String valueLabel) {
		return new Chart(new ChartData.Waffle(categories.clone(), values.clone(), valueLabel,
				Provenance.full(categories.length)));
	}

	// ---- charts over a Table, columns addressed by name ------------------------------------------------

	/** A bar per {@code category} value, counting rows; refine with {@code by}/{@code stacked}/{@code horizontal}. */
	public static TableBarChart bar(Table table, String category) {
		return new TableBarChart(table, table.column(category), null, Aggregate.COUNT);
	}

	/** A bar per {@code category} value, summing {@code value}. */
	public static TableBarChart bar(Table table, String category, String value) {
		return bar(table, category, value, Aggregate.SUM);
	}

	/** A bar per {@code category} value, reducing {@code value} with {@code agg}. */
	public static TableBarChart bar(Table table, String category, String value, Aggregate agg) {
		return new TableBarChart(table, table.column(category), table.column(value), agg);
	}

	/** A line of {@code y} over {@code x} (numbers or timestamps); refine with {@code by}/{@code points}. */
	public static TableLineChart line(Table table, String x, String y) {
		return new TableLineChart(table, table.column(x), table.column(y));
	}

	/** Points of {@code y} against {@code x}; colour by another column with {@code by}. */
	public static TableScatterChart scatter(Table table, String x, String y) {
		return new TableScatterChart(table, table.column(x), table.column(y));
	}

	/** Shares of a whole: a slice per {@code category} value, counting rows. */
	public static Chart doughnut(Table table, String category) {
		return of(table, new ChartSpec.Doughnut(table.column(category), null));
	}

	/** Shares of a whole: a slice per {@code category} value, summing {@code value}. */
	public static Chart doughnut(Table table, String category, String value) {
		return of(table, new ChartSpec.Doughnut(table.column(category), table.column(value)));
	}

	/** The same shares as proportional tiles, counting rows (or summing {@code value} in the overload). */
	public static Chart treemap(Table table, String category) {
		return of(table, new ChartSpec.Treemap(table.column(category), null));
	}

	public static Chart treemap(Table table, String category, String value) {
		return of(table, new ChartSpec.Treemap(table.column(category), table.column(value)));
	}

	/** The same shares as a unit grid, counting rows (or summing {@code value} in the overload). */
	public static Chart waffle(Table table, String category) {
		return of(table, new ChartSpec.Waffle(table.column(category), null));
	}

	public static Chart waffle(Table table, String category, String value) {
		return of(table, new ChartSpec.Waffle(table.column(category), table.column(value)));
	}

	/** The distribution of {@code value}, binned automatically. */
	public static Chart histogram(Table table, String value) {
		return of(table, new ChartSpec.Histogram(table.column(value), 0));
	}

	/** The distribution of {@code value} as a smooth density curve. */
	public static Chart density(Table table, String value) {
		return of(table, new ChartSpec.Density(table.column(value), 1.0));
	}

	/** A box-and-whisker summary of {@code value}. */
	public static Chart box(Table table, String value) {
		return of(table, new ChartSpec.Box(table.column(value), null));
	}

	/** A box of {@code value} per {@code group} value. */
	public static Chart box(Table table, String value, String group) {
		return of(table, new ChartSpec.Box(table.column(value), table.column(group)));
	}

	// ---- the power path --------------------------------------------------------------------------------

	/** A chart from data already prepared by the pipeline — the power path. */
	public static Chart of(ChartData data) {
		return new Chart(data);
	}

	/** The best-guess chart for a tabular result: columns are classified, a form is suggested, and built. */
	public static Chart auto(Table snapshot) {
		ChartColumns columns = new ChartColumns(snapshot);
		ChartSpec spec = ChartSpecs.initial(ChartAuto.suggest(columns), columns);
		if (spec == null) {
			throw new IllegalArgumentException("result has no chartable columns");
		}
		return of(snapshot, spec);
	}

	/** A chart built from a tabular result by an explicit {@link ChartSpec} — axes, aggregate, split. */
	public static Chart of(Table snapshot, ChartSpec spec) {
		return new Chart(ChartBuilder.build(spec, snapshot, 0));
	}
}
