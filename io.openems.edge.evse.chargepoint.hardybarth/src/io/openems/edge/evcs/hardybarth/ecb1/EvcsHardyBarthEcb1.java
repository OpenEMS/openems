package io.openems.edge.evcs.hardybarth.ecb1;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.meter.api.ElectricityMeter;

public interface EvcsHardyBarthEcb1 extends OpenemsComponent, Evcs, ManagedEvcs, ElectricityMeter {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * ECB1 state ID.
		 *
		 * <ul>
		 * <li>5 = Charging
		 * <li>17 = Stopped / paused
		 * </ul>
		 */
		RAW_STATE_ID(Doc.of(OpenemsType.INTEGER) //
				.text("ECB1 state ID (5=charging, 17=stopped)")),

		/**
		 * ECB1 IEC 61851 charge-point state.
		 *
		 * <ul>
		 * <li>A = No vehicle connected
		 * <li>B = Vehicle connected, not charging
		 * <li>C = Charging
		 * <li>D = Charging with ventilation
		 * <li>E = Deactivated socket
		 * <li>F = Fault
		 * </ul>
		 */
		RAW_STATE(Doc.of(OpenemsType.STRING) //
				.text("ECB1 IEC 61851 state (A/B/C/D/E/F)")),

		/** ECB1 charge mode (e.g. "manual"). */
		RAW_MODE(Doc.of(OpenemsType.STRING) //
				.text("ECB1 charge mode")),

		/** Actual PWM current amplitude in Ampere. */
		RAW_CURRENT_PWM_AMP(Doc.of(OpenemsType.DOUBLE) //
				.unit(Unit.AMPERE) //
				.text("ECB1 current PWM amplitude")),

		/** Manual-mode current setpoint in Ampere. */
		RAW_MANUAL_MODE_AMP(Doc.of(OpenemsType.DOUBLE) //
				.unit(Unit.AMPERE) //
				.text("ECB1 manual-mode current setpoint")),

		/** True when a vehicle is plugged in. */
		RAW_CONNECTED(Doc.of(OpenemsType.BOOLEAN) //
				.text("Vehicle connected")),

		/** EVCC vendor name. */
		RAW_VENDOR(Doc.of(OpenemsType.STRING) //
				.text("EVCC vendor")),

		/** EVCC firmware version. */
		RAW_VERSION(Doc.of(OpenemsType.STRING) //
				.text("EVCC firmware version")),

		/** Meter serial number. */
		RAW_METER_SERIAL(Doc.of(OpenemsType.INTEGER) //
				.text("Meter serial number")),

		/** Meter vendor. */
		RAW_METER_VENDOR(Doc.of(OpenemsType.STRING) //
				.text("Meter vendor")),

		/** Meter type. */
		RAW_METER_TYPE(Doc.of(OpenemsType.STRING) //
				.text("Meter type"));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
}
