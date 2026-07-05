package io.github.wesleym.inkplot;

import io.github.wesleym.inkplot.data.ChartData;
import io.github.wesleym.inkplot.data.Provenance;

import java.time.Instant;
import java.util.Random;

/**
 * Canned, realistic demo data for the preview harness and renderer tests — one builder per chart type, so
 * previews and unit tests draw the same shapes. Kept in the test tree; not shipped.
 */
final class ChartFixtures {

	private ChartFixtures() { }

	private static final long DAY = 24L * 60 * 60 * 1000;

	static ChartData.Bar barSingle() {
		return new ChartData.Bar(
				new String[] { "Delivered", "Processing", "Cancelled", "Refunded", "On hold" },
				new String[] { "Orders" },
				new double[][] { { 12410, 6200, 1930, 410, 880 } },
				false, Provenance.full(21830));
	}

	/** Long category labels down the Y gutter — the case horizontal orientation exists for. */
	static ChartData.Bar barHorizontal() {
		return new ChartData.Bar(
				new String[] { "Billing and account questions", "Shipping and delivery delays",
						"Product setup and installation", "Returns and refund requests",
						"Technical troubleshooting", "Other" },
				new String[] { "Tickets" },
				new double[][] { { 9400, 7200, 3100, 2400, 1900, 800 } },
				false, false, true, Provenance.full(24800));
	}

	static ChartData.Bar barGrouped() {
		return new ChartData.Bar(
				new String[] { "Jan", "Feb", "Mar", "Apr", "May", "Jun" },
				new String[] { "2025", "2026" },
				new double[][] {
						{ 4200, 4800, 5300, 5000, 6100, 6600 },
						{ 5100, 5600, 6400, 6900, 7300, 8100 } },
				false, Provenance.full(72000));
	}

	static ChartData.Bar barStacked() {
		return new ChartData.Bar(
				new String[] { "North", "South", "East", "West" },
				new String[] { "Phone", "Web", "Walk-in" },
				new double[][] {
						{ 1200, 900, 1500, 1100 },
						{ 3400, 2800, 3900, 3100 },
						{ 600, 500, 700, 450 } },
				true, Provenance.full(22350));
	}

	/** A top-values breakdown at embedded strip height — long value labels, horizontal. */
	static ChartData.Bar barTopValues() {
		return new ChartData.Bar(
				new String[] { "delivered", "shipped", "cancelled by customer", "awaiting payment",
						"returned to sender", "pending fraud review", "picked up in store", "in transit" },
				new String[] { "Rows" },
				new double[][] { { 9400, 7200, 3100, 2400, 1900, 1200, 800, 350 } },
				false, false, true, Provenance.full(26350));
	}

	/**
	 * The label-placement stress case: one dominant slice and a long tail of tiny ones, whose callouts all
	 * project near 12 o'clock and must stack in the gutter columns instead of overprinting the ring.
	 */
	static ChartData.Doughnut doughnutTail() {
		return new ChartData.Doughnut(
				new String[] { "CHROME_131", "SAFARI_18", "FIREFOX_133", "EDGE_131",
						"CHROME_130", "SAFARI_17", "OPERA_115", "BRAVE_1_73",
						"VIVALDI_7", "ARC_1_77" },
				new double[] { 318, 100, 71, 53, 18, 15, 6, 4, 2, 2 },
				"sessions", Provenance.full(589));
	}

	/** A waffle over more categories than a doughnut admits — the fine grid keeps thirteen groups legible. */
	static ChartData.Waffle waffle() {
		return new ChartData.Waffle(
				new String[] { "CHROME_131", "SAFARI_18", "FIREFOX_133", "EDGE_131",
						"CHROME_130", "SAFARI_17", "OPERA_115", "BRAVE_1_73",
						"VIVALDI_7", "ARC_1_77", "FIREFOX_132", "CHROME_129", "Other" },
				new double[] { 318, 100, 71, 53, 18, 15, 6, 4, 2, 2, 2, 1, 1 },
				"sessions", Provenance.full(593));
	}

	/** The same skewed shares as a treemap — the biggest tile dominates, the tail still gets visible tiles. */
	static ChartData.Treemap treemap() {
		return new ChartData.Treemap(
				new String[] { "CHROME_131", "SAFARI_18", "FIREFOX_133", "EDGE_131",
						"CHROME_130", "SAFARI_17", "OPERA_115", "BRAVE_1_73",
						"VIVALDI_7", "ARC_1_77", "FIREFOX_132", "CHROME_129", "Other" },
				new double[] { 318, 100, 71, 53, 18, 15, 6, 4, 2, 2, 2, 1, 1 },
				"sessions", Provenance.full(593));
	}

	/** A boolean-column split at embedded strip height. */
	static ChartData.Doughnut doughnutBoolean() {
		return new ChartData.Doughnut(
				new String[] { "true", "false" },
				new double[] { 31200, 9400 },
				"Rows", Provenance.full(40600));
	}

	static ChartData.Doughnut doughnut() {
		return new ChartData.Doughnut(
				new String[] { "Card", "PayPal", "Bank transfer", "Gift card", "Cash on delivery", "Other" },
				new double[] { 18200, 12400, 6100, 2900, 1400, 800 },
				"Payments", Provenance.full(41800));
	}

	static ChartData.Line lineTime() {
		long start = Instant.parse("2026-01-01T00:00:00Z").toEpochMilli();
		int n = 45;
		double[] x = new double[n];
		double[] y = new double[n];
		for (int i = 0; i < n; i++) {
			x[i] = start + (long) i * DAY;
			y[i] = 5200 + 1400 * Math.sin(i / 6.0) + i * 40;
		}
		return new ChartData.Line(new ChartData.Line.Series[] { new ChartData.Line.Series("Revenue", x, y) },
				true, Provenance.full(n));
	}

	static ChartData.Line lineMulti() {
		int n = 60;
		double[] x = new double[n];
		double[] a = new double[n];
		double[] b = new double[n];
		for (int i = 0; i < n; i++) {
			x[i] = i;
			a[i] = 40 + 30 * Math.sin(i / 8.0);
			b[i] = 55 + 22 * Math.cos(i / 5.0);
		}
		return new ChartData.Line(new ChartData.Line.Series[] {
				new ChartData.Line.Series("Latency p50", x, a),
				new ChartData.Line.Series("Latency p95", x, b) },
				false, Provenance.full(n));
	}

	static ChartData.Histogram histogram() {
		int bins = 16;
		double lo = 0;
		double hi = 100;
		double[] edges = new double[bins + 1];
		long[] counts = new long[bins];
		for (int i = 0; i <= bins; i++) {
			edges[i] = lo + (hi - lo) * i / bins;
		}
		for (int i = 0; i < bins; i++) {
			double centre = (edges[i] + edges[i + 1]) / 2;
			double bell = Math.exp(-Math.pow((centre - 58) / 18, 2));
			counts[i] = Math.round(20 + 4200 * bell);
		}
		return new ChartData.Histogram(edges, counts, new Provenance(48000, 190000, true, 320, null));
	}

	static ChartData.Density density() {
		int grid = 160;
		double min = 0;
		double max = 100;
		double[] gx = new double[grid];
		double[] d = new double[grid];
		for (int i = 0; i < grid; i++) {
			double x = min + (max - min) * i / (grid - 1);
			gx[i] = x;
			// A gentle bimodal shape, so the curve clearly isn't a single bell.
			d[i] = 0.6 * gauss(x, 38, 10) + 0.4 * gauss(x, 72, 8);
		}
		return new ChartData.Density(gx, d, min, max, 50000,
				new Provenance(50000, 260000, true, 0, "smoothed over 50,000 sampled values"));
	}

	static ChartData.Scatter scatter() {
		Random rng = new Random(42);
		int n = 3000;
		double[] x = new double[n];
		double[] y = new double[n];
		for (int i = 0; i < n; i++) {
			double base = rng.nextGaussian() * 18 + 50;
			x[i] = base;
			y[i] = base * 1.4 + rng.nextGaussian() * 22 + 10;
		}
		return new ChartData.Scatter(x, y, new int[0], new String[] { "Rows" }, false,
				new Provenance(20000, 480000, true, 0, "sampled 20,000 of 480,000 points"));
	}

	static ChartData.Scatter scatterSeries() {
		Random rng = new Random(7);
		int per = 700;
		int groups = 3;
		int n = per * groups;
		double[] x = new double[n];
		double[] y = new double[n];
		int[] series = new int[n];
		double[] cx = { 30, 55, 78 };
		for (int gI = 0; gI < groups; gI++) {
			for (int i = 0; i < per; i++) {
				int idx = gI * per + i;
				x[idx] = cx[gI] + rng.nextGaussian() * 9;
				y[idx] = 20 + gI * 25 + rng.nextGaussian() * 12;
				series[idx] = gI;
			}
		}
		return new ChartData.Scatter(x, y, series, new String[] { "Cohort A", "Cohort B", "Cohort C" }, false,
				Provenance.full(n));
	}

	static ChartData.Box box() {
		ChartData.Box.Group[] groups = {
				group("Mon", 12, 20, 28, 38, 52, new double[] { 62, 66 }),
				group("Tue", 10, 18, 25, 34, 47, new double[] { 58 }),
				group("Wed", 14, 24, 33, 44, 60, new double[] { 71, 74, 78 }),
				group("Thu", 8, 16, 22, 30, 41, new double[] { }),
				group("Fri", 16, 28, 38, 50, 66, new double[] { 79 }) };
		return new ChartData.Box(groups, Provenance.full(38000));
	}

	private static ChartData.Box.Group group(String label, double lo, double q1, double med, double q3, double hi,
			double[] outliers) {
		return new ChartData.Box.Group(label, lo, q1, med, q3, hi, outliers);
	}

	private static double gauss(double x, double mean, double sd) {
		return Math.exp(-0.5 * Math.pow((x - mean) / sd, 2)) / (sd * Math.sqrt(2 * Math.PI));
	}
}
