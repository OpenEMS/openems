package io.openems.edge.bridge.modbus.api;

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
					if (convertedValue != null) {
						channel.setNextValue(convertedValue);
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
	 * @param channelDoc
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
	 * @param channelDoc
	 * @param element
	 * @return the element parameter
	 */
	protected final AbstractModbusElement<?> m(io.openems.edge.common.channel.doc.ChannelId channelId,
			AbstractModbusElement<?> element, ElementToChannelConverter converter) {
		return new ChannelMapper(element) //
				.m(channelId, converter) //
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
					if (value << ~bitIndex < 0) {
						channel.setNextValue(true);
					} else {
						channel.setNextValue(false);
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
