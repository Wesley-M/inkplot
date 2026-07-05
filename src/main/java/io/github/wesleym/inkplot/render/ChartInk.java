package io.github.wesleym.inkplot.render;

import io.github.wesleym.inkplot.ChartTheme;

import java.awt.Color;

/**
 * The chart layer's colour vocabulary — pure functions over the live {@link ChartTheme}. Keeping every chart colour
 * behind one helper means the marks read from the theme on each paint (so a theme switch is a bare repaint) and the
 * design language lives in one place: series identity from the categorical palette, a ~10% area wash, a hover lift,
 * a single-hue sequential ramp for distributions, and recessive grid/axis ink.
 */
public final class ChartInk {

	private ChartInk() { }

	/** Identity colour for a series slot (wraps past the eighth; callers fold the overflow into "Other"). */
	public static Color series(ChartTheme theme, int slot) {
		return theme.series(slot);
	}

	/** The muted fill for the folded "Other" bucket — never a categorical hue. */
	public static Color otherGray(ChartTheme theme) {
		return theme.muted();
	}

	/** A translucent copy of {@code c} at the given opacity (0..1). */
	public static Color alpha(Color c, double opacity) {
		int a = (int) Math.round(Math.max(0, Math.min(1, opacity)) * 255);
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
	}

	/** The ~10% area wash beneath a line or density curve — a hint of the hue, never a saturated block. */
	public static Color areaWash(Color c) {
		return alpha(c, 0.10);
	}

	/** The hovered-mark lift: nudges toward white on a dark theme, toward black on a light one, so it reads either way. */
	public static Color lift(ChartTheme theme, Color c) {
		return mix(c, theme.dark() ? Color.WHITE : Color.BLACK, 0.16);
	}

	/** Solid single-hue fill for a distribution (histogram bars) — slot 1, no identity job to do. */
	public static Color distributionFill(ChartTheme theme) {
		return theme.series(0);
	}

	/**
	 * A point on the slot-1 sequential ramp for {@code t} in 0..1 (0 = near the surface, 1 = the full hue) — for a
	 * magnitude encoding such as a density fill or a future heat grid.
	 */
	public static Color sequential(ChartTheme theme, double t) {
		double clamped = Math.max(0, Math.min(1, t));
		return mix(theme.surface(), theme.series(0), 0.20 + 0.80 * clamped);
	}

	/** Recessive gridline / axis ink — one step off the surface. */
	public static Color grid(ChartTheme theme) {
		return theme.hairline();
	}

	/** Axis-tick and axis-label ink — a text token, never a series colour. */
	public static Color axisText(ChartTheme theme) {
		return theme.muted();
	}

	/** The 2px separating gap / marker ring colour — the surface itself doing the separating. */
	public static Color separator(ChartTheme theme) {
		return theme.surface();
	}

	/** Linear blend: {@code t} of {@code b} over {@code a}, preserving {@code a}'s alpha. */
	static Color mix(Color a, Color b, double t) {
		return new Color(
				clamp8(a.getRed() + (b.getRed() - a.getRed()) * t),
				clamp8(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
				clamp8(a.getBlue() + (b.getBlue() - a.getBlue()) * t),
				a.getAlpha());
	}

	private static int clamp8(double v) {
		return (int) Math.round(Math.max(0, Math.min(255, v)));
	}
}
