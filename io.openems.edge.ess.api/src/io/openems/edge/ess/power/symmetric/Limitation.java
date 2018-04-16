package io.openems.edge.ess.power.symmetric;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import io.openems.edge.ess.power.PowerException;

public abstract class Limitation {

	protected static final Coordinate ZERO = new Coordinate(0, 0);

	protected SymmetricPower power;
	// TODO private List<LimitationChangedListener> listeners;

	public Limitation(SymmetricPower power) {
		this.power = power;
		// this.listeners = new ArrayList<>();
	}

	protected void notifyListeners() {
		// for (LimitationChangedListener listener : listeners) {
		// listener.onLimitationChange(this);
		// }
	}

	// public void addListener(LimitationChangedListener listener) {
	// this.listeners.add(listener);
	// }
	//
	// public void removeListener(LimitationChangedListener listener) {
	// this.listeners.remove(listener);
	// }

	protected abstract Geometry applyLimit(Geometry geometry) throws PowerException;

	@Override
	public abstract String toString();
}
