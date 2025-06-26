package io.openems.edge.evcs.openwb.series2;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface EvcsOpenWBSeries2 extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/* Integration of Modbus register set: https://openwb.de/main/wp-content/uploads/2023/10/ModbusTCP-openWB-series2-Pro-1.pdf*/

		PLUGGED_STATE(Doc.of(OpenWBEnums.PluggedState.values()) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("State of the cable socket connection")),

		CHARGING_ACTIVE(Doc.of(OpenWBEnums.ChargingActiveState.values()) //
				.accessMode(AccessMode.READ_ONLY) //
				.text("State of the charging device")),

		CHARGE_ENERGY_SESSION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.text("Sum of charged energy for the current session in Wh")),

		ACTUAL_CURRENT_CONFIGURED(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.text("Configured current")),

		HARDWARE_TYPE(Doc.of(OpenWBEnums.HardwareType.values()) //
				.text("1 = series2, 2 = Pro")),

		APPLY_CURRENT_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE).accessMode(AccessMode.WRITE_ONLY) //
				.text("Maximum charging current limit")),

		PHASE_TARGET(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.text("Trigger phase Switch, 1 = one Phase or 3 = three Phase")),

		TRIGGER_PHASE_SWITCHING(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.text("1 = trigger Phase switching")),
		
		HEARTBEAT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE).accessMode(AccessMode.WRITE_ONLY) //
				.text("Configure Heartbeat, 0 = deactivated or 1 = activated. If heartbeat is\n"
						+ "enabled every read through modbus resets the heartbeat counter. If the\n"
						+ "counter is above 60 seconds the charging (if active) will be stopped.")),
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
	 * Gets the Channel for {@link ChannelId#APPLY_CURRENT_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getApplyCurrentLimitChannel() {
		return this.channel(ChannelId.APPLY_CURRENT_LIMIT);
	}

	/**
	 * Sets the charge current limit of the EVCS in [A] on
	 * {@link ChannelId#APPLY_CURRENT_LIMIT} Channel.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setApplyCurrentLimit(double value) throws OpenemsNamedException {
		this.getApplyCurrentLimitChannel().setNextWriteValue((int) (value * 100));//cA
	}

}
