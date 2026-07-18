package io.github.wesleym.inkplot.data;

import java.awt.Color;

/**
 * The prepared, ready-to-draw form of a chart — one immutable record per chart type, holding only compact primitive
 * arrays (never a copy of the source string rows). The data pipeline produces one of these off the EDT; the matching
 * renderer consumes it. Every variant carries its {@link Provenance} so the view can state what it covers.
 */
public sealed interface ChartData
		permits ChartData.Stat, ChartData.Bar, ChartData.Doughnut, ChartData.Waffle, ChartData.Treemap,
		ChartData.Line, ChartData.Histogram,
		ChartData.Density, ChartData.Scatter, ChartData.Box {

	Provenance provenance();

	/**
	 * A single reduced value for a big-number "stat" card.
	 *
	 * @param value      the reduced number
	 * @param valueLabel what it measures ("Count", "Sum of price"), drawn under the number
	 * @param integral   whether to show it with no decimals (a count) rather than grouped decimals
	 * @param provenance what this chart covers of the source
	 */
	record Stat(double value, String valueLabel, boolean integral, Provenance provenance) implements ChartData { }

	/**
	 * Grouped or stacked bars. {@code values[s][c]} is series {@code s}'s value in category {@code c}; a single
	 * series ({@code seriesNames.length == 1}) is the common case and draws without a legend. When
	 * {@code colorByCategory} is set the (single-series) bars are coloured by their category instead of one hue — the
	 * "colour each bar by its own name" case — so each category is a full-width, distinctly-coloured bar.
	 * {@code horizontal} lays the category band down the Y axis with the value axis on X.
	 *
	 * @param categories      the category labels, one per band
	 * @param seriesNames     the series labels, one per row of {@code values}
	 * @param values          {@code values[s][c]}: series {@code s}'s value in category {@code c}
	 * @param stacked         one stacked bar per category instead of grouped bars
	 * @param colorByCategory colour single-series bars by their category (identity colours)
	 * @param horizontal      categories down the Y axis, values along X
	 * @param provenance      what this chart covers of the source
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
	 *
	 * @param categories  the slice labels, largest share first ("Other" fold last)
	 * @param values      one positive value per category
	 * @param valueLabel  what the values measure ("Sessions", "GB"), for tooltips and the centre read-out
	 * @param provenance  what this chart covers of the source
	 * @param sliceColors semantic per-slice overrides, or null (array or entry) for the palette
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

	/**
	 * The same shares of a whole, drawn as a unit grid ({@code Waffle} and {@code Doughnut} share the builder).
	 *
	 * @param categories the group labels, largest share first
	 * @param values     one positive value per category
	 * @param valueLabel what the values measure, for the cell tooltips
	 * @param provenance what this chart covers of the source
	 */
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

	/**
	 * The same shares of a whole, drawn as squarified proportional tiles (shares the doughnut's builder).
	 *
	 * @param categories the tile labels, largest share first
	 * @param values     one positive value per category
	 * @param valueLabel what the values measure, for the tile tooltips
	 * @param provenance what this chart covers of the source
	 */
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

	/**
	 * One or more line series over a shared continuous or time X.
	 *
	 * @param series     the lines, drawn in slot order
	 * @param timeAxis   read X values as epoch milliseconds and label the axis with calendar ticks
	 * @param provenance what this chart covers of the source
	 */
	record Line(Series[] series, boolean timeAxis, Provenance provenance) implements ChartData {
		/**
		 * A single line.
		 *
		 * @param name the series label
		 * @param x    X values, parallel to {@code y} (epoch-millis when the chart's axis is time)
		 * @param y    Y values, parallel to {@code x}
		 */
		public record Series(String name, double[] x, double[] y) { }
	}

	/**
	 * A binned distribution of one numeric column.
	 *
	 * @param edges      the bin boundaries, one more entry than {@code counts}; bin {@code i} is {@code [edges[i], edges[i+1])}
	 * @param counts     how many values land in each bin
	 * @param provenance what this chart covers of the source
	 */
	record Histogram(double[] edges, long[] counts, Provenance provenance) implements ChartData { }

	/**
	 * A smoothed 1-D density (KDE) of one numeric column, evaluated on a regular grid over {@code [min, max]}.
	 *
	 * @param gridX      the evaluation grid, regular over the value range
	 * @param density    the density at each grid point, parallel to {@code gridX}
	 * @param min        the smallest source value
	 * @param max        the largest source value
	 * @param sampleN    how many values the estimate was computed from
	 * @param provenance what this chart covers of the source
	 */
	record Density(double[] gridX, double[] density, double min, double max, int sampleN, Provenance provenance)
			implements ChartData { }

	/**
	 * Points over two numeric (or time-X) axes, coloured by a series id per point.
	 *
	 * @param x           X values, parallel to {@code y}
	 * @param y           Y values, parallel to {@code x}
	 * @param series      a palette-slot id per point (all zeros for a single series)
	 * @param seriesNames the series labels the ids index into
	 * @param timeAxisX   read X values as epoch milliseconds
	 * @param provenance  what this chart covers of the source
	 */
	record Scatter(double[] x, double[] y, int[] series, String[] seriesNames, boolean timeAxisX, Provenance provenance)
			implements ChartData {
		public int seriesCount() {
			return seriesNames.length;
		}
	}

	/**
	 * Five-number summaries per group for a box-and-whisker plot.
	 *
	 * @param groups     one box per group, in draw order
	 * @param provenance what this chart covers of the source
	 */
	record Box(Group[] groups, Provenance provenance) implements ChartData {
		/**
		 * One box: the five-number summary plus the points beyond the whiskers.
		 *
		 * @param label    the group name under the box
		 * @param lo       the lower whisker (smallest non-outlier)
		 * @param q1       the first quartile (box bottom)
		 * @param median   the median line
		 * @param q3       the third quartile (box top)
		 * @param hi       the upper whisker (largest non-outlier)
		 * @param outliers values beyond the whisker fences, drawn as points
		 */
		public record Group(String label, double lo, double q1, double median, double q3, double hi,
				double[] outliers) { }
	}
}
