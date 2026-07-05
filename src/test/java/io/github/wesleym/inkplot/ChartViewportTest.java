package io.github.wesleym.inkplot;

import org.junit.jupiter.api.Test;

import java.awt.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The chart camera's math: pivot-fixed zoom, below-fit centring, covering clamps, and the fit reset. */
class ChartViewportTest {

	private static final int W = 800;
	private static final int H = 600;

	@Test
	void zoomKeepsThePivotPointFixed() {
		ChartViewport v = new ChartViewport();
		assertTrue(v.zoomAt(200, 150, 2.0, W, H));
		assertEquals(2.0, v.scale());
		assertEquals(new Point(200, 150), v.toBase(new Point(200, 150)));
	}

	@Test
	void zoomOutBelowFitCentresTheShrunkChart() {
		ChartViewport v = new ChartViewport();
		assertTrue(v.zoomAt(0, 0, 0.5, W, H));
		assertEquals(0.5, v.scale());
		assertTrue(v.isZoomed());
		assertEquals(W * 0.25, v.offsetX(), 1e-9);
		assertEquals(H * 0.25, v.offsetY(), 1e-9);
		// Below fit, panning can't dislodge the centred view.
		v.panBy(500, 500, W, H);
		assertEquals(W * 0.25, v.offsetX(), 1e-9);
	}

	@Test
	void panIsClampedToKeepTheMagnifiedImageCoveringTheViewport() {
		ChartViewport v = new ChartViewport();
		v.zoomAt(0, 0, 2.0, W, H);
		v.panBy(10_000, 10_000, W, H);
		assertEquals(0, v.offsetX(), 1e-9);
		assertEquals(0, v.offsetY(), 1e-9);
		v.panBy(-100_000, -100_000, W, H);
		assertEquals(W - W * 2.0, v.offsetX(), 1e-9);
		assertEquals(H - H * 2.0, v.offsetY(), 1e-9);
	}

	@Test
	void scaleStopsAtTheBounds() {
		ChartViewport v = new ChartViewport();
		for (int i = 0; i < 100; i++) {
			v.zoomAt(0, 0, 1.5, W, H);
		}
		assertEquals(ChartViewport.MAX_SCALE, v.scale());
		assertFalse(v.zoomAt(0, 0, 1.5, W, H), "a zoom pinned at the bound reports no change");
		for (int i = 0; i < 100; i++) {
			v.zoomAt(0, 0, 0.5, W, H);
		}
		assertEquals(ChartViewport.MIN_SCALE, v.scale());
		assertFalse(v.zoomAt(0, 0, 0.5, W, H));
	}

	@Test
	void fitResetsTheCamera() {
		ChartViewport v = new ChartViewport();
		v.zoomAt(100, 100, 3.0, W, H);
		v.panBy(-50, -30, W, H);
		v.fit();
		assertTrue(v.atFit());
		assertFalse(v.isZoomed());
		assertEquals(new Point(7, 11), v.toBase(new Point(7, 11)));
	}
}
