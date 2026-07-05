package io.github.wesleym.inkplot.data;

import io.github.wesleym.inkplot.data.ChartData.Box.Group;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoxStatsTest {

	@Test
	void quartilesOfAKnownSet() {
		double[] values = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
		Group g = BoxStats.summarize("x", values, 100);
		assertEquals(5, g.median(), 1e-9);
		assertEquals(3, g.q1(), 1e-9);
		assertEquals(7, g.q3(), 1e-9);
	}

	@Test
	void flagsOutliersBeyondFences() {
		double[] values = new double[102];
		for (int i = 0; i < 100; i++) {
			values[i] = 50 + (i % 5);   // a tight cluster
		}
		values[100] = 1000;   // a high outlier
		values[101] = -900;   // a low outlier
		Group g = BoxStats.summarize("x", values, 100);
		assertEquals(2, g.outliers().length, "both extremes are flagged as outliers");
		assertTrue(g.hi() < 1000 && g.lo() > -900, "whiskers stop at the fence, not the outlier");
	}

	@Test
	void capsOutlierCount() {
		double[] values = new double[1000];
		for (int i = 0; i < 500; i++) {
			values[i] = 50;
		}
		for (int i = 500; i < 1000; i++) {
			values[i] = 100_000 + i;   // 500 high outliers
		}
		Group g = BoxStats.summarize("x", values, 20);
		assertTrue(g.outliers().length <= 20, "outlier dots are capped");
	}
}
