package io.github.wesleym.inkplot.spec;

/**
 * How a category axis orders its slots. Value sorts read fastest (the eye scans a monotonic profile), so largest
 * first is the default; the folded "Other" bucket stays last under every ordering except the source order.
 */
public enum CategoryOrder {

	VALUE_DESC("Largest first"),
	VALUE_ASC("Smallest first"),
	LABEL("A to Z"),
	SOURCE("As appears");

	private final String label;

	CategoryOrder(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}
}
