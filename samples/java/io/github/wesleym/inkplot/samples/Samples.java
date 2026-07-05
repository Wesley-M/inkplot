package io.github.wesleym.inkplot.samples;

import io.github.wesleym.inkplot.data.Table;

import javax.swing.JComponent;
import javax.swing.JFrame;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** The two things every sample needs: a window, and the bundled demo table. */
final class Samples {

	private Samples() { }

	/** Opens a sample window around {@code content} — plain Swing, nothing inkplot-specific. */
	static void show(String title, JComponent content, int width, int height) {
		JFrame frame = new JFrame("inkplot — " + title);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setBackground(new Color(0xE9, 0xE0, 0xCF));
		frame.setContentPane(content);
		frame.setSize(width, height);
		frame.setLocationByPlatform(true);
		frame.setVisible(true);
	}

	/**
	 * The bundled dataset: 90 days of café sales (date, region, product, units, revenue) as a CSV —
	 * loaded the way any table reaches inkplot: column names plus string rows, nothing parsed up front.
	 */
	static Table coffeeSales() {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(
				Samples.class.getResourceAsStream("/data/coffee-sales.csv"), StandardCharsets.UTF_8))) {
			List<String> header = List.of(in.readLine().split(","));
			List<List<String>> rows = new ArrayList<>();
			for (String line = in.readLine(); line != null; line = in.readLine()) {
				rows.add(List.of(line.split(",")));
			}
			return Table.of(header, rows);
		}
		catch (Exception e) {
			throw new IllegalStateException("couldn't read the bundled demo data", e);
		}
	}
}
