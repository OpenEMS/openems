package io.openems.impl.protocol.studer.internal.object;

import io.openems.api.thing.Thing;
import io.openems.impl.protocol.studer.internal.property.ReadFloatProperty;

public class FloatUserinfoObject extends UserinfoObject<Float> {

	public FloatUserinfoObject(int objectId, String name, String unit, Thing parent) {
		super(objectId, name, unit, parent);
	}

	@Override
	protected ReadFloatProperty initValue() {
		return new ReadFloatProperty(name, PropertyId.VALUE, this, parent);
	}
}
