package io.openems.edge.io.opendtu.inverter;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;

import io.openems.edge.common.channel.Doc;

import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.SinglePhaseMeter;

public interface Opendtu
		extends SinglePhaseMeter, ElectricityMeter, OpenemsComponent, EventHandler {

	

    /**
     * Channel for setting the Power Limit.
     */
	public default WriteChannel<Integer> setPowerLimit() {
	    return this.channel(ChannelId.SET_POWER_LIMIT);
	}

    
	
	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Slave Communication Failed Fault.
		 *
		 * <ul>
		 * <li>Interface: Opendtu
		 * <li>Type: State
		 * </ul>
		 */
		/**
		 * Maximum Ever Actual Power.
		 *
		 * <ul>
		 * <li>Interface: Ess DC Charger
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: positive or '0'
		 * <li>Implementation Note: value is automatically derived from ACTUAL_POWER
		 * </ul>
		 */
		MAX_ACTUAL_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)), //
	
	    WARN_INVERTER_NOT_REACHABLE(Doc.of(Level.INFO) //
				.text("One or more Inverter is not reachable")),
		SLAVE_COMMUNICATION_FAILED(Doc.of(Level.FAULT)),
        LIMIT_STATUS(Doc.of(OpenemsType.STRING).text("Limit Status")),
        SET_POWER_LIMIT(Doc.of(OpenemsType.INTEGER).text("Set Power Limit Status").accessMode(AccessMode.READ_WRITE));
        

		
		
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
}
