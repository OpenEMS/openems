package io.openems.core.utilities.power.symmetric;

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public abstract class Limitation {

	protected static final Coordinate ZERO = new Coordinate(0, 0);

	protected SymmetricPower power;
	private List<LimitationChangedListener> listeners;

	public Limitation(SymmetricPower power) {
		this.power = power;
		this.listeners = new ArrayList<>();
	}

	protected void notifyListeners() {
		for(LimitationChangedListener listener: listeners) {
			listener.onLimitationChange(this);
		}
	}

	public void addListener(LimitationChangedListener listener) {
		this.listeners.add(listener);
	}

	public void removeListener(LimitationChangedListener listener) {
		this.listeners.remove(listener);
	}

	protected abstract Geometry applyLimit(Geometry geometry) throws PowerException;

	@Override
	public abstract String toString();
}
