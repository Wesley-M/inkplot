package io.github.wesleym.inkplot.samples;

import io.github.wesleym.inkplot.ChartCanvas;
import io.github.wesleym.inkplot.ChartTheme;
import io.github.wesleym.inkplot.Charts;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Live re-theming: one chart, every built-in theme plus a custom one, switched while you watch.
 * {@code restyle} is a bare repaint — nothing is rebuilt — which is exactly how a host application
 * follows its own light/dark toggle.
 *
 * <p>Run with {@code gradlew runThemeGallery}.
 */
public final class ThemeGallery {

	public static void main(String[] args) {
		// A custom theme is one expression: surface, inks, accent, tooltip, and eight series colours.
		ChartTheme midnight = new ChartTheme(true,
				new Color(0x10, 0x16, 0x22), new Color(0xF2, 0xF5, 0xFA), new Color(0x8A, 0x93, 0xA6),
				new Color(0x1D, 0x26, 0x35), new Color(0x5B, 0x8D, 0xEF), new Color(0x1A, 0x23, 0x33),
				List.of(new Color(0x5B, 0x8D, 0xEF), new Color(0x3E, 0xCF, 0x8E), new Color(0xF5, 0xA6, 0x23),
						new Color(0xE8, 0x61, 0x8C), new Color(0x9B, 0x7E, 0xF7), new Color(0x38, 0xC6, 0xD9),
						new Color(0xE9, 0x78, 0x52), new Color(0xC3, 0xD3, 0x4F)));

		Map<String, ChartTheme> themes = new LinkedHashMap<>();
		themes.put("Paper — the default", ChartTheme.PAPER);
		themes.put("Gazette — newsprint", ChartTheme.GAZETTE);
		themes.put("Atlas — an old map", ChartTheme.ATLAS);
		themes.put("Inkwell — Paper after dark", ChartTheme.INKWELL);
		themes.put("Nocturne — brass-lamp light", ChartTheme.NOCTURNE);
		themes.put("Midnight — custom, defined above", midnight);

		ChartCanvas canvas = Charts.bar(Samples.coffeeSales(), "region", "revenue")
				.by("product").stacked().legendBelow()
				.title("Revenue by region and product", "the same chart, every theme")
				.component();

		JComboBox<String> picker = new JComboBox<>(themes.keySet().toArray(String[]::new));
		picker.addActionListener(e -> canvas.restyle(themes.get((String) picker.getSelectedItem())));

		JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
		bar.add(new JLabel("Theme:"));
		bar.add(picker);
		JPanel root = new JPanel(new BorderLayout());
		root.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		root.add(bar, BorderLayout.NORTH);
		root.add(canvas, BorderLayout.CENTER);

		EventQueue.invokeLater(() -> Samples.show("theme gallery", root, 860, 560));
	}
}
