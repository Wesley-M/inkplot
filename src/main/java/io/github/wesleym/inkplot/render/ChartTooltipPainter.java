package io.github.wesleym.inkplot.render;

import io.github.wesleym.inkplot.ChartStyle;
import io.github.wesleym.inkplot.ChartTheme;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;

/**
 * Paints a chart tooltip as a lightweight rounded panel drawn straight onto the canvas overlay — no heavyweight Swing
 * popup, so it repaints in the same frame as the crosshair. The value leads each row in strong ink with the series
 * name following in a secondary tone, keyed by a short stroke of the series colour; the title (the category or X) sits
 * above in the muted tone. The panel flips across the pointer to stay inside the canvas bounds.
 */
public final class ChartTooltipPainter {

	private ChartTooltipPainter() { }

	public static void paint(Graphics2D g, Rectangle bounds, int anchorX, int anchorY, TooltipContent content,
			ChartTheme theme) {
		if (content == null || content.rows().isEmpty()) {
			return;
		}
		g.setFont(ChartStyle.caption());
		var fm = g.getFontMetrics();
		int pad = ChartStyle.px(ChartStyle.SPACE_S);
		int keyLen = ChartStyle.px(10);
		int keyGap = ChartStyle.px(6);
		int valueGap = ChartStyle.px(6);
		int rowH = fm.getHeight();

		int titleW = content.title() == null ? 0 : fm.stringWidth(content.title());
		int contentW = titleW;
		for (TooltipContent.Row row : content.rows()) {
			int w = keyLen + keyGap + fm.stringWidth(row.value()) + valueGap + fm.stringWidth(row.label());
			contentW = Math.max(contentW, w);
		}
		int titleH = content.title() == null ? 0 : rowH + ChartStyle.px(2);
		int boxW = contentW + pad * 2;
		int boxH = titleH + content.rows().size() * rowH + pad * 2;

		int x = anchorX + ChartStyle.px(14);
		int y = anchorY + ChartStyle.px(14);
		if (x + boxW > bounds.x + bounds.width) {
			x = anchorX - ChartStyle.px(14) - boxW;
		}
		if (y + boxH > bounds.y + bounds.height) {
			y = anchorY - ChartStyle.px(14) - boxH;
		}
		x = Math.max(bounds.x + ChartStyle.px(2), x);
		y = Math.max(bounds.y + ChartStyle.px(2), y);

		g.setColor(theme.elevated());
		g.fill(new RoundRectangle2D.Double(x, y, boxW, boxH, ChartStyle.RADIUS, ChartStyle.RADIUS));
		g.setColor(theme.hairline());
		g.draw(new RoundRectangle2D.Double(x, y, boxW, boxH, ChartStyle.RADIUS, ChartStyle.RADIUS));

		int textX = x + pad;
		int cursorY = y + pad + fm.getAscent();
		if (content.title() != null) {
			g.setColor(theme.muted());
			g.drawString(content.title(), textX, cursorY);
			cursorY += titleH;
		}
		for (TooltipContent.Row row : content.rows()) {
			int mid = cursorY - fm.getAscent() / 2 + 1;
			g.setColor(row.key());
			g.fillRect(textX, mid - ChartStyle.px(1), keyLen, Math.max(1, ChartStyle.px(2)));
			int vx = textX + keyLen + keyGap;
			g.setColor(theme.text());
			g.drawString(row.value(), vx, cursorY);
			g.setColor(theme.muted());
			g.drawString(row.label(), vx + fm.stringWidth(row.value()) + valueGap, cursorY);
			cursorY += rowH;
		}
	}
}
