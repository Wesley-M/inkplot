package io.github.wesleym.inkplot.render;

import java.awt.geom.Rectangle2D;

/**
 * A mark the pointer is over. The two indices are interpreted by the renderer that produced the hit (for a bar:
 * series and category; a scatter point: point index; a line: series and point; a box: group), and {@code bounds} is
 * the mark's device-space rectangle so the canvas can lift/outline exactly it.
 */
public record MarkHit(int a, int b, Rectangle2D bounds) {

	public static MarkHit of(int a, int b, Rectangle2D bounds) {
		return new MarkHit(a, b, bounds);
	}
}
