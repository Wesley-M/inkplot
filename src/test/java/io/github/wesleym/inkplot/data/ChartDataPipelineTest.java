package io.github.wesleym.inkplot.data;

import io.github.wesleym.inkplot.spec.Aggregate;
import io.github.wesleym.inkplot.spec.ChartSpec;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartDataPipelineTest {

	private static ResultSnapshot oneCategory(int rows) {
		List<List<String>> data = new ArrayList<>();
		for (int i = 0; i < rows; i++) {
			data.add(List.of("A"));
		}
		return new ResultSnapshot(List.of("k"), List.of("varchar"), data, false);
	}

	@Test
	void deliversPreparedDataOnTheEdt() throws Exception {
		ChartDataPipeline pipeline = new ChartDataPipeline();
		CountDownLatch done = new CountDownLatch(1);
		boolean[] onEdt = { false };
		ChartData[] result = new ChartData[1];
		pipeline.prepare(new ChartSpec.Bar(0, null, Aggregate.COUNT, null, false), oneCategory(7), 400, data -> {
			onEdt[0] = SwingUtilities.isEventDispatchThread();
			result[0] = data;
			done.countDown();
		}, err -> done.countDown());

		assertTrue(done.await(5, TimeUnit.SECONDS), "the pipeline delivered");
		assertTrue(onEdt[0], "delivery is on the EDT");
		assertEquals(7.0, ((ChartData.Bar) result[0]).values()[0][0], 1e-9);
		pipeline.shutdown();
	}

	@Test
	void supersededPreparesNeverLandOnlyTheLatest() throws Exception {
		ChartDataPipeline pipeline = new ChartDataPipeline();
		List<Double> delivered = new CopyOnWriteArrayList<>();
		// Fire five in a row; the generation counter bumps synchronously on each call, so by the time any worker task
		// checks, only the fifth is current — the earlier four must be dropped.
		for (int i = 1; i <= 5; i++) {
			pipeline.prepare(new ChartSpec.Bar(0, null, Aggregate.COUNT, null, false), oneCategory(i), 400,
					data -> delivered.add(((ChartData.Bar) data).values()[0][0]), err -> { });
		}
		Thread.sleep(400);
		SwingUtilities.invokeAndWait(() -> { });   // flush any pending EDT deliveries

		assertEquals(1, delivered.size(), "exactly one result lands");
		assertEquals(5.0, delivered.get(0), 1e-9, "and it is the most recent request");
		pipeline.shutdown();
	}

	@Test
	void shutdownDropsDeliveryAlreadyQueuedForTheEdt() throws Exception {
		ChartDataPipeline pipeline = new ChartDataPipeline();
		CountDownLatch edtBlocked = new CountDownLatch(1);
		CountDownLatch releaseEdt = new CountDownLatch(1);
		List<ChartData> delivered = new CopyOnWriteArrayList<>();

		SwingUtilities.invokeLater(() -> {
			edtBlocked.countDown();
			try {
				releaseEdt.await(3, TimeUnit.SECONDS);
			}
			catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
			}
		});
		assertTrue(edtBlocked.await(5, TimeUnit.SECONDS), "the EDT blocker started");

		pipeline.prepare(new ChartSpec.Bar(0, null, Aggregate.COUNT, null, false), oneCategory(3), 400,
				delivered::add, err -> { });
		Thread.sleep(150);
		pipeline.shutdown();
		releaseEdt.countDown();
		SwingUtilities.invokeAndWait(() -> { });

		assertTrue(delivered.isEmpty(), "shutdown invalidates even a result already queued for the EDT");
	}
}
