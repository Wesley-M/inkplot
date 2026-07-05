package io.github.wesleym.inkplot.samples;

import io.github.wesleym.inkplot.Charts;

import java.awt.EventQueue;

/**
 * The ten-line starter: one chart in one window. Hover the bars, scroll to zoom, drag to pan,
 * double-click to reset — all of that is on by default.
 *
 * <p>Run with {@code gradlew runHelloBars}.
 */
public final class HelloBars {

	public static void main(String[] args) {
		EventQueue.invokeLater(() -> Samples.show("hello bars",
				Charts.bar("Mon", "Tue", "Wed", "Thu", "Fri")
						.series("Espresso", 132, 148, 156, 161, 202)
						.series("Filter", 98, 91, 104, 112, 151)
						.title("Cups sold", "one café, one week")
						.component(),
				760, 460));
	}
}
