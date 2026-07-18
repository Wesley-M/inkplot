package io.github.wesleym.inkplot.render;

import io.github.wesleym.inkplot.ChartFormat;
import io.github.wesleym.inkplot.ChartStyle;
import io.github.wesleym.inkplot.ChartTheme;
import io.github.wesleym.inkplot.data.ChartData;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * The "stat" card: one headline number, centred and large, with its caption beneath. Axis-free like the shares
 * charts — it takes the whole padded plot and no scales. The number sizes itself to fill the plot (bounded), so
 * the same stat reads big in a wide tile and still fits a narrow one. No marks to hit-test; the number is the
 * whole chart, and the caption already says what it measures.
 */
public final class StatRenderer implements MarkRenderer {

	private final ChartData.Stat data;

	public StatRenderer(ChartData.Stat data) {
		this.data = data;
	}

	@Override
	public boolean axisFree() {
		return true;
	}

	@Override
	public XAxisModel xModel() {
		return new XAxisModel.Continuous(0, 1);
	}

	@Override
	public YAxisModel yModel() {
		return YAxisModel.linear(0, 1);
	}

	@Override
	public List<LegendEntry> legend(ChartTheme theme) {
		return List.of();
	}

	@Override
	public void paintMarks(Graphics2D g, PlotContext ctx) {
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		Rectangle plot = ctx.plot();
		ChartTheme theme = ctx.theme();
		FontRenderContext frc = g.getFontRenderContext();

		String number = format();
		String caption = data.valueLabel() == null ? "" : data.valueLabel();

		// Size the number so it fills most of the plot on whichever axis binds first, clamped to a legible band.
		Font ref = ChartStyle.font(40f, Font.BOLD);
		double refW = ref.getStringBounds(number, frc).getWidth();
		LineMetrics refM = ref.getLineMetrics(number, frc);
		double refH = refM.getAscent() + refM.getDescent();
		double widthScale = refW > 0 ? plot.width * 0.84 / refW : 1;
		double heightScale = refH > 0 ? plot.height * 0.52 / refH : 1;
		float numberSize = (float) Math.max(16, Math.min(104, 40 * Math.min(widthScale, heightScale)));
		Font numberFont = ChartStyle.font(numberSize, Font.BOLD);

		Rectangle2D numBounds = numberFont.getStringBounds(number, frc);
		LineMetrics numM = numberFont.getLineMetrics(number, frc);
		Font captionFont = ChartStyle.font(ChartStyle.BODY, Font.PLAIN);
		LineMetrics capM = captionFont.getLineMetrics(caption, frc);
		double captionH = caption.isEmpty() ? 0 : capM.getAscent() + capM.getDescent();
		int gap = caption.isEmpty() ? 0 : ChartStyle.px(6);

		double blockH = numM.getAscent() + numM.getDescent() + gap + captionH;
		double top = plot.y + (plot.height - blockH) / 2.0;

		g.setFont(numberFont);
		g.setColor(theme.text());
		float numX = (float) (plot.x + (plot.width - numBounds.getWidth()) / 2.0);
		float numBaseline = (float) (top + numM.getAscent());
		g.drawString(number, numX, numBaseline);

		if (!caption.isEmpty()) {
			g.setFont(captionFont);
			g.setColor(theme.muted());
			double capW = captionFont.getStringBounds(caption, frc).getWidth();
			float capBaseline = (float) (numBaseline + numM.getDescent() + gap + capM.getAscent());
			g.drawString(caption, (float) (plot.x + (plot.width - capW) / 2.0), capBaseline);
		}
	}

	private String format() {
		return data.integral()
				? ChartFormat.grouped(Math.round(data.value()))
				: ChartFormat.groupedDecimal(data.value());
	}
}
