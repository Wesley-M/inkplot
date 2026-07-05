package io.github.wesleym.inkplot.samples;

import io.github.wesleym.inkplot.ChartCanvas;
import io.github.wesleym.inkplot.ChartTheme;
import io.github.wesleym.inkplot.data.ChartDataPipeline;
import io.github.wesleym.inkplot.data.Table;
import io.github.wesleym.inkplot.spec.Aggregate;
import io.github.wesleym.inkplot.spec.ChartSpec;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.FlowLayout;

/**
 * The interactive-host pattern, live: pickers assemble a {@link ChartSpec}, the async
 * {@link ChartDataPipeline} builds off the EDT, and one long-lived {@link ChartCanvas} receives every
 * result. Mash the pickers — superseded builds are dropped, only the latest chart lands, and a bad
 * combination surfaces as a status message instead of a broken chart. This is the miniature version of
 * a full chart explorer.
 *
 * <p>Run with {@code gradlew runExplorerLite}.
 */
public final class ExplorerLite {

	public static void main(String[] args) {
		Table sales = Samples.coffeeSales();

		ChartCanvas canvas = new ChartCanvas(ChartTheme.PAPER);   // one canvas for the app's lifetime
		ChartDataPipeline pipeline = new ChartDataPipeline();
		JLabel status = new JLabel(" ");

		JComboBox<String> form = new JComboBox<>(new String[] { "Bar", "Line", "Doughnut", "Histogram", "Box" });
		JComboBox<String> x = new JComboBox<>(sales.columns().toArray(String[]::new));
		JComboBox<String> y = new JComboBox<>(sales.columns().toArray(String[]::new));
		x.setSelectedItem("region");
		y.setSelectedItem("revenue");

		Runnable rebuild = () -> {
			ChartSpec spec = switch ((String) form.getSelectedItem()) {
				case "Line" -> new ChartSpec.Line(col(sales, x), col(sales, y), null, Aggregate.SUM);
				case "Doughnut" -> new ChartSpec.Doughnut(col(sales, x), col(sales, y));
				case "Histogram" -> new ChartSpec.Histogram(col(sales, y), 0);
				case "Box" -> new ChartSpec.Box(col(sales, y), col(sales, x));
				default -> new ChartSpec.Bar(col(sales, x), col(sales, y), Aggregate.SUM, null, false);
			};
			status.setText(" ");
			pipeline.prepare(spec, sales, canvas.getWidth(),
					canvas::setData,          // delivered on the EDT, only if still current
					status::setText);         // e.g. "that column isn't numeric"
		};
		form.addActionListener(e -> rebuild.run());
		x.addActionListener(e -> rebuild.run());
		y.addActionListener(e -> rebuild.run());

		JPanel pickers = new JPanel(new FlowLayout(FlowLayout.LEFT));
		pickers.add(new JLabel("Chart:"));
		pickers.add(form);
		pickers.add(new JLabel("Category / X:"));
		pickers.add(x);
		pickers.add(new JLabel("Value / Y:"));
		pickers.add(y);
		JPanel root = new JPanel(new BorderLayout());
		root.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		root.add(pickers, BorderLayout.NORTH);
		root.add(canvas, BorderLayout.CENTER);
		root.add(status, BorderLayout.SOUTH);

		EventQueue.invokeLater(() -> {
			Samples.show("explorer lite", root, 920, 580);
			rebuild.run();   // first chart once the canvas has a size
		});
	}

	private static int col(Table table, JComboBox<String> picker) {
		return table.column((String) picker.getSelectedItem());
	}
}
