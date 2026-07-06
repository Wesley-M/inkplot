package io.github.wesleym.inkplot;

import io.github.wesleym.inkplot.render.SvgGraphics2D;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Composes chart SVGs (text emitted as real text, so compact) into the README's documentation images: a grid
 * of rounded tiles, or a cross-fade reel that cycles through charts. Test-only — every frame is rendered by
 * the library's own {@code renderTo}.
 */
final class ChartSvg {

	private ChartSvg() { }

	/** One chart rendered to SVG content (no outer {@code <svg>}), filling {@code w × h}. */
	static String content(Chart chart, ChartTheme theme, int w, int h) {
		SvgGraphics2D svg = new SvgGraphics2D(w, h, true);
		chart.theme(theme).component().renderTo(svg, w, h);
		return svg.content();
	}

	/** A grid of chart tiles, each rounded, on a page-coloured plane. */
	static String tiles(List<String> frames, int cols, int tileW, int tileH, int gap, Color page) {
		int rows = (frames.size() + cols - 1) / cols;
		int w = cols * tileW + (cols + 1) * gap;
		int h = rows * tileH + (rows + 1) * gap;
		StringBuilder sb = new StringBuilder();
		open(sb, w, h);
		sb.append("<defs><clipPath id=\"tile\"><rect width=\"").append(tileW).append("\" height=\"").append(tileH)
				.append("\" rx=\"18\"/></clipPath></defs>\n");
		rect(sb, 0, 0, w, h, hex(page));
		for (int i = 0; i < frames.size(); i++) {
			int x = gap + (i % cols) * (tileW + gap);
			int y = gap + (i / cols) * (tileH + gap);
			sb.append("<g transform=\"translate(").append(x).append(' ').append(y)
					.append(")\" clip-path=\"url(#tile)\">\n").append(frames.get(i)).append("</g>\n");
		}
		return sb.append("</svg>\n").toString();
	}

	/**
	 * A cross-fade reel: full-frame chart contents stacked over a base fill, each one's opacity pulsing in
	 * turn so it holds then dissolves into the next, looping seamlessly (frame 0's pulse wraps the seam).
	 */
	static String reel(List<String> frames, Color base, int w, int h, int durSeconds) {
		int n = frames.size();
		double slot = 1.0 / n;
		double cf = slot * 0.28;
		StringBuilder sb = new StringBuilder();
		open(sb, w, h);
		rect(sb, 0, 0, w, h, hex(base));
		for (int i = 0; i < n; i++) {
			sb.append("<g opacity=\"").append(i == 0 ? "1" : "0")
					.append("\"><animate attributeName=\"opacity\" dur=\"").append(durSeconds)
					.append("s\" repeatCount=\"indefinite\" calcMode=\"linear\" keyTimes=\"")
					.append(keyTimes(i, slot, cf)).append("\" values=\"").append(values(i, n)).append("\"/>\n")
					.append(frames.get(i)).append("</g>\n");
		}
		return sb.append("</svg>\n").toString();
	}

	// ---- internals -----------------------------------------------------------------------------------

	private static void open(StringBuilder sb, int w, int h) {
		sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(w).append("\" height=\"").append(h)
				.append("\" viewBox=\"0 0 ").append(w).append(' ').append(h).append("\" role=\"img\">\n");
	}

	private static void rect(StringBuilder sb, int x, int y, int w, int h, String fill) {
		sb.append("<rect x=\"").append(x).append("\" y=\"").append(y).append("\" width=\"").append(w)
				.append("\" height=\"").append(h).append("\" fill=\"").append(fill).append("\"/>\n");
	}

	private static String keyTimes(int i, double slot, double cf) {
		List<Double> t = i == 0
				? new ArrayList<>(List.of(0.0, slot - cf, slot, 1 - cf, 1.0))
				: new ArrayList<>(List.of(0.0, i * slot - cf, i * slot, (i + 1) * slot - cf, (i + 1) * slot));
		if (i > 0 && (i + 1) * slot < 0.999) {
			t.add(1.0);
		}
		StringBuilder sb = new StringBuilder();
		for (int k = 0; k < t.size(); k++) {
			sb.append(k == 0 ? "" : ";").append(String.format(Locale.ROOT, "%.4f", Math.max(0, Math.min(1, t.get(k)))));
		}
		return sb.toString();
	}

	private static String values(int i, int n) {
		if (i == 0) {
			return "1;1;0;0;1";
		}
		return (i + 1) * (1.0 / n) < 0.999 ? "0;0;1;1;0;0" : "0;0;1;1;0";
	}

	private static String hex(Color c) {
		return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
	}
}
