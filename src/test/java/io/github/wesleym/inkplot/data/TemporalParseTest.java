package io.github.wesleym.inkplot.data;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TemporalParseTest {

	@Test
	void parsesAbsurdFractionalPrecision() {
		// The shape SQL Anywhere / c-tree return: 35 fractional digits. Milliseconds are kept, the padding discarded.
		Long ms = TemporalParse.parseMillis("2024-12-14 16:29:06.97300000000000000000000000000000000");
		assertNotNull(ms, "over-precise fractional seconds still parse");
		assertEquals(Instant.parse("2024-12-14T16:29:06.973Z").toEpochMilli(), ms);
	}

	@Test
	void parsesCommonShapes() {
		assertNotNull(TemporalParse.parseMillis("2024-12-14 18:04:19.539"));
		assertNotNull(TemporalParse.parseMillis("2024-12-14T18:04:19"));
		assertNotNull(TemporalParse.parseMillis("2024-12-14 18:04"));
		assertEquals(Instant.parse("2024-12-14T00:00:00Z").toEpochMilli(),
				TemporalParse.parseMillis("2024-12-14"));
		assertNotNull(TemporalParse.parseMillis("2024-12-14T18:04:19.539Z"), "ISO with offset");
	}

	@Test
	void rejectsNonTimestamps() {
		assertNull(TemporalParse.parseMillis("hello"));
		assertNull(TemporalParse.parseMillis("156"));       // a plain small integer is not a timestamp
		assertNull(TemporalParse.parseMillis(""));
		assertNull(TemporalParse.parseMillis(null));
	}
}
