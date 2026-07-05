package io.github.wesleym.inkplot;

import io.github.wesleym.inkplot.render.ChartHoverState;
import io.github.wesleym.inkplot.render.PlotContext;
import io.github.wesleym.inkplot.render.WaffleRenderer;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaffleRendererTest {

	@Test
	void quotasSumToTheGridAndFollowShares() {
		int[] q = WaffleRenderer.quotas(new double[] { 50, 30, 20 }, 400);
		assertEquals(400, Arrays.stream(q).sum());
		assertEquals(200, q[0]);
		assertEquals(120, q[1]);
		assertEquals(80, q[2]);
	}

	@Test
	void largestRemainderResolvesRoundingWithoutDriftingTheSum() {
		int[] q = WaffleRenderer.quotas(new double[] { 1, 1, 1 }, 400);   // 133.3 each — one gets the spare cell
		assertEquals(400, Arrays.stream(q).sum());
		int max = Arrays.stream(q).max().orElseThrow();
		int min = Arrays.stream(q).min().orElseThrow();
		assertTrue(max - min <= 1, "the spare cells spread by largest remainder, never lumping");
	}

	@Test
	void hoverHasNoDeadPixelsInsideTheGrid() {
		// Sweeping the pointer across a category must hold the highlight steady — the 2px gaps between cells
		// (which the hover silhouette paints as solid) must hit-test to their cells, never to nothing.
		WaffleRenderer renderer = new WaffleRenderer(ChartFixtures.waffle());
		PlotContext ctx = new PlotContext(new Rectangle(0, 0, 640, 400), null, null,
				ChartTheme.LIGHT, ChartHoverState.NONE);
		int y = 200;   // a mid-height scanline through the grid
		int firstHit = -1;
		int run = 0;
		for (int x = 0; x < 640; x++) {
			boolean hit = renderer.hitTest(ctx, new Point(x, y)).isPresent();
			if (hit && firstHit < 0) {
				firstHit = x;
			}
			if (firstHit >= 0 && hit && run == x - firstHit) {
				run++;
			}
		}
		assertTrue(firstHit >= 0, "the scanline crosses the grid");
		assertTrue(run > 250, "the grid hit-tests as one continuous region, not per-cell islands (run: " + run + ")");
	}

	@Test
	void everyPositiveCategoryGetsAtLeastOneCell() {
		int[] q = WaffleRenderer.quotas(new double[] { 10_000, 1, 1 }, 400);   // far below half a cell each
		assertEquals(400, Arrays.stream(q).sum());
		assertTrue(q[1] >= 1 && q[2] >= 1, "tiny shares stay visible on the grid");
		assertEquals(400 - q[1] - q[2], q[0], "their cells come out of the largest category");
	}
}
