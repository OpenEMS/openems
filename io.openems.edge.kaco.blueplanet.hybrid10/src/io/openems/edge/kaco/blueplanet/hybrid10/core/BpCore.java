package io.openems.edge.kaco.blueplanet.hybrid10.core;

import com.ed.data.BatteryData;
import com.ed.data.EnergyMeter;
import com.ed.data.InverterData;
import com.ed.data.Settings;
import com.ed.data.Status;
import com.ed.data.VectisData;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface BpCore extends OpenemsComponent {
	public BatteryData getBatteryData();

	public InverterData getInverterData();

	public Status getStatusData();

	public boolean isConnected();

	public Settings getSettings();

	public VectisData getVectis();

	public EnergyMeter getEnergyMeter();

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		COMMUNICATION_FAILED(Doc.of(Level.FAULT) //
				.text("Communication to KACO blueplanet hybrid 10 failed")); //
		;

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
	 * Gets the Slave Communication Failed State. See
	 * {@link ChannelId#COMMUNICATION_FAILED}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getCommunicationFailed() {
		return this.getCommunicationFailedChannel().value();
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

}
