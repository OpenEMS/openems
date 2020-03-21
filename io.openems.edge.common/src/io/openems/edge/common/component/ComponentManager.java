package io.openems.edge.common.component;

import java.time.Clock;
import java.util.List;

import io.openems.common.OpenemsConstants;
import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.EdgeConfig;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.jsonapi.JsonApi;

/**
 * A Service that provides access to OpenEMS-Components.
 */

// TODO rename to "Openems"
public interface ComponentManager extends OpenemsComponent, JsonApi {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		CONFIG_NOT_ACTIVATED(Doc.of(Level.WARNING) //
				.text("A configured OpenEMS Component was not activated")), //
		WAS_OUT_OF_MEMORY(Doc.of(Level.FAULT) //
				.text("OutOfMemory had happened. Found heap dump files.")),
		DEFAULT_CONFIGURATION_FAILED(Doc.of(Level.FAULT) //
				.text("Applying the default configuration failed.")),;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the OpenEMS Clock - either the real system clock or a mocked clock for
	 * simulations.
	 * 
	 * @return the Clock
	 */
	public Clock getClock();

	/**
	 * Gets all enabled OpenEMS-Components.
	 * 
	 * @return a List of OpenEMS-Components
	 * @throws IllegalArgumentException if the Component was not found
	 */
	public List<OpenemsComponent> getEnabledComponents();

	/**
	 * Gets all OpenEMS-Components.
	 * 
	 * @return a List of OpenEMS-Components
	 * @throws IllegalArgumentException if the Component was not found
	 */
	public List<OpenemsComponent> getAllComponents();

	/**
	 * Gets a OpenEMS-Component by its Component-ID. The Component is guaranteed to
	 * be enabled.
	 * 
	 * @param componentId the Component-ID (e.g. "_sum")
	 * @param <T>         the typed Component
	 * @return the OpenEMS-Component
	 * @throws OpenemsNamedException if the Component was not found
	 */
	@SuppressWarnings("unchecked")
	public default <T extends OpenemsComponent> T getComponent(String componentId) throws OpenemsNamedException {
		if (componentId.equals(OpenemsConstants.COMPONENT_MANAGER_ID)) {
			return (T) this;
		}
		List<OpenemsComponent> components = this.getEnabledComponents();
		for (OpenemsComponent component : components) {
			if (component.id().equals(componentId)) {
				return (T) component;
			}
		}
		throw OpenemsError.EDGE_NO_COMPONENT_WITH_ID.exception(componentId);
	}

	/**
	 * Gets a OpenEMS-Component by its Component-ID. Be careful, that the Component
	 * might not be 'enabled'. If in doubt, use {@link #getComponent(String)}
	 * instead.
	 * 
	 * @param componentId the Component-ID (e.g. "_sum")
	 * @param <T>         the typed Component
	 * @return the OpenEMS-Component
	 * @throws OpenemsNamedException if the Component was not found
	 */
	@SuppressWarnings("unchecked")
	public default <T extends OpenemsComponent> T getPossiblyDisabledComponent(String componentId)
			throws OpenemsNamedException {
		if (componentId == OpenemsConstants.COMPONENT_MANAGER_ID) {
			return (T) this;
		}
		List<OpenemsComponent> components = this.getAllComponents();
		for (OpenemsComponent component : components) {
			if (component.id().equals(componentId)) {
				return (T) component;
			}
		}
		throw OpenemsError.EDGE_NO_COMPONENT_WITH_ID.exception(componentId);
	}

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
		OpenemsComponent component = this.getComponent(channelAddress.getComponentId());
		return component.channel(channelAddress.getChannelId());
	}

	/**
	 * Gets the complete configuration of this OpenEMS Edge.
	 * 
	 * Internally updates updates the cache if necessary and publishes a
	 * CONFIG_UPDATE event on update.
	 * 
	 * @return the {@link EdgeConfig} object
	 */
	public EdgeConfig getEdgeConfig();

}
