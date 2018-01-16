package io.openems.core.utilities.power;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.operation.distance.DistanceOp;
import com.vividsolutions.jts.operation.distance.GeometryLocation;
import com.vividsolutions.jts.util.GeometricShapeFactory;

import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.core.utilities.ControllerUtils;

public class SymmetricPower {

	private final long maxApparentPower;
	private final GeometryFactory factory;
	private final Point zero;
	private Geometry geometry;
	private final ShapeWriter sw;
	private final List<Geometry> geometries;
	private final SVGWriter writer;
	private static final Color[] colors = new Color[] { Color.GREEN, Color.BLUE, Color.MAGENTA, Color.YELLOW,
			Color.ORANGE, Color.RED };
	private ReadChannel<Long> currentActivePower;
	private ReadChannel<Long> currentReactivePower;
	private ReadChannel<Long> allowedApparent;
	private ReadChannel<Long> allowedCharge;
	private ReadChannel<Long> allowedDischarge;
	private WriteChannel<Long> setActivePower;
	private WriteChannel<Long> setReactivePower;

	public SymmetricPower(long maxApparentPower, ReadChannel<Long> currentActivePower,
			ReadChannel<Long> currentReactivePower, ReadChannel<Long> allowedApparent, ReadChannel<Long> allowedCharge,
			ReadChannel<Long> allowedDischarge, WriteChannel<Long> setActivePower,
			WriteChannel<Long> setReactivePower) throws PowerException {
		this.maxApparentPower = Math.abs(maxApparentPower);
		this.currentActivePower = currentActivePower;
		this.currentReactivePower = currentReactivePower;
		this.allowedApparent = allowedApparent;
		this.allowedCharge = allowedCharge;
		this.allowedDischarge = allowedDischarge;
		this.setActivePower = setActivePower;
		this.setReactivePower = setReactivePower;
		this.factory = new GeometryFactory();
		this.zero = factory.createPoint(new Coordinate(0, 0));
		this.sw = new ShapeWriter();
		this.writer = new SVGWriter();
		this.geometries = new ArrayList<>();
		reset();
	}

	public Optional<Long> getCurrentP() {
		return this.currentActivePower.valueOptional();
	}

	public Optional<Long> getCurrentQ() {
		return this.currentReactivePower.valueOptional();
	}

	public Optional<Long> getCurrentS() {
		if (getCurrentP().isPresent() && getCurrentQ().isPresent()) {
			return Optional.of(ControllerUtils.calculateApparentPower(getCurrentP().get(), getCurrentQ().get()));
		} else {
			return Optional.empty();
		}
	}

	private void setGeometry(Geometry g) {
		this.geometry = g;
		this.geometries.add(g);
	}

	public void reset() throws PowerException {
		this.geometries.clear();
		GeometricShapeFactory shape = new GeometricShapeFactory(factory);
		shape.setCentre(new Coordinate(0, 0));
		shape.setSize(this.maxApparentPower * 2);
		shape.setNumPoints(32);
		setGeometry(shape.createCircle());
		setSMax(this.allowedApparent.valueOptional().orElse(0L));
		setPGreaterOrEqual(this.allowedCharge.valueOptional().orElse(0L));
		setPSmallerOrEqual(this.allowedDischarge.valueOptional().orElse(0L));
	}

	public void setSMax(long smax) throws PowerException {
		GeometricShapeFactory shape = new GeometricShapeFactory(factory);
		shape.setCentre(new Coordinate(0, 0));
		shape.setSize(this.maxApparentPower);
		shape.setNumPoints(32);
		Geometry newGeometry = geometry.intersection(shape.createCircle());
		if (newGeometry.isEmpty()) {
			// TODO throw exception
			throw new PowerException("");
		}
		setGeometry(newGeometry);
	}

	public void setPGreaterOrEqual(long p) throws PowerException {
		Geometry newGeometry = intersectRect(this.geometry, p - 0.1, maxApparentPower, maxApparentPower * -1,
				maxApparentPower);
		if (newGeometry.isEmpty()) {
			throw new PowerException("");
		}
		setGeometry(newGeometry);
	}

	public void setPSmallerOrEqual(long p) throws PowerException {
		Geometry newGeometry = intersectRect(this.geometry, maxApparentPower * -1, p + 0.1, maxApparentPower * -1,
				maxApparentPower);
		if (newGeometry.isEmpty()) {
			throw new PowerException("");
		}
		setGeometry(newGeometry);
	}

	public void setQGreaterOrEqual(long q) throws PowerException {
		Geometry newGeometry = intersectRect(this.geometry, maxApparentPower * -1, maxApparentPower, q - 0.1,
				maxApparentPower);
		if (newGeometry.isEmpty()) {
			throw new PowerException("");
		}
		setGeometry(newGeometry);
	}

	public void setQSmallerOrEqual(long q) throws PowerException {
		Geometry newGeometry = intersectRect(this.geometry, maxApparentPower * -1, maxApparentPower,
				maxApparentPower * -1, q + 0.1);
		if (newGeometry.isEmpty()) {
			throw new PowerException("");
		}
		setGeometry(newGeometry);
	}

	public void setNoQBetween(long qmin, long qmax) throws PowerException {
		Coordinate[] coordinates = new Coordinate[] { new Coordinate(maxApparentPower, qmin),
				new Coordinate(maxApparentPower * -1, qmin), new Coordinate(maxApparentPower * -1, qmax),
				new Coordinate(maxApparentPower, qmax), new Coordinate(maxApparentPower, qmin) };
		Geometry rect = this.factory.createPolygon(coordinates);
		Geometry newGeometry = this.geometry.difference(rect);
		if (newGeometry.isEmpty()) {
			// TODO throw exception
			throw new PowerException("");
		}
		setGeometry(newGeometry);
	}

	/**
	 *
	 * @param pmin
	 *            is exclusive
	 * @param pmax
	 *            is exclusive
	 * @throws PowerException
	 */
	public void setNoPBetween(long pmin, long pmax) throws PowerException {
		Coordinate[] coordinates = new Coordinate[] { new Coordinate(pmin, maxApparentPower),
				new Coordinate(pmin, maxApparentPower * -1), new Coordinate(pmax, maxApparentPower * -1),
				new Coordinate(pmax, maxApparentPower), new Coordinate(pmin, maxApparentPower) };
		Geometry rect = this.factory.createPolygon(coordinates);
		Geometry newGeometry = this.geometry.difference(rect);
		if (newGeometry.isEmpty()) {
			// TODO throw exception
			throw new PowerException("");
		}
		setGeometry(newGeometry);
	}

	public void setMaxCosPhi(double cosPhi) throws PowerException {
		double m = Math.tan(Math.acos(cosPhi));
		double y = m * maxApparentPower;
		Coordinate[] coordinates = new Coordinate[] { new Coordinate(0, 0), new Coordinate(maxApparentPower, y),
				new Coordinate(maxApparentPower, y * -1), new Coordinate(0, 0),
				new Coordinate(maxApparentPower * -1, y * -1), new Coordinate(maxApparentPower * -1, y),
				new Coordinate(0, 0) };
		Geometry polygon = this.factory.createPolygon(coordinates);
		Geometry newGeometry = this.geometry.intersection(polygon);
		if (newGeometry.isEmpty()) {
			// TODO throw exception
			throw new PowerException("");
		}
		setGeometry(newGeometry);
	}

	private Geometry intersectRect(Geometry base, double pMin, double pMax, double qMin, double qMax) {
		Coordinate[] coordinates = new Coordinate[] { new Coordinate(pMin, qMax), new Coordinate(pMin, qMin),
				new Coordinate(pMax, qMin), new Coordinate(pMax, qMax), new Coordinate(pMin, qMax) };
		Geometry rect = this.factory.createPolygon(coordinates);
		return this.geometry.intersection(rect);
	}

	public void setP(long p) {
		Coordinate[] coordinates = new Coordinate[] { new Coordinate(p, maxApparentPower),
				new Coordinate(p, maxApparentPower * -1) };
		LineString line = this.factory.createLineString(coordinates);
		Geometry newGeometry = this.geometry.intersection(line);
		if (newGeometry.isEmpty()) {
			Geometry smallerP = intersectRect(this.geometry, 0, p, maxApparentPower * -1, maxApparentPower);
			if (!smallerP.isEmpty()) {
				DistanceOp distance = new DistanceOp(smallerP, line);
				GeometryLocation[] locations = distance.nearestLocations();
				long maxP = 0;
				for (GeometryLocation location : locations) {
					if (!location.getGeometryComponent().equals(line)) {
						maxP = (long) location.getCoordinate().x;
						break;
					}
				}
				coordinates = new Coordinate[] { new Coordinate(maxP, maxApparentPower),
						new Coordinate(maxP, maxApparentPower * -1) };
				line = this.factory.createLineString(coordinates);
				setGeometry(this.geometry.intersection(line));
			} else {
				DistanceOp distance = new DistanceOp(geometry, line);
				GeometryLocation[] locations = distance.nearestLocations();
				for (GeometryLocation location : locations) {
					if (!location.getGeometryComponent().equals(line)) {
						coordinates = new Coordinate[] { new Coordinate(location.getCoordinate().x, maxApparentPower),
								new Coordinate(location.getCoordinate().x, maxApparentPower * -1) };
						line = this.factory.createLineString(coordinates);
						setGeometry(this.geometry.intersection(line));
						break;
					}
				}
			}
		} else {
			setGeometry(newGeometry);
		}
	}

	public void setQ(long q) {
		Coordinate[] coordinates = new Coordinate[] { new Coordinate(maxApparentPower, q),
				new Coordinate(maxApparentPower * -1, q) };
		LineString line = this.factory.createLineString(coordinates);
		Geometry newGeometry = this.geometry.intersection(line);
		if (newGeometry.isEmpty()) {
			Geometry smallerQ = intersectRect(this.geometry, maxApparentPower * -1, maxApparentPower, 0, q);
			if (!smallerQ.isEmpty()) {
				DistanceOp distance = new DistanceOp(smallerQ, line);
				GeometryLocation[] locations = distance.nearestLocations();
				long maxQ = 0;
				for (GeometryLocation location : locations) {
					if (!location.getGeometryComponent().equals(line)) {
						maxQ = (long) location.getCoordinate().y;
						break;
					}
				}
				coordinates = new Coordinate[] { new Coordinate(maxApparentPower, maxQ),
						new Coordinate(maxApparentPower * -1, maxQ) };
				line = this.factory.createLineString(coordinates);
				setGeometry(this.geometry.intersection(line));
			} else {
				DistanceOp distance = new DistanceOp(geometry, line);
				GeometryLocation[] locations = distance.nearestLocations();
				for (GeometryLocation location : locations) {
					if (!location.getGeometryComponent().equals(line)) {
						coordinates = new Coordinate[] { new Coordinate(maxApparentPower, location.getCoordinate().y),
								new Coordinate(maxApparentPower * -1, location.getCoordinate().y) };
						line = this.factory.createLineString(coordinates);
						setGeometry(this.geometry.intersection(line));
						break;
					}
				}
			}
		} else {
			setGeometry(newGeometry);
		}
	}

	public void setPQ(double m, long t) {
		double y1 = m * (maxApparentPower * -1) + t;
		double y2 = m * maxApparentPower + t;
		Coordinate[] coordinates = new Coordinate[] { new Coordinate(maxApparentPower * -1, y1),
				new Coordinate(maxApparentPower, y2) };
		LineString line = this.factory.createLineString(coordinates);
		Geometry newGeometry = this.geometry.intersection(line);
		if (newGeometry.isEmpty()) {
			DistanceOp distance = new DistanceOp(geometry, line);
			GeometryLocation[] locations = distance.nearestLocations();
			for (GeometryLocation location : locations) {
				if (!location.getGeometryComponent().equals(line)) {
					setGeometry(this.factory.createPoint(location.getCoordinate()));
					break;
				}
			}
		} else {
			setGeometry(newGeometry);
		}
	}

	public void printPower() {
		JFrame frame = new JFrame("asdf");
		frame.setVisible(true);
		frame.setSize(600, 400);
		JPanel panel = new JPanel() {
			@Override
			protected void paintComponent(java.awt.Graphics g) {
				super.paintComponent(g);
				Graphics2D g2 = (Graphics2D) g;
				g2.translate((int) maxApparentPower, (int) maxApparentPower);
				int i = 0;
				for (Geometry geo : geometries) {
					g2.draw(sw.toShape(geo));
					i++;
					i %= colors.length;
					g2.setColor(colors[i]);
				}
				g2.translate((int) maxApparentPower * -1, (int) maxApparentPower * -1);
			};
		};
		panel.setLayout(null);
		frame.add(panel);
		frame.validate();
		frame.repaint();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	public long getP() {
		return (long) this.reduceToZero().getCoordinate().y;
	}

	public long getQ() {
		return (long) this.reduceToZero().getCoordinate().y;
	}

	private Point reduceToZero() {
		if (this.geometry instanceof Point) {
			return (Point) this.geometry;
		}
		DistanceOp distance = new DistanceOp(geometry, this.zero);
		GeometryLocation[] locations = distance.nearestLocations();
		Point result = this.zero;
		for (GeometryLocation location : locations) {
			Geometry g = location.getGeometryComponent();
			if (!g.equals(this.zero) && g instanceof Point) {
				result = (Point) location.getGeometryComponent();
				break;
			}
		}
		setGeometry(result);
		return result;
	}

	public String getAsSVG() {
		StringBuffer text = new StringBuffer();

		String viewBox = this.maxApparentPower * -1 + " " + this.maxApparentPower * -1 + " " + this.maxApparentPower * 2
				+ " " + this.maxApparentPower * 2;

		text.append("<?xml version='1.0' standalone='no'?>\n");
		text.append(
				"<!DOCTYPE svg PUBLIC '-//W3C//DTD SVG 1.1//EN' 'http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd'>\n");
		text.append("<svg width='400' height='400' viewBox='" + viewBox
				+ "'  version='1.1' xmlns='http://www.w3.org/2000/svg' xmlns:xlink='http://www.w3.org/1999/xlink'>\n");
		// String name = testable.getName() == null ? "" : testable.getName();
		// String description = testable.getDescription() == null ? "" : testable.getDescription();
		// text.append(" \"" + name + "\",\n");
		// text.append(" <desc>" + description + "</desc>\n");
		int i = 0;
		for (Geometry geo : geometries) {
			String a = writeGeometryStyled(geo, "#" + Integer.toHexString(colors[i].getRGB()).substring(2));
			text.append(a + "\n");
			text.append("\n");
			i++;
			i %= colors.length;
		}
		text.append("</svg>\n");
		return text.toString();
	}

	private String writeGeometryStyled(Geometry g, String fillClr) {
		String s = "<g fill='" + fillClr + "' >\n";
		s += write(g);
		s += "</g>";
		return s;
	}

	private String write(Geometry geometry) {
		if (geometry == null) {
			return "";
		}
		return writer.write(geometry);
	}

}
