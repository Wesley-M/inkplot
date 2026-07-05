# Charting tables

The path for anyone who thinks in dataframes: wrap your rows in a `Table`, address columns **by name**,
refine fluently. If you know `px.bar(df, x="region", y="amount")`, you already know this API.

## A table in two lines

A `Table` is column names plus string rows — exactly what a query result, a CSV reader, or a report
job already has. No parsing up front; values are interpreted per chart (numbers, timestamps — including
messy real-world ones — and categories are sniffed as needed).

```java
import io.github.wesleym.inkplot.Charts;
import io.github.wesleym.inkplot.data.Table;

Table table = Table.of(
        List.of("region", "amount", "created"),
        rows);                       // List<List<String>>, straight from your source
```

If your source declares column types or was truncated upstream, use the full constructor —
`new Table(columns, types, rows, truncated)` — and charts will say "showing a sample" honestly.

## One call per question

```java
Charts.bar(table, "region")                            // how many rows per region?
Charts.bar(table, "region", "amount")                  // how much amount per region? (sum)
Charts.bar(table, "region", "amount", Aggregate.AVG)   // average instead
Charts.line(table, "created", "amount")                // amount over time (calendar ticks)
Charts.histogram(table, "amount")                      // how is amount distributed?
Charts.density(table, "amount")                        // same, as a smooth curve
Charts.scatter(table, "amount", "discount")            // do these two relate?
Charts.doughnut(table, "region")                       // shares of a whole
Charts.treemap(table, "region", "amount")              // skewed shares as tiles
Charts.box(table, "amount", "region")                  // spread per group
Charts.auto(table)                                     // let inkplot pick
```

Every factory returns a `Chart`: add `.title(...)`, `.theme(...)`, then `.component()` for the live
widget or `.image(w, h)` for a render. Typos fail fast with the table's real column names — never a
blank chart.

## Refinements

Bar, line, and scatter return builders with the refinements those forms need:

```java
Charts.bar(table, "quarter", "signups").by("channel").stacked()   // split + stack
Charts.bar(table, "queue", "tickets").horizontal()                // long labels sideways
Charts.line(table, "created", "amount").by("region")              // one line per region
Charts.line(table, "created", "amount").points()                  // raw rows, no bucketing
Charts.scatter(table, "spend", "activation").by("plan")           // coloured cohorts
```

## Full control: specs

When you outgrow the named factories (category ordering, bin counts, waffle caps), say exactly what you
want with a `ChartSpec` — the declarative form the factories compile to:

```java
ChartSpec spec = new ChartSpec.Bar(table.column("region"), table.column("amount"),
        Aggregate.SUM, null, false, CategoryOrder.LABEL, false);
Charts.of(table, spec).component();
```

`ChartColumns` classifies a table's columns into roles (numeric / temporal / categorical) — it's how
you populate pickers so users are only offered columns that make sense — and `ChartSpecs.initial` picks
default columns for any chart type, which is what powers `Charts.auto`.

## Honesty is built in

Every prepared chart carries a `Provenance`: how many rows it drew of how many there were, whether the
source was already a sample, how many cells didn't parse. Anything worth telling the reader surfaces as
a quiet figure note on the chart itself — a partial picture is never presented as the whole.

---

*Next: [Theming](theming.md) · [Interactive hosts](interactive-hosts.md) · [README](../README.md)*
