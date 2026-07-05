package io.github.wesleym.inkplot.scale;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogTicksTest {

	@Test
	void roundsDomainToWholeDecades() {
		LogTicks.Result r = LogTicks.of(3, 700);
		assertEquals(1.0, r.niceMin(), 1e-9, "min rounds down to a power of ten");
		assertEquals(1000.0, r.niceMax(), 1e-9, "max rounds up to a power of ten");
	}

	@Test
	void showsMinorMarksOverAShortRangeOnly() {
		LogTicks.Result few = LogTicks.of(1, 100);
		boolean hasTwo = false;
		for (double v : few.values()) {
			if (Math.abs(v - 20) < 1e-9) {
				hasTwo = true;
			}
		}
		assertTrue(hasTwo, "a two-decade range shows 2× minor marks");

		LogTicks.Result wide = LogTicks.of(1, 1e8);
		for (double v : wide.values()) {
			double mantissa = v / Math.pow(10, Math.floor(Math.log10(v) + 1e-9));
			assertTrue(Math.abs(mantissa - 1) < 1e-6, "a wide range shows decade marks only, saw mantissa " + mantissa);
		}
	}

	@Test
	void valuesAscendWithLabels() {
		LogTicks.Result r = LogTicks.of(5, 5000);
		assertEquals(r.values().length, r.labels().length, "every tick has a label");
		for (int i = 1; i < r.values().length; i++) {
			assertTrue(r.values()[i] > r.values()[i - 1], "ticks strictly increasing");
		}
	}

	@Test
	void nonFiniteInputFallsBackToReadableDomain() {
		LogTicks.Result r = LogTicks.of(Double.NaN, Double.POSITIVE_INFINITY);

		assertTrue(Double.isFinite(r.niceMin()), "fallback min is finite");
		assertTrue(Double.isFinite(r.niceMax()), "fallback max is finite");
		assertTrue(r.niceMax() > r.niceMin(), "fallback domain has a positive span");
		assertTrue(r.values().length >= 2, "fallback axis has multiple ticks");
	}

	@Test
	void mapsAndInvertsRoundTrip() {
		Scale.Log scale = new Scale.Log(1, 1000, 0, 300);
		double px = scale.map(100);
		assertEquals(200, px, 0.5, "100 sits two-thirds up a 1..1000 decade axis");
		assertEquals(100, scale.invert(px), 1e-6, "invert round-trips");
	}
}
