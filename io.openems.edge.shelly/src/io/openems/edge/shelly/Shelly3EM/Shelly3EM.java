package io.openems.edge.shelly.Shelly3EM;

import java.util.Collection;

import org.osgi.service.component.ComponentContext;

import com.google.gson.JsonObject;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.io.api.DigitalOutput;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.shelly.core.ShellyComponent;

public interface Shelly3EM extends DigitalOutput, SymmetricMeter, AsymmetricMeter, ShellyComponent {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Holds status of Relay.
		 * 
		 * <ul>
		 * <li>Interface: Shelly3EM
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		RELAY(new BooleanDoc() //
			  .accessMode(AccessMode.READ_WRITE)); //
		
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}
	
	/**
	 * Gets the Channel for {@link ChannelId#RELAY}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getRelayChannel() {
		return this.channel(ChannelId.RELAY);
	}

	/**
	 * Gets the Relay Output 1. See {@link ChannelId#RELAY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getRelay() {
		return this.getRelayChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#RELAY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setRelay(boolean value) {
		this.getRelayChannel().setNextValue(value);
	}

	/**
	 * Sets the Relay Output. See {@link ChannelId#RELAY}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setRelay(boolean value) throws OpenemsNamedException {
		this.getRelayChannel().setNextWriteValue(value);
	}

}
