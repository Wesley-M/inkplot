package io.github.wesleym.inkplot.data;

import java.awt.Color;

/**
 * The prepared, ready-to-draw form of a chart — one immutable record per chart type, holding only compact primitive
 * arrays (never a copy of the source string rows). The data pipeline produces one of these off the EDT; the matching
 * renderer consumes it. Every variant carries its {@link Provenance} so the view can state what it covers.
 */
public sealed interface ChartData
		permits ChartData.Bar, ChartData.Doughnut, ChartData.Waffle, ChartData.Treemap, ChartData.Line,
		ChartData.Histogram,
		ChartData.Density, ChartData.Scatter, ChartData.Box {

	Provenance provenance();

	/**
	 * Grouped or stacked bars. {@code values[s][c]} is series {@code s}'s value in category {@code c}; a single
	 * series ({@code seriesNames.length == 1}) is the common case and draws without a legend. When
	 * {@code colorByCategory} is set the (single-series) bars are coloured by their category instead of one hue — the
	 * "colour each bar by its own name" case — so each category is a full-width, distinctly-coloured bar.
	 * {@code horizontal} lays the category band down the Y axis with the value axis on X.
	 */
	record Bar(String[] categories, String[] seriesNames, double[][] values, boolean stacked, boolean colorByCategory,
			boolean horizontal, Provenance provenance) implements ChartData {

		public Bar(String[] categories, String[] seriesNames, double[][] values, boolean stacked,
				boolean colorByCategory, Provenance prov) {
			this(categories, seriesNames, values, stacked, colorByCategory, false, prov);
		}

		public Bar(String[] categories, String[] seriesNames, double[][] values, boolean stacked, Provenance prov) {
			this(categories, seriesNames, values, stacked, false, false, prov);
		}

		public int seriesCount() {
			return seriesNames.length;
		}
	}

	/**
	 * Shares of a whole: one positive value per category, ordered largest-first with any folded "Other" bucket last,
	 * drawn as ring slices. {@code valueLabel} names what the slices measure ("Count" or the summed column) for the
	 * slice tooltips and the centre read-out. {@code sliceColors} carries semantic per-slice colours where the
	 * categories have inherent meaning (a boolean's true/false); {@code null} — the whole array or one entry —
	 * falls back to the theme's series palette.
	 */
	record Doughnut(String[] categories, double[] values, String valueLabel, Provenance provenance,
			Color[] sliceColors) implements ChartData {

		public Doughnut(String[] categories, double[] values, String valueLabel, Provenance provenance) {
			this(categories, values, valueLabel, provenance, null);
		}

		public double total() {
			double sum = 0;
			for (double v : values) {
				sum += v;
			}
			return sum;
		}
	}

	/** The same shares of a whole, drawn as a unit grid ({@code Waffle} and {@code Doughnut} share the builder). */
	record Waffle(String[] categories, double[] values, String valueLabel, Provenance provenance)
			implements ChartData {

		public double total() {
			double sum = 0;
			for (double v : values) {
				sum += v;
			}
			return sum;
		}
	}

	/** The same shares of a whole, drawn as squarified proportional tiles (shares the doughnut's builder). */
	record Treemap(String[] categories, double[] values, String valueLabel, Provenance provenance)
			implements ChartData {

		public double total() {
			double sum = 0;
			for (double v : values) {
				sum += v;
			}
			return sum;
		}
	}

	/** One or more line series over a shared continuous or time X. */
	record Line(Series[] series, boolean timeAxis, Provenance provenance) implements ChartData {
		/** A single line: its name and parallel X/Y arrays (X in epoch-millis when the chart's axis is time). */
		public record Series(String name, double[] x, double[] y) { }
	}

	/** A binned distribution of one numeric column: {@code edges} has one more entry than {@code counts}. */
	record Histogram(double[] edges, long[] counts, Provenance provenance) implements ChartData { }

	/** A smoothed 1-D density (KDE) of one numeric column, evaluated on a regular grid over {@code [min, max]}. */
	record Density(double[] gridX, double[] density, double min, double max, int sampleN, Provenance provenance)
			implements ChartData { }

	/** Points over two numeric (or time-X) axes, coloured by an optional series id per point. */
	record Scatter(double[] x, double[] y, int[] series, String[] seriesNames, boolean timeAxisX, Provenance provenance)
			implements ChartData {
		public int seriesCount() {
			return seriesNames.length;
		}
	}

	/** Five-number summaries per group for a box-and-whisker plot. */
	record Box(Group[] groups, Provenance provenance) implements ChartData {
		public record Group(String label, double lo, double q1, double median, double q3, double hi,
				double[] outliers) { }
	}
}
