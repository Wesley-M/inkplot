package io.github.wesleym.inkplot;

import java.awt.Point;

/**
 * The screen-only camera over a chart surface: a uniform magnification plus pan offset, applied at paint
 * time and never to exports. Fit is scale 1 with zero offset. Past fit the image must keep covering the
 * viewport (no panning off into empty margins); below fit the shrunk chart stays pinned centred, so
 * zooming out reads as stepping back, not as a loose floating thumbnail.
 */
final class ChartViewport {

	static final double MIN_SCALE = 0.25;
	static final double MAX_SCALE = 8.0;

	private double scale = 1.0;
	private double offsetX;
	private double offsetY;

	double scale() {
		return scale;
	}

	double offsetX() {
		return offsetX;
	}

	double offsetY() {
		return offsetY;
	}

	/** True at the untransformed 1:1 fit — the state the cached base image and every export assume. */
	boolean atFit() {
		return scale == 1.0 && offsetX == 0 && offsetY == 0;
	}

	/** True when magnified away from fit in either direction. */
	boolean isZoomed() {
		return scale != 1.0;
	}

	/**
	 * Zooms by {@code factor} about a screen pivot, keeping the point under the pivot fixed (until the
	 * clamp re-centres a below-fit view). Returns whether anything actually changed.
	 */
	boolean zoomAt(int pivotX, int pivotY, double factor, int width, int height) {
		double next = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale * factor));
		if (next == scale) {
			return false;
		}
		double applied = next / scale;
		offsetX = pivotX - (pivotX - offsetX) * applied;
		offsetY = pivotY - (pivotY - offsetY) * applied;
		scale = next;
		clamp(width, height);
		return true;
	}

	/** Pans by a screen-pixel delta; meaningful only past fit (below it the clamp keeps the view centred). */
	void panBy(double dx, double dy, int width, int height) {
		offsetX += dx;
		offsetY += dy;
		clamp(width, height);
	}

	void fit() {
		scale = 1.0;
		offsetX = 0;
		offsetY = 0;
	}

	/** A screen point in base (unzoomed) coordinates: base = (screen - offset) / scale. */
	Point toBase(Point screen) {
		return new Point((int) Math.round((screen.x - offsetX) / scale),
				(int) Math.round((screen.y - offsetY) / scale));
	}

	/** Re-imposes the offset rules for the current viewport size (also needed after a component resize). */
	void clamp(int width, int height) {
		if (scale == 1.0) {
			offsetX = 0;
			offsetY = 0;
		}
		else if (scale < 1.0) {
			offsetX = (width - width * scale) / 2;
			offsetY = (height - height * scale) / 2;
		}
		else {
			offsetX = Math.max(width - width * scale, Math.min(0, offsetX));
			offsetY = Math.max(height - height * scale, Math.min(0, offsetY));
		}
	}
}
