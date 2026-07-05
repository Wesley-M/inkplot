package io.github.wesleym.inkplot.data;

/**
 * What a chart's data does and doesn't cover, so the view can be honest about it — never a silently partial picture.
 * Carries how many result rows were charted of how many the query returned, whether the result itself was capped at
 * the grid's row limit, how many cells were dropped as unparseable, and an optional note when a chart-side point cap
 * engaged (e.g. a sampled scatter).
 */
public record Provenance(int shownRows, int totalRows, boolean resultTruncated, int skippedCells, String capNote) {

	/** A clean provenance for data that charted every row with nothing dropped. */
	public static Provenance full(int rows) {
		return new Provenance(rows, rows, false, 0, null);
	}

	/** Whether fewer rows were charted than the query produced (a chart-side extraction cap engaged). */
	public boolean rowsCapped() {
		return shownRows < totalRows;
	}

	/** Whether there is anything at all worth telling the reader about coverage. */
	public boolean hasNotice() {
		return resultTruncated || rowsCapped() || skippedCells > 0 || capNote != null;
	}
}
