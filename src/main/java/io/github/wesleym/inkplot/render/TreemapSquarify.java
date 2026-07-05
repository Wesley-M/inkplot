package io.github.wesleym.inkplot.render;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The squarified treemap layout (Bruls, Huizing, van Wijk): tiles whose areas are proportional to their
 * values, packed row by row along the free region's shorter side, each row admitting items only while the
 * worst aspect ratio in it improves — so tiles come out near-square instead of splintering. Values may
 * arrive in any order (the algorithm works size-descending internally); the returned rectangles are in the
 * caller's original order, so tile index i always maps to value i.
 */
public final class TreemapSquarify {

	private TreemapSquarify() { }

	/** One rectangle per value, areas proportional, exactly tiling {@code bounds}; zero values get empty rects. */
	public static Rectangle2D[] layout(double[] values, Rectangle2D bounds) {
		Rectangle2D[] tiles = new Rectangle2D[values.length];
		List<Integer> order = new ArrayList<>();
		double total = 0;
		for (int i = 0; i < values.length; i++) {
			if (values[i] > 0) {
				order.add(i);
				total += values[i];
			}
			else {
				tiles[i] = new Rectangle2D.Double(bounds.getX(), bounds.getY(), 0, 0);
			}
		}
		if (order.isEmpty() || total <= 0) {
			return tiles;
		}
		order.sort(Comparator.comparingDouble((Integer i) -> values[i]).reversed());

		double scale = bounds.getWidth() * bounds.getHeight() / total;   // value -> area
		Rectangle2D free = new Rectangle2D.Double(bounds.getX(), bounds.getY(),
				bounds.getWidth(), bounds.getHeight());
		List<Integer> row = new ArrayList<>();
		double rowArea = 0;
		for (int index : order) {
			double area = values[index] * scale;
			double side = Math.min(free.getWidth(), free.getHeight());
			if (!row.isEmpty() && worst(rowArea + area, minArea(row, values, scale, area), maxArea(row, values, scale, area), side)
					> worst(rowArea, minArea(row, values, scale, -1), maxArea(row, values, scale, -1), side)) {
				free = placeRow(row, values, scale, free, tiles);
				row.clear();
				rowArea = 0;
			}
			row.add(index);
			rowArea += area;
		}
		placeRow(row, values, scale, free, tiles);
		return tiles;
	}

	// The worst (largest) aspect ratio a row of tiles would have, laid along a side of the given length.
	private static double worst(double rowArea, double minArea, double maxArea, double side) {
		double s2 = rowArea * rowArea;
		double w2 = side * side;
		return Math.max(w2 * maxArea / s2, s2 / (w2 * minArea));
	}

	private static double minArea(List<Integer> row, double[] values, double scale, double candidate) {
		double min = candidate > 0 ? candidate : Double.MAX_VALUE;
		for (int i : row) {
			min = Math.min(min, values[i] * scale);
		}
		return min;
	}

	private static double maxArea(List<Integer> row, double[] values, double scale, double candidate) {
		double max = candidate > 0 ? candidate : 0;
		for (int i : row) {
			max = Math.max(max, values[i] * scale);
		}
		return max;
	}

	// Lays the row along the free region's shorter side and returns the remaining free region.
	private static Rectangle2D placeRow(List<Integer> row, double[] values, double scale, Rectangle2D free,
			Rectangle2D[] tiles) {
		if (row.isEmpty()) {
			return free;
		}
		double rowArea = 0;
		for (int i : row) {
			rowArea += values[i] * scale;
		}
		boolean vertical = free.getWidth() >= free.getHeight();   // row stacks down the LEFT edge of a wide region
		double side = vertical ? free.getHeight() : free.getWidth();
		double thickness = rowArea / side;
		double offset = 0;
		for (int i : row) {
			double length = values[i] * scale / thickness;
			if (vertical) {
				tiles[i] = new Rectangle2D.Double(free.getX(), free.getY() + offset, thickness, length);
			}
			else {
				tiles[i] = new Rectangle2D.Double(free.getX() + offset, free.getY(), length, thickness);
			}
			offset += length;
		}
		return vertical
				? new Rectangle2D.Double(free.getX() + thickness, free.getY(),
						free.getWidth() - thickness, free.getHeight())
				: new Rectangle2D.Double(free.getX(), free.getY() + thickness,
						free.getWidth(), free.getHeight() - thickness);
	}
}
