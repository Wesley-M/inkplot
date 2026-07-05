package io.github.wesleym.inkplot.data;

/**
 * Every hard cap the chart data layer enforces, in one place, so the memory a chart can consume is bounded and
 * auditable. The source string rows stay the single copy of the data; a chart only ever holds the compact primitive
 * arrays these limits bound, and whenever a cap engages the view says so (never a silently partial picture).
 */
public final class ChartLimits {

	private ChartLimits() { }

	/** Max values extracted per column into a {@code double[]} (~4 MB each); rows past it are skipped and flagged. */
	public static final int MAX_VALUES_PER_COLUMN = 500_000;

	/** Max scatter points actually rendered; beyond it a uniform-stride sample keeps the shape at bounded cost. */
	public static final int MAX_SCATTER_POINTS = 20_000;

	/** Max distinct categories on a bar/box axis; the overflow folds into a single "Other" bucket. */
	public static final int MAX_CATEGORIES = 40;

	/** Default doughnut slices before the tail folds into "Other" — past this, slices stop being readable as
	 *  shares. The picker's Slices knob overrides it per chart, ceilinged at {@link #MAX_SERIES}. */
	public static final int MAX_DOUGHNUT_SLICES = 10;

	/** Default waffle categories before the tail folds into "Other" — its fine-grained grid stays legible with
	 *  more groups than a ring does, so it gets a higher default. Same picker override as the doughnut. */
	public static final int MAX_WAFFLE_CATEGORIES = 15;

	/** Default treemap tiles before the tail folds into "Other" — area encodes the share, so a treemap stays
	 *  readable with the most categories of the share charts. Same picker override as the doughnut. */
	public static final int MAX_TREEMAP_TILES = 24;

	/** Hard stop on category discovery — past this a column is not category-like and the chart says so. */
	public static final int MAX_CATEGORY_SCAN = MAX_CATEGORIES * 25;

	/** Max series slots before the tail folds into "Other". Past its base eight the theme generates colours along
	 *  two axes — golden-angle hue rotation crossed with lighter/vivid/deeper shade tiers — so slots stay
	 *  distinguishable this deep. */
	public static final int MAX_SERIES = 96;

	/** Max outlier dots drawn per box group. */
	public static final int MAX_BOX_OUTLIERS = 200;

	/** Max values fed to the KDE, so density estimation stays O(grid × cap) regardless of result size. */
	public static final int KDE_SAMPLE = 100_000;

	/** Number of points the KDE and density curve are evaluated on. */
	public static final int KDE_GRID = 256;

	/** Upper bound on histogram bins, whatever the auto-sizing or the user requests. */
	public static final int HISTOGRAM_MAX_BINS = 60;
}
