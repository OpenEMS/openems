package io.openems.edge.wago;

import java.util.concurrent.atomic.AtomicInteger;

import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;

public class Fieldbus501DO2Ch extends FieldbusModule {

	private final static AtomicInteger count = new AtomicInteger(0);
	private final static String ID_TEMPLATE = "DIGITAL_OUTPUT_M";

	private final AbstractModbusElement<?>[] inputElements;
	private final AbstractModbusElement<?>[] outputElements;
	private final BooleanReadChannel[] readChannels;

	public Fieldbus501DO2Ch(Wago parent, int inputOffset, int outputOffset) {
		String id = ID_TEMPLATE + count.incrementAndGet();

		BooleanWriteChannel channel1 = new BooleanWriteChannel(parent, new FieldbusChannel(id + "_C1"));
		BooleanWriteChannel channel2 = new BooleanWriteChannel(parent, new FieldbusChannel(id + "_C2"));
		this.readChannels = new BooleanReadChannel[] { channel1, channel2 };

		parent.addChannel(channel1);
		parent.addChannel(channel2);

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
