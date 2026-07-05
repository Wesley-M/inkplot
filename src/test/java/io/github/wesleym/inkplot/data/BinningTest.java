package io.github.wesleym.inkplot.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BinningTest {

	@Test
	void countsSumToSampleSize() {
		double[] values = new double[1000];
		for (int i = 0; i < values.length; i++) {
			values[i] = i % 100;
		}
		Binning.Result r = Binning.bins(values, 10);
		long total = 0;
		for (long c : r.counts()) {
			total += c;
		}
		assertEquals(1000, total, "every value lands in exactly one bin");
		assertEquals(11, r.edges().length, "edges are one more than bins");
	}

	@Test
	void requestedBinCountIsHonouredAndClamped() {
		double[] values = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
		assertEquals(4, Binning.bins(values, 4).counts().length);
		assertTrue(Binning.bins(values, 10_000).counts().length <= ChartLimits.HISTOGRAM_MAX_BINS,
				"an absurd request is clamped");
	}

	@Test
	void degenerateColumnCollapsesToOneBin() {
		double[] values = { 5, 5, 5, 5 };
		Binning.Result r = Binning.bins(values, 0);
		assertEquals(1, r.counts().length);
		assertEquals(4, r.counts()[0]);
	}

	@Test
	void autoBinsFallInRange() {
		double[] values = new double[5000];
		for (int i = 0; i < values.length; i++) {
			values[i] = Math.sin(i) * 50 + 50;
		}
		int bins = Binning.bins(values, 0).counts().length;
		assertTrue(bins >= 5 && bins <= ChartLimits.HISTOGRAM_MAX_BINS, "auto bin count " + bins + " is in range");
	}
}
