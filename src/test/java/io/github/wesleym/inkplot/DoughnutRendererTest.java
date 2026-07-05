package io.github.wesleym.inkplot;

import io.github.wesleym.inkplot.data.ChartData;
import io.github.wesleym.inkplot.data.Provenance;
import io.github.wesleym.inkplot.render.ChartHoverState;
import io.github.wesleym.inkplot.render.DoughnutRenderer;
import io.github.wesleym.inkplot.render.MarkHit;
import io.github.wesleym.inkplot.render.PlotContext;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoughnutRendererTest {

	@Test
	void calloutLabelsHitTestAsTheirSlice() {
		DoughnutRenderer renderer = new DoughnutRenderer(ChartFixtures.doughnutTail());
		PlotContext ctx = new PlotContext(new Rectangle(0, 0, 640, 400), null, null,
				ChartTheme.LIGHT, ChartHoverState.NONE);

		// The far-left band lies entirely outside any possible ring radius (the ring fits within height/2 of the
		// centre), so any hit found there can only be a callout label standing in for its slice.
		boolean labelHit = false;
		for (int y = 0; y < 400 && !labelHit; y += 2) {
			for (int x = 0; x < 120 && !labelHit; x += 2) {
				Optional<MarkHit> hit = renderer.hitTest(ctx, new Point(x, y));
				if (hit.isPresent()) {
					labelHit = true;
					assertTrue(hit.get().a() >= 0 && hit.get().a() < 10, "the hit names a real slice");
				}
			}
		}
		assertTrue(labelHit, "some point in the label gutter hit-tests to a slice");
	}

	@Test
	void semanticSliceColorsOverrideThePaletteWherePresent() {
		ChartTheme theme = ChartTheme.LIGHT;
		// The caller supplies semantic colours (a positive green, a danger red); the third (unparsed) slice
		// is left null and must keep its series-palette slot.
		Color positive = new Color(0x0C, 0xA3, 0x0C);
		Color danger = new Color(0xD0, 0x3B, 0x3B);
		ChartData.Doughnut data = new ChartData.Doughnut(
				new String[] { "true", "false", "maybe" }, new double[] { 6, 3, 1 }, "Rows",
				Provenance.full(0), new Color[] { positive, danger, null });
		DoughnutRenderer renderer = new DoughnutRenderer(data);
		PlotContext ctx = new PlotContext(new Rectangle(0, 0, 640, 400), null, null, theme, ChartHoverState.NONE);

		assertEquals(positive, renderer.tooltip(ctx, MarkHit.of(0, 0, null)).rows().get(0).key());
		assertEquals(danger, renderer.tooltip(ctx, MarkHit.of(1, 0, null)).rows().get(0).key());
		assertEquals(theme.series(2), renderer.tooltip(ctx, MarkHit.of(2, 0, null)).rows().get(0).key());
	}
}
