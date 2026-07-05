package io.github.wesleym.inkplot.data;

import io.github.wesleym.inkplot.ChartFormat;

import io.github.wesleym.inkplot.data.Numbers;
import io.github.wesleym.inkplot.data.CategoryKeyer.Resolved;
import io.github.wesleym.inkplot.spec.Aggregate;
import io.github.wesleym.inkplot.spec.CategoryOrder;
import io.github.wesleym.inkplot.spec.ChartSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Turns a {@link ChartSpec} and a {@link Table} into the prepared, primitive-array {@link ChartData} a
 * renderer draws — the one place that reads the string rows. Every path is bounded by {@link ChartLimits}: columns
 * extract into capped arrays, categories fold into "Other", scatter is stride-sampled, and each result carries a
 * {@link Provenance} recording exactly what was left out. Runs off the EDT (see {@link ChartDataPipeline}).
 */
public final class ChartBuilder {

	private ChartBuilder() { }

	public static ChartData build(ChartSpec spec, Table snapshot, int plotWidth) {
		return switch (spec) {
			case ChartSpec.Bar bar -> bar(snapshot, bar);
			case ChartSpec.Doughnut doughnut -> doughnut(snapshot, doughnut);
			case ChartSpec.Waffle waffle -> waffle(snapshot, waffle);
			case ChartSpec.Treemap treemap -> treemap(snapshot, treemap);
			case ChartSpec.Line line -> line(snapshot, line);
			case ChartSpec.Histogram histogram -> histogram(snapshot, histogram);
			case ChartSpec.Density density -> density(snapshot, density);
			case ChartSpec.Scatter scatter -> scatter(snapshot, scatter);
			case ChartSpec.Box box -> box(snapshot, box);
		};
	}

	// ---- bar -----------------------------------------------------------------------------------------

	private static ChartData.Bar bar(Table s, ChartSpec.Bar spec) {
		List<List<String>> rows = s.rows();
		// Colouring bars by their own category (series == the category column) is one full-width bar per category,
		// each a distinct colour — not a grouped bar split into empty per-series slots.
		boolean colorByCategory = spec.series() != null && spec.series().equals(spec.x());
		CategoryKeyer cats = new CategoryKeyer(ChartLimits.MAX_CATEGORY_SCAN);
		CategoryKeyer seriesKeyer = spec.series() != null && !colorByCategory
				? new CategoryKeyer(ChartLimits.MAX_CATEGORY_SCAN) : null;
		for (List<String> row : rows) {
			cats.observe(cell(row, spec.x()));
			if (seriesKeyer != null) {
				seriesKeyer.observe(cell(row, spec.series()));
			}
		}
		if (cats.overflowed()) {
			throw new ChartDataException("Too many distinct values in "
					+ s.columnName(spec.x()) + " to chart as bars — try a histogram or aggregate in SQL.");
		}
		Resolved catR = cats.resolve(ChartLimits.MAX_CATEGORIES);
		Resolved serR = seriesKeyer != null ? seriesKeyer.resolve(ChartLimits.MAX_SERIES) : null;
		int nCat = catR.labels().length;
		int nSer = serR != null ? serR.labels().length : 1;

		double[][] sum = new double[nSer][nCat];
		long[][] count = new long[nSer][nCat];
		double[][] min = new double[nSer][nCat];
		double[][] max = new double[nSer][nCat];
		for (double[] r : min) {
			java.util.Arrays.fill(r, Double.POSITIVE_INFINITY);
		}
		for (double[] r : max) {
			java.util.Arrays.fill(r, Double.NEGATIVE_INFINITY);
		}

		Aggregate agg = spec.agg();
		int skipped = 0;
		for (List<String> row : rows) {
			int c = catR.slotOf(cell(row, spec.x()));
			if (c < 0) {
				continue;
			}
			int ser = serR != null ? serR.slotOf(cell(row, spec.series())) : 0;
			if (ser < 0) {
				ser = 0;
			}
			double value = 1;
			if (agg != Aggregate.COUNT) {
				Double d = numeric(cell(row, spec.y()));
				if (d == null) {
					skipped++;
					continue;
				}
				value = d;
			}
			sum[ser][c] += value;
			count[ser][c]++;
			min[ser][c] = Math.min(min[ser][c], value);
			max[ser][c] = Math.max(max[ser][c], value);
		}

		double[][] values = new double[nSer][nCat];
		for (int ser = 0; ser < nSer; ser++) {
			for (int c = 0; c < nCat; c++) {
				values[ser][c] = finalizeAggregate(agg, sum[ser][c], count[ser][c], min[ser][c], max[ser][c]);
			}
		}

		int[] order = categoryOrder(spec.order(), catR.labels(), values, catR.otherIndex());
		String[] categories = new String[nCat];
		double[][] ordered = new double[nSer][nCat];
		for (int i = 0; i < nCat; i++) {
			categories[i] = catR.labels()[order[i]];
			for (int ser = 0; ser < nSer; ser++) {
				ordered[ser][i] = values[ser][order[i]];
			}
		}

		String[] seriesNames = serR != null ? serR.labels()
				: new String[] { agg == Aggregate.COUNT ? "Count" : s.columnName(spec.y()) };
		Provenance prov = new Provenance(rows.size() - skipped, rows.size(), s.truncated(), skipped, null);
		return new ChartData.Bar(categories, seriesNames, ordered, spec.stacked() && nSer > 1, colorByCategory,
				spec.horizontal(), prov);
	}

	// The category axis in the requested order — value sorts rank by the across-series total, and the folded
	// "Other" bucket stays last under everything except the source order (it's a remainder, not a peer).
	private static int[] categoryOrder(CategoryOrder order, String[] labels, double[][] values, int otherIndex) {
		List<Integer> slots = new ArrayList<>();
		for (int i = 0; i < labels.length; i++) {
			if (order == CategoryOrder.SOURCE || i != otherIndex) {
				slots.add(i);
			}
		}
		switch (order) {
			case VALUE_DESC -> slots.sort((a, b) -> Double.compare(columnTotal(values, b), columnTotal(values, a)));
			case VALUE_ASC -> slots.sort((a, b) -> Double.compare(columnTotal(values, a), columnTotal(values, b)));
			case LABEL -> slots.sort((a, b) -> labels[a].compareToIgnoreCase(labels[b]));
			case SOURCE -> { }
		}
		if (order != CategoryOrder.SOURCE && otherIndex >= 0) {
			slots.add(otherIndex);
		}
		return slots.stream().mapToInt(Integer::intValue).toArray();
	}

	private static double columnTotal(double[][] values, int category) {
		double sum = 0;
		for (double[] series : values) {
			sum += series[category];
		}
		return sum;
	}

	private static double finalizeAggregate(Aggregate agg, double sum, long count, double min, double max) {
		if (count == 0) {
			return 0;
		}
		return switch (agg) {
			case COUNT -> count;
			case SUM -> sum;
			case AVG -> sum / count;
			case MIN -> min;
			case MAX -> max;
		};
	}

	// ---- doughnut / waffle (shares of a whole) ---------------------------------------------------------

	private static ChartData.Doughnut doughnut(Table s, ChartSpec.Doughnut spec) {
		Shares shares = shares(s, spec.x(), spec.y(), "doughnut",
				shareCap(spec.maxSlices(), ChartLimits.MAX_DOUGHNUT_SLICES));
		return new ChartData.Doughnut(shares.labels(), shares.values(), shares.valueLabel(), shares.provenance());
	}

	private static ChartData.Waffle waffle(Table s, ChartSpec.Waffle spec) {
		Shares shares = shares(s, spec.x(), spec.y(), "waffle",
				shareCap(spec.maxCategories(), ChartLimits.MAX_WAFFLE_CATEGORIES));
		return new ChartData.Waffle(shares.labels(), shares.values(), shares.valueLabel(), shares.provenance());
	}

	private static ChartData.Treemap treemap(Table s, ChartSpec.Treemap spec) {
		Shares shares = shares(s, spec.x(), spec.y(), "treemap",
				shareCap(spec.maxCategories(), ChartLimits.MAX_TREEMAP_TILES));
		return new ChartData.Treemap(shares.labels(), shares.values(), shares.valueLabel(), shares.provenance());
	}

	// The user's requested fold point, or the chart's default for 0/unset — floored at two real categories and
	// ceilinged at the palette's distinguishable range (past it, more slices would just repeat colours).
	private static int shareCap(int requested, int chartDefault) {
		return requested <= 0 ? chartDefault : Math.max(2, Math.min(requested, ChartLimits.MAX_SERIES));
	}

	/** The prepared shares-of-a-whole both radial-ish charts draw: positive values, largest-first, "Other" last. */
	private record Shares(String[] labels, double[] values, String valueLabel, Provenance provenance) { }

	private static Shares shares(Table s, int x, Integer y, String chartName, int maxCategories) {
		List<List<String>> rows = s.rows();
		CategoryKeyer cats = new CategoryKeyer(ChartLimits.MAX_CATEGORY_SCAN);
		for (List<String> row : rows) {
			cats.observe(cell(row, x));
		}
		if (cats.overflowed()) {
			throw new ChartDataException("Too many distinct values in " + s.columnName(x)
					+ " to chart as a " + chartName + " — aggregate in SQL first.");
		}
		// Every category gets a slot: the fold to "Other" happens AFTER aggregation, ranked by share, so the
		// kept slices are the biggest ones — never the first-arriving ones (a result ordered smallest-first
		// would otherwise fold the giants into "Other").
		Resolved catR = cats.resolve(Integer.MAX_VALUE);

		boolean countRows = y == null;
		double[] totals = new double[catR.labels().length];
		int skipped = 0;
		for (List<String> row : rows) {
			int c = catR.slotOf(cell(row, x));
			if (c < 0) {
				continue;
			}
			double value = 1;
			if (!countRows) {
				Double d = numeric(cell(row, y));
				if (d == null) {
					skipped++;
					continue;
				}
				value = d;
			}
			totals[c] += value;
		}

		for (int c = 0; c < totals.length; c++) {
			if (totals[c] < 0) {
				throw new ChartDataException(catR.labels()[c] + " totals negative in "
						+ s.columnName(y) + " — a " + chartName + " shows shares of a whole; try bars.");
			}
		}
		List<Integer> positive = new ArrayList<>();
		for (int c = 0; c < totals.length; c++) {
			if (totals[c] > 0) {
				positive.add(c);
			}
		}
		if (positive.isEmpty()) {
			throw new ChartDataException("No positive values in "
					+ s.columnName(countRows ? x : y) + " to chart as a " + chartName + ".");
		}
		positive.sort((a, b) -> Double.compare(totals[b], totals[a]));   // largest share first

		int kept = positive.size() > maxCategories ? maxCategories - 1 : positive.size();
		double otherTotal = 0;
		for (int i = kept; i < positive.size(); i++) {
			otherTotal += totals[positive.get(i)];
		}
		int n = kept + (otherTotal > 0 ? 1 : 0);
		String[] labels = new String[n];
		double[] values = new double[n];
		for (int i = 0; i < kept; i++) {
			labels[i] = catR.labels()[positive.get(i)];
			values[i] = totals[positive.get(i)];
		}
		if (otherTotal > 0) {
			labels[kept] = CategoryKeyer.OTHER;   // the remainder, pinned last whatever its size
			values[kept] = otherTotal;
		}
		String valueLabel = countRows ? "Count" : s.columnName(y);
		Provenance prov = new Provenance(rows.size() - skipped, rows.size(), s.truncated(), skipped, null);
		return new Shares(labels, values, valueLabel, prov);
	}

	// ---- line ----------------------------------------------------------------------------------------

	private static ChartData.Line line(Table s, ChartSpec.Line spec) {
		boolean xTime = new ChartColumns(s).isTemporal(spec.x());
		List<List<String>> rows = s.rows();
		CategoryKeyer seriesKeyer = spec.series() != null ? new CategoryKeyer(ChartLimits.MAX_CATEGORY_SCAN) : null;
		if (seriesKeyer != null) {
			for (List<String> row : rows) {
				seriesKeyer.observe(cell(row, spec.series()));
			}
		}
		Resolved serR = seriesKeyer != null ? seriesKeyer.resolve(ChartLimits.MAX_SERIES) : null;
		int nSer = serR != null ? serR.labels().length : 1;

		int skipped = 0;
		List<ChartData.Line.Series> out = new ArrayList<>();
		if (spec.agg() == null) {
			DoubleList[] xs = news(nSer);
			DoubleList[] ys = news(nSer);
			for (List<String> row : rows) {
				Double x = xTime ? asDouble(TemporalParse.parseMillis(cell(row, spec.x())))
						: numeric(cell(row, spec.x()));
				Double y = numeric(cell(row, spec.y()));
				if (x == null || y == null) {
					skipped++;
					continue;
				}
				int ser = serR != null ? serR.slotOf(cell(row, spec.series())) : 0;
				if (ser < 0) {
					ser = 0;
				}
				xs[ser].add(x);
				ys[ser].add(y);
			}
			for (int ser = 0; ser < nSer; ser++) {
				out.add(sortedSeries(name(serR, ser, s, spec.y()), xs[ser].toArray(), ys[ser].toArray()));
			}
		}
		else {
			List<TreeMap<Double, double[]>> perSeries = new ArrayList<>();
			for (int i = 0; i < nSer; i++) {
				perSeries.add(new TreeMap<>());
			}
			for (List<String> row : rows) {
				Double x = xTime ? asDouble(TemporalParse.parseMillis(cell(row, spec.x())))
						: numeric(cell(row, spec.x()));
				Double y = numeric(cell(row, spec.y()));
				if (x == null || y == null) {
					skipped++;
					continue;
				}
				int ser = serR != null ? serR.slotOf(cell(row, spec.series())) : 0;
				if (ser < 0) {
					ser = 0;
				}
				accumulate(perSeries.get(ser), x, y);
			}
			for (int ser = 0; ser < nSer; ser++) {
				TreeMap<Double, double[]> map = perSeries.get(ser);
				double[] x = new double[map.size()];
				double[] y = new double[map.size()];
				int i = 0;
				for (var e : map.entrySet()) {
					x[i] = e.getKey();
					double[] acc = e.getValue();
					y[i] = finalizeAggregate(spec.agg(), acc[0], (long) acc[1], acc[2], acc[3]);
					i++;
				}
				out.add(new ChartData.Line.Series(name(serR, ser, s, spec.y()), x, y));
			}
		}
		Provenance prov = new Provenance(rows.size() - skipped, rows.size(), s.truncated(), skipped, null);
		return new ChartData.Line(out.toArray(new ChartData.Line.Series[0]), xTime, prov);
	}

	private static void accumulate(TreeMap<Double, double[]> map, double x, double y) {
		double[] acc = map.get(x);
		if (acc == null) {
			map.put(x, new double[] { y, 1, y, y });
		}
		else {
			acc[0] += y;
			acc[1] += 1;
			acc[2] = Math.min(acc[2], y);
			acc[3] = Math.max(acc[3], y);
		}
	}

	// ---- histogram / density -------------------------------------------------------------------------

	private static ChartData.Histogram histogram(Table s, ChartSpec.Histogram spec) {
		ChartExtract.Column col = ChartExtract.numeric(s.rows(), spec.value(), ChartLimits.MAX_VALUES_PER_COLUMN);
		Binning.Result bins = Binning.bins(col.values(), spec.bins());
		return new ChartData.Histogram(bins.edges(), bins.counts(), provenanceFor(s, col, null));
	}

	private static ChartData.Density density(Table s, ChartSpec.Density spec) {
		ChartExtract.Column col = ChartExtract.numeric(s.rows(), spec.value(), ChartLimits.MAX_VALUES_PER_COLUMN);
		Kde.Result kde = Kde.estimate(col.values(), spec.bandwidthFactor(), ChartLimits.KDE_GRID);
		int sampled = Math.min(col.values().length, ChartLimits.KDE_SAMPLE);
		String note = col.values().length > ChartLimits.KDE_SAMPLE
				? "smoothed over " + ChartFormat.grouped(sampled) + " sampled values"
				: null;
		Provenance base = provenanceFor(s, col, note);
		return new ChartData.Density(kde.gridX(), kde.density(), kde.min(), kde.max(), sampled, base);
	}

	// ---- scatter -------------------------------------------------------------------------------------

	private static ChartData.Scatter scatter(Table s, ChartSpec.Scatter spec) {
		boolean xTime = new ChartColumns(s).isTemporal(spec.x());
		ScatterRows rows = scatterRows(s, spec, xTime);
		int[] keep = ChartExtract.stride(rows.x().length, ChartLimits.MAX_SCATTER_POINTS);
		double[] x = gather(rows.x(), keep);
		double[] y = gather(rows.y(), keep);
		int[] series = rows.series().length == 0 ? new int[0] : gather(rows.series(), keep);

		String capNote = scatterCapNote(x.length, rows.x().length, rows.overCap());
		Provenance prov = new Provenance(x.length, s.rowCount(), s.truncated(), rows.skipped(), capNote);
		return new ChartData.Scatter(x, y, series, rows.seriesNames(), xTime, prov);
	}

	private record ScatterRows(double[] x, double[] y, int[] series, String[] seriesNames, int skipped, int overCap) { }

	private static ScatterRows scatterRows(Table s, ChartSpec.Scatter spec, boolean xTime) {
		int cap = ChartLimits.MAX_VALUES_PER_COLUMN;
		int size = Math.min(s.rows().size(), cap);
		double[] xs = new double[size];
		double[] ys = new double[size];
		List<String> rawSeries = new ArrayList<>();
		CategoryKeyer keyer = spec.series() == null ? null : new CategoryKeyer(ChartLimits.MAX_CATEGORY_SCAN);
		int n = 0;
		int skipped = 0;
		int overCap = 0;
		for (List<String> row : s.rows()) {
			Double x = xTime ? asDouble(TemporalParse.parseMillis(cell(row, spec.x())))
					: numeric(cell(row, spec.x()));
			Double y = numeric(cell(row, spec.y()));
			if (x == null || y == null) {
				skipped++;
				continue;
			}
			if (n >= cap) {
				overCap++;
				continue;
			}
			xs[n] = x;
			ys[n] = y;
			if (keyer != null) {
				String series = cell(row, spec.series());
				rawSeries.add(series);
				keyer.observe(series);
			}
			n++;
		}
		Resolved resolved = keyer == null ? null : keyer.resolve(ChartLimits.MAX_SERIES);
		int[] seriesIds = resolved == null ? new int[0] : seriesIds(rawSeries, resolved);
		String[] seriesNames = resolved == null ? new String[] { s.columnName(spec.y()) } : resolved.labels();
		return new ScatterRows(java.util.Arrays.copyOf(xs, n), java.util.Arrays.copyOf(ys, n),
				seriesIds, seriesNames, skipped, overCap);
	}

	private static int[] seriesIds(List<String> rawSeries, Resolved resolved) {
		int[] ids = new int[rawSeries.size()];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = Math.max(0, resolved.slotOf(rawSeries.get(i)));
		}
		return ids;
	}

	private static String scatterCapNote(int plotted, int extracted, int overCap) {
		if (overCap > 0) {
			return "charted the first "
					+ ChartFormat.grouped(extracted)
					+ " valid points";
		}
		if (extracted > ChartLimits.MAX_SCATTER_POINTS) {
			return "sampled " + ChartFormat.grouped(plotted) + " of "
					+ ChartFormat.grouped(extracted) + " points";
		}
		return null;
	}

	// ---- box -----------------------------------------------------------------------------------------

	private static ChartData.Box box(Table s, ChartSpec.Box spec) {
		if (spec.group() == null) {
			ChartExtract.Column col = ChartExtract.numeric(s.rows(), spec.value(), ChartLimits.MAX_VALUES_PER_COLUMN);
			ChartData.Box.Group group = BoxStats.summarize(s.columnName(spec.value()), col.values(),
					ChartLimits.MAX_BOX_OUTLIERS);
			return new ChartData.Box(new ChartData.Box.Group[] { group }, provenanceFor(s, col, null));
		}
		CategoryKeyer keyer = new CategoryKeyer(ChartLimits.MAX_CATEGORY_SCAN);
		for (List<String> row : s.rows()) {
			keyer.observe(cell(row, spec.group()));
		}
		if (keyer.overflowed()) {
			throw new ChartDataException("Too many groups in " + s.columnName(spec.group())
					+ " to chart as boxes — aggregate in SQL first.");
		}
		Resolved resolved = keyer.resolve(ChartLimits.MAX_CATEGORIES);
		DoubleList[] buckets = news(resolved.labels().length);
		int skipped = 0;
		for (List<String> row : s.rows()) {
			int slot = resolved.slotOf(cell(row, spec.group()));
			Double v = numeric(cell(row, spec.value()));
			if (slot < 0 || v == null) {
				skipped++;
				continue;
			}
			buckets[slot].add(v);
		}
		ChartData.Box.Group[] groups = new ChartData.Box.Group[resolved.labels().length];
		for (int i = 0; i < groups.length; i++) {
			groups[i] = BoxStats.summarize(resolved.labels()[i], buckets[i].toArray(), ChartLimits.MAX_BOX_OUTLIERS);
		}
		Provenance prov = new Provenance(s.rowCount() - skipped, s.rowCount(), s.truncated(), skipped, null);
		return new ChartData.Box(groups, prov);
	}

	// ---- shared helpers ------------------------------------------------------------------------------

	private static Provenance provenanceFor(Table s, ChartExtract.Column col, String capNote) {
		String note = capNote;
		if (note == null && col.overCap() > 0) {
			note = "charted the first " + ChartFormat.grouped(col.values().length)
					+ " values";
		}
		return new Provenance(col.values().length, s.rowCount(), s.truncated(), col.skipped(), note);
	}

	private static ChartData.Line.Series sortedSeries(String name, double[] x, double[] y) {
		Integer[] order = new Integer[x.length];
		for (int i = 0; i < order.length; i++) {
			order[i] = i;
		}
		java.util.Arrays.sort(order, (a, b) -> Double.compare(x[a], x[b]));
		double[] sx = new double[x.length];
		double[] sy = new double[y.length];
		for (int i = 0; i < order.length; i++) {
			sx[i] = x[order[i]];
			sy[i] = y[order[i]];
		}
		return new ChartData.Line.Series(name, sx, sy);
	}

	private static String name(Resolved serR, int ser, Table s, int yCol) {
		return serR != null ? serR.labels()[ser] : s.columnName(yCol);
	}

	private static DoubleList[] news(int n) {
		DoubleList[] arr = new DoubleList[n];
		for (int i = 0; i < n; i++) {
			arr[i] = new DoubleList(256, ChartLimits.MAX_VALUES_PER_COLUMN);
		}
		return arr;
	}

	private static double[] gather(double[] source, int[] indices) {
		double[] out = new double[indices.length];
		for (int i = 0; i < indices.length; i++) {
			out[i] = source[indices[i]];
		}
		return out;
	}

	private static int[] gather(int[] source, int[] indices) {
		int[] out = new int[indices.length];
		for (int i = 0; i < indices.length; i++) {
			out[i] = source[indices[i]];
		}
		return out;
	}

	private static Double asDouble(Long v) {
		return v == null ? null : (double) v;
	}

	// Parses a numeric cell, treating a non-finite result as unparseable — Double.valueOf accepts "NaN"/"Infinity"/
	// overflowing literals, and a single one would poison aggregates and axis domains (NaN spreads through min/max).
	private static Double numeric(String cell) {
		Double d = Numbers.parse(cell);
		return d != null && Double.isFinite(d) ? d : null;
	}

	private static String cell(List<String> row, int col) {
		return col >= 0 && col < row.size() ? row.get(col) : null;
	}
}
