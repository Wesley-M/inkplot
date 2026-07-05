# Interactive hosts

The factories cover embed-a-chart. This guide is for a chart *view*: pickers, live re-queries, a chart
that stays on screen and reconfigures. The pattern is one long-lived `ChartCanvas` fed by the async
`ChartDataPipeline` — exactly how the database console inkplot was extracted from drives its explorer.

## The pattern

```java
// One canvas for the lifetime of the view; prepared data swaps in place.
ChartCanvas canvas = new ChartCanvas(ChartTheme.PAPER);
ChartDataPipeline pipeline = new ChartDataPipeline();

// Classify the columns once, suggest a form, and derive a starting spec from them.
ChartColumns columns = new ChartColumns(table);
ChartSpec spec = ChartSpecs.initial(ChartAuto.suggest(columns), columns);

// Rebuild off the EDT whenever a picker changes; superseded builds are dropped, errors land
// as a message instead of a broken chart.
pipeline.prepare(spec, table, canvas.getWidth(),
        canvas::setData,
        statusBar::setText);
```

`ChartDataPipeline` is what makes this safe to wire straight to UI events: it prepares on a worker
thread, delivers on the EDT, and a generation counter drops superseded work — mash the pickers and only
the latest chart lands. Call `cancel()` when the view hides, `shutdown()` when it closes. (This code
runs as `HostExampleTest`, so it can't rot.)

## The canvas's stateful API

| Call | Effect |
|---|---|
| `setData(ChartData)` | Swap the chart in place (viewport and brush reset — a new dataset demands it). |
| `hasData()` / `hasLegend()` | Probe state for toolbars ("is anything on screen? show the legend picker?"). |
| `setYScale(LINEAR \| LOG)` / `supportsLog()` | Flip the value axis; log is offered for positive data. |
| `restyle(ChartTheme)` | Re-theme live — a bare repaint, no rebuild. |
| `setTitle`, `setNotes`, `setLegendPlacement` | The furniture; all render into exports too. |
| `zoomIn` / `zoomOut` / `fitView` / `isZoomed` | Drive the viewport from toolbar buttons. |
| `renderTo(Graphics2D, w, h)` | Export at any size — always the full 1:1 view, never the screen viewport. |

## What the reader gets for free

- **Hover** — a snapping crosshair with an all-series read-out on line/density charts; a lifted mark
  plus tooltip everywhere else.
- **Zoom & pan** — wheel zoom about the cursor (0.25×–8×), drag to pan, crisp vector re-render at any
  magnification, double-click to reset.
- **Brush** — drag across a continuous or time X axis to zoom the data domain; axes re-derive.

<p>
  <img src="chart-hover-bar.png" width="49%" alt="Lifted bar with tooltip">
  <img src="chart-hover-line.png" width="49%" alt="Crosshair with all-series readout">
</p>

## Exporting

`Chart.image(w, h)` covers the one-shot case. For a canvas the user is looking at, render it yourself —
to a file, the clipboard, a report:

```java
BufferedImage png = new BufferedImage(1280, 800, BufferedImage.TYPE_INT_RGB);
Graphics2D g = png.createGraphics();
g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
canvas.renderTo(g, 1280, 800);
g.dispose();
ImageIO.write(png, "png", new File("revenue.png"));
```

Render at 2× and downscale for print-crisp output; the drawing is vector throughout, so it sharpens at
any scale.

---

*Next: [Charting tables](charting-tables.md) · [Theming](theming.md) · [README](../README.md)*
