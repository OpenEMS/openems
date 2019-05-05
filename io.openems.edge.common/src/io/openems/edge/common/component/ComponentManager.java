package io.openems.edge.common.component;

import java.util.List;

import io.openems.common.OpenemsConstants;
import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.EdgeConfig;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;

/**
 * A Service that provides access to OpenEMS-Components.
 */
public interface ComponentManager extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		CONFIG_NOT_ACTIVATED(Doc.of(Level.WARNING) //
				.text("A configured OpenEMS Component was not activated"));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets all enabled OpenEMS-Components.
	 * 
	 * @return a List of OpenEMS-Components
	 * @throws IllegalArgumentException if the Component was not found
	 */
	public List<OpenemsComponent> getComponents();

	/**
	 * Gets a OpenEMS-Component by its Component-ID.
	 * 
	 * @param componentId the Component-ID (e.g. "_sum")
	 * @param <T>         the typed Component
	 * @return the OpenEMS-Component
	 * @throws OpenemsNamedException if the Component was not found
	 */
	@SuppressWarnings("unchecked")
	public default <T extends OpenemsComponent> T getComponent(String componentId) throws OpenemsNamedException {
		if (componentId == OpenemsConstants.COMPONENT_MANAGER_ID) {
			return (T) this;
		}
		List<OpenemsComponent> components = this.getComponents();
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
	 * @return the EdgeConfig object
	 */
	public EdgeConfig getEdgeConfig();

}
