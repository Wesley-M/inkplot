package io.github.wesleym.inkplot.spec;

/**
 * How repeated values are combined when a chart groups rows (a bar per category, a point per time bucket). COUNT needs
 * no value column; the rest reduce the chosen numeric column.
 */
public enum Aggregate {

	/** Tally the rows in each group; the only aggregate that needs no value column. */
	COUNT("Count"),
	/** Sum the value column within each group. */
	SUM("Sum"),
	/** Average the value column within each group. */
	AVG("Average"),
	/** The smallest value in each group. */
	MIN("Minimum"),
	/** The largest value in each group. */
	MAX("Maximum");

	private final String label;

	Aggregate(String label) {
		this.label = label;
	}

	/** The human-readable name for pickers and chart titles ("Sum", "Average"). */
	public String label() {
		return label;
	}

	/** Whether this aggregate reads a value column (everything except COUNT, which just tallies rows). */
	public boolean needsValue() {
		return this != COUNT;
	}
}
