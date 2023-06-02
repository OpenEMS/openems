package io.openems.edge.wago;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.edge.bridge.modbus.api.element.ModbusCoilElement;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;

public class Fieldbus5xxDO extends FieldbusModule {

	private static final String ID_TEMPLATE = "DIGITAL_OUTPUT_M";

	private final ModbusCoilElement[] inputCoil0Elements = {};
	private final ModbusCoilElement[] inputCoil512Elements;
	private final ModbusCoilElement[] outputCoil512Elements;
	private final BooleanReadChannel[] readChannels;

	public Fieldbus5xxDO(WagoImpl parent, int moduleCount, int coilOffset512, int channelsCount) {
		var id = ID_TEMPLATE + moduleCount;

		this.readChannels = new BooleanReadChannel[channelsCount];
		this.inputCoil512Elements = new ModbusCoilElement[channelsCount];
		this.outputCoil512Elements = new ModbusCoilElement[channelsCount];

		for (var i = 0; i < channelsCount; i++) {
			var doc = new BooleanDoc() //
					.accessMode(AccessMode.READ_WRITE);
			doc.persistencePriority(PersistencePriority.MEDIUM);
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
	public ModbusCoilElement[] getInputCoil0Elements() {
		return this.inputCoil0Elements;
	}

	@Override
	public ModbusCoilElement[] getInputCoil512Elements() {
		return this.inputCoil512Elements;
	}

	@Override
	public ModbusCoilElement[] getOutputCoil512Elements() {
		return this.outputCoil512Elements;
	}

	@Override
	public BooleanReadChannel[] getChannels() {
		return this.readChannels;
	}
}
