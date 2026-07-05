package io.github.wesleym.inkplot;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProportionStripTest {

	@Test
	void tooltipNamesTheSegmentUnderThePointerWithCountAndShare() {
		ProportionStrip strip = strip();
		strip.setSegments(List.of(
				new ProportionStrip.Segment("Filled", 0.6, 42_371, Color.GREEN),
				new ProportionStrip.Segment("Blank", 0.2, 14_124, Color.ORANGE)));

		assertEquals("Filled · 42,371 · 60%", strip.getToolTipText(at(strip, 30)));
		assertEquals("Blank · 14,124 · 20%", strip.getToolTipText(at(strip, 70)));
		assertEquals("Null · 14,123 · 20%", strip.getToolTipText(at(strip, 95)),
				"the remainder track answers with its label and count");
	}

	@Test
	void unknownCountShowsShareOnly() {
		ProportionStrip strip = strip();
		strip.setSegments(List.of(new ProportionStrip.Segment("Match", 0.5, -1, Color.GREEN)));
		assertEquals("Match · 50%", strip.getToolTipText(at(strip, 30)));
	}

	@Test
	void unnamedRemainderStaysMute() {
		ProportionStrip strip = strip();
		strip.setRemainder(null, -1);
		strip.setSegments(List.of(new ProportionStrip.Segment("Match", 0.5, 10, Color.GREEN)));
		assertNull(strip.getToolTipText(at(strip, 80)));
	}

	private static ProportionStrip strip() {
		ProportionStrip strip = new ProportionStrip(ChartTheme.LIGHT);
		strip.setSize(100, 8);   // never shown, so setSegments paints complete (progress 1) — widths are exact
		strip.setRemainder("Null", 14_123);
		return strip;
	}

	private static MouseEvent at(ProportionStrip strip, int x) {
		return new MouseEvent(strip, MouseEvent.MOUSE_MOVED, 0, 0, x, 4, 0, false);
	}
}
