package io.github.wesleym.inkplot.data;

import java.util.Arrays;

/**
 * A minimal growable {@code double[]} with a hard capacity cap, for building a chart series without boxing every value
 * into a {@code Double}. Once the cap is reached it silently stops accepting values (the caller counts the overflow),
 * so a runaway series can never grow the heap without bound.
 */
final class DoubleList {

	private double[] data;
	private int size;
	private final int cap;

	DoubleList(int initialCapacity, int cap) {
		this.data = new double[Math.max(4, Math.min(initialCapacity, cap))];
		this.cap = cap;
	}

	/** Appends {@code v}; returns false (and drops it) once the cap is reached. */
	boolean add(double v) {
		if (size >= cap) {
			return false;
		}
		if (size == data.length) {
			data = Arrays.copyOf(data, Math.min(cap, Math.max(data.length * 2, 4)));
		}
		data[size++] = v;
		return true;
	}

	int size() {
		return size;
	}

	double[] toArray() {
		return Arrays.copyOf(data, size);
	}
}
