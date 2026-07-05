package io.github.wesleym.inkplot;

import io.github.wesleym.inkplot.data.ChartData;
import io.github.wesleym.inkplot.render.ChartHoverState;
import io.github.wesleym.inkplot.render.PlotContext;
import io.github.wesleym.inkplot.scale.Scale;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dev-only visual harness for the hover layer: renders the crosshair + all-series tooltip on a line, and a lifted mark
 * with its tooltip on a bar and a scatter point, so the hover typography and highlight can be eyeballed. Writes
 * build/chart-hover-*.png.
 */
class ChartHoverPreviewTest {

	private static final int W = 640;
	private static final int H = 400;

	@Test
	void rendersHoverStates() throws Exception {
		renderHover("line", ChartFixtures.lineMulti(),
				ctx -> new Point(ctx.plot().x + ctx.plot().width * 3 / 5, ctx.plot().y + ctx.plot().height / 2));

		renderHover("bar", ChartFixtures.barSingle(),
				ctx -> new Point((int) ((Scale.Band) ctx.xScale()).center(0), (int) ctx.yScale().map(6000)));

		renderHover("scatter", ChartFixtures.scatter(),
				ctx -> new Point(ctx.plot().x + ctx.plot().width / 2, ctx.plot().y + ctx.plot().height / 2));

		// Mid-ring bottom-left (the second-largest slice) — its lift, its colour-lit leader, and the count tooltip,
		// with the callout label far enough from the pointer that the tooltip doesn't cover it.
		renderHover("doughnut", ChartFixtures.doughnutTail(),
				ctx -> new Point(ctx.plot().x + (int) (ctx.plot().width * 0.325),
						ctx.plot().y + (int) (ctx.plot().height * 0.735)));

		// A cell in the second-largest waffle band — its whole category merges into one solid silhouette.
		renderHover("waffle", ChartFixtures.waffle(),
				ctx -> new Point(ctx.plot().x + (int) (ctx.plot().width * 0.35),
						ctx.plot().y + (int) (ctx.plot().height * 0.62)));
	}

	private void renderHover(String name, ChartData data, Function<PlotContext, Point> picker) throws Exception {
		ChartTheme theme = ChartTheme.LIGHT;
		ChartCanvas canvas = new ChartCanvas(theme);
		canvas.setData(data);
		canvas.setSize(W, H);

		paint(canvas);   // first pass populates the plot context (scales) the picker reads
		Point p = picker.apply(canvas.plotContext());
		canvas.setHover(ChartHoverState.at(p.x, p.y));
		BufferedImage img = paint(canvas);

		File out = new File("build/chart-hover-" + name + ".png");
		ImageIO.write(img, "png", out);
		assertTrue(out.exists() && out.length() > 0, "hover preview written: " + out);
	}

	private static BufferedImage paint(ChartCanvas canvas) {
		BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		canvas.paint(g);
		g.dispose();
		return img;
	}
}
