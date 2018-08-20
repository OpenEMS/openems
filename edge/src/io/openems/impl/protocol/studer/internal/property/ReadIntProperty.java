package io.openems.impl.protocol.studer.internal.property;

import io.openems.api.thing.Thing;
import io.openems.impl.protocol.studer.StuderChannel;
import io.openems.impl.protocol.studer.internal.object.PropertyId;
import io.openems.impl.protocol.studer.internal.object.StuderObject;
import io.openems.impl.protocol.studer.internal.request.ReadIntRequest;
import io.openems.impl.protocol.studer.internal.request.ReadRequest;

public class ReadIntProperty extends StuderPropertyImpl<Integer> implements ReadProperty<Integer> {

	public ReadIntProperty(String objectName, PropertyId propertyId, StuderObject<Integer> object, Thing parent) {
		super(objectName, propertyId, object, parent);
	}

	@Override
	protected StuderChannel<Integer> initChannel(String id, Thing parent) {
		return new StuderReadChannel<>(id, parent);
	}

	@Override
	public StuderReadChannel<Integer> channel() {
		return (StuderReadChannel<Integer>) super.channel();
	}

	@Override
	public ReadRequest<Integer> readRequest(int srcAddress, int dstAddress) {
		return new ReadIntRequest(srcAddress, dstAddress, this.object.getObjectType(), this.propertyId,
				this.object.getObjectId());
	}
}
