/**
 * 
 */
package io.openems.edge.shelly.core;

import com.google.gson.JsonObject;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;


/**
 * @author scholty
 *
 */
public interface ShellyComponent  extends OpenemsComponent {
	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Slave Communication Failed Fault.
		 * 
		 * <ul>
		 * <li>Interface: Shelly25
		 * <li>Type: State
		 * </ul>
		 */
		SLAVE_COMMUNICATION_FAILED(Doc.of(Level.FAULT)); //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}
	
	/**
	 * Gets the Channel for {@link ChannelId#SLAVE_COMMUNICATION_FAILED}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getSlaveCommunicationFailedChannel() {
		return this.channel(ChannelId.SLAVE_COMMUNICATION_FAILED);
	}

	/**
	 * Gets the Slave Communication Failed State. See
	 * {@link ChannelId#SLAVE_COMMUNICATION_FAILED}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getSlaveCommunicationFailed() {
		return this.getSlaveCommunicationFailedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#SLAVE_COMMUNICATION_FAILED} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setSlaveCommunicationFailed(boolean value) {
		this.getSlaveCommunicationFailedChannel().setNextValue(value);
	}
	
	public Boolean wantsExtendedData();	
	public Boolean setBaseChannels();
	public Integer wantedIndex();	
	public void setExtendedData(JsonObject o);

}
