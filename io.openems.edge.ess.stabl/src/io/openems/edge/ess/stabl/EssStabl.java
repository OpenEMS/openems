package io.openems.edge.ess.stabl;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;

public interface EssStabl extends OpenemsComponent, ManagedSymmetricEss, SymmetricEss, ModbusComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		COM_TIME_OUT_EMS(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.SECONDS)), // default 120 seconds, 0 deactivate the time out
		SOFTWARE_RESET(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)), //
		ACTIVATE_POWER_STAGE(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)), //

		GRID_TYPE(new IntegerDoc()//
				.accessMode(AccessMode.READ_WRITE) //
				.<EssStabl>onChannelChange((self, value) -> {
					final GridMode gridMode;
					if (!value.isDefined()) {
						gridMode = GridMode.UNDEFINED;
					} else if (value.get() == 0) {
						gridMode = GridMode.ON_GRID;
					} else {
						gridMode = GridMode.OFF_GRID;
					}
					self._setGridMode(gridMode);
				})), //
		SYSTEM_SIGNED_POWER_SET_POINT_AC(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.KILOVOLT_AMPERE)),

		ACTIVE_POWER_M0D_SETPOINT_STRING_1_MODULEX(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.WRITE_ONLY) //
				.unit(Unit.WATT)),
		ACTIVE_POWER_M0D_SETPOINT_STRING_2_MODULEX(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.WRITE_ONLY) //
				.unit(Unit.WATT)),
		ACTIVE_POWER_M0D_SETPOINT_STRING_3_MODULEX(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.WRITE_ONLY) //
				.unit(Unit.WATT)),

		SOC_MIN_SYSTEM(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOC_MAX_SYSTEM(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOC_AVG_SYSTEM(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		CURRENT_LIMIT_DISCHARGE_SYSTEM(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)), //
		CURRENT_LIMIT_CHARGE_SYSTEM(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)), //
		ACTUAL_MAIN_STATE(Doc.of(OpenemsType.INTEGER)), //
		AC_DC_ACTIVE_GRID_TYPE(Doc.of(OpenemsType.INTEGER)), //
		ACTUAL_POWER_AC_L1(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.KILOWATT)), //
		ACTUAL_POWER_AC_L2(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.KILOWATT)), //
		ACTUAL_POWER_AC_L3(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.KILOWATT)), //
		GRID_VOLTAGE_L1(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)), //
		GRID_VOLTAGE_L2(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)), //
		GRID_VOLTAGE_L3(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)), //

		INLET_AIR_TEMPERATURE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)), //
		MCU_CORE_TEMPERATURE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)), //

		TOTAL_ALARMS_CNT(Doc.of(OpenemsType.INTEGER)), //
		ALARMS(Doc.of(OpenemsType.INTEGER)), //
		ALARM1(Doc.of(OpenemsType.INTEGER)), //
		ALARM2(Doc.of(OpenemsType.INTEGER)), //
		ALARM3(Doc.of(OpenemsType.INTEGER)), //
		ALARM4(Doc.of(OpenemsType.INTEGER)), //
		ALARM5(Doc.of(OpenemsType.INTEGER)), //
		ALARM6(Doc.of(OpenemsType.INTEGER)), //
		ALARM7(Doc.of(OpenemsType.INTEGER)), //
		ALARM8(Doc.of(OpenemsType.INTEGER)), //
		ALARM9(Doc.of(OpenemsType.INTEGER)), //
		ALARM10(Doc.of(OpenemsType.INTEGER)), //
		ALARM11(Doc.of(OpenemsType.INTEGER)), //
		ALARM12(Doc.of(OpenemsType.INTEGER)), //
		ALARM13(Doc.of(OpenemsType.INTEGER)), //
		ALARM14(Doc.of(OpenemsType.INTEGER)), //
		ALARM15(Doc.of(OpenemsType.INTEGER)), //
		ALARM16(Doc.of(OpenemsType.INTEGER)), //
		ALARM17(Doc.of(OpenemsType.INTEGER)), //
		ALARM18(Doc.of(OpenemsType.INTEGER)), //
		ALARM19(Doc.of(OpenemsType.INTEGER)), //
		ALARM20(Doc.of(OpenemsType.INTEGER)), //

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

}