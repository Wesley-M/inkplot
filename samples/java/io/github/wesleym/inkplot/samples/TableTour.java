package io.github.wesleym.inkplot.samples;

import io.github.wesleym.inkplot.Charts;
import io.github.wesleym.inkplot.data.Table;
import io.github.wesleym.inkplot.spec.Aggregate;

import javax.swing.JTabbedPane;

import java.awt.EventQueue;

/**
 * One real CSV through the named-column factories — a tab per question you'd ask of it. This is the
 * whole tabular API in one window: {@code Table.of} wraps the rows, every chart addresses columns by
 * name, and refinements ({@code by}, {@code stacked}, {@code horizontal}) read like the question.
 *
 * <p>Run with {@code gradlew runTableTour}.
 */
public final class TableTour {

	public static void main(String[] args) {
		Table sales = Samples.coffeeSales();   // date, region, product, units, revenue — plain strings

		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("How much, per region?", Charts.bar(sales, "region", "revenue")
				.title("Revenue by region", "90 days")
				.component());
		tabs.addTab("…split by product?", Charts.bar(sales, "region", "revenue")
				.by("product").stacked().legendBelow()
				.title("Revenue by region and product")
				.component());
		tabs.addTab("Trend over time?", Charts.line(sales, "date", "revenue")
				.by("region")
				.title("Daily revenue", "one line per region")
				.component());
		tabs.addTab("Typical order size?", Charts.histogram(sales, "units")
				.title("Units per sale")
				.component());
		tabs.addTab("Product mix?", Charts.doughnut(sales, "product", "revenue")
				.title("Revenue share by product")
				.component());
		tabs.addTab("Spread per product?", Charts.box(sales, "units", "product")
				.title("Units per sale, by product")
				.component());
		tabs.addTab("Average sale?", Charts.bar(sales, "product", "revenue", Aggregate.AVG)
				.horizontal()
				.title("Average revenue per sale")
				.component());

		EventQueue.invokeLater(() -> Samples.show("table tour", tabs, 900, 560));
	}
}
