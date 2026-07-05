package io.github.wesleym.inkplot.data;

import io.github.wesleym.inkplot.spec.ChartSpec;

import javax.swing.SwingUtilities;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Prepares chart data off the EDT and delivers it back on the EDT, superseding stale work with a generation counter so
 * rapid picker changes and query re-runs never stack concurrent extractions or land an out-of-date chart. A single
 * worker thread does the parsing/binning/downsampling; the EDT only ever receives the finished, current result.
 */
public final class ChartDataPipeline {

	private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "cb-chart-prep");
		t.setDaemon(true);
		return t;
	});
	private final AtomicInteger generation = new AtomicInteger();

	/**
	 * Builds the chart for {@code spec} over {@code snapshot} on the worker thread and calls {@code onReady} (or
	 * {@code onError}) on the EDT — unless a newer request superseded this one first, in which case the result is
	 * dropped. {@code plotWidth} sizes pixel-aware downsampling.
	 */
	public void prepare(ChartSpec spec, ResultSnapshot snapshot, int plotWidth, Consumer<ChartData> onReady,
			Consumer<String> onError) {
		int mine = generation.incrementAndGet();
		try {
			submit(spec, snapshot, plotWidth, onReady, onError, mine);
		}
		catch (java.util.concurrent.RejectedExecutionException shutDown) {
			// The worker was shut down — never leave the view hung; report it instead.
			deliver(mine, () -> onError.accept("chart engine unavailable"));
		}
	}

	private void submit(ChartSpec spec, ResultSnapshot snapshot, int plotWidth, Consumer<ChartData> onReady,
			Consumer<String> onError, int mine) {
		worker.submit(() -> {
			try {
				ChartData data = ChartBuilder.build(spec, snapshot, Math.max(64, plotWidth));
				deliver(mine, () -> onReady.accept(data));
			}
			catch (ChartDataException expected) {
				deliver(mine, () -> onError.accept(expected.getMessage()));
			}
			catch (RuntimeException unexpected) {
				deliver(mine, () -> onError.accept("Couldn't build the chart: " + unexpected.getMessage()));
			}
		});
	}

	/** Bumps the generation so any in-flight preparation is dropped when it finishes (e.g. the chart view is hidden). */
	public void cancel() {
		generation.incrementAndGet();
	}

	public void shutdown() {
		generation.incrementAndGet();
		worker.shutdownNow();
	}

	// Marshals `action` onto the EDT, but only if this generation is still the current one both when scheduling and
	// when it actually runs — so a superseded build never repaints the chart.
	private void deliver(int mine, Runnable action) {
		if (mine != generation.get()) {
			return;
		}
		SwingUtilities.invokeLater(() -> {
			if (mine == generation.get()) {
				action.run();
			}
		});
	}
}
