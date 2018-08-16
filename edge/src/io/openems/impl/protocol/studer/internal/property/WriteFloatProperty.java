package io.openems.impl.protocol.studer.internal.property;

import io.openems.api.thing.Thing;
import io.openems.impl.protocol.studer.StuderChannel;
import io.openems.impl.protocol.studer.internal.object.PropertyId;
import io.openems.impl.protocol.studer.internal.object.StuderObject;
import io.openems.impl.protocol.studer.internal.request.ReadFloatRequest;
import io.openems.impl.protocol.studer.internal.request.ReadRequest;
import io.openems.impl.protocol.studer.internal.request.WriteFloatRequest;
import io.openems.impl.protocol.studer.internal.request.WriteRequest;

public class WriteFloatProperty extends StuderPropertyImpl<Float> implements WriteProperty<Float> {

	public WriteFloatProperty(String objectName, PropertyId propertyId, StuderObject<Float> object, Thing parent) {
		super(objectName, propertyId, object, parent);
	}

	@Override
	protected StuderChannel<Float> initChannel(String id, Thing parent) {
		return new StuderWriteChannel<>(id, parent);
	}

	@Override
	public StuderWriteChannel<Float> channel() {
		return (StuderWriteChannel<Float>) super.channel();
	}

	@Override
	public ReadRequest<Float> readRequest(int srcAddress, int dstAddress) {
		return new ReadFloatRequest(srcAddress, dstAddress, this.object.getObjectType(), this.propertyId,
				this.object.getObjectId());
	}

	@Override
	public WriteRequest<Float> writeRequest(int srcAddress, int dstAddress, Float value) {
		return new WriteFloatRequest(srcAddress, dstAddress, this.object.getObjectType(), this.propertyId,
				this.object.getObjectId(), value);
	}
}
