/**
 * The tabular layer: a table of strings in, ready-to-draw chart data out.
 *
 * <p>{@link io.github.wesleym.inkplot.data.ResultSnapshot} is the seam — column names, optional type
 * names, string rows. From there: {@link io.github.wesleym.inkplot.data.ChartColumns} classifies each
 * column into chart roles (numeric / temporal / categorical, sniffing values when types are missing),
 * {@link io.github.wesleym.inkplot.data.ChartAuto} suggests the right chart form for the table's shape,
 * and {@link io.github.wesleym.inkplot.data.ChartBuilder} executes a
 * {@link io.github.wesleym.inkplot.spec.ChartSpec} into immutable
 * {@link io.github.wesleym.inkplot.data.ChartData} — one compact record per chart type, carrying a
 * {@link io.github.wesleym.inkplot.data.Provenance} that states honestly what the chart does and doesn't
 * cover.
 *
 * <p>Interactive hosts should build through {@link io.github.wesleym.inkplot.data.ChartDataPipeline}:
 * it prepares off the EDT, delivers on the EDT, and drops superseded work so rapid picker changes never
 * land a stale chart.
 */
package io.github.wesleym.inkplot.data;
