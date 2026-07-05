package io.github.wesleym.inkplot.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a stream of category/series string values into a small, bounded set of slot indices: it keeps first-seen order
 * when the values fit, and once they exceed the target it keeps the most frequent and folds the long tail into a single
 * "Other" slot. Discovery itself is capped, so a runaway high-cardinality column is refused rather than allowed to
 * build an unbounded map.
 */
final class CategoryKeyer {

	static final String OTHER = "Other";

	private final LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();
	private final int scanCap;
	private boolean overflowed;

	CategoryKeyer(int scanCap) {
		this.scanCap = scanCap;
	}

	void observe(String key) {
		String k = key == null || key.isEmpty() ? "(blank)" : key;
		if (!counts.containsKey(k) && counts.size() >= scanCap) {
			overflowed = true;
			return;
		}
		counts.merge(k, 1, Integer::sum);
	}

	boolean overflowed() {
		return overflowed;
	}

	int distinct() {
		return counts.size();
	}

	/** Resolves the observed keys into up to {@code maxSlots} slots (the last being "Other" when the tail is folded). */
	Resolved resolve(int maxSlots) {
		if (counts.size() <= maxSlots) {
			List<String> labels = new ArrayList<>(counts.keySet());
			Map<String, Integer> index = new LinkedHashMap<>();
			for (int i = 0; i < labels.size(); i++) {
				index.put(labels.get(i), i);
			}
			return new Resolved(labels.toArray(new String[0]), index, -1);
		}

		// Keep the most frequent (maxSlots − 1); everything else folds into "Other".
		List<Map.Entry<String, Integer>> byCount = new ArrayList<>(counts.entrySet());
		byCount.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
		java.util.Set<String> kept = new java.util.HashSet<>();
		for (int i = 0; i < maxSlots - 1 && i < byCount.size(); i++) {
			kept.add(byCount.get(i).getKey());
		}
		// Preserve first-seen order among the kept keys, then append "Other".
		List<String> labels = new ArrayList<>();
		Map<String, Integer> index = new LinkedHashMap<>();
		for (String key : counts.keySet()) {
			if (kept.contains(key)) {
				index.put(key, labels.size());
				labels.add(key);
			}
		}
		int otherIndex = labels.size();
		labels.add(OTHER);
		return new Resolved(labels.toArray(new String[0]), index, otherIndex);
	}

	/** The resolved slots: the labels in draw order, the key→slot map, and the "Other" slot (−1 if none). */
	record Resolved(String[] labels, Map<String, Integer> index, int otherIndex) {

		int slotOf(String key) {
			String k = key == null || key.isEmpty() ? "(blank)" : key;
			Integer slot = index.get(k);
			return slot != null ? slot : otherIndex;
		}
	}
}
