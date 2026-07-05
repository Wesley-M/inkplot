package io.github.wesleym.inkplot;

import java.awt.Font;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/**
 * The library's layout and type tokens: a 4-pt spacing rhythm, a small type scale, and the pixel scale every
 * dimension routes through. Out of the box the scale is 1.0 and the base font is the Swing default — a host
 * application with its own UI zoom or font installs suppliers once via {@link #scaleWith} / {@link #fontWith},
 * and every chart tracks them from then on (dimensions are read at paint time, not cached at construction).
 */
public final class ChartStyle {

	private ChartStyle() { }

	// A single 4-pt spacing rhythm; every gap/inset is one of these steps.
	public static final int SPACE_XS = 4;
	public static final int SPACE_S = 8;
	public static final int SPACE_M = 12;
	public static final int SPACE_L = 16;
	public static final int SPACE_XL = 24;

	public static final int RADIUS = 8;

	// Type scale, in points.
	public static final float CAPTION = 11.5f;
	public static final float BODY = 13f;
	public static final float TITLE = 15f;

	private static volatile DoubleSupplier scale = () -> 1.0;
	private static volatile Supplier<Font> baseFont = ChartStyle::systemFont;

	/** Routes every chart dimension through the host's UI scale (e.g. an app-wide zoom). */
	public static void scaleWith(DoubleSupplier supplier) {
		scale = supplier != null ? supplier : () -> 1.0;
	}

	/** Uses the host's base font for chart text (sizes still come from the library's type scale). */
	public static void fontWith(Supplier<Font> supplier) {
		baseFont = supplier != null ? supplier : ChartStyle::systemFont;
	}

	/** A logical dimension scaled to pixels — never below one. */
	public static int px(int value) {
		return Math.max(1, (int) Math.round(value * scale.getAsDouble()));
	}

	public static Font font(float size, int style) {
		return baseFont.get().deriveFont(style, (float) (size * scale.getAsDouble()));
	}

	public static Font caption() {
		return font(CAPTION, Font.PLAIN);
	}

	public static Font body() {
		return font(BODY, Font.PLAIN);
	}

	public static Font title() {
		return font(TITLE, Font.BOLD);
	}

	private static Font systemFont() {
		Font font = javax.swing.UIManager.getFont("defaultFont");
		return font != null ? font : new Font("SansSerif", Font.PLAIN, 13);
	}
}
