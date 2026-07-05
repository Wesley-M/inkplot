/**
 * The inkplot API surface, stated as a module: the facade and widget layer ({@code inkplot}), the
 * tabular data-in layer ({@code inkplot.data}), and the chart specification ({@code inkplot.spec}).
 * The {@code render} and {@code scale} packages are implementation — not exported, free to change
 * between versions. Classpath consumers see no hard wall, but the contract is machine-stated here and
 * enforced for anyone on the module path.
 */
module io.github.wesleym.inkplot {
	requires transitive java.desktop;

	exports io.github.wesleym.inkplot;
	exports io.github.wesleym.inkplot.data;
	exports io.github.wesleym.inkplot.spec;
}
