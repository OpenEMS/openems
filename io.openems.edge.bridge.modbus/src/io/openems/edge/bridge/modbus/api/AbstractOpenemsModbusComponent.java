package io.openems.edge.bridge.modbus.api;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.ModbusCoilElement;
import io.openems.edge.bridge.modbus.api.element.ModbusRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.TypeUtils;

public abstract class AbstractOpenemsModbusComponent extends AbstractOpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(AbstractOpenemsModbusComponent.class);

	private Integer unitId;

	/*
	 * The protocol. Consume via 'getModbusProtocol()'
	 */
	private ModbusProtocol protocol = null;

	protected void activate(String id) {
		throw new IllegalArgumentException("Use the other activate() method.");
	}

	/**
	 * Call this method from Component implementations activate().
	 * 
	 * @param context         ComponentContext of this component. Receive it from
	 *                        parameter for @Activate
	 * @param servicePid      The service_pid of this Component. Typically
	 *                        'config.service_pid()'
	 * @param id              ID of this component. Typically 'config.id()'
	 * @param enabled         Whether the component should be enabled. Typically
	 *                        'config.enabled()'
	 * @param unitId          Unit-ID of the Modbus target
	 * @param cm              An instance of ConfigurationAdmin. Receive it
	 *                        using @Reference
	 * @param modbusReference The name of the @Reference setter method for the
	 *                        Modbus bridge - e.g. 'Modbus' if you have a
	 *                        setModbus()-method
	 * @param modbusId        The ID of the Modbus brige. Typically
	 *                        'config.modbus_id()'
	 */
	protected void activate(ComponentContext context, String servicePid, String id, boolean enabled, int unitId,
			ConfigurationAdmin cm, String modbusReference, String modbusId) {
		super.activate(context, servicePid, id, enabled);
		// update filter for 'Modbus'
		if (OpenemsComponent.updateReferenceFilter(cm, servicePid, "Modbus", modbusId)) {
			return;
		}
		this.unitId = unitId;
		BridgeModbus modbus = this.modbus.get();
		if (this.isEnabled() && modbus != null) {
			modbus.addProtocol(this.id(), this.getModbusProtocol(this.unitId));
		}
	}

	protected void activate(ComponentContext context, String service_pid, String id, boolean enabled) {
		throw new IllegalArgumentException("Use the other activate() for Modbus compoenents!");
	}

	@Override
	protected void deactivate() {
		super.deactivate();
	}

	public Integer getUnitId() {
		return unitId;
	}

	private AtomicReference<BridgeModbus> modbus = new AtomicReference<BridgeModbus>(null);

	/**
	 * Set the Modbus bridge. Should be called by @Reference
	 * 
	 * @param modbus
	 */
	protected void setModbus(BridgeModbus modbus) {
		this.modbus.set(modbus);
	}

	/**
	 * Unset the Modbus bridge. Should be called by @Reference
	 * 
	 * @param modbus
	 */
	protected void unsetModbus(BridgeModbus modbus) {
		this.modbus.compareAndSet(modbus, null);
		if (modbus != null) {
			modbus.removeProtocol(this.id());
		}
	}

	private ModbusProtocol getModbusProtocol(int unitId) {
		ModbusProtocol protocol = this.protocol;
		if (protocol != null) {
			return protocol;
		}
		this.protocol = this.defineModbusProtocol();
		return this.protocol;
	}

	/**
	 * Defines the Modbus protocol
	 * 
	 * @return
	 */
	protected abstract ModbusProtocol defineModbusProtocol();

	/**
	 * Maps an Element to one or more ModbusChannels using converters, that convert
	 * the value forward and backwards.
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
					Object convertedValue;
					try {
						convertedValue = converter.elementToChannel(value);
					} catch (IllegalArgumentException e) {
						throw new IllegalArgumentException("Conversion for [" + channel.channelId() + "] failed", e);
					}
					channel.setNextValue(convertedValue);
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
				((WriteChannel<?>) channel).onSetNextWrite(value -> {
					Object convertedValue = converter.channelToElement(value);
					if (this.element instanceof ModbusRegisterElement) {
						try {
							((ModbusRegisterElement<?>) element).setNextWriteValue(Optional.ofNullable(convertedValue));
						} catch (OpenemsException e) {
							log.warn("Unable to write to ModbusRegisterElement [" + this.element.getStartAddress()
									+ "]: " + e.getMessage());
						}
					} else if (this.element instanceof ModbusCoilElement) {
						try {
							((ModbusCoilElement) element).setNextWriteValue(
									Optional.ofNullable(TypeUtils.getAsType(OpenemsType.BOOLEAN, convertedValue)));
						} catch (OpenemsException e) {
							log.warn("Unable to write to ModbusCoilElement [" + this.element.getStartAddress() + "]: "
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
				Function<Object, Object> elementToChannel, Function<Object, Object> channelToElement) {
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
	 * Maps the given element 1-to-1 to the Channel identified by channelId.
	 * 
	 * @param channelId
	 * @param element
	 * @return the element parameter
	 */
	protected final AbstractModbusElement<?> m(io.openems.edge.common.channel.doc.ChannelId channelId,
			AbstractModbusElement<?> element) {
		return new ChannelMapper(element) //
				.m(channelId, ElementToChannelConverter.DIRECT_1_TO_1) //
				.build();
	}

	/**
	 * Maps the given element to the Channel identified by channelId, applying the
	 * given @link{ElementToChannelConverter}
	 * 
	 * @param channelId
	 * @param element
	 * @param converter
	 * @return the element parameter
	 */
	protected final AbstractModbusElement<?> m(io.openems.edge.common.channel.doc.ChannelId channelId,
			AbstractModbusElement<?> element, ElementToChannelConverter converter) {
		return new ChannelMapper(element) //
				.m(channelId, converter) //
				.build();
	}

	public enum BitConverter {
		DIRECT_1_TO_1, INVERT
	}

	/**
	 * Private subclass to handle Channels that are mapping to one bit of a Modbus
	 * Unsigned Word element
	 */
	public class BitChannelMapper {
		private class ChannelWrapper {
			private final Channel<?> channel;
			private final BitConverter converter;

			protected ChannelWrapper(Channel<?> channel, BitConverter converter) {
				this.channel = channel;
				this.converter = converter;
			}
		}

		private final UnsignedWordElement element;
		private final Map<Integer, ChannelWrapper> channels = new HashMap<>();

		public BitChannelMapper(UnsignedWordElement element) {
			this.element = element;
			this.element.onUpdateCallback((value) -> {
				if(value == null) {
					return;
				}
				
				this.channels.forEach((bitIndex, channelWrapper) -> {
					if (bitIndex == null) {
						log.warn("BitIndex is null for Channel [" + channelWrapper.channel.address() + "]");
						return;
					}

					// Get value for this Channel
					boolean setValue;
					if (value << ~bitIndex < 0) {
						setValue = true;
					} else {
						setValue = false;
					}

					// Apply Bit-Conversion
					BitConverter converter = channelWrapper.converter;
					switch (converter) {
					case DIRECT_1_TO_1:
						break;
					case INVERT:
						setValue = !setValue;
						break;
					}

					// Set Value to Channel
					Channel<?> channel = channelWrapper.channel;
					channel.setNextValue(setValue);
				});
			});
		}

		public BitChannelMapper m(io.openems.edge.common.channel.doc.ChannelId channelId, int bitIndex,
				BitConverter converter) {
			Channel<?> channel = channel(channelId);
			if (channel.getType() != OpenemsType.BOOLEAN) {
				throw new IllegalArgumentException(
						"Channel [" + channelId + "] must be of type [BOOLEAN] for bit-mapping.");
			}
			this.channels.put(bitIndex, new ChannelWrapper(channel, converter));
			return this;
		}

		public BitChannelMapper m(io.openems.edge.common.channel.doc.ChannelId channelId, int bitIndex) {
			return m(channelId, bitIndex, BitConverter.DIRECT_1_TO_1);
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

	/**
	 * Handles channels that are mapping to one bit of a modbus unsigned double word
	 * element
	 */
	public class DoubleWordBitChannelMapper {
		private final UnsignedDoublewordElement element;
		private final Map<Integer, Channel<?>> channels = new HashMap<>();

		public DoubleWordBitChannelMapper(UnsignedDoublewordElement element) {
			this.element = element;
			this.element.onUpdateCallback((value) -> {
				this.channels.forEach((bitIndex, channel) -> {
					channel.setNextValue(value << ~bitIndex < 0);
				});
			});
		}

		public DoubleWordBitChannelMapper m(io.openems.edge.common.channel.doc.ChannelId channelId, int bitIndex) {
			Channel<?> channel = channel(channelId);
			if (channel.getType() != OpenemsType.BOOLEAN) {
				throw new IllegalArgumentException(
						"Channel [" + channelId + "] must be of type [BOOLEAN] for bit-mapping.");
			}
			this.channels.put(bitIndex, channel);
			return this;
		}

		public UnsignedDoublewordElement build() {
			return this.element;
		}
	}

	/**
	 * Creates a DoubleWordBitChannelMapper that can be used with builder pattern
	 * inside the protocol definition.
	 * 
	 * @param element
	 * @return
	 */
	protected final DoubleWordBitChannelMapper bm(UnsignedDoublewordElement element) {
		return new DoubleWordBitChannelMapper(element);
	}

	/**
	 * Handles channels that are mapping two bytes of a modbus unsigned double word
	 * element
	 */
	public class DoubleWordByteChannelMapper {
		private final UnsignedDoublewordElement element;
		private final Map<Integer, Channel<?>> channels = new HashMap<>();

		public DoubleWordByteChannelMapper(UnsignedDoublewordElement element) {
			this.element = element;
			this.element.onUpdateCallback((value) -> {
				this.channels.forEach((index, channel) -> {

					Integer val = value.intValue();

					Short valueToSet = convert(val, index);

					channel.setNextValue(valueToSet);
				});
			});
		}

		/**
		 * 
		 * @param channelId
		 * @param upperBytes 1 = upper two bytes, 0 = lower two bytes
		 * @return
		 */
		public DoubleWordByteChannelMapper mapByte(io.openems.edge.common.channel.doc.ChannelId channelId,
				int upperBytes) {
			Channel<?> channel = channel(channelId);
			if (channel.getType() != OpenemsType.SHORT) {
				throw new IllegalArgumentException(
						"Channel [" + channelId + "] must be of type [SHORT] for byte-mapping.");
			}
			this.channels.put(upperBytes, channel);
			return this;
		}

		public UnsignedDoublewordElement build() {
			return this.element;
		}

	}

	/**
	 * Creates a DoubleWordBitChannelMapper that can be used with builder pattern
	 * inside the protocol definition.
	 * 
	 * @param element
	 * @return
	 */
	protected final DoubleWordByteChannelMapper byteMap(UnsignedDoublewordElement element) {
		return new DoubleWordByteChannelMapper(element);
	}

	/**
	 * 
	 * @param value
	 * @param upperBytes 1 = upper two bytes, 0 = lower two bytes
	 * @return
	 */
	public static Short convert(int value, int upperBytes) {
		ByteBuffer b = ByteBuffer.allocate(4);
		b.order(ByteOrder.LITTLE_ENDIAN);
		b.putInt(value);

		byte byte0 = b.get(upperBytes * 2);
		byte byte1 = b.get(upperBytes * 2 + 1);

		ByteBuffer shortBuf = ByteBuffer.allocate(2);
		shortBuf.order(ByteOrder.LITTLE_ENDIAN);
		shortBuf.put(0, byte0);
		shortBuf.put(1, byte1);

		return shortBuf.getShort();
	}
}
