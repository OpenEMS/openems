package io.openems.edge.bridge.modbus.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.Log;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.ModbusRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;

public abstract class AbstractOpenemsModbusComponent extends AbstractOpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(AbstractOpenemsModbusComponent.class);

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
		this.clearModbusBridge();
		this.channels().clear();
		super.deactivate();
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
			this.modbus.addProtocol(this.id(), this.getModbusProtocol(this.unitId));
		}
	}

	private void clearModbusBridge() {
		if (this.modbus != null) {
			modbus.removeProtocol(this.id());
		}
	}

	private ModbusProtocol getModbusProtocol(int unitId) {
		ModbusProtocol protocol = this.protocol;
		if (protocol != null) {
			return protocol;
		}
		this.protocol = defineModbusProtocol(unitId);
		return this.protocol;
	}

	/**
	 * Defines the Modbus protocol
	 * 
	 * @param unitId
	 * 
	 * @return
	 */
	protected abstract ModbusProtocol defineModbusProtocol(int unitId);

	/**
	 * Maps an Element to one or more ModbusChannels using converters, that convert
	 * the value forwards and backwards.
	 */
	public class ChannelMapper {

		private final AbstractModbusElement<?> element;
		private Map<Channel<?>, ElementToChannelConverter> channelMaps = new HashMap<>();

		public ChannelMapper(AbstractModbusElement<?> element) {
			this.element = element;
			this.element.onUpdateCallback((value) -> {
				/*
				 * Applies the updated value on every Channel in ChannelMaps using the given
				 * Converter. If the converter returns an Optional.empty, the value is ignored.
				 */
				this.channelMaps.forEach((channel, converter) -> {
					Optional<Object> convertedValueOpt = converter.elementToChannel(value);
					if (convertedValueOpt.isPresent()) {
						try {
							channel.setNextValue(convertedValueOpt.get());
						} catch (OpenemsException e) {
							Log.warn("Channel [" + channel.address() + "] unable to set next value: " + e.getMessage());
						}
					}
				});
			});
		}

		public ChannelMapper m(io.openems.edge.common.channel.doc.ChannelId channelId,
				ElementToChannelConverter converter) {
			Channel<?> channel = channel(channelId);
			this.channelMaps.put(channel, converter);
			/*
			 * handle Channel Write to Element
			 */
			if (channel instanceof WriteChannel<?>) {
				((WriteChannel<?>) channel).onSetNextWriteCallback(value -> {
					Object convertedValue = converter.channelToElement(value);
					if (this.element instanceof ModbusRegisterElement) {
						try {
							((ModbusRegisterElement<?>) element).setNextWriteValue(Optional.ofNullable(convertedValue));
						} catch (OpenemsException e) {
							log.warn("Unable to write to Element [" + this.element.getStartAddress() + "]: "
									+ e.getMessage());
						}
					} else {
						log.warn("Unable to write to Element [" + this.element.getStartAddress()
								+ "]: it is not a ModbusElement");
					}
				});
			}
			return this;
		}

		public ChannelMapper m(io.openems.edge.common.channel.doc.ChannelId channelId,
				Function<Object, Optional<Object>> elementToChannel, Function<Object, Object> channelToElement) {
			ElementToChannelConverter converter = new ElementToChannelConverter(elementToChannel, channelToElement);
			return this.m(channelId, converter);
		}

		public AbstractModbusElement<?> build() {
			return this.element;
		}
	}

	/**
	 * Creates a ChannelMapper that can be used with builder pattern inside the
	 * protocol definition.
	 * 
	 * @param element
	 * @return
	 */
	protected final ChannelMapper cm(AbstractModbusElement<?> element) {
		return new ChannelMapper(element);
	}

	/**
	 * Maps the given element to the Channel identified by channelId.
	 * 
	 * @param channelDoc
	 * @param element
	 * @return the element parameter
	 */
	protected final AbstractModbusElement<?> m(io.openems.edge.common.channel.doc.ChannelId channelId,
			AbstractModbusElement<?> element) {
		return new ChannelMapper(element) //
				.m(channelId, ElementToChannelConverter.CONVERT_1_TO_1) //
				.build();
	}

	/**
	 * Private subclass to handle Channels that are mapping to one bit of a Modbus
	 * Unsigned Word element
	 */
	public class BitChannelMapper {
		private final UnsignedWordElement element;
		private final Map<Integer, Channel<?>> channels = new HashMap<>();

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
			Channel<?> channel = channel(channelId);
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

	/**
	 * Creates a BitChannelMapper that can be used with builder pattern inside the
	 * protocol definition.
	 * 
	 * @param element
	 * @return
	 */
	protected final BitChannelMapper bm(UnsignedWordElement element) {
		return new BitChannelMapper(element);
	}
}
