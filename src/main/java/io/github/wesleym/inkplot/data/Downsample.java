package io.github.wesleym.inkplot.data;

/**
 * Reduces a dense series to about as many points as there are pixels, so a line over a million rows draws in
 * pixel-time and never allocates a path with more vertices than the screen can show. Both methods return the indices
 * to keep (never a copy of the data), and both preserve the visual extremes a naive stride would drop.
 */
public final class Downsample {

	private Downsample() { }

	/**
	 * Largest-Triangle-Three-Buckets downsampling to {@code target} points: keeps the first and last, and within each
	 * bucket the point that best preserves the curve's shape — so peaks and troughs survive. Returns all indices when
	 * the series already fits.
	 */
	public static int[] lttb(double[] x, double[] y, int target) {
		int n = x.length;
		if (target >= n || target < 3) {
			return identity(n);
		}
		int[] sampled = new int[target];
		int si = 0;
		sampled[si++] = 0;
		double every = (double) (n - 2) / (target - 2);
		int a = 0;
		for (int i = 0; i < target - 2; i++) {
			int avgStart = (int) Math.floor((i + 1) * every) + 1;
			int avgEnd = Math.min((int) Math.floor((i + 2) * every) + 1, n);
			double avgX = 0;
			double avgY = 0;
			int len = Math.max(1, avgEnd - avgStart);
			for (int j = avgStart; j < avgEnd; j++) {
				avgX += x[j];
				avgY += y[j];
			}
			avgX /= len;
			avgY /= len;

			int rangeFrom = (int) Math.floor(i * every) + 1;
			int rangeTo = (int) Math.floor((i + 1) * every) + 1;
			double ax = x[a];
			double ay = y[a];
			double maxArea = -1;
			int next = rangeFrom;
			for (int j = rangeFrom; j < rangeTo && j < n; j++) {
				double area = Math.abs((ax - avgX) * (y[j] - ay) - (ax - x[j]) * (avgY - ay));
				if (area > maxArea) {
					maxArea = area;
					next = j;
				}
			}
			sampled[si++] = next;
			a = next;
		}
		sampled[si] = n - 1;
		return sampled;
	}

	/**
	 * Min/max decimation: splits the index range into {@code buckets} and keeps the lowest and highest {@code y} in
	 * each (in index order), so the drawn envelope never loses a spike. Best where x is evenly spaced.
	 */
	public static int[] minMaxPerBucket(double[] y, int buckets) {
		int n = y.length;
		if (buckets <= 0 || n <= 2 * buckets) {
			return identity(n);
		}
		int[] out = new int[buckets * 2];
		int count = 0;
		for (int b = 0; b < buckets; b++) {
			int from = (int) ((long) b * n / buckets);
			int to = (int) ((long) (b + 1) * n / buckets);
			if (to <= from) {
				continue;
			}
			int minIdx = from;
			int maxIdx = from;
			for (int j = from + 1; j < to; j++) {
				if (y[j] < y[minIdx]) {
					minIdx = j;
				}
				if (y[j] > y[maxIdx]) {
					maxIdx = j;
				}
			}
			int first = Math.min(minIdx, maxIdx);
			int second = Math.max(minIdx, maxIdx);
			out[count++] = first;
			if (second != first) {
				out[count++] = second;
			}
		}
		if (count == out.length) {
			return out;
		}
		int[] trimmed = new int[count];
		System.arraycopy(out, 0, trimmed, 0, count);
		return trimmed;
	}

	private static int[] identity(int n) {
		int[] all = new int[n];
		for (int i = 0; i < n; i++) {
			all[i] = i;
		}
		return all;
	}
}
