package io.openems.edge.kaco.blueplanet.hybrid10.ess;

import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.kaco.blueplanet.hybrid10.BatteryStatus;
import io.openems.edge.kaco.blueplanet.hybrid10.InverterStatus;

public interface KacoBlueplanetHybrid10Ess extends OpenemsComponent {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		COMMUNICATION_FAILED(Doc.of(Level.FAULT) //
				.text("Communication to KACO blueplanet hybrid 10 failed. "
						+ "Please check the network connection and the status of the inverter")), //

		BMS_VOLTAGE(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT)), //
		RISO(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.OHM)), //

		INVERTER_STATUS(Doc.of(InverterStatus.values())), //
		BATTERY_STATUS(Doc.of(BatteryStatus.values())), //
		POWER_MANAGEMENT_CONFIGURATION(Doc.of(PowerManagementConfiguration.values())), //

		EXTERNAL_CONTROL_FAULT(Doc.of(Level.FAULT) //
				.text("Inverter is not configured for External EMS")), //
		/**
		 * To be able to set Set-Points on KACO inverter starting from firmware version
		 * 8, it is required to change the user password to something different than
		 * 'user' via the KACO Hy-Sys tool.
		 */
		EXTERNAL_CONTROL_FAULT_VERSION_8(Doc.of(Level.FAULT) //
				.text("Starting with Firmware Version 8 KACO inverter cannot be controlled via FEMS")),

		NO_GRID_METER_DETECTED(Doc.of(Level.WARNING) //
				.text("No hy-switch Grid-Meter detected. Read-Only mode can not work correctly"));

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
	 * {@link ChannelId#USER_ACCESS_DENIED} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setCommunicationFailed(boolean value) {
		this.getCommunicationFailedChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#EXTERNAL_CONTROL_FAULT}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getExternalControlFaultChannel() {
		return this.channel(ChannelId.EXTERNAL_CONTROL_FAULT);
	}

	/**
	 * Gets the Run-Failed State. See {@link ChannelId#EXTERNAL_CONTROL_FAULT}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getExternalControlFault() {
		return this.getExternalControlFaultChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#EXTERNAL_CONTROL_FAULT} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setExternalControlFault(boolean value) {
		this.getExternalControlFaultChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#EXTERNAL_CONTROL_FAULT_VERSION_8}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getUserPasswordNotChangedWithExternalKacoVersion8() {
		return this.channel(ChannelId.EXTERNAL_CONTROL_FAULT_VERSION_8);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#EXTERNAL_CONTROL_FAULT_VERSION_8} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setUserPasswordNotChangedWithExternalKacoVersion8(boolean value) {
		this.getUserPasswordNotChangedWithExternalKacoVersion8().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#NO_GRID_METER_DETECTED}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getNoGridMeterDetectedChannel() {
		return this.channel(ChannelId.NO_GRID_METER_DETECTED);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#NO_GRID_METER_DETECTED} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setNoGridMeterDetected(boolean value) {
		this.getNoGridMeterDetectedChannel().setNextValue(value);
	}

}