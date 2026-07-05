package io.github.wesleym.inkplot.render;

import io.github.wesleym.inkplot.ChartFormat;

/**
 * The label vocabulary the shares-of-a-whole charts (doughnut, waffle) have in common: the "name · pct" on-chart
 * label and the "count · pct" tooltip value — one place, so the two charts never phrase the same share differently.
 */
final class ShareText {

	private ShareText() { }

	static String label(String category, double share) {
		return category + " · " + percent(share);
	}

	static String tooltipValue(double value, double share) {
		return ChartFormat.groupedDecimal(value) + " · " + percent(share);
	}

	static String percent(double share) {
		long pct = Math.round(share * 100);
		return pct < 1 ? "<1%" : pct + "%";
	}
}
