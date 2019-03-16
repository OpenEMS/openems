package io.openems.edge.common.channel.internal;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Unit;

public abstract class OpenemsTypeDoc<T> extends AbstractDoc<T> {

	public static OpenemsTypeDoc<?> of(OpenemsType type) {
		switch (type) {
		case BOOLEAN:
			return new BooleanDoc();
		case DOUBLE:
			return new DoubleDoc();
		case FLOAT:
			return new FloatDoc();
		case INTEGER:
			return new IntegerDoc();
		case LONG:
			return new LongDoc();
		case SHORT:
			return new ShortDoc();
		case STRING:
			return new StringDoc();
		}
		throw new IllegalArgumentException("OpenemsType [" + type + "] is unhandled. This should never happen.");
	}

	protected OpenemsTypeDoc(OpenemsType type) {
		super(type);
	}

	/*
	 * Unit
	 */
	private Unit unit = Unit.NONE;

	/**
	 * Unit. Default: none
	 * 
	 * @param unit the Unit
	 * @return myself
	 */
	public OpenemsTypeDoc<T> unit(Unit unit) {
		this.unit = unit;
		return this;
	}

	/**
	 * Gets the Unit.
	 * 
	 * @return the unit
	 */
	@Override
	public Unit getUnit() {
		return this.unit;
	}

}
