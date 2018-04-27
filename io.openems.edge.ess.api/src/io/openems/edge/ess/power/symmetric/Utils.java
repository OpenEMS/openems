package io.openems.edge.ess.power.symmetric;

import java.awt.Color;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import io.openems.edge.ess.power.SVGWriter;

public class Utils {

	protected final static GeometryFactory FACTORY = new GeometryFactory();

	protected static final SVGWriter writer = new SVGWriter();
	protected static final Color[] COLORS = new Color[] { Color.GREEN, Color.BLUE, Color.MAGENTA, Color.YELLOW,
			Color.ORANGE, Color.RED };
	protected static final Coordinate ZERO = new Coordinate(0, 0);

	/**
	 * Creates a Rect with the coordinates pMin, pMax, qMin, qMax and intersects the
	 * rect with the base geometry.
	 *
	 * @param base
	 * @param pMin
	 * @param pMax
	 * @param qMin
	 * @param qMax
	 * @return resulting polygon after the intersection
	 */
	public static Geometry intersectRect(Geometry base, double pMin, double pMax, double qMin, double qMax) {
		Coordinate[] coordinates = new Coordinate[] { new Coordinate(pMin, qMax), new Coordinate(pMin, qMin),
				new Coordinate(pMax, qMin), new Coordinate(pMax, qMax), new Coordinate(pMin, qMax) };
		Geometry rect = new GeometryFactory().createPolygon(coordinates);
		return base.intersection(rect);
	}

	public static String getAsSVG(long viewBoxMinX, long viewBoxMinY, long viewBoxMaxX, long viewBoxMaxY,
			Geometry... geometries) {
		String viewBox = viewBoxMinX + " " + viewBoxMinY + " " + viewBoxMaxX * 2 + " " + viewBoxMaxY * 2;

		StringBuilder b = new StringBuilder();
		b.append("<?xml version='1.0' standalone='no'?>\n");
		b.append(
				"<!DOCTYPE svg PUBLIC '-//W3C//DTD SVG 1.1//EN' 'http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd'>\n");
		b.append("<svg width='400' height='400' transform='scale(1,-1)' viewBox='" + viewBox
				+ "'  version='1.1' xmlns='http://www.w3.org/2000/svg' xmlns:xlink='http://www.w3.org/1999/xlink'>\n");
		int i = 0;
		for (Geometry geo : geometries) {
			String color = "#" + Integer.toHexString(Utils.COLORS[i].getRGB()).substring(2);
			String a = Utils.writeGeometryStyled(geo, color, color);
			b.append(a + "\n");
			b.append("\n");
			i++;
			i %= Utils.COLORS.length;
		}
		b.append("</svg>\n");
		return b.toString();
	}

	public static String getAsSVG(Geometry geometry) {
		Envelope env = geometry.getEnvelopeInternal();
		return Utils.getAsSVG(Math.round(env.getMinX()), Math.round(env.getMinY()), Math.round(env.getMaxX()),
				Math.round(env.getMaxY()), geometry);
	}

	protected static String writeGeometryStyled(Geometry g, String fillClr, String strokeClr) {
		String s = "<g fill='" + fillClr + "' stroke='" + strokeClr + "' >\n";
		s += write(g);
		s += "</g>";
		return s;
	}

	private static String write(Geometry geometry) {
		if (geometry == null) {
			return "";
		}
		return Utils.writer.write(geometry);
	}
}
