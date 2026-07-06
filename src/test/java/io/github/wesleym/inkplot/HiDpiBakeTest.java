package io.github.wesleym.inkplot;

import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The at-fit paint must be crisp on a HiDPI display: the cached base image is baked at DEVICE resolution, not
 * logical, so the first (unzoomed) paint lands on real pixels instead of upscaling a soft logical bitmap that
 * only sharpens once a zoom re-bakes it. A 2x paint transform must produce a 2x-sized base buffer.
 */
class HiDpiBakeTest {

	@Test
	void baseImageBakesAtDeviceResolution() {
		ChartCanvas canvas = new ChartCanvas(ChartTheme.PAPER);
		canvas.setData(ChartFixtures.barSingle());
		canvas.setSize(400, 300);

		BufferedImage target = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = target.createGraphics();
		g.scale(2.0, 2.0);   // simulate a 2x HiDPI paint transform, as Swing supplies on a Retina display
		try {
			canvas.paint(g);
		}
		finally {
			g.dispose();
		}

		assertEquals(800, canvas.baseLayerWidthForTest(),
				"base image is baked at device resolution (2x logical width), so the fit blit stays crisp");
	}

	@Test
	void baseImageBakesAtLogicalResolutionAtScaleOne() {
		ChartCanvas canvas = new ChartCanvas(ChartTheme.PAPER);
		canvas.setData(ChartFixtures.barSingle());
		canvas.setSize(400, 300);

		BufferedImage target = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = target.createGraphics();
		try {
			canvas.paint(g);
		}
		finally {
			g.dispose();
		}

		assertEquals(400, canvas.baseLayerWidthForTest(), "at 1x the base buffer matches the logical size");
	}
}
