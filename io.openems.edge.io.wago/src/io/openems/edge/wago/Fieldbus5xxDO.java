package io.openems.edge.wago;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;

public class Fieldbus5xxDO extends FieldbusModule {

	private static final String ID_TEMPLATE = "DIGITAL_OUTPUT_M";

	private final CoilElement[] inputCoil0Elements = {};
	private final CoilElement[] inputCoil512Elements;
	private final CoilElement[] outputCoil512Elements;
	private final BooleanReadChannel[] readChannels;

	public Fieldbus5xxDO(IoWagoImpl parent, int moduleCount, int coilOffset512, int channelsCount) {
		var id = ID_TEMPLATE + moduleCount;

		this.readChannels = new BooleanReadChannel[channelsCount];
		this.inputCoil512Elements = new CoilElement[channelsCount];
		this.outputCoil512Elements = new CoilElement[channelsCount];

		for (var i = 0; i < channelsCount; i++) {
			var doc = new BooleanDoc() //
					.accessMode(AccessMode.READ_WRITE);
			doc.persistencePriority(PersistencePriority.HIGH);
			var channelId = new FieldbusChannelId(id + "_C" + (i + 1), doc);
			var channel = (BooleanWriteChannel) parent.addChannel(channelId);

			this.readChannels[i] = channel;

			this.inputCoil512Elements[i] = parent.createModbusCoilElement(channel.channelId(), coilOffset512 + i);
			this.outputCoil512Elements[i] = parent.createModbusCoilElement(channel.channelId(), coilOffset512 + i);
		}
	}

	@Override
	public String getName() {
		return "WAGO I/O 750-5xx digital output module";
	}

	@Override
	public CoilElement[] getInputCoil0Elements() {
		return this.inputCoil0Elements;
	}

	@Override
	public CoilElement[] getInputCoil512Elements() {
		return this.inputCoil512Elements;
	}

	@Override
	public CoilElement[] getOutputCoil512Elements() {
		return this.outputCoil512Elements;
	}

	@Override
	public BooleanReadChannel[] getChannels() {
		return this.readChannels;
	}
}
