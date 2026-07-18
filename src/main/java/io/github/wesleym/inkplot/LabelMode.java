package io.github.wesleym.inkplot;

/**
 * How much of a chart's category-axis labels to draw. The chart is interactive, so labels are progressive
 * disclosure, not decoration — hover always reveals the exact category and value of the mark under the pointer.
 *
 * <ul>
 *   <li>{@link #AUTO} — draw all labels when they fit, thin to an evenly-spaced subset when they'd collide.</li>
 *   <li>{@link #ALL} — always draw every label (rotated and elided as needed), even when cramped.</li>
 *   <li>{@link #OFF} — draw no category labels; rely on hover to name each mark.</li>
 * </ul>
 *
 * The value axis (its few round numbers) is unaffected — this governs the category/band axis, the one that walls up.
 */
public enum LabelMode {
	AUTO,
	ALL,
	OFF
}
