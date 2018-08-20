package io.openems.impl.protocol.studer.internal.property;

import io.openems.api.thing.Thing;
import io.openems.impl.protocol.studer.StuderChannel;
import io.openems.impl.protocol.studer.internal.object.PropertyId;
import io.openems.impl.protocol.studer.internal.object.StuderObject;
import io.openems.impl.protocol.studer.internal.request.ReadIntRequest;
import io.openems.impl.protocol.studer.internal.request.ReadRequest;
import io.openems.impl.protocol.studer.internal.request.WriteIntRequest;
import io.openems.impl.protocol.studer.internal.request.WriteRequest;

public class WriteIntProperty extends StuderPropertyImpl<Integer> implements WriteProperty<Integer> {

	public WriteIntProperty(String objectName, PropertyId propertyId, StuderObject<Integer> object, Thing parent) {
		super(objectName, propertyId, object, parent);
	}

	@Override
	protected StuderChannel<Integer> initChannel(String id, Thing parent) {
		return new StuderWriteChannel<>(id, parent);
	}

	@Override
	public StuderWriteChannel<Integer> channel() {
		return (StuderWriteChannel<Integer>) super.channel();
	}

	@Override
	public ReadRequest<Integer> readRequest(int srcAddress, int dstAddress) {
		return new ReadIntRequest(srcAddress, dstAddress, this.object.getObjectType(), this.propertyId,
				this.object.getObjectId());
	}

	@Override
	public WriteRequest<Integer> writeRequest(int srcAddress, int dstAddress, Integer value) {
		return new WriteIntRequest(srcAddress, dstAddress, this.object.getObjectType(), this.propertyId,
				this.object.getObjectId(), value);
	}
}
