package io.github.wesleym.inkplot.samples;

import io.github.wesleym.inkplot.Charts;
import io.github.wesleym.inkplot.data.Table;
import io.github.wesleym.inkplot.spec.Aggregate;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.GridLayout;

/**
 * Six live charts composed into one window — every tile keeps its own hover, zoom, and brush. Charts
 * are ordinary Swing components, so a dashboard is just a layout manager; the same six charts render
 * headless in the README's hero image.
 *
 * <p>Run with {@code gradlew runLiveDashboard}.
 */
public final class LiveDashboard {

	public static void main(String[] args) {
		Table sales = Samples.coffeeSales();

		JPanel grid = new JPanel(new GridLayout(2, 3, 10, 10));
		grid.setBackground(new Color(0xE9, 0xE0, 0xCF));
		grid.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		grid.add(Charts.line(sales, "date", "revenue").by("region")
				.title("Daily revenue", "by region").component());
		grid.add(Charts.bar(sales, "region", "revenue").by("product").stacked().legendBelow()
				.title("Revenue mix").component());
		grid.add(Charts.doughnut(sales, "product", "revenue")
				.legendBelow().title("Product share").component());
		grid.add(Charts.histogram(sales, "units")
				.title("Units per sale").component());
		grid.add(Charts.box(sales, "units", "region")
				.title("Order size by region").component());
		grid.add(Charts.bar(sales, "product", "units", Aggregate.AVG).horizontal()
				.title("Average units per sale").component());

		EventQueue.invokeLater(() -> Samples.show("live dashboard", grid, 1460, 900));
	}
}
