package io.github.wesleym.inkplot.data;

import java.util.Arrays;

/**
 * Bins a numeric sample into a histogram. When no bin count is requested it sizes itself by the Freedman–Diaconis
 * rule (robust to outliers), clamped to a sane range; a degenerate all-equal column collapses to a single bin. The
 * only allocation beyond the returned arrays is one sorted copy for the quartile width, bounded by the caller's cap.
 */
public final class Binning {

	private Binning() { }

	/** Bin edges (one more than counts) and the count in each bin. */
	/**
	 * The computed bins.
	 *
	 * @param edges  bin boundaries; bin {@code i} is {@code [edges[i], edges[i+1])}
	 * @param counts values per bin, one fewer entry than {@code edges}
	 */
	public record Result(double[] edges, long[] counts) { }

	public static Result bins(double[] values, int requestedBins) {
		if (values.length == 0) {
			return new Result(new double[] { 0, 1 }, new long[] { 0 });
		}
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		for (double v : values) {
			min = Math.min(min, v);
			max = Math.max(max, v);
		}
		if (min == max) {
			return new Result(new double[] { min, min + 1 }, new long[] { values.length });
		}

		int binCount = requestedBins > 0
				? Math.min(requestedBins, ChartLimits.HISTOGRAM_MAX_BINS)
				: freedmanDiaconis(values, min, max);
		binCount = Math.max(1, Math.min(binCount, ChartLimits.HISTOGRAM_MAX_BINS));

		double width = (max - min) / binCount;
		double[] edges = new double[binCount + 1];
		for (int i = 0; i <= binCount; i++) {
			edges[i] = min + width * i;
		}
		long[] counts = new long[binCount];
		for (double v : values) {
			int idx = (int) ((v - min) / width);
			if (idx >= binCount) {
				idx = binCount - 1;   // the maximum falls in the last bin, not a phantom overflow bin
			}
			counts[idx]++;
		}
		return new Result(edges, counts);
	}

	private static int freedmanDiaconis(double[] values, double min, double max) {
		double[] sorted = values.clone();
		Arrays.sort(sorted);
		double iqr = percentile(sorted, 0.75) - percentile(sorted, 0.25);
		if (iqr <= 0) {
			return (int) Math.ceil(Math.sqrt(values.length));   // fall back to the √n rule for a spread-less sample
		}
		double width = 2 * iqr / Math.cbrt(values.length);
		int bins = (int) Math.ceil((max - min) / width);
		return Math.max(5, Math.min(bins, ChartLimits.HISTOGRAM_MAX_BINS));
	}

	static double percentile(double[] sorted, double p) {
		if (sorted.length == 0) {
			return 0;
		}
		double idx = p * (sorted.length - 1);
		int lo = (int) Math.floor(idx);
		int hi = (int) Math.ceil(idx);
		if (lo == hi) {
			return sorted[lo];
		}
		double frac = idx - lo;
		return sorted[lo] * (1 - frac) + sorted[hi] * frac;
	}
}
