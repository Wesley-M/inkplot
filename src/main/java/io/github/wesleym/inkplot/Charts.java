package io.github.wesleym.inkplot;

import io.github.wesleym.inkplot.data.Binning;
import io.github.wesleym.inkplot.data.ChartAuto;
import io.github.wesleym.inkplot.data.ChartBuilder;
import io.github.wesleym.inkplot.data.ChartColumns;
import io.github.wesleym.inkplot.data.ChartData;
import io.github.wesleym.inkplot.data.Provenance;
import io.github.wesleym.inkplot.data.ResultSnapshot;
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
 * For tabular data (query results, CSVs), wrap it in a {@link ResultSnapshot} and let {@link #auto} pick
 * the best form, or drive the full pipeline with a {@link ChartSpec} via {@link #of(ResultSnapshot, ChartSpec)}.
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

	/** A chart from data already prepared by the pipeline — the power path. */
	public static Chart of(ChartData data) {
		return new Chart(data);
	}

	/** The best-guess chart for a tabular result: columns are classified, a form is suggested, and built. */
	public static Chart auto(ResultSnapshot snapshot) {
		ChartColumns columns = new ChartColumns(snapshot);
		ChartSpec spec = ChartSpecs.initial(ChartAuto.suggest(columns), columns);
		if (spec == null) {
			throw new IllegalArgumentException("result has no chartable columns");
		}
		return of(snapshot, spec);
	}

	/** A chart built from a tabular result by an explicit {@link ChartSpec} — axes, aggregate, split. */
	public static Chart of(ResultSnapshot snapshot, ChartSpec spec) {
		return new Chart(ChartBuilder.build(spec, snapshot, 0));
	}
}
