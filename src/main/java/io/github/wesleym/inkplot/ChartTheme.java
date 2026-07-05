package io.github.wesleym.inkplot;

import java.awt.Color;
import java.util.List;

/**
 * Every colour a chart draws with, as one immutable value. Five vintage-leaning themes are built in —
 * {@link #PAPER} (the default), {@link #GAZETTE}, and {@link #ATLAS} on light surfaces, {@link #INKWELL}
 * and {@link #NOCTURNE} on dark — each palette machine-checked for lightness band, chroma, and adjacent
 * colour-vision separation. Pass a custom instance to restyle everything at once.
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

	/** The default theme: a warm paper surface with earthy series inks — quiet, print-like, vintage. */
	public static final ChartTheme PAPER = new ChartTheme(false,
			new Color(0xFA, 0xF6, 0xEF),   // surface
			new Color(0x2B, 0x26, 0x20),   // text
			new Color(0x8C, 0x84, 0x78),   // muted
			new Color(0xE6, 0xDF, 0xD2),   // hairline
			new Color(0xB1, 0x51, 0x2D),   // accent
			new Color(0xF1, 0xEA, 0xE0),   // elevated
			List.of(
					new Color(0xB1, 0x51, 0x2D),    // terracotta
					new Color(0x00, 0x7C, 0x5D),    // deep teal
					new Color(0xB6, 0x8B, 0x16),    // ochre
					new Color(0x61, 0x49, 0x89),    // plum
					new Color(0xA6, 0x4D, 0x40),    // brick
					new Color(0x8A, 0x94, 0x3F),    // olive
					new Color(0x25, 0x5E, 0x93),    // slate blue
					new Color(0xAA, 0x66, 0x94)));  // mauve

	/** Newsprint: a cool off-white sheet, true-black headlines, and the restrained inks of a broadsheet. */
	public static final ChartTheme GAZETTE = new ChartTheme(false,
			new Color(0xF4, 0xF3, 0xEE),   // surface
			new Color(0x1A, 0x1A, 0x18),   // text
			new Color(0x77, 0x76, 0x6E),   // muted
			new Color(0xDB, 0xD9, 0xD0),   // hairline
			new Color(0xAB, 0x41, 0x3E),   // accent (the masthead red)
			new Color(0xE9, 0xE7, 0xDE),   // elevated
			List.of(
					new Color(0xAB, 0x41, 0x3E),    // masthead red
					new Color(0xB7, 0x9D, 0x33),    // mustard
					new Color(0x31, 0x54, 0x8F),    // ink navy
					new Color(0xC0, 0x83, 0x43),    // sepia
					new Color(0x28, 0x6E, 0x3C),    // forest
					new Color(0x8F, 0x6F, 0xB2),    // plum
					new Color(0x91, 0x3E, 0x55),    // rosewood
					new Color(0x03, 0x99, 0x9F)));  // petrol

	/** An old map: aged chart-paper tan, cartographer's navy, and the inks of a hand-coloured atlas. */
	public static final ChartTheme ATLAS = new ChartTheme(false,
			new Color(0xF2, 0xE8, 0xD5),   // surface
			new Color(0x3B, 0x2F, 0x22),   // text
			new Color(0x8F, 0x7F, 0x66),   // muted
			new Color(0xDF, 0xD2, 0xB8),   // hairline
			new Color(0x28, 0x56, 0x8E),   // accent (map navy)
			new Color(0xE9, 0xDD, 0xC4),   // elevated
			List.of(
					new Color(0x28, 0x56, 0x8E),    // map navy
					new Color(0xBF, 0x6F, 0x33),    // sienna
					new Color(0x00, 0x7F, 0x68),    // teal
					new Color(0xBD, 0x9A, 0x32),    // gold
					new Color(0x8F, 0x37, 0x34),    // oxblood
					new Color(0xB5, 0x64, 0x72),    // dusty rose
					new Color(0x32, 0x6D, 0x36),    // deep green
					new Color(0x84, 0x6B, 0xAF)));  // ink violet

	/** The dark companion to {@link #PAPER}: a warm near-black of dried ink, with the same inks lifted to read. */
	public static final ChartTheme INKWELL = new ChartTheme(true,
			new Color(0x20, 0x1B, 0x16),   // surface
			new Color(0xF0, 0xE9, 0xDC),   // text
			new Color(0x9A, 0x90, 0x83),   // muted
			new Color(0x38, 0x30, 0x28),   // hairline
			new Color(0xC7, 0x68, 0x3A),   // accent (ember)
			new Color(0x32, 0x2A, 0x22),   // elevated
			List.of(
					new Color(0xC7, 0x68, 0x3A),    // ember
					new Color(0x3C, 0x6A, 0xA4),    // dusty blue
					new Color(0xB5, 0x8B, 0x22),    // brass
					new Color(0x77, 0x63, 0xAB),    // wisteria
					new Color(0x39, 0x9E, 0x78),    // verdigris
					new Color(0x9E, 0x49, 0x60),    // rosewood
					new Color(0x84, 0x96, 0x44),    // moss
					new Color(0x96, 0x4A, 0x2F)));  // copper

	/** A study after dark: deep viridian-black, brass-lamp light, and muted club-room colours. */
	public static final ChartTheme NOCTURNE = new ChartTheme(true,
			new Color(0x17, 0x21, 0x1D),   // surface
			new Color(0xEA, 0xE6, 0xD8),   // text
			new Color(0x8C, 0x94, 0x8A),   // muted
			new Color(0x26, 0x33, 0x2D),   // hairline
			new Color(0xB0, 0x8E, 0x21),   // accent (brass)
			new Color(0x22, 0x30, 0x29),   // elevated
			List.of(
					new Color(0xB0, 0x8E, 0x21),    // brass
					new Color(0x00, 0x86, 0x72),    // petrol
					new Color(0xC3, 0x69, 0x53),    // coral
					new Color(0x2E, 0x66, 0x9C),    // steel blue
					new Color(0x8D, 0x97, 0x42),    // olive
					new Color(0x9C, 0x4E, 0x6F),    // rose
					new Color(0x4A, 0xA0, 0x76),    // lamp green
					new Color(0x69, 0x5D, 0xA1)));  // lavender

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
