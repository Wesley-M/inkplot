/**
 * inkplot's front door and widget layer.
 *
 * <p>Most callers need exactly two names: the {@link io.github.wesleym.inkplot.Charts} factories build a
 * chart from plain values (or a tabular {@link io.github.wesleym.inkplot.data.Table}) and return
 * a fluent {@link io.github.wesleym.inkplot.Chart}; its {@code component()} is a live Swing widget, its
 * {@code image(w, h)} a headless render.
 *
 * <p>Interactive hosts that keep a chart on screen and reconfigure it hold the widget itself: a
 * {@link io.github.wesleym.inkplot.ChartCanvas} is a long-lived {@code JComponent} whose supported
 * stateful API swaps data in place ({@code setData}), toggles the value axis ({@code setYScale}),
 * re-themes on the fly ({@code restyle}), drives the viewport ({@code zoomIn}/{@code zoomOut}/{@code
 * fitView}), and exports ({@code renderTo}).
 *
 * <p>Appearance lives in two values: {@link io.github.wesleym.inkplot.ChartTheme} (five built-ins, every
 * palette machine-checked; or construct your own) and {@link io.github.wesleym.inkplot.ChartStyle}
 * (spacing/type tokens with pluggable scale and font, for hosts with their own UI zoom).
 */
package io.github.wesleym.inkplot;
