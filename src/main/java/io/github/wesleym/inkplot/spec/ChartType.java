package io.github.wesleym.inkplot.spec;

/**
 * The chart forms the module offers, grouped by the question each answers — the picker menu is generated from
 * this order and grouping, so adding a type here is what adds it to the UI. Each names the roles its picker needs
 * and whether it consumes a category axis, a continuous X, or a single value column — the panel reads this to show
 * the right pickers and offer only eligible columns.
 */
public enum ChartType {

	STAT("Stat", Group.SUMMARY, "A single headline number, big and clear"),
	BAR("Bar", Group.COMPARE, "Compare totals across categories"),
	DOUGHNUT("Doughnut", Group.SHARE, "Shares of a whole, up to ten slices"),
	WAFFLE("Waffle", Group.SHARE, "Shares of a whole on a unit grid"),
	TREEMAP("Treemap", Group.SHARE, "Shares of a whole as proportional tiles"),
	LINE("Line", Group.TREND, "A value over time or a sequence"),
	HISTOGRAM("Histogram", Group.DISTRIBUTION, "How one number distributes"),
	DENSITY("Density", Group.DISTRIBUTION, "The distribution as a smooth curve"),
	BOX("Box", Group.DISTRIBUTION, "The spread per group at a glance"),
	SCATTER("Scatter", Group.RELATIONSHIP, "How two numbers relate");

	/** The question a chart answers — the picker menu's section headings. */
	public enum Group {

		SUMMARY("Summary"),
		COMPARE("Compare"),
		SHARE("Share of a whole"),
		TREND("Trend"),
		DISTRIBUTION("Distribution"),
		RELATIONSHIP("Relationship");

		private final String displayName;

		Group(String displayName) {
			this.displayName = displayName;
		}

		public String displayName() {
			return displayName;
		}
	}

	private final String displayName;
	private final Group group;
	private final String whenToUse;

	ChartType(String displayName, Group group, String whenToUse) {
		this.displayName = displayName;
		this.group = group;
		this.whenToUse = whenToUse;
	}

	public String displayName() {
		return displayName;
	}

	public Group group() {
		return group;
	}

	/** The one-line "reach for this when…" the picker menu teaches with. */
	public String whenToUse() {
		return whenToUse;
	}
}
