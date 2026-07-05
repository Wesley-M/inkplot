package io.github.wesleym.inkplot.render;

import java.awt.Color;

/**
 * One entry in a chart legend: a colour and the series it names, plus whether its key should mirror a line (a short
 * stroke) or a filled area/bar (a rounded swatch), so the legend key matches the mark it stands for.
 */
public record LegendEntry(Color color, String label, boolean line) { }
