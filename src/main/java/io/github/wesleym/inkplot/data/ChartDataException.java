package io.github.wesleym.inkplot.data;

/**
 * A chart couldn't be prepared for a reason worth telling the reader — too many categories to group, an empty column,
 * a column that turned out not to be chartable. The message is user-facing; the panel shows it in place of the chart.
 */
public final class ChartDataException extends RuntimeException {

	public ChartDataException(String message) {
		super(message);
	}
}
