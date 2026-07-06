package io.github.wesleym.inkplot.render;

import java.awt.AlphaComposite;
import java.awt.Composite;

/**
 * Per-series emphasis driven by the interactive legend on the live component: which series are toggled off
 * (hidden), and which one — if any — is focused so the rest recede. Exports always pass {@link #NONE}.
 */
public record SeriesEmphasis(boolean[] hidden, int focus) {

	public static final SeriesEmphasis NONE = new SeriesEmphasis(new boolean[0], -1);

	public boolean isHidden(int series) {
		return hidden != null && series >= 0 && series < hidden.length && hidden[series];
	}

	/** The composite to draw {@code series} with: {@code base}, or a dimmed one when another series is focused. */
	public Composite composite(int series, Composite base) {
		return focus < 0 || series == focus ? base : AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f);
	}

	/** A dim factor for renderers that already blend per-mark (scatter): 1.0, or a fraction when another is focused. */
	public double dim(int series) {
		return focus < 0 || series == focus ? 1.0 : 0.22;
	}
}
