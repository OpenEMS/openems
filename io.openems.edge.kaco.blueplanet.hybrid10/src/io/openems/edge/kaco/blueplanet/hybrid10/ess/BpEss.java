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

public interface BpEss extends OpenemsComponent {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		BMS_VOLTAGE(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT)), //
		RISO(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.OHM)), //
		SURPLUS_FEED_IN(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //

		INVERTER_STATUS(Doc.of(InverterStatus.values())), //
		BATTERY_STATUS(Doc.of(BatteryStatus.values())), //
		POWER_MANAGEMENT_CONFIGURATION(Doc.of(PowerManagementConfiguration.values())), //

		EXTERNAL_CONTROL_FAULT(Doc.of(Level.FAULT) //
				.text("Inverter is not configured for External EMS")) //
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

}