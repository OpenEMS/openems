package io.openems.impl.protocol.studer.internal.property;

import io.openems.api.thing.Thing;
import io.openems.impl.protocol.studer.StuderChannel;
import io.openems.impl.protocol.studer.internal.object.PropertyId;
import io.openems.impl.protocol.studer.internal.object.StuderObject;
import io.openems.impl.protocol.studer.internal.request.ReadBooleanRequest;
import io.openems.impl.protocol.studer.internal.request.ReadRequest;

public class ReadBooleanProperty extends StuderPropertyImpl<Boolean> implements ReadProperty<Boolean> {

	public ReadBooleanProperty(String objectName, PropertyId propertyId, StuderObject<Boolean> object, Thing parent) {
		super(objectName, propertyId, object, parent);
	}

	@Override
	protected StuderChannel<Boolean> initChannel(String id, Thing parent) {
		return new StuderReadChannel<>(id, parent);
	}

	@Override
	public StuderReadChannel<Boolean> channel() {
		return (StuderReadChannel<Boolean>) super.channel();
	}

	@Override
	public ReadRequest<Boolean> readRequest(int srcAddress, int dstAddress) {
		return new ReadBooleanRequest(srcAddress, dstAddress, this.object.getObjectType(), this.propertyId,
				this.object.getObjectId());
	}
}
