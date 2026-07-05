/**
 * The declarative chart specification: which columns play which roles, and how values aggregate.
 *
 * <p>A {@link io.github.wesleym.inkplot.spec.ChartSpec} is one immutable record per
 * {@link io.github.wesleym.inkplot.spec.ChartType} — e.g. {@code new ChartSpec.Bar(categoryCol,
 * valueCol, Aggregate.SUM, seriesCol, stacked)} — handed to the data layer together with a snapshot.
 * {@link io.github.wesleym.inkplot.spec.ChartSpecs#initial} picks sensible default columns for a type,
 * which is what makes "table in, chart out" one call.
 */
package io.github.wesleym.inkplot.spec;
