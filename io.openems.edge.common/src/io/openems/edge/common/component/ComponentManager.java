package io.openems.edge.common.component;

import java.time.Clock;
import java.util.List;

import org.osgi.framework.BundleContext;

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.EdgeConfig;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.jsonapi.JsonApi;

/**
 * A Service that provides access to OpenEMS-Components.
 */
public interface ComponentManager extends OpenemsComponent, JsonApi, ClockProvider {

	public static final String SINGLETON_SERVICE_PID = "Core.ComponentManager";
	public static final String SINGLETON_COMPONENT_ID = "_componentManager";

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		CONFIG_NOT_ACTIVATED(Doc.of(Level.FAULT) //
				.text("A configured OpenEMS Component was not activated")), //
		DUPLICATED_COMPONENT_ID(Doc.of(Level.FAULT) //
				.text("Configuration has duplicated Component-IDs")), //
		WAS_OUT_OF_MEMORY(Doc.of(Level.INFO) //
				.text("OutOfMemory had happened. Found heap dump files.")),
		DEFAULT_CONFIGURATION_FAILED(Doc.of(Level.FAULT) //
				.text("Applying the default configuration failed.")),;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the Channel for {@link ChannelId#CONFIG_NOT_ACTIVATED}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getConfigNotActivatedChannel() {
		return this.channel(ChannelId.CONFIG_NOT_ACTIVATED);
	}

	/**
	 * Gets the Config Not Activated Warning State. See
	 * {@link ChannelId#CONFIG_NOT_ACTIVATED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getConfigNotActivated() {
		return this.getConfigNotActivatedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CONFIG_NOT_ACTIVATED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setConfigNotActivated(Boolean value) {
		this.getConfigNotActivatedChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#DUPLICATED_COMPONENT_ID}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getDuplicatedComponentIdChannel() {
		return this.channel(ChannelId.DUPLICATED_COMPONENT_ID);
	}

	/**
	 * Gets the Duplicated Component-ID Fault State. See
	 * {@link ChannelId#DUPLICATED_COMPONENT_ID}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getDuplicatedComponentId() {
		return this.getDuplicatedComponentIdChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DUPLICATED_COMPONENT_ID} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDuplicatedComponentId(Boolean value) {
		this.getDuplicatedComponentIdChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#WAS_OUT_OF_MEMORY}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getWasOutOfMemoryChannel() {
		return this.channel(ChannelId.WAS_OUT_OF_MEMORY);
	}

	/**
	 * Gets the Was Out of Memory Fault State. See
	 * {@link ChannelId#WAS_OUT_OF_MEMORY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getWasOutOfMemory() {
		return this.getWasOutOfMemoryChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#WAS_OUT_OF_MEMORY}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setWasOutOfMemory(boolean value) {
		this.getWasOutOfMemoryChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#DEFAULT_CONFIGURATION_FAILED}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getDefaultConfigurationFailedChannel() {
		return this.channel(ChannelId.DEFAULT_CONFIGURATION_FAILED);
	}

	/**
	 * Gets the Default Configuration Failed State. See
	 * {@link ChannelId#DEFAULT_CONFIGURATION_FAILED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getDefaultConfigurationFailed() {
		return this.getDefaultConfigurationFailedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DEFAULT_CONFIGURATION_FAILED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDefaultConfigurationFailed(boolean value) {
		this.getDefaultConfigurationFailedChannel().setNextValue(value);
	}

	/**
	 * Gets the OpenEMS Clock - either the real system clock or a mocked clock for
	 * simulations.
	 *
	 * @return the Clock
	 */
	@Override
	public Clock getClock();

	/**
	 * Gets all enabled OpenEMS-Components.
	 * 
	 * <p>
	 * Be aware that via this method usage of the Component service is not tracked
	 * by the bundle's use count (See
	 * {@link BundleContext#getService(org.osgi.framework.ServiceReference)}). Make
	 * sure to use the references as shortly as possible.
	 * 
	 * @return a List of OpenEMS-Components
	 * @throws IllegalArgumentException if the Component was not found
	 */
	public List<OpenemsComponent> getEnabledComponents();

	/**
	 * Gets all enabled OpenEMS-Components of the given Type.
	 * 
	 * <p>
	 * Be aware that via this method usage of the Component service is not tracked
	 * by the bundle's use count (See
	 * {@link BundleContext#getService(org.osgi.framework.ServiceReference)}). Make
	 * sure to use the references as shortly as possible.
	 *
	 * @param <T>   the given Type, subclass of {@link OpenemsComponent}
	 * @param clazz the given Type, subclass of {@link OpenemsComponent}
	 * @return a List of OpenEMS-Components
	 */
	public <T extends OpenemsComponent> List<T> getEnabledComponentsOfType(Class<T> clazz);

	/**
	 * Gets all OpenEMS-Components.
	 * 
	 * <p>
	 * Be aware that via this method usage of the Component service is not tracked
	 * by the bundle's use count (See
	 * {@link BundleContext#getService(org.osgi.framework.ServiceReference)}). Make
	 * sure to use the references as shortly as possible.
	 * 
	 * @return a List of OpenEMS-Components
	 * @throws IllegalArgumentException if the Component was not found
	 */
	public List<OpenemsComponent> getAllComponents();

	/**
	 * Gets a OpenEMS-Component by its Component-ID. The Component is guaranteed to
	 * be enabled.
	 * 
	 * <p>
	 * Be aware that via this method usage of the Component service is not tracked
	 * by the bundle's use count (See
	 * {@link BundleContext#getService(org.osgi.framework.ServiceReference)}). Make
	 * sure to use the references as shortly as possible.
	 *
	 * @param componentId the Component-ID (e.g. "_sum")
	 * @param <T>         the typed Component
	 * @return the OpenEMS-Component
	 * @throws OpenemsNamedException if the Component was not found
	 */
	public <T extends OpenemsComponent> T getComponent(String componentId) throws OpenemsNamedException;

	/**
	 * Gets a OpenEMS-Component by its Component-ID. Be careful, that the Component
	 * might not be 'enabled'. If in doubt, use {@link #getComponent(String)}
	 * instead.
	 *
	 * <p>
	 * Be aware that via this method usage of the Component service is not tracked
	 * by the bundle's use count (See
	 * {@link BundleContext#getService(org.osgi.framework.ServiceReference)}). Make
	 * sure to use the references as shortly as possible.
	 * 
	 * @param componentId the Component-ID (e.g. "_sum")
	 * @param <T>         the typed Component
	 * @return the OpenEMS-Component
	 * @throws OpenemsNamedException if the Component was not found
	 */
	public <T extends OpenemsComponent> T getPossiblyDisabledComponent(String componentId) throws OpenemsNamedException;

	/**
	 * Gets a Channel by its Channel-Address.
	 *
	 * @param channelAddress the Channel-Address
	 * @param <T>            the typed Channel
	 * @return the Channel
	 * @throws IllegalArgumentException if the Channel is not available
	 * @throws OpenemsNamedException    on error
	 */
	public default <T extends Channel<?>> T getChannel(ChannelAddress channelAddress)
			throws IllegalArgumentException, OpenemsNamedException {
		var component = this.getComponent(channelAddress.getComponentId());
		return component.channel(channelAddress.getChannelId());
	}

	/**
	 * Gets the complete configuration of this OpenEMS Edge.
	 *
	 * <p>
	 * Internally updates the cache if necessary and publishes a CONFIG_UPDATE event
	 * on update.
	 *
	 * @return the {@link EdgeConfig} object
	 */
	public EdgeConfig getEdgeConfig();

}