package io.github.wesleym.inkplot.samples;

import io.github.wesleym.inkplot.ChartCanvas;
import io.github.wesleym.inkplot.ChartNotice;
import io.github.wesleym.inkplot.ChartTheme;
import io.github.wesleym.inkplot.ProportionStrip;
import io.github.wesleym.inkplot.data.ChartBuilder;
import io.github.wesleym.inkplot.data.ChartColumns;
import io.github.wesleym.inkplot.data.ChartData;
import io.github.wesleym.inkplot.data.Table;
import io.github.wesleym.inkplot.spec.ChartSpec;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Embedded analytics — one compact card per column, chosen by what the column <em>is</em>. This is the
 * deep-end pattern the connector-bridge insight panel uses, driven entirely through the public power
 * layer: {@link ChartColumns} classifies each column, a {@link ChartSpec} matched to the role is
 * executed by {@link ChartBuilder}, the prepared data's provenance feeds a {@link ChartNotice}, and a
 * {@link ProportionStrip} draws the compact share bar. Skewed histograms get a live log toggle.
 *
 * <p>Run with {@code gradlew runInsightCards}.
 */
public final class InsightCards {

	private static final ChartTheme THEME = ChartTheme.PAPER;

	public static void main(String[] args) {
		Table sales = Samples.coffeeSales();
		ChartColumns roles = new ChartColumns(sales);   // numeric / temporal / categorical, sniffed as needed

		JPanel stack = new JPanel();
		stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
		stack.setBackground(THEME.surface());
		stack.setBorder(new EmptyBorder(10, 10, 10, 10));
		for (int col = 0; col < sales.columnCount(); col++) {
			stack.add(card(sales, roles, col));
			stack.add(Box.createVerticalStrut(10));
		}

		JScrollPane scroll = new JScrollPane(stack);
		scroll.getVerticalScrollBar().setUnitIncrement(24);
		EventQueue.invokeLater(() -> Samples.show("insight cards", scroll, 760, 820));
	}

	// One column's card: a role-tagged header, the top-values share strip, and the compact chart the
	// role calls for — a histogram for numbers, top categories for text, busiest buckets for dates.
	private static JComponent card(Table table, ChartColumns roles, int col) {
		String role = roles.isNumeric(col) ? "number" : roles.isTemporal(col) ? "date · time" : "category";

		ChartSpec spec = roles.isNumeric(col)
				? new ChartSpec.Histogram(col, 0)
				: new ChartSpec.Bar(col, null, io.github.wesleym.inkplot.spec.Aggregate.COUNT, null,
						false, io.github.wesleym.inkplot.spec.CategoryOrder.VALUE_DESC, true);
		ChartData data = ChartBuilder.build(spec, table, 640);   // the pipeline does this off-EDT in a real host

		ChartCanvas chart = new ChartCanvas(THEME);
		chart.setData(data);
		chart.setPreferredSize(new Dimension(640, roles.isNumeric(col) ? 170 : 200));

		// The card is honest for free: a category fold or a parse drop arrives in the data's provenance.
		ChartNotice notice = new ChartNotice(THEME);
		notice.update(data.provenance());

		JLabel name = new JLabel(table.columnName(col));
		name.setFont(name.getFont().deriveFont(Font.BOLD, 14f));
		JLabel tag = new JLabel(role);
		tag.setForeground(THEME.muted());
		JPanel header = new JPanel();
		header.setOpaque(false);
		header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
		header.add(name);
		header.add(Box.createHorizontalStrut(10));
		header.add(tag);
		header.add(Box.createHorizontalGlue());
		if (chart.supportsLog()) {
			JToggleButton log = new JToggleButton("log");
			log.addActionListener(e -> chart.setYScale(
					log.isSelected() ? ChartCanvas.YScale.LOG : ChartCanvas.YScale.LINEAR));
			header.add(log);
		}

		JPanel body = new JPanel(new BorderLayout(0, 6));
		body.setOpaque(false);
		body.add(header, BorderLayout.NORTH);
		body.add(chart, BorderLayout.CENTER);
		JPanel south = new JPanel(new BorderLayout());
		south.setOpaque(false);
		south.add(shareStrip(table, col), BorderLayout.NORTH);
		south.add(notice, BorderLayout.SOUTH);
		body.add(south, BorderLayout.SOUTH);

		JPanel card = new JPanel(new BorderLayout());
		card.setBackground(THEME.elevated());
		card.setBorder(new EmptyBorder(10, 12, 10, 12));
		card.add(body, BorderLayout.CENTER);
		card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
		return card;
	}

	// A compact "what dominates this column" bar: the top three values in identity colours, the rest
	// folded into a muted tail — the strip the insight cards run under every column name.
	private static ProportionStrip shareStrip(Table table, int col) {
		Map<String, Long> counts = new LinkedHashMap<>();
		for (List<String> row : table.rows()) {
			counts.merge(row.get(col), 1L, Long::sum);
		}
		List<Map.Entry<String, Long>> top = counts.entrySet().stream()
				.sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
				.toList();
		List<ProportionStrip.Segment> segments = new ArrayList<>();
		long shown = 0;
		for (int i = 0; i < Math.min(3, top.size()); i++) {
			shown += top.get(i).getValue();
			segments.add(new ProportionStrip.Segment(top.get(i).getKey(),
					(double) top.get(i).getValue() / table.rowCount(), top.get(i).getValue(), THEME.series(i)));
		}
		segments.add(new ProportionStrip.Segment("other values",
				(double) (table.rowCount() - shown) / table.rowCount(), table.rowCount() - shown, THEME.muted()));
		ProportionStrip strip = new ProportionStrip(THEME);
		strip.setSegments(segments);
		strip.setPreferredSize(new Dimension(100, 8));
		return strip;
	}
}
