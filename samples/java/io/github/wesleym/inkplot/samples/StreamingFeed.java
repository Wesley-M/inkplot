package io.github.wesleym.inkplot.samples;

import io.github.wesleym.inkplot.ChartCanvas;
import io.github.wesleym.inkplot.ChartTheme;
import io.github.wesleym.inkplot.ProportionStrip;
import io.github.wesleym.inkplot.data.ChartDataPipeline;
import io.github.wesleym.inkplot.data.Table;
import io.github.wesleym.inkplot.spec.Aggregate;
import io.github.wesleym.inkplot.spec.ChartSpec;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.Timer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A chart over data that never stops arriving — the pattern a monitoring view or a streaming scan
 * uses. Every tick appends rows, rebuilds the {@link Table} (it holds references, so this is cheap),
 * and asks the {@link ChartDataPipeline} to re-prepare; the pipeline's generation counter is what makes
 * this safe, because a tick that arrives before the last build finished simply supersedes it. The strip
 * above the chart re-renders the live status mix the same way the connector-bridge insight cards do.
 *
 * <p>Run with {@code gradlew runStreamingFeed}.
 */
public final class StreamingFeed {

	private static final String[] STATUSES = { "ok", "retried", "failed" };

	public static void main(String[] args) {
		Random rng = new Random(7);
		List<List<String>> rows = new ArrayList<>();

		ChartCanvas canvas = new ChartCanvas(ChartTheme.INKWELL);
		canvas.setTitle("Requests per second", "live — streaming in");
		ChartDataPipeline pipeline = new ChartDataPipeline();
		ProportionStrip mix = new ProportionStrip(ChartTheme.INKWELL);
		mix.setPreferredSize(new Dimension(100, 10));
		JLabel count = new JLabel("0 rows");

		// One tick of the feed: a burst of new rows, then re-prepare. Rebuilding the Table per tick is
		// the intended pattern — it's a view over the same row lists, not a copy.
		Timer feed = new Timer(300, e -> {
			long now = System.currentTimeMillis();
			int burst = 2 + rng.nextInt(7);
			for (int i = 0; i < burst; i++) {
				int statusDie = rng.nextInt(20);
				rows.add(List.of(
						Instant.ofEpochMilli(now - rng.nextInt(300)).toString(),
						STATUSES[statusDie >= 19 ? 2 : statusDie >= 16 ? 1 : 0],
						String.valueOf(40 + rng.nextGaussian() * 9)));
			}
			if (rows.size() > 1500) {
				rows.subList(0, rows.size() - 1500).clear();   // a rolling window, like a real feed
			}
			Table table = Table.of(List.of("at", "status", "latency"), List.copyOf(rows));

			// One line per status over time; a stale build landing after a newer tick is dropped.
			pipeline.prepare(new ChartSpec.Line(0, 2, 1, Aggregate.AVG), table, canvas.getWidth(),
					canvas::setData, msg -> canvas.setTitle("Requests per second", msg));

			count.setText(rows.size() + " rows");
			mix.setSegments(statusMix(rows));
		});

		JToggleButton pause = new JToggleButton("Pause");
		pause.addActionListener(e -> {
			if (pause.isSelected()) {
				feed.stop();
				pipeline.cancel();   // drop any in-flight build; the last delivered chart stays up
			}
			else {
				feed.start();
			}
		});

		JPanel top = new JPanel(new BorderLayout(8, 0));
		top.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
		top.add(pause, BorderLayout.WEST);
		top.add(mix, BorderLayout.CENTER);
		top.add(count, BorderLayout.EAST);
		JPanel root = new JPanel(new BorderLayout());
		root.add(top, BorderLayout.NORTH);
		root.add(canvas, BorderLayout.CENTER);

		EventQueue.invokeLater(() -> {
			Samples.show("streaming feed", root, 940, 560);
			javax.swing.SwingUtilities.getWindowAncestor(root).addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosed(WindowEvent e) {
					pipeline.shutdown();   // a host that owns a pipeline shuts it down with the view
				}
			});
			feed.start();
		});
	}

	// The live status mix for the strip — identity colours from the same theme the chart uses.
	private static List<ProportionStrip.Segment> statusMix(List<List<String>> rows) {
		long[] counts = new long[STATUSES.length];
		for (List<String> row : rows) {
			for (int s = 0; s < STATUSES.length; s++) {
				if (STATUSES[s].equals(row.get(1))) {
					counts[s]++;
				}
			}
		}
		List<ProportionStrip.Segment> segments = new ArrayList<>();
		for (int s = 0; s < STATUSES.length; s++) {
			segments.add(new ProportionStrip.Segment(STATUSES[s], (double) counts[s] / Math.max(1, rows.size()),
					counts[s], ChartTheme.INKWELL.series(s)));
		}
		return segments;
	}
}
