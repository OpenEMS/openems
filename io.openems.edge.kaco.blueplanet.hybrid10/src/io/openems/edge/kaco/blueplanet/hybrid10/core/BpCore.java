package io.openems.edge.kaco.blueplanet.hybrid10.core;

import com.ed.data.BatteryData;
import com.ed.data.EnergyMeter;
import com.ed.data.InverterData;
import com.ed.data.Settings;
import com.ed.data.Status;
import com.ed.data.SystemInfo;
import com.ed.data.VectisData;

import io.openems.common.channel.Level;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface BpCore extends OpenemsComponent {

	/**
	 * Gets the {@link BatteryData}.
	 * 
	 * @return {@link BatteryData}
	 */
	public BatteryData getBatteryData();

	/**
	 * Gets the {@link InverterData}.
	 * 
	 * @return {@link InverterData}
	 */
	public InverterData getInverterData();

	/**
	 * Gets the {@link Status}.
	 * 
	 * @return {@link Status}
	 */
	public Status getStatusData();

	/**
	 * Is the Client connected?.
	 * 
	 * @return true if connected
	 */
	public boolean isConnected();

	/**
	 * Is the default user logged in?.
	 * 
	 * @return true if the default password for user was not changed
	 */
	public boolean isDefaultUser();

	/**
	 * Gets the {@link Settings}.
	 * 
	 * @return {@link Settings}
	 */
	public Settings getSettings();

	/**
	 * Gets the {@link VectisData}.
	 * 
	 * @return {@link VectisData}
	 */
	public VectisData getVectis();

	/**
	 * Gets the {@link EnergyMeter}.
	 * 
	 * @return {@link EnergyMeter}
	 */
	public EnergyMeter getEnergyMeter();

	/**
	 * Gets the {@link SystemInfo}.
	 * 
	 * @return {@link SystemInfo}
	 */
	public SystemInfo getSystemInfo();

	/**
	 * Gets the {@link StableVersion}.
	 * 
	 * @return {@link StableVersion}
	 */
	public StableVersion getStableVersion();

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		COMMUNICATION_FAILED(Doc.of(Level.FAULT) //
				.text("Communication to KACO blueplanet hybrid 10 failed. Please check the network connection and the status of the inverter")), //
		USER_ACCESS_DENIED(Doc.of(Level.FAULT) //
				/*
				 * Additional text when we are able to ensure the functionality of the external
				 * mode: Please forward the new user password (set in the KACO Hy-Sys tool) to
				 * our service department (service@fenecon.de)
				 */
				.text("KACO user access denied.")), //
		MULTIPLE_ACCESS(Doc.of(Level.FAULT) //
				.text("KACO is read by multiple devices. Authentication might fail because KACO is not able to handle multiple requests")), //
		VERSION_COM(Doc.of(OpenemsType.FLOAT) //
				.text("Version of COM")), ///
		STABLE_VERSION(Doc.of(StableVersion.values()) //
				.text("Stable Version")),
		SERIALNUMBER(Doc.of(OpenemsType.STRING) //
				.text("Serial-Number")),;

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
	 * {@link ChannelId#USER_ACCESS_DENIED} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setCommunicationFailed(boolean value) {
		this.getCommunicationFailedChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#USER_ACCESS_DENIED}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getUserAccessDeniedChannel() {
		return this.channel(ChannelId.USER_ACCESS_DENIED);
	}

	/**
	 * Gets the Slave Communication Failed State. See
	 * {@link ChannelId#USER_ACCESS_DENIED}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getUserAccessDenied() {
		return this.getUserAccessDeniedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#COMMUNICATION_FAILED} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setUserAccessDenied(boolean value) {
		this.getUserAccessDeniedChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#VERSION_COM}.
	 * 
	 * @return the Channel
	 */
	public default Channel<Float> getVersionComChannel() {
		return this.channel(ChannelId.VERSION_COM);
	}

	/**
	 * Gets the COM Version. See {@link ChannelId#VERSION_COM}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Float> getVersionCom() {
		return this.getVersionComChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#VERSION_COM}
	 * Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setVersionCom(Float value) {
		this.getVersionComChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SERIALNUMBER}.
	 * 
	 * @return the Channel
	 */
	public default Channel<String> getSerialnumberChannel() {
		return this.channel(ChannelId.SERIALNUMBER);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#SERIALNUMBER}
	 * Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setSerialnumber(String value) {
		this.getSerialnumberChannel().setNextValue(value);
	}
}
