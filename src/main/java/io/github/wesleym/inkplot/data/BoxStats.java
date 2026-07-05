package io.github.wesleym.inkplot.data;

import io.github.wesleym.inkplot.data.ChartData.Box.Group;

import java.util.Arrays;

/**
 * Computes a Tukey box summary for one numeric sample: the quartiles, the whiskers reaching to the furthest values
 * inside 1.5×IQR of the box, and the values beyond that as outliers (capped so a pathological column can't produce a
 * million dots). Sorts one bounded copy of the sample and nothing else.
 */
public final class BoxStats {

	private BoxStats() { }

	public static Group summarize(String label, double[] values, int maxOutliers) {
		if (values.length == 0) {
			return new Group(label, 0, 0, 0, 0, 0, new double[0]);
		}
		double[] sorted = values.clone();
		Arrays.sort(sorted);
		double q1 = Binning.percentile(sorted, 0.25);
		double median = Binning.percentile(sorted, 0.5);
		double q3 = Binning.percentile(sorted, 0.75);
		double iqr = q3 - q1;
		double lowerFence = q1 - 1.5 * iqr;
		double upperFence = q3 + 1.5 * iqr;

		double whiskerLo = sorted[0];
		for (double v : sorted) {
			if (v >= lowerFence) {
				whiskerLo = v;
				break;
			}
		}
		double whiskerHi = sorted[sorted.length - 1];
		for (int i = sorted.length - 1; i >= 0; i--) {
			if (sorted[i] <= upperFence) {
				whiskerHi = sorted[i];
				break;
			}
		}

		double[] outliers = collectOutliers(sorted, lowerFence, upperFence, maxOutliers);
		return new Group(label, whiskerLo, q1, median, q3, whiskerHi, outliers);
	}

	private static double[] collectOutliers(double[] sorted, double lowerFence, double upperFence, int max) {
		double[] buffer = new double[Math.min(max, sorted.length)];
		int n = 0;
		int lo = 0;                     // next low outlier, ascending from the bottom
		int hi = sorted.length - 1;     // next high outlier, descending from the top
		// Interleave the two ends so a dense low tail can't consume the whole budget and hide every high outlier.
		while (n < buffer.length) {
			boolean progressed = false;
			if (lo <= hi && sorted[lo] < lowerFence) {
				buffer[n++] = sorted[lo++];
				progressed = true;
			}
			if (n < buffer.length && lo <= hi && sorted[hi] > upperFence) {
				buffer[n++] = sorted[hi--];
				progressed = true;
			}
			if (!progressed) {
				break;   // neither end has an outlier left
			}
		}
		return n == buffer.length ? buffer : Arrays.copyOf(buffer, n);
	}
}
