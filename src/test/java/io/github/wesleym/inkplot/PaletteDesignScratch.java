package io.github.wesleym.inkplot;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;

/** Design-time scratch: prints hexes for OKLCH-specified palettes and their check reports. */
class PaletteDesignScratch {

	record Spec(String name, double l, double c, double h) { }

	@Test
	void printCandidates() {
		emit("PAPER", false, new Color(0xFA, 0xF6, 0xEF), List.of(
				new Spec("terracotta", 0.55, 0.135, 40),
				new Spec("teal", 0.52, 0.108, 168),
				new Spec("ochre", 0.66, 0.130, 85),
				new Spec("plum", 0.46, 0.105, 300),
				new Spec("brick", 0.53, 0.120, 30),
				new Spec("olive", 0.64, 0.110, 115),
				new Spec("slate", 0.47, 0.105, 250),
				new Spec("mauve", 0.60, 0.105, 340)));
		emit("GAZETTE", false, new Color(0xF4, 0xF3, 0xEE), List.of(
				new Spec("masthead", 0.52, 0.140, 25),
				new Spec("mustard", 0.70, 0.125, 95),
				new Spec("navy", 0.45, 0.105, 260),
				new Spec("sepia", 0.66, 0.110, 65),
				new Spec("forest", 0.48, 0.105, 150),
				new Spec("plum", 0.60, 0.105, 305),
				new Spec("rosewood", 0.48, 0.115, 5),
				new Spec("petrol", 0.62, 0.105, 200)));
		emit("ATLAS", false, new Color(0xF2, 0xE8, 0xD5), List.of(
				new Spec("navy", 0.45, 0.105, 255),
				new Spec("sienna", 0.62, 0.125, 55),
				new Spec("teal", 0.53, 0.108, 175),
				new Spec("gold", 0.70, 0.125, 90),
				new Spec("oxblood", 0.46, 0.120, 25),
				new Spec("rose", 0.60, 0.105, 10),
				new Spec("green", 0.48, 0.105, 145),
				new Spec("violet", 0.58, 0.105, 300)));
		emit("INKWELL", true, new Color(0x20, 0x1B, 0x16), List.of(
				new Spec("ember", 0.62, 0.135, 45),
				new Spec("dustyblue", 0.52, 0.105, 255),
				new Spec("brass", 0.66, 0.125, 85),
				new Spec("wisteria", 0.55, 0.110, 295),
				new Spec("verdigris", 0.63, 0.110, 165),
				new Spec("rosewood", 0.52, 0.115, 5),
				new Spec("moss", 0.64, 0.110, 120),
				new Spec("copper", 0.50, 0.110, 40)));
		emit("NOCTURNE", true, new Color(0x17, 0x21, 0x1D), List.of(
				new Spec("brass", 0.66, 0.125, 90),
				new Spec("petrol", 0.55, 0.108, 178),
				new Spec("coral", 0.62, 0.120, 35),
				new Spec("steel", 0.50, 0.105, 250),
				new Spec("olive", 0.65, 0.110, 115),
				new Spec("rose", 0.53, 0.110, 355),
				new Spec("lampgreen", 0.64, 0.105, 160),
				new Spec("lavender", 0.52, 0.105, 290)));
	}

	private static void emit(String name, boolean dark, Color surface, List<Spec> specs) {
		List<Color> palette = specs.stream().map(s -> PaletteChecks.fromOklch(s.l(), s.c(), s.h())).toList();
		System.out.println("== " + name);
		for (int i = 0; i < specs.size(); i++) {
			System.out.printf("  %-10s %s%n", specs.get(i).name(), PaletteChecks.hex(palette.get(i)));
		}
		ChartTheme probe = new ChartTheme(dark, surface, Color.BLACK, Color.GRAY, Color.GRAY, palette.get(0),
				surface, palette);
		PaletteChecks.Report r = PaletteChecks.validate(probe);
		System.out.printf("  band=%s chroma=%s worstAdjacentCvd=%.1f%n", r.bandOk(), r.chromaOk(), r.worstAdjacentCvd());
		r.lines().forEach(System.out::println);
	}
}
