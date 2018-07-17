package io.openems.impl.protocol.studer.internal.object;

import io.openems.api.thing.Thing;
import io.openems.impl.protocol.studer.internal.property.WriteIntProperty;

public class IntParameterObject extends ParameterObject<Integer> {

	public IntParameterObject(int objectId, String name, String unit, Thing parent) {
		super(objectId, name, unit, parent);
	}

	@Override
	protected WriteIntProperty initValue() {
		return new WriteIntProperty(name, PropertyId.VALUE_QSP, this, parent);
	}

	@Override
	protected WriteIntProperty initUnsavedValue() {
		return new WriteIntProperty(name, PropertyId.UNSAVED_VALUE_QSP, this, parent);
	}
}
