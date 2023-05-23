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
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.ModbusCoilElement;
import io.openems.edge.bridge.modbus.api.element.ModbusRegisterElement;
import io.openems.edge.bridge.modbus.api.task.ReadTask;
import io.openems.edge.bridge.modbus.api.task.WriteTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.TypeUtils;

public abstract class AbstractOpenemsModbusComponent extends AbstractOpenemsComponent implements ModbusComponent {

	private final Logger log = LoggerFactory.getLogger(AbstractOpenemsModbusComponent.class);

	private Integer unitId;

	/*
	 * The protocol. Consume via 'getModbusProtocol()'
	 */
	private ModbusProtocol protocol = null;

	/**
	 * Default constructor for AbstractOpenemsModbusComponent.
	 *
	 * <p>
	 * Automatically initializes (i.e. creates {@link Channel} instances for each
	 * given ChannelId using the Channel-{@link Doc}.
	 *
	 * <p>
	 * It is important to list all Channel-ID enums of all inherited
	 * OpenEMS-Natures, i.e. for every OpenEMS Java interface you are implementing,
	 * you need to list the interface' ChannelID-enum here like
	 * Interface.ChannelId.values().
	 *
	 * <p>
	 * Use as follows:
	 *
	 * <pre>
	 * public YourPhantasticOpenemsComponent() {
	 * 	super(//
	 * 			OpenemsComponent.ChannelId.values(), //
	 * 			YourPhantasticOpenemsComponent.ChannelId.values());
	 * }
	 * </pre>
	 *
	 * <p>
	 * Note: the separation in firstInitialChannelIds and furtherInitialChannelIds
	 * is only there to enforce that calling the constructor cannot be forgotten.
	 * This way it needs to be called with at least one parameter - which is always
	 * at least "OpenemsComponent.ChannelId.values()". Just use it as if it was:
	 *
	 * <pre>
	 * AbstractOpenemsComponent(ChannelId[]... channelIds)
	 * </pre>
	 *
	 * @param firstInitialChannelIds   the Channel-IDs to initialize.
	 * @param furtherInitialChannelIds the Channel-IDs to initialize.
	 */
	protected AbstractOpenemsModbusComponent(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	protected void activate(String id) {
		throw new IllegalArgumentException("Use the other activate() method.");
	}

	/**
	 * Call this method from Component implementations activate().
	 *
	 * @param context         ComponentContext of this component. Receive it from
	 *                        parameter for @Activate
	 * @param id              ID of this component. Typically 'config.id()'
	 * @param alias           Human-readable name of this Component. Typically
	 *                        'config.alias()'. Defaults to 'id' if empty
	 * @param enabled         Whether the component should be enabled. Typically
	 *                        'config.enabled()'
	 * @param unitId          Unit-ID of the Modbus target
	 * @param cm              An instance of ConfigurationAdmin. Receive it
	 *                        using @Reference
	 * @param modbusReference The name of the @Reference setter method for the
	 *                        Modbus bridge - e.g. 'Modbus' if you have a
	 *                        setModbus()-method
	 * @param modbusId        The ID of the Modbus bridge. Typically
	 *                        'config.modbus_id()'
	 * @return true if the target filter was updated. You may use it to abort the
	 *         activate() method.
	 * @throws OpenemsException on error
	 */
	protected boolean activate(ComponentContext context, String id, String alias, boolean enabled, int unitId,
			ConfigurationAdmin cm, String modbusReference, String modbusId) throws OpenemsException {
		super.activate(context, id, alias, enabled);
		// Initialize ModbusCommunicationFailed State (this also tests if the
		// ModbusComponent nature was properly implemented)
		this._setModbusCommunicationFailed(false);
		// update filter for 'Modbus'
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "Modbus", modbusId)) {
			return true;
		}
		this.unitId = unitId;
		var modbus = this.modbus.get();
		if (this.isEnabled() && modbus != null) {
			modbus.addProtocol(this.id(), this.getModbusProtocol());
		}
		return false;
	}

	@Override
	protected void activate(ComponentContext context, String id, String alias, boolean enabled) {
		throw new IllegalArgumentException("Use the other activate() for Modbus components!");
	}

	/**
	 * Call this method from Component implementations activate().
	 *
	 * @param context         ComponentContext of this component. Receive it from
	 *                        parameter for @Activate
	 * @param id              ID of this component. Typically 'config.id()'
	 * @param alias           Human-readable name of this Component. Typically
	 *                        'config.alias()'. Defaults to 'id' if empty
	 * @param enabled         Whether the component should be enabled. Typically
	 *                        'config.enabled()'
	 * @param unitId          Unit-ID of the Modbus target
	 * @param cm              An instance of ConfigurationAdmin. Receive it
	 *                        using @Reference
	 * @param modbusReference The name of the @Reference setter method for the
	 *                        Modbus bridge - e.g. 'Modbus' if you have a
	 *                        setModbus()-method
	 * @param modbusId        The ID of the Modbus bridge. Typically
	 *                        'config.modbus_id()'
	 * @return true if the target filter was updated. You may use it to abort the
	 *         activate() method.
	 * @throws OpenemsException on error
	 */
	protected boolean modified(ComponentContext context, String id, String alias, boolean enabled, int unitId,
			ConfigurationAdmin cm, String modbusReference, String modbusId) throws OpenemsException {
		super.modified(context, id, alias, enabled);
		// update filter for 'Modbus'
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "Modbus", modbusId)) {
			return true;
		}
		this.unitId = unitId;
		var modbus = this.modbus.get();
		modbus.removeProtocol(this.id());
		if (this.isEnabled() && modbus != null) {
			modbus.addProtocol(this.id(), this.getModbusProtocol());
		}
		return false;
	}

	@Override
	protected void modified(ComponentContext context, String id, String alias, boolean enabled) {
		throw new IllegalArgumentException("Use the other activate() for Modbus components!");
	}

	@Override
	protected void deactivate() {
		super.deactivate();
		var modbus = this.modbus.getAndSet(null);
		if (modbus != null) {
			modbus.removeProtocol(this.id());
		}
	}

	/**
	 * Gets the Modbus Unit-ID.
	 *
	 * @return the Modbus Unit-ID
	 */
	public Integer getUnitId() {
		return this.unitId;
	}

	private final AtomicReference<BridgeModbus> modbus = new AtomicReference<>(null);

	/**
	 * Set the Modbus bridge. Should be called by @Reference
	 *
	 * @param modbus the BridgeModbus Reference
	 */
	protected void setModbus(BridgeModbus modbus) {
		this.modbus.set(modbus);
	}

	/**
	 * Unset the Modbus bridge. Should be called by @Reference
	 *
	 * @param modbus the BridgeModbus Reference
	 */
	protected void unsetModbus(BridgeModbus modbus) {
		this.modbus.compareAndSet(modbus, null);
		if (modbus != null) {
			modbus.removeProtocol(this.id());
		}
	}

	/**
	 * Gets the Modbus-Bridge.
	 *
	 * @return the {@link BridgeModbus} - either {@link BridgeModbusSerial} or
	 *         {@link BridgeModbusTcp}
	 */
	public BridgeModbus getBridgeModbus() {
		return this.modbus.get();
	}

	/**
	 * Gets the {@link ModbusProtocol}. Creates it via
	 * {@link #defineModbusProtocol()} if it does not yet exist.
	 *
	 * @return the {@link ModbusProtocol}
	 * @throws OpenemsException on error
	 */
	protected ModbusProtocol getModbusProtocol() throws OpenemsException {
		var protocol = this.protocol;
		if (protocol != null) {
			return protocol;
		}
		this.protocol = this.defineModbusProtocol();
		return this.protocol;
	}

	@Override
	public void retryModbusCommunication() {
		var bridge = this.modbus.get();
		bridge.retryModbusCommunication(this.id());
	}

	/**
	 * Defines the Modbus protocol.
	 *
	 * @return the ModbusProtocol
	 * @throws OpenemsException on error
	 */
	protected abstract ModbusProtocol defineModbusProtocol() throws OpenemsException;

	/**
	 * Maps an Element to one or more ModbusChannels using converters, that convert
	 * the value forward and backwards.
	 */
	public class ChannelMapper<T extends AbstractModbusElement<?>> {

		private final T element;
		private final Map<Channel<?>, ElementToChannelConverter> channelMaps = new HashMap<>();

		public ChannelMapper(T element) {
			this.element = element;
		}

		/**
		 * Maps the given element 1-to-1 to the Channel identified by channelId.
		 *
		 * @param channelId the Channel-ID
		 * @param converter the {@link ElementToChannelConverter}
		 * @return the element parameter
		 */
		public ChannelMapper<T> m(io.openems.edge.common.channel.ChannelId channelId,
				ElementToChannelConverter converter) {
			return this.m(channelId, converter, new ChannelMetaInfo(this.element.getStartAddress()));
		}

		/**
		 * Maps the given element 1-to-1 to the Channel identified by channelId.
		 *
		 * @param channelId       the Channel-ID
		 * @param converter       the {@link ElementToChannelConverter}
		 * @param channelMetaInfo an object that holds meta information about the
		 *                        Channel
		 * @return the element parameter
		 */
		public ChannelMapper<T> m(io.openems.edge.common.channel.ChannelId channelId,
				ElementToChannelConverter converter, ChannelMetaInfo channelMetaInfo) {
			Channel<?> channel = AbstractOpenemsModbusComponent.this.channel(channelId);
			channel.setMetaInfo(channelMetaInfo);
			this.channelMaps.put(channel, converter);
			return this;
		}

		/**
		 * Maps the given element to the Channel identified by channelId, applying the
		 * given @link{ElementToChannelConverter}.
		 *
		 * @param channelId        the Channel-ID
		 * @param elementToChannel the Element-To-Channel converter function for
		 *                         {@link ReadTask}s
		 * @param channelToElement the Channel-To-Channel converter function for
		 *                         {@link WriteTask}s
		 * @return the element parameter
		 */
		public ChannelMapper<T> m(io.openems.edge.common.channel.ChannelId channelId,
				Function<Object, Object> elementToChannel, Function<Object, Object> channelToElement) {
			var converter = new ElementToChannelConverter(elementToChannel, channelToElement);
			return this.m(channelId, converter);
		}

		/**
		 * Builds the {@link ChannelMapper}.
		 *
		 * @return the {@link ChannelMapper}
		 */
		public T build() {
			/*
			 * Forward Element Read-Value to Channel
			 */
			this.element.onUpdateCallback(value -> { //
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

			/*
			 * Forward Channel Write-Value to Element
			 */
			this.channelMaps.keySet().forEach(channel -> {
				if (channel instanceof WriteChannel<?>) {
					((WriteChannel<?>) channel).onSetNextWrite(value -> {
						// dynamically get the Converter; this allows the converter to be changed
						var converter = this.channelMaps.get(channel);
						var convertedValue = converter.channelToElement(value);
						if (this.element instanceof ModbusRegisterElement) {
							try {
								((ModbusRegisterElement<?>) this.element)
										.setNextWriteValue(Optional.ofNullable(convertedValue));
							} catch (OpenemsException | IllegalArgumentException e) {
								AbstractOpenemsModbusComponent.this.logWarn(AbstractOpenemsModbusComponent.this.log,
										"Unable to write to ModbusRegisterElement. " //
												+ "Address [" + this.element.getStartAddress() + "] " //
												+ "Channel [" + channel.address() + "]. " //
												+ "Exception [" + e.getClass().getSimpleName() + "] " //
												+ ": " + e.getMessage());
								if (e instanceof IllegalArgumentException) {
									// This is likely a software development bug. Draw some attention:
									e.printStackTrace();
								}
							}
						} else if (this.element instanceof ModbusCoilElement) {
							try {
								((ModbusCoilElement) this.element).setNextWriteValue(
										Optional.ofNullable(TypeUtils.getAsType(OpenemsType.BOOLEAN, convertedValue)));
							} catch (OpenemsException e) {
								AbstractOpenemsModbusComponent.this.logWarn(AbstractOpenemsModbusComponent.this.log,
										"Unable to write to ModbusCoilElement " //
												+ "[" + this.element.getStartAddress() + "]: " + e.getMessage());
							}
						} else {
							AbstractOpenemsModbusComponent.this.logWarn(AbstractOpenemsModbusComponent.this.log,
									"Unable to write to Element " //
											+ "[" + this.element.getStartAddress() + "]: it is not a ModbusElement");
						}
					});
				}
			});

			return this.element;
		}
	}

	/**
	 * Creates a ChannelMapper that can be used with builder pattern inside the
	 * protocol definition.
	 *
	 * @param <T>     the type of the {@link AbstractModbusElement}d
	 * @param element the ModbusElement
	 * @return a {@link ChannelMapper}
	 */
	protected final <T extends AbstractModbusElement<?>> ChannelMapper<T> m(T element) {
		return new ChannelMapper<>(element);
	}

	/**
	 * Maps the given BitsWordElement.
	 *
	 * @param bitsWordElement the ModbusElement
	 * @return the element parameter
	 */
	protected final AbstractModbusElement<?> m(BitsWordElement bitsWordElement) {
		return bitsWordElement;
	}

	/**
	 * Maps the given element 1-to-1 to the Channel identified by channelId.
	 *
	 * @param <T>       the type of the {@link AbstractModbusElement}d
	 * @param channelId the Channel-ID
	 * @param element   the ModbusElement
	 * @return the element parameter
	 */
	protected final <T extends AbstractModbusElement<?>> T m(io.openems.edge.common.channel.ChannelId channelId,
			T element) {
		return this.m(channelId, element, ElementToChannelConverter.DIRECT_1_TO_1);
	}

	/**
	 * Maps the given element 1-to-1 to the Channel identified by channelId.
	 *
	 * @param <T>             the type of the {@link AbstractModbusElement}d
	 * @param channelId       the Channel-ID
	 * @param element         the ModbusElement
	 * @param channelMetaInfo an object that holds meta information about the
	 *                        Channel
	 * @return the element parameter
	 */
	protected final <T extends AbstractModbusElement<?>> T m(io.openems.edge.common.channel.ChannelId channelId,
			T element, ChannelMetaInfo channelMetaInfo) {
		return this.m(channelId, element, ElementToChannelConverter.DIRECT_1_TO_1, channelMetaInfo);
	}

	/**
	 * Maps the given element to the Channel identified by channelId, applying the
	 * given @link{ElementToChannelConverter}.
	 *
	 * @param <T>       the type of the {@link AbstractModbusElement}d
	 * @param channelId the Channel-ID
	 * @param element   the ModbusElement
	 * @param converter the ElementToChannelConverter
	 * @return the element parameter
	 */
	protected final <T extends AbstractModbusElement<?>> T m(io.openems.edge.common.channel.ChannelId channelId,
			T element, ElementToChannelConverter converter) {
		return new ChannelMapper<>(element) //
				.m(channelId, converter) //
				.build();
	}

	/**
	 * Maps the given element to the Channel identified by channelId, applying the
	 * given @link{ElementToChannelConverter}.
	 *
	 * @param <T>             the type of the {@link AbstractModbusElement}d
	 * @param channelId       the Channel-ID
	 * @param element         the ModbusElement
	 * @param converter       the ElementToChannelConverter
	 * @param channelMetaInfo an object that holds meta information about the
	 *                        Channel
	 * @return the element parameter
	 */
	protected final <T extends AbstractModbusElement<?>> T m(io.openems.edge.common.channel.ChannelId channelId,
			T element, ElementToChannelConverter converter, ChannelMetaInfo channelMetaInfo) {
		return new ChannelMapper<>(element) //
				.m(channelId, converter, channelMetaInfo) //
				.build();
	}

	public enum BitConverter {
		DIRECT_1_TO_1, INVERT
	}

}
