package io.github.wesleym.inkplot;

import io.github.wesleym.inkplot.data.ChartAuto;
import io.github.wesleym.inkplot.data.ChartColumns;
import io.github.wesleym.inkplot.data.ChartDataPipeline;
import io.github.wesleym.inkplot.data.Table;
import io.github.wesleym.inkplot.spec.ChartSpec;
import io.github.wesleym.inkplot.spec.ChartSpecs;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The README's "interactive host" walkthrough, run for real: a long-lived canvas fed by the async
 * pipeline, with the spec derived from classified columns — the exact composition an explorer-style
 * host uses. Keeps the published example compiling and behaving.
 */
class HostExampleTest {

	@Test
	void aLongLivedCanvasFedByThePipeline() throws Exception {
		Table table = ordersTable();
		JLabel statusBar = new JLabel();

		// One canvas for the lifetime of the view; prepared data swaps in place.
		ChartCanvas canvas = new ChartCanvas(ChartTheme.PAPER);
		ChartDataPipeline pipeline = new ChartDataPipeline();

		// Classify the columns once, suggest a form, and derive a starting spec from them.
		ChartColumns columns = new ChartColumns(table);
		ChartSpec spec = ChartSpecs.initial(ChartAuto.suggest(columns), columns);
		assertNotNull(spec, "a category + a measure always yields a spec");

		// Rebuild off the EDT whenever a picker changes; superseded builds are dropped, errors land
		// as a message instead of a broken chart.
		pipeline.prepare(spec, table, canvas.getWidth(),
				canvas::setData,
				statusBar::setText);

		waitForData(canvas);
		assertTrue(canvas.hasData(), "the pipeline delivered onto the canvas");
		assertTrue(statusBar.getText().isEmpty(), "no error surfaced: " + statusBar.getText());
		pipeline.shutdown();
	}

	private static void waitForData(ChartCanvas canvas) throws Exception {
		for (int i = 0; i < 100 && !canvas.hasData(); i++) {
			Thread.sleep(20);
			SwingUtilities.invokeAndWait(() -> { });   // flush the EDT so the delivery lands
		}
	}

	private static Table ordersTable() {
		List<List<String>> rows = new ArrayList<>();
		for (int i = 0; i < 200; i++) {
			rows.add(List.of("region-" + (i % 4), String.valueOf(20 + i % 60)));
		}
		return Table.of(List.of("region", "amount"), rows);
	}
}
