package io.github.wesleym.inkplot.render;

import org.junit.jupiter.api.Test;

import java.awt.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreemapSquarifyTest {

	private static final Rectangle2D BOUNDS = new Rectangle2D.Double(10, 20, 600, 400);

	@Test
	void areasAreProportionalAndTileTheBounds() {
		double[] values = { 6, 6, 4, 3, 2, 2, 1 };
		Rectangle2D[] tiles = TreemapSquarify.layout(values, BOUNDS);
		double total = 24;
		double boundsArea = BOUNDS.getWidth() * BOUNDS.getHeight();
		double tiled = 0;
		for (int i = 0; i < tiles.length; i++) {
			double area = tiles[i].getWidth() * tiles[i].getHeight();
			assertEquals(values[i] / total * boundsArea, area, boundsArea * 1e-9,
					"tile " + i + " area proportional to its value");
			assertTrue(BOUNDS.contains(tiles[i]) || tiles[i].isEmpty(), "tile " + i + " inside the bounds");
			tiled += area;
		}
		assertEquals(boundsArea, tiled, boundsArea * 1e-9, "the tiles exactly fill the bounds");
	}

	@Test
	void tilesNeverOverlap() {
		double[] values = { 9, 7, 5, 4, 3, 2, 1, 1, 1 };
		Rectangle2D[] tiles = TreemapSquarify.layout(values, BOUNDS);
		for (int a = 0; a < tiles.length; a++) {
			for (int b = a + 1; b < tiles.length; b++) {
				Rectangle2D overlap = tiles[a].createIntersection(tiles[b]);
				double area = Math.max(0, overlap.getWidth()) * Math.max(0, overlap.getHeight());
				assertTrue(area < 1e-6, "tiles " + a + " and " + b + " overlap by " + area);
			}
		}
	}

	@Test
	void resultOrderMatchesInputOrderWhateverTheSizes() {
		double[] values = { 1, 10, 5 };   // deliberately unsorted
		Rectangle2D[] tiles = TreemapSquarify.layout(values, BOUNDS);
		double boundsArea = BOUNDS.getWidth() * BOUNDS.getHeight();
		assertEquals(10.0 / 16 * boundsArea, tiles[1].getWidth() * tiles[1].getHeight(), 1e-6,
				"the rect at index 1 carries that value's share — order in equals order out");
	}

	@Test
	void zeroAndSingleValuesDegradeGracefully() {
		Rectangle2D[] single = TreemapSquarify.layout(new double[] { 5 }, BOUNDS);
		assertEquals(BOUNDS, single[0], "a single value takes the whole bounds");

		Rectangle2D[] withZero = TreemapSquarify.layout(new double[] { 5, 0 }, BOUNDS);
		assertTrue(withZero[1].isEmpty(), "a zero value gets an empty tile");
	}
}
