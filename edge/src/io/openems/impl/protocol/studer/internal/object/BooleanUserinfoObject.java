package io.openems.impl.protocol.studer.internal.object;

import io.openems.api.thing.Thing;
import io.openems.impl.protocol.studer.internal.property.ReadBooleanProperty;

public class BooleanUserinfoObject extends UserinfoObject<Boolean> {

	public BooleanUserinfoObject(int objectId, String name, String unit, Thing parent) {
		super(objectId, name, unit, parent);
	}

	@Override
	protected ReadBooleanProperty initValue() {
		return new ReadBooleanProperty(name, PropertyId.VALUE_QSP, this, parent);
	}
}
