package io.github.wesleym.inkplot.data;


import java.util.ArrayList;
import java.util.List;

/**
 * Classifies each result column into the roles a chart picker cares about — numeric, temporal, category-like — from
 * the declared type when there is one, and from a small value sample when there isn't (many sources return
 * untyped columns). Cheap enough to run on the EDT when the picker opens: typed columns cost nothing and untyped ones sniff
 * only a bounded sample.
 */
public final class ChartColumns {

	private static final int SNIFF_ROWS = 200;
	private static final int SNIFF_MIN_MATCH = 8;      // need a few real values before trusting a sniff
	private static final double SNIFF_RATIO = 0.9;     // this share of sampled values must match the role
	private static final int CATEGORY_DISTINCT_CAP = 40;

	private final Table snapshot;
	private final boolean[] numeric;
	private final boolean[] temporal;
	private final boolean[] categorical;

	public ChartColumns(Table snapshot) {
		this.snapshot = snapshot;
		int n = snapshot.columnCount();
		this.numeric = new boolean[n];
		this.temporal = new boolean[n];
		this.categorical = new boolean[n];
		for (int col = 0; col < n; col++) {
			classify(col);
		}
	}

	private void classify(int col) {
		ColumnKind kind = ColumnKind.of(snapshot.columnType(col));
		switch (kind) {
			case NUMERIC -> numeric[col] = true;
			case TEMPORAL -> temporal[col] = true;
			case TEXT, BOOLEAN -> categorical[col] = true;
			case OTHER -> sniff(col);
		}
		// A low-cardinality numeric (status codes, small ids) is also usable as a category axis.
		if (numeric[col] && distinct(col) <= CATEGORY_DISTINCT_CAP) {
			categorical[col] = true;
		}
	}

	// Untyped column: decide from a bounded value sample — all-numeric wins, else mostly-timestamps, else category.
	private void sniff(int col) {
		int seen = 0;
		int nums = 0;
		int times = 0;
		for (int r = 0; r < snapshot.rowCount() && seen < SNIFF_ROWS; r++) {
			String v = cell(col, r);
			if (v == null || v.isBlank()) {
				continue;
			}
			seen++;
			if (Numbers.parse(v) != null) {
				nums++;
			}
			else if (TemporalParse.parseMillis(v) != null) {
				times++;
			}
		}
		if (seen >= SNIFF_MIN_MATCH && nums >= seen * SNIFF_RATIO) {
			numeric[col] = true;
		}
		else if (seen >= SNIFF_MIN_MATCH && (nums + times) >= seen * SNIFF_RATIO && times >= nums) {
			temporal[col] = true;
		}
		else {
			categorical[col] = true;   // anything else reads as an identity/label column
		}
	}

	public boolean isNumeric(int col) {
		return inRange(col) && numeric[col];
	}

	public boolean isTemporal(int col) {
		return inRange(col) && temporal[col];
	}

	public boolean isCategorical(int col) {
		return inRange(col) && categorical[col];
	}

	/** Columns usable as a value/Y axis (numeric). */
	public List<Integer> numericColumns() {
		return collect(numeric);
	}

	/** Columns usable as a continuous/time X (numeric or temporal). */
	public List<Integer> continuousColumns() {
		List<Integer> out = new ArrayList<>();
		for (int c = 0; c < snapshot.columnCount(); c++) {
			if (numeric[c] || temporal[c]) {
				out.add(c);
			}
		}
		return out;
	}

	/** Columns usable as a category / series split. */
	public List<Integer> categoryColumns() {
		return collect(categorical);
	}

	/** Columns that are timestamps. */
	public List<Integer> temporalColumns() {
		return collect(temporal);
	}

	public String name(int col) {
		return snapshot.columnName(col);
	}

	// Approximate distinctness from a bounded sample, so a high-cardinality numeric isn't offered as a
	// category axis.
	private int distinct(int col) {
		int seen = 0;
		java.util.HashSet<String> values = new java.util.HashSet<>();
		for (int r = 0; r < snapshot.rowCount() && seen < SNIFF_ROWS; r++) {
			String v = cell(col, r);
			if (v != null) {
				seen++;
				values.add(v);
			}
		}
		return values.size() >= SNIFF_ROWS ? Integer.MAX_VALUE : values.size();
	}

	private String cell(int col, int row) {
		List<String> r = snapshot.rows().get(row);
		return col >= 0 && col < r.size() ? r.get(col) : null;
	}

	private List<Integer> collect(boolean[] flags) {
		List<Integer> out = new ArrayList<>();
		for (int c = 0; c < flags.length; c++) {
			if (flags[c]) {
				out.add(c);
			}
		}
		return out;
	}

	private boolean inRange(int col) {
		return col >= 0 && col < numeric.length;
	}
}
