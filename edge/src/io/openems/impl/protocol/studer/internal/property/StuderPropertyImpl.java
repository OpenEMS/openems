package io.openems.impl.protocol.studer.internal.property;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.thing.Thing;
import io.openems.impl.protocol.studer.StuderChannel;
import io.openems.impl.protocol.studer.internal.object.PropertyId;
import io.openems.impl.protocol.studer.internal.object.StuderObject;

public abstract class StuderPropertyImpl<T> {

	protected final Logger log;

	protected final StuderObject<T> object;
	protected final PropertyId propertyId;

	protected final StuderChannel<T> channel;

	public StuderPropertyImpl(String objectName, PropertyId propertyId, StuderObject<T> object, Thing parent) {
		this.log = LoggerFactory.getLogger(this.getClass());
		this.propertyId = propertyId;
		this.object = object;
		String channelId = objectName + propertyId.getName();
		this.channel = initChannel(channelId, parent);
	}

	protected abstract StuderChannel<T> initChannel(String id, Thing parent);

	public StuderChannel<T> channel() {
		return this.channel;
	};
}
