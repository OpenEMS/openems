package io.openems.edge.bridge.modbus.api;

import java.util.HashMap;
import java.util.Map;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.Log;
import io.openems.edge.bridge.modbus.channel.ModbusChannel;
import io.openems.edge.bridge.modbus.protocol.ModbusProtocol;
import io.openems.edge.bridge.modbus.protocol.RegisterElement;
import io.openems.edge.bridge.modbus.protocol.UnsignedWordElement;
import io.openems.edge.common.channel.Channel;
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
		this.channels().clear();
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
	 * Maps the given element to the Channel identified by channelId. Throws an
	 * IllegalArgumentException if Channel is not a ModbusChannel.
	 * 
	 * @param channelDoc
	 * @param element
	 * @return the element parameter
	 */
	protected final RegisterElement<?> m(io.openems.edge.common.channel.doc.ChannelId channelId,
			RegisterElement<?> element) {
		Channel channel = this.channel(channelId);
		if (!(channel instanceof ModbusChannel<?>)) {
			throw new IllegalArgumentException("Channel [" + channelId + "] is not a ModbusChannel.");
		}
		ModbusChannel<?> modbusChannel = (ModbusChannel<?>) channel;
		modbusChannel.mapToElement(element);
		return element;
	}

	/**
	 * Private subclass to handle Channels that are mapping to one bit of a Modbus
	 * Unsigned Word element
	 */
	public class BitChannelMapper {
		private final UnsignedWordElement element;
		private final Map<Integer, Channel> channels = new HashMap<>();

		public BitChannelMapper(UnsignedWordElement element) {
			this.element = element;
			this.element.onUpdateCallback((value) -> {
				this.channels.forEach((bitIndex, channel) -> {
					try {
						if (value << ~bitIndex < 0) {
							channel.setNextValue(true);
						} else {
							channel.setNextValue(false);
						}
					} catch (OpenemsException e) {
						Log.warn("Channel [" + channel.address() + "] unable to set next value: " + e.getMessage());
					}
				});
			});
		}

		public BitChannelMapper m(io.openems.edge.common.channel.doc.ChannelId channelId, int bitIndex) {
			Channel channel = channel(channelId);
			if (channel.getType() != OpenemsType.BOOLEAN) {
				throw new IllegalArgumentException(
						"Channel [" + channelId + "] must be of type [BOOLEAN] for bit-mapping.");
			}
			this.channels.put(bitIndex, channel);
			return this;
		}

		public UnsignedWordElement build() {
			return this.element;
		}
	}

	protected final BitChannelMapper bm(UnsignedWordElement element) {
		return new BitChannelMapper(element);
	}
}
