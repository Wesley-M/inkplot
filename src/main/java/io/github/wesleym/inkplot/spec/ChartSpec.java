package io.github.wesleym.inkplot.spec;

/**
 * A fully-configured chart: the type plus the result columns (by model index) and options it draws from. Produced by
 * a picker UI or {@link ChartSpecs#initial}, consumed by the data pipeline; immutable, so re-preparing after a
 * data refresh is just handing the same spec a new snapshot.
 */
public sealed interface ChartSpec
		permits ChartSpec.Bar, ChartSpec.Doughnut, ChartSpec.Waffle, ChartSpec.Treemap, ChartSpec.Line,
		ChartSpec.Histogram, ChartSpec.Density, ChartSpec.Scatter, ChartSpec.Box {

	ChartType type();

	/**
	 * A category axis with an aggregated value; {@code y == null} means COUNT. Optionally split by a series
	 * column. {@code horizontal} lays the categories down the Y axis, so long names read without rotation.
	 */
	record Bar(int x, Integer y, Aggregate agg, Integer series, boolean stacked, CategoryOrder order,
			boolean horizontal) implements ChartSpec {

		/** Convenience: the given ordering, vertical. */
		public Bar(int x, Integer y, Aggregate agg, Integer series, boolean stacked, CategoryOrder order) {
			this(x, y, agg, series, stacked, order, false);
		}

		/** Convenience: the default largest-first ordering, vertical. */
		public Bar(int x, Integer y, Aggregate agg, Integer series, boolean stacked) {
			this(x, y, agg, series, stacked, CategoryOrder.VALUE_DESC, false);
		}

		@Override
		public ChartType type() {
			return ChartType.BAR;
		}
	}

	/**
	 * Shares of a whole, one slice per category. Only the parts-of-a-whole reductions make sense here, so instead of
	 * an {@link Aggregate}: {@code y == null} counts rows per slice, otherwise the slice sums that column.
	 * {@code maxSlices} is where the tail folds into "Other"; {@code 0} takes the chart's default cap.
	 */
	record Doughnut(int x, Integer y, int maxSlices) implements ChartSpec {

		public Doughnut(int x, Integer y) {
			this(x, y, 0);
		}

		@Override
		public ChartType type() {
			return ChartType.DOUGHNUT;
		}
	}

	/**
	 * Shares of a whole as a unit grid — same roles as {@link Doughnut}: {@code y == null} counts, else sums.
	 * {@code maxCategories} is where the tail folds into "Other"; {@code 0} takes the chart's default cap.
	 */
	record Waffle(int x, Integer y, int maxCategories) implements ChartSpec {

		public Waffle(int x, Integer y) {
			this(x, y, 0);
		}

		@Override
		public ChartType type() {
			return ChartType.WAFFLE;
		}
	}

	/**
	 * Shares of a whole as proportional tiles — same roles as {@link Doughnut}: {@code y == null} counts, else
	 * sums. {@code maxCategories} is where the tail folds into "Other"; {@code 0} takes the chart's default cap.
	 */
	record Treemap(int x, Integer y, int maxCategories) implements ChartSpec {

		public Treemap(int x, Integer y) {
			this(x, y, 0);
		}

		@Override
		public ChartType type() {
			return ChartType.TREEMAP;
		}
	}

	/** A line over a continuous or time X. {@code agg == null} plots raw points; otherwise it groups by X. */
	record Line(int x, int y, Integer series, Aggregate agg) implements ChartSpec {
		@Override
		public ChartType type() {
			return ChartType.LINE;
		}
	}

	/** A binned distribution of one numeric column; {@code bins == 0} auto-sizes the bin count. */
	record Histogram(int value, int bins) implements ChartSpec {
		@Override
		public ChartType type() {
			return ChartType.HISTOGRAM;
		}
	}

	/** A smoothed 1-D density of one numeric column; {@code bandwidthFactor} scales the smoothing (1.0 = Silverman). */
	record Density(int value, double bandwidthFactor) implements ChartSpec {
		@Override
		public ChartType type() {
			return ChartType.DENSITY;
		}
	}

	/** Points over two numeric (or time-X) columns, optionally coloured by a series column. */
	record Scatter(int x, int y, Integer series) implements ChartSpec {
		@Override
		public ChartType type() {
			return ChartType.SCATTER;
		}
	}

	/** A box-and-whisker summary of one numeric column, optionally split into a box per category. */
	record Box(int value, Integer group) implements ChartSpec {
		@Override
		public ChartType type() {
			return ChartType.BOX;
		}
	}
}
