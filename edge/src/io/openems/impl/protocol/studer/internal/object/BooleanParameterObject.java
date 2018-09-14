package io.openems.impl.protocol.studer.internal.object;

import io.openems.api.thing.Thing;
import io.openems.impl.protocol.studer.internal.property.WriteBooleanProperty;

public class BooleanParameterObject extends ParameterObject<Boolean> {

	public BooleanParameterObject(int objectId, String name, String unit, Thing parent) {
		super(objectId, name, unit, parent);
	}

	@Override
	protected WriteBooleanProperty initValue() {
		return new WriteBooleanProperty(name, PropertyId.VALUE_QSP, this, parent);
	}

	@Override
	protected WriteBooleanProperty initUnsavedValue() {
		return new WriteBooleanProperty(name, PropertyId.UNSAVED_VALUE_QSP, this, parent);
	}
}
