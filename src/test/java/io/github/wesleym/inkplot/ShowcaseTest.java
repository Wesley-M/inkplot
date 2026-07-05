package io.github.wesleym.inkplot;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The gallery pieces the README leads with: a six-chart dashboard in a custom theme, and the same chart
 * across four palettes — both composed headless by the library itself, so the showcase is also a test of
 * {@code renderTo} on arbitrary graphics. Writes {@code build/showcase-*.png}.
 */
class ShowcaseTest {

	// A custom theme, defined exactly as the README shows it: deep blue-black surface, vivid series.
	static final ChartTheme MIDNIGHT = new ChartTheme(true,
			new Color(0x10, 0x16, 0x22),    // surface
			new Color(0xF2, 0xF5, 0xFA),    // text
			new Color(0x8A, 0x93, 0xA6),    // muted
			new Color(0x1D, 0x26, 0x35),    // hairline
			new Color(0x5B, 0x8D, 0xEF),    // accent
			new Color(0x1A, 0x23, 0x33),    // elevated (tooltips)
			List.of(new Color(0x5B, 0x8D, 0xEF), new Color(0x3E, 0xCF, 0x8E),
					new Color(0xF5, 0xA6, 0x23), new Color(0xE8, 0x61, 0x8C),
					new Color(0x9B, 0x7E, 0xF7), new Color(0x38, 0xC6, 0xD9),
					new Color(0xE9, 0x78, 0x52), new Color(0xC3, 0xD3, 0x4F)));

	private static final long DAY = 24L * 60 * 60 * 1000;

	@Test
	void paperDashboard() throws Exception {
		Chart[][] tiles = {
				{ revenue(), signups(), traffic() },
				{ latency(), activation(), storage() } };
		int tileW = 500;
		int tileH = 400;
		int gap = 14;
		int cols = tiles[0].length;
		int rows = tiles.length;
		int w = cols * tileW + (cols + 1) * gap;
		int h = rows * tileH + (rows + 1) * gap;

		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setColor(new Color(0xE9, 0xE0, 0xCF));   // the warm page plane behind the card tiles
		g.fillRect(0, 0, w, h);
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				int x = gap + c * (tileW + gap);
				int y = gap + r * (tileH + gap);
				Graphics2D tg = (Graphics2D) g.create();
				tg.translate(x, y);
				tg.setClip(new RoundRectangle2D.Double(0, 0, tileW, tileH, 18, 18));
				tiles[r][c].theme(ChartTheme.PAPER).component().renderTo(tg, tileW, tileH);
				tg.dispose();
			}
		}
		g.dispose();
		write("showcase-dashboard", img);
	}

	@Test
	void fourPalettesOneChart() throws Exception {
		record Named(String name, ChartTheme theme) { }
		Named[] palettes = {
				new Named("Paper — built in, the default", ChartTheme.PAPER),
				new Named("Inkwell — built in", ChartTheme.INKWELL),
				new Named("Gazette — built in", ChartTheme.GAZETTE),
				new Named("Nocturne — built in", ChartTheme.NOCTURNE),
				new Named("Atlas — built in", ChartTheme.ATLAS),
				new Named("Midnight — custom", MIDNIGHT) };
		int tileW = 560;
		int tileH = 360;
		int gap = 14;
		int rows = (palettes.length + 1) / 2;
		int w = 2 * tileW + 3 * gap;
		int h = rows * tileH + (rows + 1) * gap;
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setColor(new Color(0x60, 0x63, 0x6A));   // a neutral mat that neither palette family owns
		g.fillRect(0, 0, w, h);
		for (int i = 0; i < palettes.length; i++) {
			int x = gap + (i % 2) * (tileW + gap);
			int y = gap + (i / 2) * (tileH + gap);
			Graphics2D tg = (Graphics2D) g.create();
			tg.translate(x, y);
			tg.setClip(new RoundRectangle2D.Double(0, 0, tileW, tileH, 18, 18));
			Charts.bar("Mon", "Tue", "Wed", "Thu", "Fri")
					.series("Espresso", 132, 148, 156, 161, 202)
					.series("Filter", 98, 91, 104, 112, 151)
					.series("Cold brew", 41, 44, 63, 70, 96)
					.title(palettes[i].name())
					.theme(palettes[i].theme())
					.component()
					.renderTo(tg, tileW, tileH);
			tg.dispose();
		}
		g.dispose();
		write("showcase-palettes", img);
	}

	// Twelve series — four past the palette — so the golden-angle colour generation shows in one frame.
	@Test
	void twelveSeriesStillReadable() throws Exception {
		LineChart chart = Charts.line();
		Random rng = new Random(3);
		for (int s = 0; s < 12; s++) {
			double[] x = new double[40];
			double[] y = new double[40];
			double level = 20 + s * 6.5;
			for (int i = 0; i < 40; i++) {
				x[i] = i;
				level += rng.nextGaussian() * 2.2 + 0.35;
				y[i] = level;
			}
			chart.series("team-" + (char) ('a' + s), x, y);
		}
		BufferedImage img = chart
				.title("Velocity by team", "12 series — 4 past the base palette")
				.theme(ChartTheme.PAPER)
				.image(1020, 520);
		write("showcase-spectrum", img);
	}

	// ---- the dashboard tiles -------------------------------------------------------------------------

	private static Chart revenue() {
		long start = Instant.parse("2026-01-01T00:00:00Z").toEpochMilli();
		double[] when = new double[120];
		double[] now = new double[120];
		double[] last = new double[120];
		for (int i = 0; i < 120; i++) {
			when[i] = start + i * DAY;
			now[i] = 96 + i * 0.9 + 10 * Math.sin(i / 9.0);
			last[i] = 74 + i * 0.55 + 8 * Math.sin(i / 9.0 + 2);
		}
		return Charts.line().timeAxis()
				.series("2026", when, now)
				.series("2025", when, last)
				.title("Recurring revenue", "$k / day");
	}

	private static Chart signups() {
		return Charts.bar("Q1", "Q2", "Q3", "Q4")
				.series("Organic", 4100, 4900, 5600, 6400)
				.series("Paid", 2200, 2600, 2400, 2900)
				.series("Partner", 900, 1400, 1900, 2600)
				.stacked()
				.title("Signups by channel");
	}

	private static Chart traffic() {
		return Charts.doughnut(
				new String[] { "Organic search", "Direct", "Social", "Referral", "Email" },
				new double[] { 48200, 31400, 12100, 5900, 3400 }, "Sessions")
				.legendBelow()
				.title("Traffic mix", "last 30 days");
	}

	private static Chart latency() {
		Random rng = new Random(7);
		double[] ms = new double[9000];
		for (int i = 0; i < ms.length; i++) {
			ms[i] = Math.exp(rng.nextGaussian() * 0.45 + 4.2);
		}
		return Charts.histogram(ms).title("API latency", "ms, p99 = 214");
	}

	private static Chart activation() {
		Random rng = new Random(21);
		int per = 260;
		double[][] cx = { { 22, 34 }, { 46, 58 }, { 71, 74 } };
		double[] x = new double[per * 3];
		double[] y = new double[per * 3];
		int[] series = new int[per * 3];
		for (int s = 0; s < 3; s++) {
			for (int i = 0; i < per; i++) {
				int idx = s * per + i;
				x[idx] = cx[s][0] + rng.nextGaussian() * 7;
				y[idx] = cx[s][1] + rng.nextGaussian() * 9;
				series[idx] = s;
			}
		}
		return Charts.of(new io.github.wesleym.inkplot.data.ChartData.Scatter(x, y, series,
				new String[] { "Starter", "Growth", "Enterprise" }, false,
				io.github.wesleym.inkplot.data.Provenance.full(per * 3)))
				.title("Activation vs spend");
	}

	private static Chart storage() {
		return Charts.treemap(
				new String[] { "media", "backups", "logs", "search index", "db snapshots", "cache", "other" },
				new double[] { 412, 268, 141, 96, 74, 38, 22 }, "GB")
				.title("Storage by service", "GB");
	}

	private static void write(String name, BufferedImage img) throws Exception {
		File out = new File("build/" + name + ".png");
		ImageIO.write(img, "png", out);
		assertTrue(out.exists() && out.length() > 0, "showcase written: " + out);
	}
}
