package io.github.wesleym.inkplot;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every built-in palette must clear the computable checks — lightness band, chroma floor, and adjacent
 * colour-vision-deficiency separation at or above the 8.0 floor (the renderers supply the mandated
 * secondary encoding for the 8-12 band: 2px mark gaps, direct labels, legends). The full report prints
 * on every run, so a palette edit shows its consequences immediately.
 */
class ChartThemePaletteTest {

	@Test
	void everyBuiltInPalettePassesTheChecks() {
		Map<String, ChartTheme> builtIns = Map.of(
				"PAPER", ChartTheme.PAPER,
				"GAZETTE", ChartTheme.GAZETTE,
				"ATLAS", ChartTheme.ATLAS,
				"INKWELL", ChartTheme.INKWELL,
				"NOCTURNE", ChartTheme.NOCTURNE);
		StringBuilder failures = new StringBuilder();
		for (var e : builtIns.entrySet()) {
			PaletteChecks.Report r = PaletteChecks.validate(e.getValue());
			System.out.printf("%s: band=%s chroma=%s worstAdjacentCvd=%.1f%n",
					e.getKey(), r.bandOk(), r.chromaOk(), r.worstAdjacentCvd());
			r.lines().forEach(System.out::println);
			if (!r.bandOk() || !r.chromaOk() || r.worstAdjacentCvd() < PaletteChecks.CVD_FLOOR) {
				failures.append(e.getKey()).append(' ');
			}
		}
		assertTrue(failures.isEmpty(), "palettes failing the checks: " + failures);
	}
}
