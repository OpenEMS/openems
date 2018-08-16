package io.openems.impl.protocol.studer.internal.object;

import io.openems.api.thing.Thing;
import io.openems.impl.protocol.studer.internal.property.ReadProperty;
import io.openems.impl.protocol.studer.internal.property.StuderProperty;

public abstract class UserinfoObject<T> extends StuderObject<T> {

	protected final ReadProperty<T> value;

	public UserinfoObject(int objectId, String name, String unit, Thing parent) {
		super(objectId, name, unit, parent, ObjectType.USERINFO);
		this.value = this.initValue();
	}

	@Override
	public StuderProperty<T>[] getProperties() {
		@SuppressWarnings("unchecked") StuderProperty<T>[] properties = new StuderProperty[] { value };
		return properties;
	}

	protected abstract ReadProperty<T> initValue();

	public ReadProperty<T> value() {
		return this.value;
	}
}
