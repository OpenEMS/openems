package io.openems.edge.wago;

import io.openems.common.channel.PersistencePriority;
import io.openems.edge.bridge.modbus.api.element.ModbusCoilElement;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.BooleanReadChannel;

public class Fieldbus4xxDI extends FieldbusModule {

	private static final String ID_TEMPLATE = "DIGITAL_INPUT_M";

	private final ModbusCoilElement[] inputCoil0Elements;
	private final ModbusCoilElement[] inputCoil512Elements = {};
	private final ModbusCoilElement[] outputCoil512Elements = {};
	private final BooleanReadChannel[] readChannels;

	public Fieldbus4xxDI(WagoImpl parent, int moduleCount, int coilOffset0, int channelsCount) {
		var id = ID_TEMPLATE + moduleCount;

		this.readChannels = new BooleanReadChannel[channelsCount];
		this.inputCoil0Elements = new ModbusCoilElement[channelsCount];
		for (var i = 0; i < channelsCount; i++) {
			var doc = new BooleanDoc();
			doc.persistencePriority(PersistencePriority.MEDIUM);
			var channelId = new FieldbusChannelId(id + "_C" + (i + 1), doc);
			BooleanReadChannel channel = parent.addChannel(channelId);
			this.readChannels[i] = channel;

			var element = parent.createModbusCoilElement(channel.channelId(), coilOffset0 + i);
			this.inputCoil0Elements[i] = element;
		}
	}

	@Override
	public String getName() {
		return "WAGO I/O 750-400 " + this.readChannels.length + "-channel digital input module";
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
