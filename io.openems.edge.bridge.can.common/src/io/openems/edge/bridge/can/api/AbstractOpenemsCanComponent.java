package io.openems.edge.bridge.can.api;

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
import io.openems.edge.bridge.can.api.element.AbstractCanChannelElement;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

public abstract class AbstractOpenemsCanComponent extends AbstractOpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(AbstractOpenemsCanComponent.class);
	private CanProtocol protocol = null;
	private final AtomicReference<BridgeCan> can = new AtomicReference<>(null);

	/**
	 * Default constructor for AbstractOpenemsCanComponent.
	 *
	 * <p>
	 * Automatically initializes (i.e. creates {@link Channel} instances for each
	 * given {@link ChannelId} using the Channel-{@link Doc}.
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
	protected AbstractOpenemsCanComponent(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	@Override
	protected void activate(String id) {
		throw new IllegalArgumentException("Use the other activate() method.");
	}

	/**
	 * Call this method from Component implementations activate().
	 *
	 * @param context      ComponentContext of this component. Receive it from
	 *                     parameter for @Activate
	 * @param id           ID of this component. Typically 'config.id()'
	 * @param alias        Human-readable name of this Component. Typically
	 *                     'config.alias()'. Defaults to 'id' if empty
	 * @param enabled      Whether the component should be enabled. Typically
	 *                     'config.enabled()'
	 * @param cm           An instance of ConfigurationAdmin. Receive it
	 *                     using @Reference
	 * @param canReference The name of the @Reference setter method for the CAN
	 *                     bridge - e.g. 'Can' if you have a setCan()-method
	 * @param canId        The ID of the CAN brige. Typically 'config.can_id()'
	 * @return true if the target filter was updated. You may use it to abort the
	 *         activate() method.
	 */
	protected boolean activate(ComponentContext context, String id, String alias, boolean enabled,
			ConfigurationAdmin cm, String canReference, String canId) throws IllegalArgumentException {
		super.activate(context, id, alias, enabled);

		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), canReference, canId)) {
			return true;
		}
		var canBridge = this.can.get();
		if (this.isEnabled() && canBridge != null) {
			try {
				canBridge.addProtocol(this.id(), this.getCanProtocol());
				return true;
			} catch (OpenemsException e) {
				throw new IllegalArgumentException("CanProtocol definition is wrong Ex: " + e.getMessage());
			}
		}
		return false;
	}

	@Override
	protected void activate(ComponentContext context, String id, String alias, boolean enabled) {
		throw new IllegalArgumentException("Use the other activate() for CAN compoenents!");
	}

	@Override
	protected void deactivate() {
		super.deactivate();
		var can = this.can.getAndSet(null);
		if (can != null) {
			can.removeProtocol(this.id());
		}
	}

	/**
	 * Set the CAN bridge. Should be called by @Reference
	 *
	 * @param can the BridgeCan Reference
	 */
	protected void setCan(BridgeCan can) {
		this.can.set(can);
	}

	protected BridgeCan getCanbridge() {
		return this.can.get();
	}

	/**
	 * Unset the CAN bridge. Should be called by @Reference
	 *
	 * @param can the BridgeCan Reference
	 */
	protected void unsetCan(BridgeCan can) {
		this.can.compareAndSet(can, null);
		if (can != null) {
			can.removeProtocol(this.id());
		}
	}

	private CanProtocol getCanProtocol() throws OpenemsException {
		var protocol = this.protocol;
		if (protocol != null) {
			return protocol;
		}
		this.protocol = this.defineCanProtocol();
		return this.protocol;
	}

	/**
	 * Defines the CAN protocol.
	 *
	 * @return the CanProtocol
	 */
	protected abstract CanProtocol defineCanProtocol() throws OpenemsException;

	/**
	 * Maps an Element to one CanChannels using converters, that convert the value
	 * forward and backwards.
	 */
	public class ChannelMapper {

		private final AbstractCanChannelElement<?, ?> element;
		private final Map<Channel<?>, ElementToChannelConverter> channelMaps = new HashMap<>();

		public ChannelMapper(AbstractCanChannelElement<?, ?> element) {
			this.element = element;
		}

		/**
		 * Puts the given Channel with the given converter to its channel map.
		 *
		 * @param channelId the {@link io.openems.edge.common.channel.ChannelId}
		 * @param converter the {@link ElementToChannelConverter}
		 * @return itself
		 */
		public ChannelMapper m(io.openems.edge.common.channel.ChannelId channelId,
				ElementToChannelConverter converter) {
			Channel<?> channel = AbstractOpenemsCanComponent.this.channel(channelId);
			this.channelMaps.put(channel, converter);
			return this;
		}

		/**
		 * Puts the given Channel with the given converters to its channel map.
		 *
		 * @param channelId        the {@link io.openems.edge.common.channel.ChannelId}
		 * @param elementToChannel the function that converts an element to the channel
		 *                         value
		 * @param channelToElement the function that converts a channel value to an
		 *                         element
		 * @return itself
		 */
		public ChannelMapper m(io.openems.edge.common.channel.ChannelId channelId, //
				Function<Object, Object> elementToChannel, Function<Object, Object> channelToElement) {
			var converter = new ElementToChannelConverter(elementToChannel, channelToElement);
			return this.m(channelId, converter);
		}

		public AbstractCanChannelElement<?, ?> build() {
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
						if (this.element instanceof AbstractCanChannelElement) {
							try {
								((AbstractCanChannelElement<?, ?>) this.element)
										.setNextWriteValue(Optional.ofNullable(convertedValue));
							} catch (OpenemsException e) {
								AbstractOpenemsCanComponent.this.log.warn("Unable to write to CanElement "
										+ this.element.toString() + "]: " + e.getMessage());
							}

							// TODO add other Channel elements here
							// } else if (this.element instanceof ModbusCoilElement) {
							//
						} else {
							AbstractOpenemsCanComponent.this.log.warn("Unable to write to Element ["
									+ this.element.toString() + "]: it is not a ModbusElement");
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
	 * @param element the ModbusElement
	 * @return a {@link ChannelMapper}
	 */
	protected final ChannelMapper m(AbstractCanChannelElement<?, ?> element) {
		return new ChannelMapper(element);
	}

	/**
	 * Maps the given element to the Channel identified by channelId, applying the
	 * given @link{ElementToChannelConverter}.
	 *
	 * @param channelId the Channel-ID
	 * @param element   the CanChannelElement
	 * @param converter the ElementToChannelConverter
	 * @return the element parameter
	 */
	public final AbstractCanChannelElement<?, ?> m(io.openems.edge.common.channel.ChannelId channelId,
			AbstractCanChannelElement<?, ?> element, ElementToChannelConverter converter) {
		return new ChannelMapper(element) //
				.m(channelId, converter) //
				.build();
	}

	/**
	 * Maps the given element 1-to-1 to the Channel identified by channelId.
	 *
	 * @param channelId the Channel-ID
	 * @param element   the CanChannelElement
	 * @return the element parameter
	 */
	public final AbstractCanChannelElement<?, ?> m(io.openems.edge.common.channel.ChannelId channelId,
			AbstractCanChannelElement<?, ?> element) {
		return new ChannelMapper(element) //
				.m(channelId, ElementToChannelConverter.DIRECT_1_TO_1) //
				.build();
	}

	/**
	 * Maps the given element with an offset of -40 to the Channel identified by
	 * channelId.
	 *
	 * @param channelId the Channel-ID
	 * @param element   the CanChannelElement
	 * @return the element parameter
	 */
	public final AbstractCanChannelElement<?, ?> mo40(io.openems.edge.common.channel.ChannelId channelId,
			AbstractCanChannelElement<?, ?> element) {
		return new ChannelMapper(element) //
				.m(channelId, new ElementToChannelOffsetConverter(-40)) //
				.build();
	}

	public enum BitConverter {
		DIRECT_1_TO_1, INVERT
	}

	/**
	 * Converts upper/lower bytes to Short.
	 *
	 * @param value      the int value
	 * @param upperBytes 1 = upper two bytes, 0 = lower two bytes
	 * @return the Short
	 */
	public static Short convert(int value, int upperBytes) {
		var b = ByteBuffer.allocate(4);
		b.order(ByteOrder.LITTLE_ENDIAN);
		b.putInt(value);

		var byte0 = b.get(upperBytes * 2);
		var byte1 = b.get(upperBytes * 2 + 1);

		var shortBuf = ByteBuffer.allocate(2);
		shortBuf.order(ByteOrder.LITTLE_ENDIAN);
		shortBuf.put(0, byte0);
		shortBuf.put(1, byte1);

		return shortBuf.getShort();
	}
}
