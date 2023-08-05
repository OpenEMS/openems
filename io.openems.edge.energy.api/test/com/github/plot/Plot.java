package com.github.plot;

//CHECKSTYLE:OFF
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

/**
 * Simple implementation of plot. Minimal features, no dependencies besides standard libraries.
 * Options are self-descriptive, see also samples.
 * 
 * @author Yuriy Guskov
 */
public class Plot {

	public enum Line { NONE, SOLID, DASHED };
	public enum Marker { NONE, CIRCLE, SQUARE, DIAMOND, COLUMN, BAR };
	public enum AxisFormat { NUMBER, NUMBER_KGM, NUMBER_INT, TIME_HM, TIME_HMS, DATE, DATETIME_HM, DATETIME_HMS }
	public enum LegendFormat { NONE, TOP, RIGHT, BOTTOM }
	
	private enum HorizAlign { LEFT, CENTER, RIGHT }
	private enum VertAlign { TOP, CENTER, BOTTOM }

	private PlotOptions opts = new PlotOptions();
	
	private Rectangle boundRect;
	private PlotArea plotArea;
	private Map<String, Axis> xAxes = new HashMap<String, Axis>(3);
	private Map<String, Axis> yAxes = new HashMap<String, Axis>(3);
	private Map<String, DataSeries> dataSeriesMap = new LinkedHashMap<String, DataSeries>(5);
	
	public static Plot plot(PlotOptions opts) {
		return new Plot(opts);
	}
	
	public static PlotOptions plotOpts() {
		return new PlotOptions();
	}
	
	public static class PlotOptions {
		
		private String title = "";
		private int width = 800;
		private int height = 600;
		private Color backgroundColor = Color.WHITE;
		private Color foregroundColor = Color.BLACK;
		private Font titleFont = new Font("Arial", Font.BOLD, 16);
		private int padding = 10; // padding for the entire image
		private int plotPadding = 5; // padding for plot area (to have min and max values padded)
		private int labelPadding = 10;
		private int defaultLegendSignSize = 10;
		private int legendSignSize = 10;
		private Point grids = new Point(10 ,10); // grid lines by x and y
		private Color gridColor = Color.GRAY;
		private Stroke gridStroke = new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
		        BasicStroke.JOIN_MITER, 10.0f, new float[] { 5.0f }, 0.0f);
		private int tickSize = 5;
		private Font labelFont = new Font("Arial", 0, 12);
		private LegendFormat legend = LegendFormat.NONE;
		
		private PlotOptions() {}
		
		public PlotOptions title(String title) {
			this.title = title;
			return this;
		}
		
		public PlotOptions width(int width) {
			this.width = width;
			return this;
		}
		
		public PlotOptions height(int height) {
			this.height = height;
			return this;
		}
		
		public PlotOptions bgColor(Color color) {
			this.backgroundColor = color;
			return this;
		}
		
		public PlotOptions fgColor(Color color) {
			this.foregroundColor = color;
			return this;
		}
		
		public PlotOptions titleFont(Font font) {
			this.titleFont = font;
			return this;
		}
		
		public PlotOptions padding(int padding) {
			this.padding = padding;
			return this;
		}
		
		public PlotOptions plotPadding(int padding) {
			this.plotPadding = padding;
			return this;
		}
		
		public PlotOptions labelPadding(int padding) {
			this.labelPadding = padding;
			return this;
		}

		public PlotOptions labelFont(Font font) {
			this.labelFont = font;
			return this;
		}

		public PlotOptions grids(int byX, int byY) {
			this.grids = new Point(byX, byY);
			return this;
		}

		public PlotOptions gridColor(Color color) {
			this.gridColor = color;
			return this;
		}
		
		public PlotOptions gridStroke(Stroke stroke) {
			this.gridStroke = stroke;
			return this;
		}
		
		public PlotOptions tickSize(int value) {
			this.tickSize = value;
			return this;
		}
		
		public PlotOptions legend(LegendFormat legend) {
			this.legend = legend;
			return this;
		}
		
	}
	
	private Plot(PlotOptions opts) {
		if (opts != null)
			this.opts = opts;
		boundRect = new Rectangle(0, 0, this.opts.width, this.opts.height);
		plotArea = new PlotArea();
	}

	public PlotOptions opts() {
		return opts;
	}

	public Plot xAxis(String name, AxisOptions opts) {
		xAxes.put(name, new Axis(name, opts));
		return this;
	}

	public Plot yAxis(String name, AxisOptions opts) {
		yAxes.put(name, new Axis(name, opts));
		return this;
	}

	public Plot series(String name, Data data, DataSeriesOptions opts) {
		DataSeries series = dataSeriesMap.get(name);
		if (opts != null)
			opts.setPlot(this);
		if (series == null) {
			series = new DataSeries(name, data, opts);
			dataSeriesMap.put(name, series);
		} else {
			series.data = data;
			series.opts = opts;
		}
		return this;
	}

	public Plot series(String name, DataSeriesOptions opts) {
		DataSeries series = dataSeriesMap.get(name);
		if (opts != null)
			opts.setPlot(this);
		if (series != null)
			series.opts = opts;
		return this;
	}

	private void calc(Graphics2D g) {
		plotArea.calc(g);
	}
	
	private void clear() {
		plotArea.clear();
		for (DataSeries series : dataSeriesMap.values())
			series.clear();
	}
	
	private BufferedImage draw() {
		BufferedImage image = new BufferedImage(opts.width, opts.height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		try {
			calc(g);
			drawBackground(g);
			plotArea.draw(g);
			for (DataSeries series : dataSeriesMap.values())
				series.draw(g);
			return image;
		} finally {
			g.dispose();
		}
	}

	private void drawBackground(Graphics2D g) {
		g.setColor(opts.backgroundColor); 
		g.fillRect(0, 0, opts.width, opts.height);
	}

	public void save(String fileName, String type) throws IOException {
		clear();
		BufferedImage bi = draw();
		File outputFile = new File(fileName + "." + type);
		ImageIO.write(bi, type, outputFile);
	}
	
	private class Legend {
		Rectangle rect;
		Rectangle2D labelRect;
		public int entryWidth;
		public int entryWidthPadded;
		public int entryCount;
		public int xCount;
		public int yCount;
	}
	
	private class PlotArea {
	
		private Rectangle plotBorderRect = new Rectangle(); // boundRect | labels/legend | plotBorderRect | plotPadding | plotRect/clipRect
		private Rectangle plotRect = new Rectangle();
		private Rectangle plotClipRect = new Rectangle();
		private Legend legend = new Legend();
		
		private Range xPlotRange = new Range(0, 0);
		private Range yPlotRange = new Range(0, 0);
		
		public PlotArea() {
			clear();
		}
		
		private void clear() {
			plotBorderRect.setBounds(boundRect);
			plotRectChanged();
		}
		
		private void offset(int dx, int dy, int dw, int dh) {
			plotBorderRect.translate(dx, dy);
			plotBorderRect.setSize(plotBorderRect.width - dx - dw, plotBorderRect.height - dy - dh);
			plotRectChanged();
		}
		
		private void plotRectChanged() {
			plotRect.setBounds(plotBorderRect.x + opts.plotPadding, plotBorderRect.y + opts.plotPadding, 
					plotBorderRect.width - opts.plotPadding * 2, plotBorderRect.height - opts.plotPadding * 2);
			xPlotRange.setMin(plotRect.getX());
			xPlotRange.setMax(plotRect.getX() + plotRect.getWidth());
			yPlotRange.setMin(plotRect.getY());
			yPlotRange.setMax(plotRect.getY() + plotRect.getHeight());
			
			plotClipRect.setBounds(plotBorderRect.x + 1, plotBorderRect.y + 1, plotBorderRect.width - 1, plotBorderRect.height - 1);
		}

		private void calc(Graphics2D g) {
			calcAxes(g);
			calcRange(true);
			calcRange(false);
			calcAxisLabels(g, true);
			calcAxisLabels(g, false);
			g.setFont(opts.titleFont);
			FontMetrics fm = g.getFontMetrics();
			Rectangle2D titleRect = fm.getStringBounds(opts.title, g);
			g.setFont(opts.labelFont);
			fm = g.getFontMetrics();
			int xAxesHeight = 0, xAxesHalfWidth = 0;
			for (Map.Entry<String, Axis> entry : xAxes.entrySet()) {
				Axis xAxis = entry.getValue();
				xAxesHeight += toInt(xAxis.labelRect.getHeight()) + opts.labelPadding * 2;
				if (xAxis.labelRect.getWidth() > xAxesHalfWidth)
					xAxesHalfWidth = toInt(xAxis.labelRect.getWidth());
			}
			int yAxesWidth = 0;
			for (Map.Entry<String, Axis> entry : yAxes.entrySet())
				yAxesWidth += toInt(entry.getValue().labelRect.getWidth()) + opts.labelPadding * 2;
			int dx = opts.padding + yAxesWidth; 
			int dy = opts.padding + toInt(titleRect.getHeight() + opts.labelPadding); 
			int dw = opts.padding;
			if (opts.legend != LegendFormat.RIGHT)
				dw += xAxesHalfWidth; // half of label goes beyond a plot in right bottom corner 
			int dh = opts.padding + xAxesHeight;
			// offset for legend
			Rectangle temp = new Rectangle(plotBorderRect); // save plotRect
			offset(dx, dy, dw, dh);
			calcLegend(g); // use plotRect
			plotBorderRect.setBounds(temp); // restore plotRect
			switch (opts.legend) {
			case TOP: dy += legend.rect.height + opts.labelPadding; break;
			case RIGHT: dw += legend.rect.width + opts.labelPadding; break;
			case BOTTOM: dh += legend.rect.height; break;
			default: 
			}
			offset(dx, dy, dw, dh);
		}
		
		private void draw(Graphics2D g) {
			drawPlotArea(g);
			drawGrid(g);
			drawAxes(g);
			drawLegend(g);
			// if check needed that content is inside padding
			//g.setColor(Color.GRAY);
			//g.drawRect(boundRect.x + opts.padding, boundRect.y + opts.padding, boundRect.width - opts.padding * 2, boundRect.height - opts.padding * 2);
		}

		private void drawPlotArea(Graphics2D g) {
			g.setColor(opts.foregroundColor);
			g.drawRect(plotBorderRect.x, plotBorderRect.y, plotBorderRect.width, plotBorderRect.height);
			g.setFont(opts.titleFont);
			drawLabel(g, opts.title, plotBorderRect.x + toInt(plotBorderRect.getWidth() / 2), opts.padding, HorizAlign.CENTER, VertAlign.TOP);
		}

		private void drawGrid(Graphics2D g) {
			Stroke stroke = g.getStroke();
			g.setStroke(opts.gridStroke);
			g.setColor(opts.gridColor);
			
			int leftX = plotBorderRect.x + 1;
			int rightX = plotBorderRect.x + plotBorderRect.width - 1;
			int topY = plotBorderRect.y + 1;
			int bottomY = plotBorderRect.y + plotBorderRect.height - 1;

			for (int i = 0; i < opts.grids.x + 1; i++) {
				int x = toInt(plotRect.x + (plotRect.getWidth() / opts.grids.x) * i);
				g.drawLine(x, topY, x, bottomY);
			}
			
			for (int i = 0; i < opts.grids.y + 1; i++) {
				int y = toInt(plotRect.y + (plotRect.getHeight() / opts.grids.y) * i);
				g.drawLine(leftX, y, rightX, y);
			}
			
			g.setStroke(stroke);
		}

		private void calcAxes(Graphics2D g) {
			Axis xAxis = xAxes.isEmpty() ? new Axis("", null) : xAxes.values().iterator().next();
			Axis yAxis = yAxes.isEmpty() ? new Axis("", null) : yAxes.values().iterator().next();
			int xCount = 0, yCount = 0;
			for (DataSeries series : dataSeriesMap.values()) {
				if (series.opts.xAxis == null) {
					series.opts.xAxis = xAxis;
					xCount++;
				}
				if (series.opts.yAxis == null) {
					series.opts.yAxis = yAxis;
					yCount++;
				}
				series.addAxesToName();
			}
			if (xAxes.isEmpty() && xCount > 0)
				xAxes.put("x", xAxis);
			if (yAxes.isEmpty() && yCount > 0)
				yAxes.put("y", yAxis);
		}

		private void calcAxisLabels(Graphics2D g, boolean isX) {
			FontMetrics fm = g.getFontMetrics();
			Rectangle2D rect = null;
			double w = 0, h = 0;
			Map<String, Axis> axes = isX ? xAxes : yAxes;
			int grids = isX ? opts.grids.x : opts.grids.y;
			for (Map.Entry<String, Axis> entry : axes.entrySet()) {
				Axis axis = entry.getValue();
				axis.labels = new String[grids + 1];
				axis.labelRect = fm.getStringBounds("", g);
				double xStep = axis.opts.range.diff / grids;
				for (int j = 0; j < grids + 1; j++) {
					axis.labels[j] = formatDouble(axis.opts.range.min + xStep * j, axis.opts.format);
					rect = fm.getStringBounds(axis.labels[j], g);
					if (rect.getWidth() > w)
						w = rect.getWidth();
					if (rect.getHeight() > h)
						h = rect.getHeight();
				}
				axis.labelRect.setRect(0, 0, w, h);
			}
		}

		private void calcRange(boolean isX) {
			for (DataSeries series : dataSeriesMap.values()) {
				Axis axis = isX ? series.opts.xAxis : series.opts.yAxis;
				if (axis.opts.dynamicRange) {
					Range range = isX ? series.xRange() : series.yRange();
					if (axis.opts.range == null)
						axis.opts.range = range;
					else {
						if (range.max > axis.opts.range.max)
							axis.opts.range.setMax(range.max);
						if (range.min < axis.opts.range.min)
							axis.opts.range.setMin(range.min);
					}
				}
			}
			Map<String, Axis> axes = isX ? xAxes : yAxes;
			for (Iterator<Axis> it = axes.values().iterator(); it.hasNext(); ) {
				Axis axis = it.next();
				if (axis.opts.range == null)
					it.remove();
			}
		}

		private void drawAxes(Graphics2D g) {
			g.setFont(opts.labelFont);
			g.setColor(opts.foregroundColor);
			
			int leftXPadded = plotBorderRect.x - opts.labelPadding;
			int rightX = plotBorderRect.x + plotBorderRect.width;
			int bottomY = plotBorderRect.y + plotBorderRect.height;
			int bottomYPadded = bottomY + opts.labelPadding;
			
			int axisOffset = 0;
			for (Map.Entry<String, Axis> entry : xAxes.entrySet()) {
				Axis axis = entry.getValue();
				double xStep = axis.opts.range.diff / opts.grids.x;
				
				drawLabel(g, axis.name, rightX + opts.labelPadding, bottomY + axisOffset, HorizAlign.LEFT, VertAlign.CENTER);
				g.drawLine(plotRect.x, bottomY + axisOffset, plotRect.x + plotRect.width, bottomY + axisOffset);
				
				for (int j = 0; j < opts.grids.x + 1; j++) {
					int x = toInt(plotRect.x + (plotRect.getWidth() / opts.grids.x) * j);
					drawLabel(g, formatDouble(axis.opts.range.min + xStep * j, axis.opts.format), x, bottomYPadded + axisOffset, HorizAlign.CENTER, VertAlign.TOP);
					g.drawLine(x, bottomY + axisOffset, x, bottomY + opts.tickSize + axisOffset);
				}
				axisOffset += toInt(axis.labelRect.getHeight() + opts.labelPadding * 2);
			}
			
			axisOffset = 0;
			for (Map.Entry<String, Axis> entry : yAxes.entrySet()) {
				Axis axis = entry.getValue();
				double yStep = axis.opts.range.diff / opts.grids.y;

				drawLabel(g, axis.name, leftXPadded - axisOffset, plotBorderRect.y - toInt(axis.labelRect.getHeight() + opts.labelPadding), HorizAlign.RIGHT, VertAlign.CENTER);
				g.drawLine(plotBorderRect.x - axisOffset, plotRect.y + plotRect.height, plotBorderRect.x - axisOffset, plotRect.y);

				for (int j = 0; j < opts.grids.y + 1; j++) {
					int y = toInt(plotRect.y + (plotRect.getHeight() / opts.grids.y) * j);
					drawLabel(g, formatDouble(axis.opts.range.max - yStep * j, axis.opts.format), leftXPadded - axisOffset, y, HorizAlign.RIGHT, VertAlign.CENTER);
					g.drawLine(plotBorderRect.x - axisOffset, y,  plotBorderRect.x - opts.tickSize - axisOffset, y);
				}
				axisOffset += toInt(axis.labelRect.getWidth() + opts.labelPadding * 2);
			}
		}
		
		private void calcLegend(Graphics2D g) {
			legend.rect = new Rectangle(0, 0);
			if (opts.legend == LegendFormat.NONE)
				return;
			int size = dataSeriesMap.size();
			if (size == 0)
				return;

			FontMetrics fm = g.getFontMetrics();
			Iterator<DataSeries> it = dataSeriesMap.values().iterator();
			legend.labelRect = fm.getStringBounds(it.next().nameWithAxes, g);
			int legendSignSize = opts.defaultLegendSignSize;
			while (it.hasNext()) {
				DataSeries series = it.next();
				Rectangle2D rect = fm.getStringBounds(series.nameWithAxes, g);
				if (rect.getWidth() > legend.labelRect.getWidth())
					legend.labelRect.setRect(0, 0, rect.getWidth(), legend.labelRect.getHeight());
				if (rect.getHeight() > legend.labelRect.getHeight())
					legend.labelRect.setRect(0, 0, legend.labelRect.getWidth(), rect.getHeight());
				switch (series.opts.marker) {
				case CIRCLE: case SQUARE:
					if (series.opts.markerSize + opts.defaultLegendSignSize > legendSignSize)
						legendSignSize = series.opts.markerSize + opts.defaultLegendSignSize;
					break;
				case DIAMOND:
					if (series.getDiagMarkerSize() + opts.defaultLegendSignSize > legendSignSize)
						legendSignSize = series.getDiagMarkerSize() + opts.defaultLegendSignSize;
					break;
				default:
				}
			}
			opts.legendSignSize = legendSignSize;
			
			legend.entryWidth = legendSignSize + opts.labelPadding + toInt(legend.labelRect.getWidth());
			legend.entryWidthPadded = legend.entryWidth + opts.labelPadding;

			switch (opts.legend) {
			case TOP: case BOTTOM:
				legend.entryCount = (int) Math.floor((double) (plotBorderRect.width - opts.labelPadding) / legend.entryWidthPadded);
				legend.xCount = size <= legend.entryCount ? size : legend.entryCount;
				legend.yCount = size <= legend.entryCount ? 1 : (int) Math.ceil((double) size / legend.entryCount);
				legend.rect.width = opts.labelPadding + (legend.xCount * legend.entryWidthPadded); 
				legend.rect.height = opts.labelPadding + toInt(legend.yCount * (opts.labelPadding + legend.labelRect.getHeight()));
				legend.rect.x = plotBorderRect.x + (plotBorderRect.width - legend.rect.width) / 2;
				if (opts.legend == LegendFormat.TOP)
					legend.rect.y = plotBorderRect.y;		
				else
					legend.rect.y = boundRect.height - legend.rect.height - opts.padding;
				break;
			case RIGHT:
				legend.rect.width = opts.labelPadding * 3 + legendSignSize + toInt(legend.labelRect.getWidth()); 
				legend.rect.height = opts.labelPadding * (size + 1) + toInt(legend.labelRect.getHeight() * size);				
				legend.rect.x = boundRect.width - legend.rect.width - opts.padding;
				legend.rect.y = plotBorderRect.y + plotBorderRect.height / 2 - legend.rect.height / 2;
				break;
			default: 
			}
		}

		private void drawLegend(Graphics2D g) {
			if (opts.legend == LegendFormat.NONE)
				return;
			
			g.drawRect(legend.rect.x, legend.rect.y, legend.rect.width, legend.rect.height);
			int labelHeight = toInt(legend.labelRect.getHeight());
			int x = legend.rect.x + opts.labelPadding;
			int y = legend.rect.y + opts.labelPadding + labelHeight / 2;
		
			switch (opts.legend) {
			case TOP: case BOTTOM:
				int i = 0;
				for (DataSeries series : dataSeriesMap.values()) {
					drawLegendEntry(g, series, x, y);
					x += legend.entryWidthPadded;
					if ((i + 1) % legend.xCount == 0) {
						x = legend.rect.x + opts.labelPadding;
						y += opts.labelPadding + labelHeight;
					}
					i++;
				}				
				break;
			case RIGHT:
				for (DataSeries series : dataSeriesMap.values()) {
					drawLegendEntry(g, series, x, y);
					y += opts.labelPadding + labelHeight;
				}
				break;
			default:
			}
		}
		
		private void drawLegendEntry(Graphics2D g, DataSeries series, int x, int y) {
			series.fillArea(g, x, y, x + opts.legendSignSize, y, y + opts.legendSignSize / 2);
			series.drawLine(g, x, y, x + opts.legendSignSize, y);
			series.drawMarker(g, x + opts.legendSignSize / 2, y, x, y + opts.legendSignSize / 2);
			g.setColor(opts.foregroundColor);
			drawLabel(g, series.nameWithAxes, x + opts.legendSignSize + opts.labelPadding, y, HorizAlign.LEFT, VertAlign.CENTER);
		}
		
	}
	
	public static class Range {
		
		private double min;
		private double max;
		private double diff;

		public Range(double min, double max) {
			this.min = min;
			this.max = max;
			this.diff = max - min;
		}
		
		public Range(Range range) {
			this.min = range.min;
			this.max = range.max;
			this.diff = max - min;
		}

		public void setMin(double min) {
			this.min = min;
			this.diff = max - min;
		}
	
		public void setMax(double max) {
			this.max = max;
			this.diff = max - min;
		}

		@Override
		public String toString() {
			return "Range [min=" + min + ", max=" + max + "]";
		}
		
	}

	public static AxisOptions axisOpts() {
		return new AxisOptions();
	}
	
	public static class AxisOptions {
		
		private AxisFormat format = AxisFormat.NUMBER;
		private boolean dynamicRange = true;
		private Range range;
		
		public AxisOptions format(AxisFormat format) {
			this.format = format;
			return this;
		}

		public AxisOptions range(double min, double max) {
			this.range = new Range(min, max);
			this.dynamicRange = false;
			return this;
		}

	}
	
	private class Axis {

		private String name;
		private AxisOptions opts = new AxisOptions();
		private Rectangle2D labelRect;
		private String[] labels;

		public Axis(String name, AxisOptions opts) {
			this.name = name;
			if (opts != null)
				this.opts = opts;
		}

		@Override
		public String toString() {
			return "Axis [name=" + name + ", opts=" + opts + "]";
		}
		
	}
	
	public static DataSeriesOptions seriesOpts() {
		return new DataSeriesOptions();
	}
	
	public static class DataSeriesOptions {
		
		private Color seriesColor = Color.BLUE;
		private Line line = Line.SOLID;
		private int lineWidth = 2;
		private float[] lineDash = new float[] { 3.0f, 3.0f };
		private Marker marker = Marker.NONE;
		private int markerSize = 10;
		private Color markerColor = Color.WHITE;
		private Color areaColor = null;
		private String xAxisName;
		private String yAxisName;
		private Axis xAxis;
		private Axis yAxis;

		public DataSeriesOptions color(Color seriesColor) {
			this.seriesColor = seriesColor;
			return this;
		}
		
		public DataSeriesOptions line(Line line) {
			this.line = line;
			return this;
		}

		public DataSeriesOptions lineWidth(int width) {
			this.lineWidth = width;
			return this;
		}

		public DataSeriesOptions lineDash(float[] dash) {
			this.lineDash = dash;
			return this;
		}
		
		public DataSeriesOptions marker(Marker marker) {
			this.marker = marker;
			return this;
		}

		public DataSeriesOptions markerSize(int markerSize) {
			this.markerSize = markerSize;
			return this;
		}
		
		public DataSeriesOptions markerColor(Color color) {
			this.markerColor = color;
			return this;
		}
		
		public DataSeriesOptions areaColor(Color color) {
			this.areaColor = color;
			return this;
		}
		
		public DataSeriesOptions xAxis(String name) {
			this.xAxisName = name;
			return this;
		}

		public DataSeriesOptions yAxis(String name) {
			this.yAxisName = name;
			return this;
		}

		private void setPlot(Plot plot) {
			if (plot != null)
				this.xAxis = plot.xAxes.get(xAxisName);			
			if (plot != null)
				this.yAxis = plot.yAxes.get(yAxisName);
		}
		
	}
	
	public static Data data() {
		return new Data();
	}
	
	public static class Data {
		
		private double[] x1;
		private double[] y1;
		private List<Double> x2;
		private List<Double> y2;
		
		private Data() {}
		
		public Data xy(double[] x, double[] y) {
			this.x1 = x;
			this.y1 = y;
			return this;
		}
		
		public Data xy(double x, double y) {
			if (this.x2 == null || this.y2 == null) {
				this.x2 = new ArrayList<Double>(10);
				this.y2 = new ArrayList<Double>(10);
			}
			x2.add(x);
			y2.add(y);
			return this;
		}
		
		public Data xy(List<Double> x, List<Double> y) {
			this.x2 = x;
			this.y2 = y;
			return this;
		}
		
		public int size() {
			if (x1 != null)
				return x1.length;
			if (x2 != null)
				return x2.size();
			return 0;
		}

		public double x(int i) {
			if (x1 != null)
				return x1[i];
			if (x2 != null)
				return x2.get(i);
			return 0;
		}

		public double y(int i) {
			if (y1 != null)
				return y1[i];
			if (y2 != null)
				return y2.get(i);
			return 0;
		}

	}
	
	public class DataSeries {

		private String name;
		private String nameWithAxes;
		private DataSeriesOptions opts = new DataSeriesOptions();
		private Data data;
		
		public DataSeries(String name, Data data, DataSeriesOptions opts) {
			if (opts != null)
				this.opts = opts;
			this.name = name;
			this.data = data;
			if (this.data == null)
				this.data = data();
		}
		
		public void clear() {
		}

		private void addAxesToName() {
			this.nameWithAxes = this.name + " (" + opts.yAxis.name +	"/" + opts.xAxis.name + ")";
		}
		
		private Range xRange() {
			Range range = new Range(0, 0);
			if (data != null && data.size() > 0) {
				range = new Range(data.x(0), data.x(0));
				for (int i = 1; i < data.size(); i++) {
					if (data.x(i) > range.max)
						range.setMax(data.x(i));
					if (data.x(i) < range.min)
						range.setMin(data.x(i));
				}
			}
			return range;
		}
		
		private Range yRange() {
			Range range = new Range(0, 0);
			if (data != null && data.size() > 0) {
				range = new Range(data.y(0), data.y(0));
				for (int i = 1; i < data.size(); i++) {
					if (data.y(i) > range.max)
						range.setMax(data.y(i));
					if (data.y(i) < range.min)
						range.setMin(data.y(i));
				}
			}
			return range;
		}
		
		private void draw(Graphics2D g) {
			g.setClip(plotArea.plotClipRect);
			if (data != null) {
				double x1 = 0, y1 = 0;
				int size = data.size();
				if (opts.line != Line.NONE)
					for (int j = 0; j < size; j++) {
						double x2 = x2x(data.x(j), opts.xAxis.opts.range, plotArea.xPlotRange);
						double y2 = y2y(data.y(j), opts.yAxis.opts.range, plotArea.yPlotRange);
						int ix1 = toInt(x1), iy1 = toInt(y1), ix2 = toInt(x2), iy2 = toInt(y2);
						int iy3 = plotArea.plotRect.y + plotArea.plotRect.height;
						// special case for the case when only the first point present
						if (size == 1) {
							ix1 = ix2;
							iy1 = iy2;
						}
						if (j != 0 || size == 1) {
							fillArea(g, ix1, iy1, ix2, iy2, iy3);
							drawLine(g, ix1, iy1, ix2, iy2);
						}
						x1 = x2;
						y1 = y2;
					}
				
				int halfMarkerSize = opts.markerSize / 2;
				int halfDiagMarkerSize = getDiagMarkerSize() / 2;
				g.setStroke(new BasicStroke(2));
				if (opts.marker != Marker.NONE)
					for (int j = 0; j < size; j++) {
						double x2 = x2x(data.x(j), opts.xAxis.opts.range, plotArea.xPlotRange);
						double y2 = y2y(data.y(j), opts.yAxis.opts.range, plotArea.yPlotRange);
						drawMarker(g, halfMarkerSize, halfDiagMarkerSize, x2, y2,
								plotArea.plotRect.x, plotArea.plotRect.y + plotArea.plotRect.height);
					}
			}
		}

		private int getDiagMarkerSize() {
			return (int) Math.round(Math.sqrt(2 * opts.markerSize * opts.markerSize));
		}
		
		private void fillArea(Graphics2D g, int ix1, int iy1, int ix2, int iy2, int iy3) {
			if (opts.areaColor != null) {
				g.setColor(opts.areaColor);
				g.fill(new Polygon(
						new int[] { ix1, ix2, ix2, ix1 },
						new int[] { iy1, iy2, iy3, iy3 },
						4));
				g.setColor(opts.seriesColor);
			}			
		}
		
		private void drawLine(Graphics2D g, int ix1, int iy1, int ix2, int iy2) {
			if (opts.line != Line.NONE) {
				g.setColor(opts.seriesColor);
				setStroke(g);
				g.drawLine(ix1, iy1, ix2, iy2);
			}
		}

		private void setStroke(Graphics2D g) {
			switch (opts.line) {
			case SOLID: 
				g.setStroke(new BasicStroke(opts.lineWidth)); 
				break;
			case DASHED: 
				g.setStroke(new BasicStroke(opts.lineWidth, BasicStroke.CAP_ROUND,
			        BasicStroke.JOIN_ROUND, 10.0f, opts.lineDash, 0.0f));
				break;
			default:
			}
		}
			
		private void drawMarker(Graphics2D g, int x2, int y2, int x3, int y3) {
			int halfMarkerSize = opts.markerSize / 2;
			int halfDiagMarkerSize =  getDiagMarkerSize() / 2;
			g.setStroke(new BasicStroke(2));
			drawMarker(g, halfMarkerSize, halfDiagMarkerSize, x2, y2, x3, y3);
		}
		
		private void drawMarker(Graphics2D g, int halfMarkerSize, int halfDiagMarkerSize, double x2, double y2, double x3, double y3) {
			switch (opts.marker) {
			case CIRCLE:
				g.setColor(opts.markerColor);
				g.fillOval(toInt(x2 - halfMarkerSize), toInt(y2 - halfMarkerSize), opts.markerSize, opts.markerSize);
				g.setColor(opts.seriesColor);
				g.drawOval(toInt(x2 - halfMarkerSize), toInt(y2 - halfMarkerSize), opts.markerSize, opts.markerSize);
				break;
			case SQUARE:
				g.setColor(opts.markerColor);
				g.fillRect(toInt(x2 - halfMarkerSize), toInt(y2 - halfMarkerSize), opts.markerSize, opts.markerSize);
				g.setColor(opts.seriesColor);
				g.drawRect(toInt(x2 - halfMarkerSize), toInt(y2 - halfMarkerSize), opts.markerSize, opts.markerSize);							
				break;
			case DIAMOND:
				int[] xpts = { toInt(x2), toInt(x2 + halfDiagMarkerSize), toInt(x2), toInt(x2 - halfDiagMarkerSize) };
				int[] ypts = { toInt(y2 - halfDiagMarkerSize), toInt(y2), toInt(y2 + halfDiagMarkerSize), toInt(y2) };
				g.setColor(opts.markerColor);
				g.fillPolygon(xpts, ypts, 4);
				g.setColor(opts.seriesColor);
				g.drawPolygon(xpts, ypts, 4);
				break;
			case COLUMN:
				g.setColor(opts.markerColor);
				g.fillRect(toInt(x2), toInt(y2), opts.markerSize, toInt(y3 - y2));
				g.setColor(opts.seriesColor);
				g.drawRect(toInt(x2), toInt(y2), opts.markerSize, toInt(y3 - y2));
				break;
			case BAR:
				g.setColor(opts.markerColor);
				g.fillRect(toInt(x3), toInt(y2), toInt(x2 - x3), opts.markerSize);
				g.setColor(opts.seriesColor);
				g.drawRect(toInt(x3), toInt(y2), toInt(x2 - x3), opts.markerSize);				
				break;
			default:
			} 
		}

	}
	
	private static void drawLabel(Graphics2D g, String s, int x, int y, HorizAlign hAlign, VertAlign vAlign) {
		FontMetrics fm = g.getFontMetrics();
		Rectangle2D rect = fm.getStringBounds(s, g);
		
		// by default align by left
		if (hAlign == HorizAlign.RIGHT)
			x -= rect.getWidth();
		else if (hAlign == HorizAlign.CENTER)
			x -= rect.getWidth() / 2;
			
		// by default align by bottom
		if (vAlign == VertAlign.TOP)
			y += rect.getHeight();
		else if (vAlign == VertAlign.CENTER)
			y += rect.getHeight() / 2;
		
		g.drawString(s, x, y);
	}
	
	public static String formatDouble(double d, AxisFormat format) {
		switch (format) {
		case TIME_HM: return String.format("%tR", new java.util.Date((long) d));
		case TIME_HMS: return String.format("%tT", new java.util.Date((long) d));
		case DATE: return String.format("%tF", new java.util.Date((long) d));
		case DATETIME_HM: return String.format("%tF %1$tR", new java.util.Date((long) d));
		case DATETIME_HMS: return String.format("%tF %1$tT", new java.util.Date((long) d));
		case NUMBER_KGM: return formatDoubleAsNumber(d, true);
		case NUMBER_INT: return Integer.toString((int) d); 
		default: return formatDoubleAsNumber(d, false);
		}
	}

	private static String formatDoubleAsNumber(double d, boolean useKGM) {
		if (useKGM && d > 1000 && d < 1000000000000l) {
			long[] numbers = new long[] { 1000l, 1000000l, 1000000000l };
			char[] suffix = new char[] { 'K', 'M', 'G' };
			
			int i = 0;
			double r = 0;
			for (long number : numbers) {
				r = d / number;
				if (r < 1000)
					break;
				i++;
			}
			if (i == suffix.length) 
				i--;
			return String.format("%1$,.2f%2$c", r, suffix[i]);
		}
		else
			return String.format("%1$.3G", d);
	}
	
	private static double x2x(double x, Range xr1, Range xr2) {
		return xr1.diff == 0 ? xr2.min + xr2.diff / 2 : xr2.min + (x - xr1.min) / xr1.diff * xr2.diff;
	}
	
	// y axis is reverse in Graphics
	private static double y2y(double x, Range xr1, Range xr2) {
		return xr1.diff == 0 ? xr2.min + xr2.diff / 2 : xr2.max - (x - xr1.min) / xr1.diff * xr2.diff;
	}

	private static int toInt(double d) {
		return (int) Math.round(d);
	}
	
}
//CHECKSTYLE:ON