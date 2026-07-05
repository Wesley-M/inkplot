package io.github.wesleym.inkplot.scale;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeTicksTest {

	@Test
	void ticksAreAscendingAndLabelled() {
		long start = Instant.parse("2026-03-01T00:00:00Z").toEpochMilli();
		long end = Instant.parse("2026-03-08T00:00:00Z").toEpochMilli();
		TimeTicks.Result r = TimeTicks.of(start, end, 7);
		assertTrue(r.values().length >= 2, "a week yields several ticks");
		assertEquals(r.values().length, r.labels().length, "every tick has a label");
		for (int i = 1; i < r.values().length; i++) {
			assertTrue(r.values()[i] > r.values()[i - 1], "ticks strictly increasing");
		}
	}

	@Test
	void dayStepsAreExactlyTwentyFourHoursAcrossDstBoundary() {
		// A US spring-forward weekend — a zone-based day step would drop to 23h; UTC arithmetic keeps it exact.
		long start = Instant.parse("2026-03-06T00:00:00Z").toEpochMilli();
		long end = Instant.parse("2026-03-12T00:00:00Z").toEpochMilli();
		TimeTicks.Result r = TimeTicks.of(start, end, 6);
		for (int i = 1; i < r.values().length; i++) {
			long deltaHours = Duration.ofMillis(r.values()[i] - r.values()[i - 1]).toHours();
			assertEquals(24, deltaHours, "day step stays 24h across the DST change");
		}
	}

	@Test
	void picksCoarseUnitsForWideSpans() {
		long start = Instant.parse("2000-01-01T00:00:00Z").toEpochMilli();
		long end = Instant.parse("2026-01-01T00:00:00Z").toEpochMilli();
		TimeTicks.Result r = TimeTicks.of(start, end, 6);
		assertTrue(r.values().length <= 20, "a 26-year span does not spray hundreds of ticks");
		assertTrue(r.labels()[0].matches("\\d{4}"), "wide spans label by year, got " + r.labels()[0]);
	}

	@Test
	void extremeEpochRangeUsesCoarseTicksInsteadOfOverflowingToSeconds() {
		TimeTicks.Result r = TimeTicks.of(Long.MIN_VALUE, Long.MAX_VALUE, 6);

		assertTrue(r.values().length <= 20, "an extreme range still yields a sparse axis");
		for (int i = 1; i < r.values().length; i++) {
			assertTrue(r.values()[i] > r.values()[i - 1], "ticks strictly increase");
		}
	}

	@Test
	void subMinuteSpanLabelsWithSeconds() {
		long start = Instant.parse("2026-05-01T12:00:00Z").toEpochMilli();
		long end = Instant.parse("2026-05-01T12:00:30Z").toEpochMilli();
		TimeTicks.Result r = TimeTicks.of(start, end, 6);
		assertTrue(r.labels()[0].matches("\\d{2}:\\d{2}:\\d{2}"), "second span labels HH:mm:ss, got " + r.labels()[0]);
	}
}
