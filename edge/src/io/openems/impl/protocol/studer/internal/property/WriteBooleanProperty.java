package io.openems.impl.protocol.studer.internal.property;

import io.openems.api.thing.Thing;
import io.openems.impl.protocol.studer.StuderChannel;
import io.openems.impl.protocol.studer.internal.object.PropertyId;
import io.openems.impl.protocol.studer.internal.object.StuderObject;
import io.openems.impl.protocol.studer.internal.request.ReadBooleanRequest;
import io.openems.impl.protocol.studer.internal.request.ReadRequest;
import io.openems.impl.protocol.studer.internal.request.WriteBooleanRequest;
import io.openems.impl.protocol.studer.internal.request.WriteRequest;

public class WriteBooleanProperty extends StuderPropertyImpl<Boolean> implements WriteProperty<Boolean> {

	public WriteBooleanProperty(String objectName, PropertyId propertyId, StuderObject<Boolean> object, Thing parent) {
		super(objectName, propertyId, object, parent);
	}

	@Override
	protected StuderChannel<Boolean> initChannel(String id, Thing parent) {
		return new StuderWriteChannel<>(id, parent);
	}

	@Override
	public StuderWriteChannel<Boolean> channel() {
		return (StuderWriteChannel<Boolean>) super.channel();
	}

	@Override
	public ReadRequest<Boolean> readRequest(int srcAddress, int dstAddress) {
		return new ReadBooleanRequest(srcAddress, dstAddress, this.object.getObjectType(), this.propertyId,
				this.object.getObjectId());
	}

	@Override
	public WriteRequest<Boolean> writeRequest(int srcAddress, int dstAddress, Boolean value) {
		return new WriteBooleanRequest(srcAddress, dstAddress, this.object.getObjectType(), this.propertyId,
				this.object.getObjectId(), value);
	}
}
