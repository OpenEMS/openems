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
	public BatteryData getBatteryData();

	public InverterData getInverterData();

	public Status getStatusData();

	public boolean isConnected();

	public Settings getSettings();

	public VectisData getVectis();

	public EnergyMeter getEnergyMeter();

	public SystemInfo getSystemInfo();

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		COMMUNICATION_FAILED(Doc.of(Level.FAULT) //
				.text("Communication to KACO blueplanet hybrid 10 failed")), //
		USER_ACCESS_DENIED(Doc.of(Level.FAULT) //
				.text("KACO User Access denied")), //
		VERSION_COM(Doc.of(OpenemsType.FLOAT) //
				.text("Version of COM")), //
		SERIAL_NUMBER(Doc.of(OpenemsType.LONG) //
				.text("Serial-Number")), //
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
	 * Gets the Channel for {@link ChannelId#SERIAL_NUMBER}.
	 * 
	 * @return the Channel
	 */
	public default Channel<Long> getSerialNumberChannel() {
		return this.channel(ChannelId.SERIAL_NUMBER);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#SERIAL_NUMBER}
	 * Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setSerialNumber(Long value) {
		this.getSerialNumberChannel().setNextValue(value);
	}
}
