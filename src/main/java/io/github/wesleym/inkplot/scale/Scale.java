package io.github.wesleym.inkplot.scale;

/**
 * Maps a data value onto a pixel position within a plot. Each variant carries both its data domain and the pixel
 * range it projects onto, so a scale is a self-contained, immutable value built once at layout time: renderers ask
 * it for pixels and the hover layer asks a continuous scale to invert a pixel back to a value.
 *
 * <p>For a Y axis the caller passes {@code px0} = the bottom (larger pixel) and {@code px1} = the top (smaller
 * pixel), so a larger value maps upward; for an X axis {@code px0} is the left.
 */
public sealed interface Scale permits Scale.Linear, Scale.Log, Scale.Time, Scale.Band {

	/** Projects a data value (or, for a band scale, a category index) to a pixel coordinate. */
	double map(double value);

	/** A continuous linear number axis over {@code [min, max]}. */
	record Linear(double min, double max, double px0, double px1) implements Scale {
		@Override
		public double map(double value) {
			double t = max == min ? 0.5 : (value - min) / (max - min);
			return px0 + t * (px1 - px0);
		}

		/** The data value at a pixel — for hover read-out. */
		public double invert(double px) {
			double t = px1 == px0 ? 0 : (px - px0) / (px1 - px0);
			return min + t * (max - min);
		}
	}

	/**
	 * A continuous logarithmic (base-10) number axis over {@code [min, max]}, both strictly positive. A log axis
	 * only applies to all-positive data; the caller offers it only when the domain minimum is above zero, and this
	 * clamps defensively so a stray non-positive value can never produce a NaN pixel.
	 */
	record Log(double min, double max, double px0, double px1) implements Scale {

		private static final double FLOOR = 1e-12;

		@Override
		public double map(double value) {
			double lo = Math.log10(Math.max(FLOOR, min));
			double hi = Math.log10(Math.max(FLOOR, max));
			double v = Math.log10(Math.max(FLOOR, value));
			double t = hi == lo ? 0.5 : (v - lo) / (hi - lo);
			return px0 + t * (px1 - px0);
		}

		public double invert(double px) {
			double lo = Math.log10(Math.max(FLOOR, min));
			double hi = Math.log10(Math.max(FLOOR, max));
			double t = px1 == px0 ? 0 : (px - px0) / (px1 - px0);
			return Math.pow(10, lo + t * (hi - lo));
		}
	}

	/** A continuous time axis over epoch-millis {@code [min, max]}. */
	record Time(long min, long max, double px0, double px1) implements Scale {
		@Override
		public double map(double value) {
			double t = max == min ? 0.5 : (value - min) / (double) (max - min);
			return px0 + t * (px1 - px0);
		}

		public long invert(double px) {
			double t = px1 == px0 ? 0 : (px - px0) / (px1 - px0);
			return Math.round(min + t * (max - min));
		}
	}

	/** A discrete band axis of {@code count} equal slots, each holding a bar/box with a fractional inner gap. */
	record Band(int count, double px0, double px1, double innerPad) implements Scale {
		@Override
		public double map(double index) {
			return center((int) Math.round(index));
		}

		/** The full slot width allotted to one category (mark plus its surrounding air). */
		public double slot() {
			return count <= 0 ? 0 : (px1 - px0) / count;
		}

		/** The painted mark width — the slot minus the inner gap that keeps neighbours apart. */
		public double bandWidth() {
			return slot() * (1 - innerPad);
		}

		public double center(int i) {
			return px0 + slot() * (i + 0.5);
		}

		public double start(int i) {
			return center(i) - bandWidth() / 2;
		}

		/** The category index nearest a pixel, clamped to {@code [0, count)} — for hover hit-testing. */
		public int indexAt(double px) {
			if (count <= 0 || slot() == 0) {
				return -1;
			}
			int i = (int) Math.floor((px - px0) / slot());
			return Math.max(0, Math.min(count - 1, i));
		}
	}
}
