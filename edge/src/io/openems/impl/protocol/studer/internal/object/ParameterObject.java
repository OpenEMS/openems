package io.openems.impl.protocol.studer.internal.object;

import io.openems.api.thing.Thing;
import io.openems.impl.protocol.studer.internal.property.StuderProperty;
import io.openems.impl.protocol.studer.internal.property.WriteProperty;

public abstract class ParameterObject<T> extends StuderObject<T> {

	protected final WriteProperty<T> value;
	protected final WriteProperty<T> unsavedValue;

	public ParameterObject(int objectId, String name, String unit, Thing parent) {
		super(objectId, name, unit, parent, ObjectType.PARAMETER);
		this.value = this.initValue();
		this.unsavedValue = this.initUnsavedValue();
	}

	@Override
	public StuderProperty<T>[] getProperties() {
		@SuppressWarnings("unchecked") StuderProperty<T>[] properties = new StuderProperty[] { value, unsavedValue };
		return properties;
	}

	protected abstract WriteProperty<T> initValue();

	public WriteProperty<T> value() {
		return this.value;
	}

	protected abstract WriteProperty<T> initUnsavedValue();

	public WriteProperty<T> unsavedValue() {
		return this.unsavedValue;
	}
}
