package io.github.wesleym.inkplot.spec;

/**
 * How repeated values are combined when a chart groups rows (a bar per category, a point per time bucket). COUNT needs
 * no value column; the rest reduce the chosen numeric column.
 */
public enum Aggregate {

	COUNT("Count"),
	SUM("Sum"),
	AVG("Average"),
	MIN("Minimum"),
	MAX("Maximum");

	private final String label;

	Aggregate(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

	/** Whether this aggregate reads a value column (everything except COUNT, which just tallies rows). */
	public boolean needsValue() {
		return this != COUNT;
	}
}
