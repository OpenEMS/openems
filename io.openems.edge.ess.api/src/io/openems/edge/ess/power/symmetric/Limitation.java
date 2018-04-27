package io.openems.edge.ess.power.symmetric;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import io.openems.edge.ess.power.PowerException;

public abstract class Limitation {

	protected static final Coordinate ZERO = new Coordinate(0, 0);

	protected final SymmetricPower power;

	private final List<Runnable> onChangeCallbacks = new CopyOnWriteArrayList<>();

	public Limitation(SymmetricPower power) {
		this.power = power;
	}

	protected void emitOnChangeEvent() {
		for (Runnable callback : this.onChangeCallbacks) {
			callback.run();
		}
	}

	public Limitation onChange(Runnable callback) {
		this.onChangeCallbacks.add(callback);
		return this;
	}

	protected abstract Geometry applyLimit(Geometry geometry) throws PowerException;

	@Override
	public abstract String toString();
}
