package io.openems.edge.wago;

import io.openems.common.channel.AccessMode;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.DummyCoilElement;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.internal.OpenemsTypeDoc;

public class Fieldbus523RO1Ch extends FieldbusModule {

	private static final String ID_TEMPLATE = "RELAY_M";

	private final AbstractModbusElement<?>[] inputElements;
	private final AbstractModbusElement<?>[] outputElements;
	private final BooleanReadChannel[] readChannels;

	public Fieldbus523RO1Ch(Wago parent, int moduleCount, int inputOffset, int outputOffset) {
		String id = ID_TEMPLATE + moduleCount;

		BooleanWriteChannel channel1;
		{
			OpenemsTypeDoc<Boolean> doc = new BooleanDoc() //
					.accessMode(AccessMode.READ_WRITE);
			FieldbusChannelId channelId = new FieldbusChannelId(id, doc);
			channel1 = (BooleanWriteChannel) parent.addChannel(channelId);
		}
		BooleanReadChannel channel2;
		{
			OpenemsTypeDoc<Boolean> doc = new BooleanDoc();
			FieldbusChannelId channelId = new FieldbusChannelId(id + "_HAND", doc);
			channel2 = parent.addChannel(channelId);
		}
		this.readChannels = new BooleanReadChannel[] { channel1, channel2 };

		this.inputElements = new AbstractModbusElement<?>[] { //
				parent.createModbusElement(channel1.channelId(), outputOffset), //
				new DummyCoilElement(outputOffset + 1), //
				parent.createModbusElement(channel2.channelId(), inputOffset), //
				new DummyCoilElement(inputOffset + 1) //
		};

		this.outputElements = new AbstractModbusElement<?>[] { //
				parent.createModbusElement(channel1.channelId(), outputOffset), //
				new DummyCoilElement(outputOffset + 1) //
		};
	}

	@Override
	public String getName() {
		return "WAGO I/O 750-523 1-channel relay output module";
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
		return 2;
	}

	@Override
	public BooleanReadChannel[] getChannels() {
		return this.readChannels;
	}
}
