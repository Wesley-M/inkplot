package io.github.wesleym.inkplot.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownsampleTest {

	@Test
	void lttbKeepsEndpointsAndReducesCount() {
		int n = 10_000;
		double[] x = new double[n];
		double[] y = new double[n];
		for (int i = 0; i < n; i++) {
			x[i] = i;
			y[i] = Math.sin(i / 50.0);
		}
		int[] keep = Downsample.lttb(x, y, 400);
		assertEquals(400, keep.length, "downsampled to the target count");
		assertEquals(0, keep[0], "keeps the first point");
		assertEquals(n - 1, keep[keep.length - 1], "keeps the last point");
		for (int i = 1; i < keep.length; i++) {
			assertTrue(keep[i] > keep[i - 1], "indices strictly ascending");
		}
	}

	@Test
	void lttbPreservesAVisualSpike() {
		int n = 5_000;
		double[] x = new double[n];
		double[] y = new double[n];
		for (int i = 0; i < n; i++) {
			x[i] = i;
			y[i] = 0;
		}
		y[2500] = 1000;   // a lone spike a naive stride would miss
		int[] keep = Downsample.lttb(x, y, 200);
		boolean kept = false;
		for (int idx : keep) {
			if (idx == 2500) {
				kept = true;
			}
		}
		assertTrue(kept, "the spike survives downsampling");
	}

	@Test
	void returnsAllWhenAlreadySmall() {
		double[] x = { 0, 1, 2 };
		double[] y = { 3, 4, 5 };
		assertEquals(3, Downsample.lttb(x, y, 500).length, "no downsampling when it already fits");
	}

	@Test
	void minMaxPerBucketKeepsExtremes() {
		int n = 4_000;
		double[] y = new double[n];
		for (int i = 0; i < n; i++) {
			y[i] = Math.sin(i / 30.0);
		}
		y[1000] = 5;    // global max
		y[3000] = -5;   // global min
		int[] keep = Downsample.minMaxPerBucket(y, 100);
		boolean hasMax = false;
		boolean hasMin = false;
		for (int idx : keep) {
			hasMax |= idx == 1000;
			hasMin |= idx == 3000;
		}
		assertTrue(hasMax && hasMin, "both global extremes are kept");
	}
}
