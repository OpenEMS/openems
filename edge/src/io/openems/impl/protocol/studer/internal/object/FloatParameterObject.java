package io.openems.impl.protocol.studer.internal.object;

import io.openems.api.thing.Thing;
import io.openems.impl.protocol.studer.internal.property.WriteFloatProperty;

public class FloatParameterObject extends ParameterObject<Float> {

	public FloatParameterObject(int objectId, String name, String unit, Thing parent) {
		super(objectId, name, unit, parent);
	}

	@Override
	protected WriteFloatProperty initValue() {
		return new WriteFloatProperty(name, PropertyId.VALUE_QSP, this, parent);
	}

	@Override
	protected WriteFloatProperty initUnsavedValue() {
		return new WriteFloatProperty(name, PropertyId.UNSAVED_VALUE_QSP, this, parent);
	}
}
