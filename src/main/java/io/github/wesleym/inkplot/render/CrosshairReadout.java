package io.github.wesleym.inkplot.render;

/**
 * The result of snapping a crosshair to a data position on a line or density chart: the pixel X the hairline should
 * sit on (a real data point, so the reader aims at a value, not at a 2px line) and the tooltip listing every series at
 * that X.
 */
public record CrosshairReadout(double snapX, TooltipContent tooltip) { }
