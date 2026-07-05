package io.github.wesleym.inkplot.spec;

import io.github.wesleym.inkplot.data.ChartColumns;

import java.util.List;

/**
 * Sensible default specs: given a chart type and the classified columns, picks the columns a reader would
 * choose first — the first category for the X, the first numeric for the value, a timestamp for a line's X.
 * This is what makes "table in, chart out" one call; returns null when the result genuinely can't carry the
 * type (e.g. a histogram over a table with no numeric column).
 */
public final class ChartSpecs {

	private ChartSpecs() { }

	public static ChartSpec initial(ChartType type, ChartColumns columns) {
		List<Integer> numeric = columns.numericColumns();
		List<Integer> category = columns.categoryColumns();
		List<Integer> temporal = columns.temporalColumns();
		List<Integer> continuous = columns.continuousColumns();
		return switch (type) {
			case BAR -> category.isEmpty() ? null
					: new ChartSpec.Bar(category.get(0), firstOther(numeric, category.get(0)),
							firstOther(numeric, category.get(0)) == null ? Aggregate.COUNT : Aggregate.SUM,
							null, false);
			case DOUGHNUT -> category.isEmpty() ? null
					: new ChartSpec.Doughnut(category.get(0), firstOther(numeric, category.get(0)));
			case WAFFLE -> category.isEmpty() ? null
					: new ChartSpec.Waffle(category.get(0), firstOther(numeric, category.get(0)));
			case TREEMAP -> category.isEmpty() ? null
					: new ChartSpec.Treemap(category.get(0), firstOther(numeric, category.get(0)));
			case LINE -> {
				int x = !temporal.isEmpty() ? temporal.get(0) : continuous.isEmpty() ? -1 : continuous.get(0);
				Integer y = firstOther(numeric, x);
				yield x < 0 || y == null ? null : new ChartSpec.Line(x, y, null, Aggregate.SUM);
			}
			case HISTOGRAM -> numeric.isEmpty() ? null : new ChartSpec.Histogram(numeric.get(0), 0);
			case DENSITY -> numeric.isEmpty() ? null : new ChartSpec.Density(numeric.get(0), 1.0);
			case SCATTER -> {
				Integer y = numeric.size() > 1 ? numeric.get(1) : null;
				yield numeric.isEmpty() || y == null ? null : new ChartSpec.Scatter(numeric.get(0), y, null);
			}
			case BOX -> numeric.isEmpty() ? null
					: new ChartSpec.Box(numeric.get(0), category.isEmpty() ? null : category.get(0));
		};
	}

	// The first candidate that isn't already claimed by the other axis, or null when none is.
	private static Integer firstOther(List<Integer> candidates, int taken) {
		for (int c : candidates) {
			if (c != taken) {
				return c;
			}
		}
		return null;
	}
}
