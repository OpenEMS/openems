package io.openems.edge.bridge.modbus.api;

import io.openems.edge.bridge.modbus.channel.ModbusChannel;
import io.openems.edge.bridge.modbus.protocol.ModbusProtocol;
import io.openems.edge.bridge.modbus.protocol.RegisterElement;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.doc.ChannelDoc;
import io.openems.edge.common.component.AbstractOpenemsComponent;

public abstract class AbstractOpenemsModbusComponent extends AbstractOpenemsComponent {

	private volatile BridgeModbusTcp modbus = null;

	private Integer unitId;

	/*
	 * The protocol. Consume via 'getModbusProtocol()'
	 */
	private ModbusProtocol protocol = null;

	protected void activate(String id, boolean isEnabled) {
		throw new IllegalArgumentException("Use activate(String id, boolean isEnabled, int unitId)");
	}

	protected void activate(String id, boolean isEnabled, int unitId) {
		super.activate(id, isEnabled);
		this.unitId = unitId;
		this.initializeModbusBridge();
	}

	@Override
	protected void deactivate() {
		super.deactivate();
		this.clearModbusBridge();
	}

	/**
	 * Set the Modbus bridge. Should be called by @Reference
	 * 
	 * @param modbus
	 */
	protected void setModbus(BridgeModbusTcp modbus) {
		this.modbus = modbus;
		this.initializeModbusBridge();
	}

	/**
	 * Unset the Modbus bridge. Should be called by @Reference
	 * 
	 * @param modbus
	 */
	protected void unsetModbus(BridgeModbusTcp modbus) {
		if (modbus != null) {
			this.modbus = modbus;
			this.clearModbusBridge();
		}
	}

	private void initializeModbusBridge() {
		if (this.modbus != null && this.unitId != null) {
			this.modbus.addProtocol(this.id(), this.unitId, this.getModbusProtocol());
		}
	}

	private void clearModbusBridge() {
		if (this.modbus != null) {
			modbus.removeProtocol(this.id());
		}
	}

	private ModbusProtocol getModbusProtocol() {
		ModbusProtocol protocol = this.protocol;
		if (protocol != null) {
			return protocol;
		}
		this.protocol = defineModbusProtocol();
		return this.protocol;
	}

	/**
	 * Defines the Modbus protocol
	 * 
	 * @return
	 */
	protected abstract ModbusProtocol defineModbusProtocol();
	

	/**
	 * Maps the given element to the Channel identified by channelDoc. Throws an
	 * IllegalArgumentException if Channel is not a ModbusChannel.
	 * 
	 * @param channelDoc
	 * @param element
	 * @return the element parameter
	 */
	protected final RegisterElement<?> m(ChannelDoc channelDoc, RegisterElement<?> element) {
		Channel channel = this.channel(channelDoc);
		if (!(channel instanceof ModbusChannel<?>)) {
			throw new IllegalArgumentException("Channel [" + channelDoc + "] is not a ModbusChannel.");
		}
		ModbusChannel<?> modbusChannel = (ModbusChannel<?>) channel;
		modbusChannel.mapToElement(element);
		return element;
	}
}
