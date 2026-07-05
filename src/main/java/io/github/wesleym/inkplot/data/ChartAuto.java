package io.github.wesleym.inkplot.data;

import io.github.wesleym.inkplot.spec.ChartType;

/**
 * Picks a sensible default chart from the shape of a result, so the reader sees a meaningful chart the moment data
 * loads instead of a blank canvas — the pickers are there to adjust it, not to require configuration up front. Aimed
 * at developers, not analysts: a timestamp trends over time, a category with a measure becomes bars, a lone number
 * becomes a distribution.
 */
public final class ChartAuto {

	private ChartAuto() { }

	public static ChartType suggest(ChartColumns columns) {
		boolean hasTemporal = !columns.temporalColumns().isEmpty();
		boolean hasNumeric = !columns.numericColumns().isEmpty();
		boolean hasCategory = !columns.categoryColumns().isEmpty();
		int numericCount = columns.numericColumns().size();

		if (hasTemporal && hasNumeric) {
			return ChartType.LINE;            // a timestamp + a measure reads as a trend over time
		}
		if (hasCategory && hasNumeric) {
			return ChartType.BAR;             // a category + a measure reads as bars (group by, aggregate)
		}
		if (numericCount >= 2) {
			return ChartType.SCATTER;         // two numbers, no category — a relationship
		}
		if (numericCount == 1) {
			return ChartType.HISTOGRAM;       // a single number — its distribution
		}
		return ChartType.BAR;                 // fall back to counting rows per category
	}
}
