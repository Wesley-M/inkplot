package io.github.wesleym.inkplot;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * The computable palette checks a categorical chart palette must clear, ported from the data-viz
 * method's validator: an OKLCH lightness band per mode, an OKLCH chroma floor (below it a hue reads as
 * gray), colour-vision-deficiency separation between adjacent slots (Machado 2009 protan/deutan
 * simulation, CIE76 ΔE), and WCAG contrast against the chart surface (reported, not failed — sub-3:1 is
 * legal with direct labels or mark gaps, which the renderers provide).
 */
final class PaletteChecks {

	private PaletteChecks() { }

	static final double LIGHT_BAND_LO = 0.43;
	static final double LIGHT_BAND_HI = 0.77;
	static final double DARK_BAND_LO = 0.48;
	static final double DARK_BAND_HI = 0.67;
	static final double CHROMA_FLOOR = 0.10;
	static final double CVD_TARGET = 12.0;
	static final double CVD_FLOOR = 8.0;
	static final double CONTRAST_MIN = 3.0;

	// Machado, Oliveira & Fernandes (2009) CVD transforms at severity 1.0 (linear RGB).
	private static final double[][] PROTAN = {
			{ 0.152286, 1.052583, -0.204868 },
			{ 0.114503, 0.786281, 0.099216 },
			{ -0.003882, -0.048116, 1.051998 } };
	private static final double[][] DEUTAN = {
			{ 0.367322, 0.860646, -0.227968 },
			{ 0.280085, 0.672501, 0.047413 },
			{ -0.011820, 0.042940, 0.968881 } };

	record Report(List<String> lines, boolean bandOk, boolean chromaOk, double worstAdjacentCvd) { }

	static Report validate(ChartTheme theme) {
		List<Color> palette = theme.series();
		double lo = theme.dark() ? DARK_BAND_LO : LIGHT_BAND_LO;
		double hi = theme.dark() ? DARK_BAND_HI : LIGHT_BAND_HI;
		List<String> lines = new ArrayList<>();

		boolean bandOk = true;
		boolean chromaOk = true;
		for (int i = 0; i < palette.size(); i++) {
			double[] lc = oklch(palette.get(i));
			if (lc[0] < lo || lc[0] > hi) {
				bandOk = false;
				lines.add(String.format("  slot %d %s L=%.3f outside band %.2f-%.2f", i, hex(palette.get(i)), lc[0], lo, hi));
			}
			if (lc[1] < CHROMA_FLOOR) {
				chromaOk = false;
				lines.add(String.format("  slot %d %s C=%.3f below chroma floor (reads gray)", i, hex(palette.get(i)), lc[1]));
			}
		}

		double worst = Double.MAX_VALUE;
		for (int i = 0; i + 1 < palette.size(); i++) {
			for (double[][] sim : new double[][][] { PROTAN, DEUTAN }) {
				double d = deltaE(palette.get(i), palette.get(i + 1), sim);
				if (d < worst) {
					worst = d;
					if (d < CVD_TARGET) {
						lines.add(String.format("  adjacent %s <-> %s CVD dE %.1f", hex(palette.get(i)), hex(palette.get(i + 1)), d));
					}
				}
			}
		}

		for (Color c : palette) {
			double cr = contrast(c, theme.surface());
			if (cr < CONTRAST_MIN) {
				lines.add(String.format("  relief: %s at %.2f:1 vs surface (needs labels/gaps - renderers provide)", hex(c), cr));
			}
		}
		return new Report(lines, bandOk, chromaOk, worst);
	}

	static String hex(Color c) {
		return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
	}

	/** OKLCH → sRGB, for designing palettes directly in the space the checks measure. */
	static Color fromOklch(double okL, double okC, double hueDegrees) {
		double h = Math.toRadians(hueDegrees);
		double a = okC * Math.cos(h);
		double b = okC * Math.sin(h);
		double l = Math.pow(okL + 0.3963377774 * a + 0.2158037573 * b, 3);
		double m = Math.pow(okL - 0.1055613458 * a - 0.0638541728 * b, 3);
		double s = Math.pow(okL - 0.0894841775 * a - 1.2914855480 * b, 3);
		double r = 4.0767416621 * l - 3.3077115913 * m + 0.2307590544 * s;
		double g = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s;
		double bb = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s;
		return new Color((float) lin2s(r), (float) lin2s(g), (float) lin2s(bb));
	}

	private static double lin2s(double v) {
		v = Math.max(0, Math.min(1, v));
		return v <= 0.0031308 ? 12.92 * v : 1.055 * Math.pow(v, 1 / 2.4) - 0.055;
	}

	// ---- colour math (ported verbatim from the validator) ---------------------------------------------

	private static double[] lin(Color c) {
		return new double[] { s2lin(c.getRed() / 255.0), s2lin(c.getGreen() / 255.0), s2lin(c.getBlue() / 255.0) };
	}

	private static double s2lin(double v) {
		return v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
	}

	private static double[] oklch(Color c) {
		double[] rgb = lin(c);
		double l = Math.cbrt(0.4122214708 * rgb[0] + 0.5363325363 * rgb[1] + 0.0514459929 * rgb[2]);
		double m = Math.cbrt(0.2119034982 * rgb[0] + 0.6806995451 * rgb[1] + 0.1073969566 * rgb[2]);
		double s = Math.cbrt(0.0883024619 * rgb[0] + 0.2817188376 * rgb[1] + 0.6299787005 * rgb[2]);
		double okL = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s;
		double okA = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s;
		double okB = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s;
		return new double[] { okL, Math.hypot(okA, okB) };
	}

	private static double[] lab(double[] rgb) {
		double x = 0.4124564 * rgb[0] + 0.3575761 * rgb[1] + 0.1804375 * rgb[2];
		double y = 0.2126729 * rgb[0] + 0.7151522 * rgb[1] + 0.0721750 * rgb[2];
		double z = 0.0193339 * rgb[0] + 0.1191920 * rgb[1] + 0.9503041 * rgb[2];
		double fx = f(x / 0.95047);
		double fy = f(y / 1.0);
		double fz = f(z / 1.08883);
		return new double[] { 116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz) };
	}

	private static double f(double t) {
		return t > 0.008856 ? Math.cbrt(t) : 7.787 * t + 16.0 / 116;
	}

	private static double[] simulate(Color c, double[][] m) {
		double[] rgb = lin(c);
		return new double[] {
				clamp01(m[0][0] * rgb[0] + m[0][1] * rgb[1] + m[0][2] * rgb[2]),
				clamp01(m[1][0] * rgb[0] + m[1][1] * rgb[1] + m[1][2] * rgb[2]),
				clamp01(m[2][0] * rgb[0] + m[2][1] * rgb[1] + m[2][2] * rgb[2]) };
	}

	private static double clamp01(double v) {
		return Math.max(0, Math.min(1, v));
	}

	private static double deltaE(Color a, Color b, double[][] sim) {
		double[] la = lab(simulate(a, sim));
		double[] lb = lab(simulate(b, sim));
		return Math.sqrt(Math.pow(la[0] - lb[0], 2) + Math.pow(la[1] - lb[1], 2) + Math.pow(la[2] - lb[2], 2));
	}

	private static double contrast(Color a, Color b) {
		double la = relLum(a);
		double lb = relLum(b);
		return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
	}

	private static double relLum(Color c) {
		double[] rgb = lin(c);
		return 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2];
	}
}
