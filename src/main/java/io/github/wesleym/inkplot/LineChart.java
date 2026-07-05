package io.github.wesleym.inkplot;

import io.github.wesleym.inkplot.data.ChartData;
import io.github.wesleym.inkplot.data.Provenance;

import java.util.ArrayList;
import java.util.List;

/**
 * A line chart accumulating series. X values are plain numbers by default; {@link #timeAxis()} reads them
 * as epoch milliseconds and labels the axis with calendar ticks.
 */
public final class LineChart extends Chart {

	private final List<ChartData.Line.Series> series = new ArrayList<>();
	private boolean timeAxis;
	private int points;

	LineChart() {
		super(null);
		rebuild();
	}

	/** Adds one named series of {@code (x, y)} pairs; arrays must be the same length. */
	public LineChart series(String name, double[] x, double[] y) {
		if (x.length != y.length) {
			throw new IllegalArgumentException("series '" + name + "': " + x.length + " x values for "
					+ y.length + " y values");
		}
		series.add(new ChartData.Line.Series(name, x.clone(), y.clone()));
		points += x.length;
		rebuild();
		return this;
	}

	/** Reads X values as epoch milliseconds and labels the axis with calendar ticks. */
	public LineChart timeAxis() {
		this.timeAxis = true;
		rebuild();
		return this;
	}

	private void rebuild() {
		setData(new ChartData.Line(series.toArray(ChartData.Line.Series[]::new), timeAxis,
				Provenance.full(points)));
	}
}
