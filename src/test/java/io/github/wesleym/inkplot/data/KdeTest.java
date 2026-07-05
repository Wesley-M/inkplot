package io.github.wesleym.inkplot.data;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KdeTest {

	@Test
	void densityIntegratesToRoughlyOne() {
		Random rng = new Random(1);
		double[] values = new double[5000];
		for (int i = 0; i < values.length; i++) {
			values[i] = rng.nextGaussian() * 10 + 50;
		}
		Kde.Result r = Kde.estimate(values, 1.0, 256);
		double area = 0;
		for (int i = 1; i < r.gridX().length; i++) {
			double dx = r.gridX()[i] - r.gridX()[i - 1];
			area += dx * (r.density()[i] + r.density()[i - 1]) / 2;   // trapezoidal
		}
		assertEquals(1.0, area, 0.05, "a density integrates to ~1");
	}

	@Test
	void peakSitsNearTheMode() {
		Random rng = new Random(2);
		double[] values = new double[4000];
		for (int i = 0; i < values.length; i++) {
			values[i] = rng.nextGaussian() * 5 + 100;
		}
		Kde.Result r = Kde.estimate(values, 1.0, 256);
		int peak = 0;
		for (int i = 1; i < r.density().length; i++) {
			if (r.density()[i] > r.density()[peak]) {
				peak = i;
			}
		}
		assertTrue(Math.abs(r.gridX()[peak] - 100) < 6, "peak near the mean, at " + r.gridX()[peak]);
	}

	@Test
	void constantColumnYieldsAFlatHonestLine() {
		double[] values = { 7, 7, 7, 7, 7 };
		Kde.Result r = Kde.estimate(values, 1.0, 256);
		for (double d : r.density()) {
			assertEquals(0, d, 1e-9, "no spread means no curve");
		}
	}
}
