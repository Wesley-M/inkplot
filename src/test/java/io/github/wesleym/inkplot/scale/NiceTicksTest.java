package io.github.wesleym.inkplot.scale;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NiceTicksTest {

	@Test
	void roundsToOneTwoFiveSteps() {
		NiceTicks.Result r = NiceTicks.linear(0, 97, 5);
		assertEquals(0, r.niceMin());
		assertTrue(r.niceMax() >= 97, "domain covers the data max");
		double step = r.values()[1] - r.values()[0];
		double mantissa = step / Math.pow(10, Math.floor(Math.log10(step)));
		assertTrue(mantissa == 1 || mantissa == 2 || mantissa == 5,
				"step " + step + " is a 1/2/5 multiple, mantissa " + mantissa);
	}

	@Test
	void domainEnclosesTheData() {
		NiceTicks.Result r = NiceTicks.linear(3.2, 3.9, 6);
		assertTrue(r.niceMin() <= 3.2, "nice min below data min");
		assertTrue(r.niceMax() >= 3.9, "nice max above data max");
		for (int i = 1; i < r.values().length; i++) {
			assertTrue(r.values()[i] > r.values()[i - 1], "ticks strictly increasing");
		}
	}

	@Test
	void fractionDigitsFollowSpacing() {
		NiceTicks.Result cents = NiceTicks.linear(0, 1, 5);
		assertTrue(cents.fractionDigits() >= 1, "sub-unit spacing keeps a decimal");
		NiceTicks.Result thousands = NiceTicks.linear(0, 5000, 5);
		assertEquals(0, thousands.fractionDigits(), "whole-thousand spacing needs no decimals");
	}

	@Test
	void degenerateSpanStillProducesAReadableAxis() {
		NiceTicks.Result flat = NiceTicks.linear(42, 42, 5);
		assertTrue(flat.values().length >= 2, "a flat domain still yields ticks");
		assertTrue(flat.niceMin() < 42 && flat.niceMax() > 42, "domain expands around the single value");
	}

	@Test
	void handlesNegativeThroughZero() {
		NiceTicks.Result r = NiceTicks.linear(-30, 45, 6);
		boolean hasZero = false;
		for (double v : r.values()) {
			if (v == 0) {
				hasZero = true;
			}
		}
		assertTrue(hasZero, "a domain crossing zero includes a zero tick");
	}
}
