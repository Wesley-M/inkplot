package io.github.wesleym.inkplot.data;

/**
 * Gaussian kernel density estimate of a numeric sample — the smooth curve behind the density chart. The bandwidth
 * follows Silverman's rule (scaled by a user factor), and the sample is stride-capped so the cost stays
 * O(grid × cap) whatever the result size. Allocates only the output grid arrays plus one bounded sorted copy.
 */
public final class Kde {

	private Kde() { }

	/** The evaluation grid, the density at each grid point, and the data extent the curve spans. */
	public record Result(double[] gridX, double[] density, double min, double max) { }

	public static Result estimate(double[] input, double bandwidthFactor, int gridPoints) {
		double[] values = cap(input, ChartLimits.KDE_SAMPLE);
		int n = values.length;
		if (n == 0) {
			return new Result(new double[] { 0, 1 }, new double[] { 0, 0 }, 0, 1);
		}
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		double sum = 0;
		for (double v : values) {
			min = Math.min(min, v);
			max = Math.max(max, v);
			sum += v;
		}
		double mean = sum / n;
		double variance = 0;
		for (double v : values) {
			variance += (v - mean) * (v - mean);
		}
		double sd = Math.sqrt(variance / Math.max(1, n));
		if (sd <= 0 || min == max) {
			// A spread-less sample has no density curve to draw; return a flat, honest line.
			return new Result(new double[] { min - 0.5, min + 0.5 }, new double[] { 0, 0 }, min - 0.5, min + 0.5);
		}

		double bandwidth = Math.max(1e-9, 1.06 * sd * Math.pow(n, -0.2) * Math.max(0.1, bandwidthFactor));
		double lo = min - 3 * bandwidth;
		double hi = max + 3 * bandwidth;
		int grid = Math.max(16, gridPoints);
		double[] gridX = new double[grid];
		double[] density = new double[grid];
		double norm = 1.0 / (n * bandwidth * Math.sqrt(2 * Math.PI));
		for (int i = 0; i < grid; i++) {
			double x = lo + (hi - lo) * i / (grid - 1);
			gridX[i] = x;
			double acc = 0;
			for (double v : values) {
				double u = (x - v) / bandwidth;
				acc += Math.exp(-0.5 * u * u);
			}
			density[i] = acc * norm;
		}
		return new Result(gridX, density, lo, hi);
	}

	private static double[] cap(double[] values, int limit) {
		if (values.length <= limit) {
			return values;
		}
		double[] sample = new double[limit];
		for (int i = 0; i < limit; i++) {
			sample[i] = values[(int) ((long) i * values.length / limit)];
		}
		return sample;
	}
}
