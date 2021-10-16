package io.openems.edge.shelly.core;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;

public interface ShellyCore extends OpenemsComponent, EventHandler {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
				
		/**
		 * SHELLY_TYPE.
		 * The string that describes the Shelly device
		 * 
		 * <ul>
		 * <li>Interface: ShellyCore
		 * <li>Type: String
		 * </ul>
		 */
		SHELLY_TYPE(Doc.of(OpenemsType.STRING)),
		
		/**
		 * Communication Failed Fault.
		 * 
		 * <ul>
		 * <li>Interface: ShellyCore
		 * <li>Type: State
		 * </ul>
		 */
		COMMUNICATION_FAILED(Doc.of(Level.FAULT));
		
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
	 * Gets the Channel for {@link ChannelId#COMMUNICATION_FAILED}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getCommunicationFailedChannel() {
		return this.channel(ChannelId.COMMUNICATION_FAILED);
	}
	
	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#COMMUNICATION_FAILED} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setCommunicationFailed(boolean value) {
		this.getCommunicationFailedChannel().setNextValue(value);
	}

	/**
	 * Gets the Slave Communication Failed State. See
	 * {@link ChannelId#COMMUNICATION_FAILED}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getCommunicationFailed() {
		return this.getCommunicationFailedChannel().value();
	}
	
	/**
	 * Gets the Channel for {@link ChannelId#SHELLY_TYPE}.
	 * 
	 * @return the Channel
	 */
	public default StringReadChannel getShellyTypeChannel() {
		return this.channel(ChannelId.SHELLY_TYPE);
	}
	
	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#SHELLY_TYPE} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setShellyType(String value) {
		this.getShellyTypeChannel().setNextValue(value);
	}
	
	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#SHELLY_TYPE} Channel.
	 * 
	 * @param value the next value
	 */
	public default Value<String> getShellyType() {
		return this.getShellyTypeChannel().value();
	}
	
	// Returns the API object for extended access to 
	// Parameters of the Shelly hardware
	public ShellyApi getApi();
	
	// Attaches a meter or IO object to the core so that it gets updated on a regular basis
	public void registerClient(ShellyComponent client);
}
