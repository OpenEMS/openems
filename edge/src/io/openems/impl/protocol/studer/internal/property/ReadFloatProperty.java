package io.openems.impl.protocol.studer.internal.property;

import io.openems.api.thing.Thing;
import io.openems.impl.protocol.studer.StuderChannel;
import io.openems.impl.protocol.studer.internal.object.PropertyId;
import io.openems.impl.protocol.studer.internal.object.StuderObject;
import io.openems.impl.protocol.studer.internal.request.ReadFloatRequest;
import io.openems.impl.protocol.studer.internal.request.ReadRequest;

public class ReadFloatProperty extends StuderPropertyImpl<Float> implements ReadProperty<Float> {

	public ReadFloatProperty(String objectName, PropertyId propertyId, StuderObject<Float> object, Thing parent) {
		super(objectName, propertyId, object, parent);
	}

	@Override
	protected StuderChannel<Float> initChannel(String id, Thing parent) {
		return new StuderReadChannel<>(id, parent);
	}

	@Override
	public StuderReadChannel<Float> channel() {
		return (StuderReadChannel<Float>) super.channel();
	}

	@Override
	public ReadRequest<Float> readRequest(int srcAddress, int dstAddress) {
		return new ReadFloatRequest(srcAddress, dstAddress, this.object.getObjectType(), this.propertyId,
				this.object.getObjectId());
	}
}
