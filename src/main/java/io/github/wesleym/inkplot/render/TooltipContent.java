package io.github.wesleym.inkplot.render;

import java.awt.Color;
import java.util.List;

/**
 * What a tooltip shows for a hovered mark or crosshair position: a title (the category, X value, or bin range) over
 * one row per series. In each row the value leads and the series name follows, keyed by a short stroke of the series
 * colour — never by colouring the text itself.
 */
public record TooltipContent(String title, List<Row> rows) {

	/** One series line in a tooltip: its identity colour key, its name, and its formatted value. */
	public record Row(Color key, String label, String value) { }

	public static TooltipContent single(String title, Color key, String label, String value) {
		return new TooltipContent(title, List.of(new Row(key, label, value)));
	}
}
