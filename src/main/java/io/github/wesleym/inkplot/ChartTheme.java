package io.github.wesleym.inkplot;

import java.awt.Color;
import java.util.List;

/**
 * Every colour a chart draws with, as one immutable value. Three themes are built in: {@link #LIGHT} and
 * {@link #DARK} are a validated, colourblind-aware default pair (the dark series palette is the same eight
 * hues re-stepped for the dark surface, not an automatic flip), and {@link #PAPER} is a warm print-like
 * alternative. Pass a custom instance to restyle everything at once.
 *
 * @param dark     whether this palette sits on a dark surface (drives hover lifts and contrast nudges)
 * @param surface  the chart background
 * @param text     primary ink: titles, category axis labels
 * @param muted    secondary ink: value-axis labels, legends, captions
 * @param hairline recessive gridline / border ink
 * @param accent   the interactive accent: brush band, focus highlights
 * @param elevated the raised tooltip background
 * @param series   the categorical palette, in fixed slot order (identity colours for multi-series charts)
 */
public record ChartTheme(boolean dark, Color surface, Color text, Color muted, Color hairline,
		Color accent, Color elevated, List<Color> series) {

	public ChartTheme {
		series = List.copyOf(series);
	}

	/** The built-in light theme. */
	public static final ChartTheme LIGHT = new ChartTheme(false,
			new Color(0xFC, 0xFC, 0xFB),   // surface
			new Color(0x0B, 0x0B, 0x0B),   // text
			new Color(0x89, 0x87, 0x81),   // muted
			new Color(0xE1, 0xE0, 0xD9),   // hairline
			new Color(0x2A, 0x78, 0xD6),   // accent
			new Color(0xF0, 0xEF, 0xEC),   // elevated
			List.of(
					new Color(0x2A, 0x78, 0xD6),    // blue
					new Color(0x1B, 0xAF, 0x7A),    // aqua
					new Color(0xED, 0xA1, 0x00),    // yellow
					new Color(0x00, 0x83, 0x00),    // green
					new Color(0x4A, 0x3A, 0xA7),    // violet
					new Color(0xE3, 0x49, 0x48),    // red
					new Color(0xE8, 0x7B, 0xA4),    // magenta
					new Color(0xEB, 0x68, 0x34)));  // orange

	/** The built-in warm, print-like theme: a paper surface with earthy series inks. */
	public static final ChartTheme PAPER = new ChartTheme(false,
			new Color(0xFA, 0xF6, 0xEF),   // surface
			new Color(0x2B, 0x26, 0x20),   // text
			new Color(0x8C, 0x84, 0x78),   // muted
			new Color(0xE6, 0xDF, 0xD2),   // hairline
			new Color(0xC4, 0x55, 0x1E),   // accent
			new Color(0xF1, 0xEA, 0xE0),   // elevated
			List.of(
					new Color(0xC4, 0x55, 0x1E),    // terracotta
					new Color(0x2E, 0x6E, 0x62),    // deep teal
					new Color(0xC0, 0x8B, 0x00),    // ochre
					new Color(0x5B, 0x4A, 0x8A),    // plum
					new Color(0x8A, 0x3B, 0x3B),    // brick
					new Color(0x4E, 0x7A, 0x3A),    // olive
					new Color(0x3E, 0x6B, 0x9E),    // slate blue
					new Color(0xA5, 0x6A, 0x8E)));  // mauve

	/** The built-in dark theme: the same eight hues as {@link #LIGHT}, re-stepped for the dark surface. */
	public static final ChartTheme DARK = new ChartTheme(true,
			new Color(0x1A, 0x1A, 0x19),
			new Color(0xFF, 0xFF, 0xFF),
			new Color(0x89, 0x87, 0x81),
			new Color(0x2C, 0x2C, 0x2A),
			new Color(0x39, 0x87, 0xE5),
			new Color(0x38, 0x38, 0x35),
			List.of(
					new Color(0x39, 0x87, 0xE5),
					new Color(0x19, 0x9E, 0x70),
					new Color(0xC9, 0x85, 0x00),
					new Color(0x00, 0x83, 0x00),
					new Color(0x90, 0x85, 0xE9),
					new Color(0xE6, 0x67, 0x67),
					new Color(0xD5, 0x51, 0x81),
					new Color(0xD9, 0x59, 0x26)));

	/**
	 * The series colour for {@code slot}. The base slots are the palette, untouched — existing charts keep
	 * their identity colours. Past that, each wrap generates a new distinct colour along two axes: the hue
	 * <b>rotates</b> by a golden-angle step (spreading around the wheel without repeating), and successive
	 * wraps cycle through <b>shade tiers</b> — the vivid ring, a lighter softer ring, a deeper ring — so a
	 * chart deep in the palette varies in lightness as well as hue.
	 */
	public Color series(int slot) {
		int n = series.size();
		int index = ((slot % n) + n) % n;
		int generation = Math.floorDiv(slot, n);
		Color colour = series.get(index);
		if (generation == 0) {
			return colour;
		}
		float[] hsb = Color.RGBtoHSB(colour.getRed(), colour.getGreen(), colour.getBlue(), null);
		// Golden-angle hue steps spread successive generations around the wheel without repeating; keep the
		// base's saturation (with a floor so a greyish base still separates).
		float hue = (hsb[0] + generation * 0.618f) % 1f;
		float sat = Math.max(0.45f, hsb[1]);
		float bri = hsb[2];
		switch ((generation - 1) % 3) {
			case 1 -> {   // the lighter, softer ring
				sat = Math.max(0.30f, sat * 0.62f);
				bri = Math.min(1f, bri * 1.14f);
			}
			case 2 -> {   // the deeper ring
				sat = Math.min(1f, sat * 1.12f);
				bri = bri * 0.72f;
			}
			default -> { }   // the vivid ring, as the base
		}
		Color varied = Color.getHSBColor(hue, sat, bri);
		// HSB brightness isn't perceptual lightness: a rotated hue (e.g. green) can wash out on a light
		// surface even when the base read fine. Pull it to a visible contrast on this theme's surface so a
		// chart with many categories never generates an unreadable colour.
		return legibleOn(varied, surface, 2.5);
	}

	/** Black or white, whichever reads on {@code background} — for text drawn on a filled mark. */
	public static Color readableOn(Color background) {
		double luminance = (0.299 * background.getRed() + 0.587 * background.getGreen()
				+ 0.114 * background.getBlue()) / 255.0;
		return luminance > 0.6 ? new Color(0x10, 0x14, 0x18) : Color.WHITE;
	}

	// ---- WCAG contrast, for the generated series slots ----------------------------------------------

	// Returns fg if it already clears target contrast against bg; otherwise darkens it (on a light
	// background) or lightens it (on a dark one) in small steps until it does, or it bottoms/tops out.
	private static Color legibleOn(Color fg, Color bg, double target) {
		if (contrastRatio(fg, bg) >= target) {
			return fg;
		}
		boolean darkBackground = luminance(bg) < 0.5;
		Color adjusted = fg;
		for (int i = 0; i < 28 && contrastRatio(adjusted, bg) < target; i++) {
			adjusted = darkBackground ? lighten(adjusted) : darken(adjusted);
		}
		return adjusted;
	}

	private static double contrastRatio(Color a, Color b) {
		double la = luminance(a);
		double lb = luminance(b);
		return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
	}

	private static Color darken(Color c) {
		return new Color(Math.round(c.getRed() * 0.92f), Math.round(c.getGreen() * 0.92f),
				Math.round(c.getBlue() * 0.92f));
	}

	private static Color lighten(Color c) {
		return new Color(
				c.getRed() + Math.round((255 - c.getRed()) * 0.10f),
				c.getGreen() + Math.round((255 - c.getGreen()) * 0.10f),
				c.getBlue() + Math.round((255 - c.getBlue()) * 0.10f));
	}

	private static double luminance(Color c) {
		double r = channel(c.getRed() / 255.0);
		double g = channel(c.getGreen() / 255.0);
		double b = channel(c.getBlue() / 255.0);
		return 0.2126 * r + 0.7152 * g + 0.0722 * b;
	}

	private static double channel(double v) {
		return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
	}
}
