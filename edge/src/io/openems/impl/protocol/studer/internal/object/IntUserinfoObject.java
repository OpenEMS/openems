package io.openems.impl.protocol.studer.internal.object;

import io.openems.api.thing.Thing;
import io.openems.impl.protocol.studer.internal.property.ReadIntProperty;

public class IntUserinfoObject extends UserinfoObject<Integer> {

	public IntUserinfoObject(int objectId, String name, String unit, Thing parent) {
		super(objectId, name, unit, parent);
	}

	@Override
	protected ReadIntProperty initValue() {
		return new ReadIntProperty(name, PropertyId.VALUE_QSP, this, parent);
	}
}
