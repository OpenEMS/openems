package io.openems.edge.sma.ess.sunnyboystorage;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;

public interface EssSmaSunnyBoyStorage {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Total energy delivered by the battery (charge + discharge combined).
		 *
		 * <ul>
		 * <li>Register 30513, uint64, FC3
		 * <li>Unit: Wh
		 * </ul>
		 */
		ENERGY_TOTAL(Doc.of(OpenemsType.LONG)//
				.unit(Unit.CUMULATED_WATT_HOURS)), //

		// --- Write channels (mapped to Modbus holding registers 40xxx via FC16) ---

		/**
		 * BMS operating mode (CmpBMS register 40236).
		 *
		 * <ul>
		 * <li>Register 40236, uint32, FC16
		 * <li>2289 = Charge battery
		 * <li>2290 = Discharge battery
		 * <li>2424 = Presetting (self-consumption, internal BMS)
		 * </ul>
		 */
		BMS_MODE(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.WRITE_ONLY)), //

		/**
		 * Minimum charge power sent to the inverter (CmpBMS.BatChaMinW).
		 *
		 * <ul>
		 * <li>Register 40793, uint32, FC16
		 * <li>Unit: W. Set to 0 for normal operation, equal to MAX_CHARGE_POWER to
		 * force a fixed charge power.
		 * </ul>
		 */
		MIN_CHARGE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.accessMode(AccessMode.WRITE_ONLY)), //

		/**
		 * Maximum charge power limit sent to the inverter (CmpBMS.BatChaMaxW).
		 *
		 * <ul>
		 * <li>Register 40795, uint32, FC16
		 * <li>Unit: W, Range: 0..2500
		 * </ul>
		 */
		MAX_CHARGE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.accessMode(AccessMode.WRITE_ONLY)), //

		/**
		 * Minimum discharge power sent to the inverter (CmpBMS.BatDschMinW).
		 *
		 * <ul>
		 * <li>Register 40797, uint32, FC16
		 * <li>Unit: W. Typically 0 (no minimum discharge enforced).
		 * </ul>
		 */
		MIN_DISCHARGE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.accessMode(AccessMode.WRITE_ONLY)), //

		/**
		 * Maximum discharge power limit sent to the inverter (CmpBMS.BatDschMaxW).
		 *
		 * <ul>
		 * <li>Register 40799, uint32, FC16
		 * <li>Unit: W, Range: 0..2500
		 * </ul>
		 */
		MAX_DISCHARGE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.accessMode(AccessMode.WRITE_ONLY)), //

		/**
		 * Grid power setpoint (CmpBMS.GridWSpt).
		 *
		 * <p>
		 * SMA sign convention: positive = import from grid (charging), negative =
		 * export to grid (discharging). This is the inverse of the OpenEMS ACTIVE_POWER
		 * convention; write {@code -activePower}.
		 *
		 * <ul>
		 * <li>Register 40801, int32, FC16
		 * <li>Unit: W
		 * </ul>
		 */
		GRID_POWER_SETPOINT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.accessMode(AccessMode.WRITE_ONLY)); //

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
