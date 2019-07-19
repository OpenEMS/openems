package io.openems.edge.wago;

import io.openems.common.channel.AccessMode;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.internal.OpenemsTypeDoc;

public class Fieldbus501DO2Ch extends FieldbusModule {

	private final static String ID_TEMPLATE = "DIGITAL_OUTPUT_M";

	private final AbstractModbusElement<?>[] inputElements;
	private final AbstractModbusElement<?>[] outputElements;
	private final BooleanReadChannel[] readChannels;

	public Fieldbus501DO2Ch(Wago parent, int moduleCount, int inputOffset, int outputOffset) {
		String id = ID_TEMPLATE + moduleCount;

		BooleanWriteChannel channel1;
		{
			OpenemsTypeDoc<Boolean> doc = new BooleanDoc() //
					.accessMode(AccessMode.WRITE_ONLY);
			FieldbusChannelId channelId = new FieldbusChannelId(id + "_C1", doc);
			channel1 = (BooleanWriteChannel) parent.addChannel(channelId);
		}
		BooleanWriteChannel channel2;
		{
			OpenemsTypeDoc<Boolean> doc = new BooleanDoc() //
					.accessMode(AccessMode.WRITE_ONLY);
			FieldbusChannelId channelId = new FieldbusChannelId(id + "_C2", doc);
			channel2 = (BooleanWriteChannel) parent.addChannel(channelId);
		}
		this.readChannels = new BooleanReadChannel[] { channel1, channel2 };

		this.inputElements = new AbstractModbusElement<?>[] { //
				parent.createModbusElement(channel1.channelId(), outputOffset), //
				parent.createModbusElement(channel2.channelId(), outputOffset + 1), //
		};

		this.outputElements = new AbstractModbusElement<?>[] { //
				parent.createModbusElement(channel1.channelId(), outputOffset), //
				parent.createModbusElement(channel2.channelId(), outputOffset + 1), //
		};
	}

	@Override
	public String getName() {
		return "WAGO I/O 750-501 2-channel digital output module";
	}

	@Override
	public AbstractModbusElement<?>[] getInputElements() {
		return this.inputElements;
	}

	@Override
	public AbstractModbusElement<?>[] getOutputElements() {
		return this.outputElements;
	}

	@Override
	public int getOutputCoils() {
		return 2;
	}

	@Override
	public int getInputCoils() {
		return 0;
	}

	@Override
	public BooleanReadChannel[] getChannels() {
		return this.readChannels;
	}
}
