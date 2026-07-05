package io.github.wesleym.inkplot.data;

import io.github.wesleym.inkplot.data.Numbers;

import java.util.Arrays;
import java.util.List;

/**
 * Pulls chart columns out of the string result rows into compact primitive arrays — the string rows stay the single
 * copy of the data, and the extracted arrays are pre-sized to at most the cap, so extraction can never balloon the
 * heap. Cells that don't parse are dropped and counted; rows beyond the cap are counted too, so the view can report
 * exactly what was left out.
 */
public final class ChartExtract {

	private ChartExtract() { }

	/** An extracted numeric/time column: its values plus how many cells were unparseable and how many exceeded the cap. */
	public record Column(double[] values, int skipped, int overCap) { }

	/** Two aligned columns, keeping only rows where both cells parse. */
	public record Pair(double[] x, double[] y, int skipped, int overCap) { }

	public static Column numeric(List<List<String>> rows, int col, int cap) {
		int size = Math.min(rows.size(), cap);
		double[] out = new double[size];
		int n = 0;
		int skipped = 0;
		int overCap = 0;
		for (List<String> row : rows) {
			Double d = Numbers.parse(cell(row, col));
			// Reject non-finite too: Double.valueOf accepts "NaN"/"Infinity"/"1e999", and one such cell would poison
			// every min/max/bin/kde downstream (NaN spreads through Math.min/max and slips past == guards).
			if (d == null || !Double.isFinite(d)) {
				skipped++;
			}
			else if (n < cap) {
				out[n++] = d;
			}
			else {
				overCap++;
			}
		}
		return new Column(trim(out, n), skipped, overCap);
	}

	public static Column temporal(List<List<String>> rows, int col, int cap) {
		int size = Math.min(rows.size(), cap);
		double[] out = new double[size];
		int n = 0;
		int skipped = 0;
		int overCap = 0;
		for (List<String> row : rows) {
			Long ms = TemporalParse.parseMillis(cell(row, col));
			if (ms == null) {
				skipped++;
			}
			else if (n < cap) {
				out[n++] = ms;
			}
			else {
				overCap++;
			}
		}
		return new Column(trim(out, n), skipped, overCap);
	}

	public static Pair pair(List<List<String>> rows, int xCol, int yCol, boolean xTemporal, int cap) {
		int size = Math.min(rows.size(), cap);
		double[] xs = new double[size];
		double[] ys = new double[size];
		int n = 0;
		int skipped = 0;
		int overCap = 0;
		for (List<String> row : rows) {
			Double x = xTemporal ? asDouble(TemporalParse.parseMillis(cell(row, xCol))) : Numbers.parse(cell(row, xCol));
			Double y = Numbers.parse(cell(row, yCol));
			if (x == null || y == null || !Double.isFinite(x) || !Double.isFinite(y)) {
				skipped++;   // drop non-finite (NaN/Infinity) too — see numeric()
			}
			else if (n < cap) {
				xs[n] = x;
				ys[n] = y;
				n++;
			}
			else {
				overCap++;
			}
		}
		return new Pair(trim(xs, n), trim(ys, n), skipped, overCap);
	}

	/** A uniform-stride sample of the paired arrays down to {@code target} points, preserving overall spread. */
	public static int[] stride(int length, int target) {
		if (length <= target) {
			int[] all = new int[length];
			for (int i = 0; i < length; i++) {
				all[i] = i;
			}
			return all;
		}
		int[] pick = new int[target];
		for (int i = 0; i < target; i++) {
			pick[i] = (int) ((long) i * length / target);
		}
		return pick;
	}

	private static Double asDouble(Long v) {
		return v == null ? null : (double) v;
	}

	private static double[] trim(double[] array, int n) {
		return n == array.length ? array : Arrays.copyOf(array, n);
	}

	private static String cell(List<String> row, int col) {
		return col >= 0 && col < row.size() ? row.get(col) : null;
	}
}
