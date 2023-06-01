package io.openems.edge.battery.bmw;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.bmw.enums.BmsState;
import io.openems.edge.battery.bmw.enums.ErrorBits1;
import io.openems.edge.battery.bmw.enums.ErrorBits2;
import io.openems.edge.battery.bmw.enums.InfoBits;
import io.openems.edge.battery.bmw.enums.State;
import io.openems.edge.battery.bmw.enums.WarningBits1;
import io.openems.edge.battery.bmw.enums.WarningBits2;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.component.OpenemsComponent;

public interface BmwBattery extends OpenemsComponent, EventHandler {

	public static enum BmwChannelId implements io.openems.edge.common.channel.ChannelId {

		HEART_BEAT_DEBUG(Doc.of(OpenemsType.INTEGER)), //
		HEART_BEAT(new IntegerDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onChannelSetNextWriteMirrorToDebugChannel(BmwChannelId.HEART_BEAT_DEBUG)),

		BMS_STATE_COMMAND(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)), //

		BMS_STATE_COMMAND_RESET_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
		BMS_STATE_COMMAND_RESET(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onChannelSetNextWriteMirrorToDebugChannel(BmwChannelId.BMS_STATE_COMMAND_RESET_DEBUG)),

		BMS_STATE_COMMAND_CLEAR_ERROR_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
		BMS_STATE_COMMAND_CLEAR_ERROR(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onChannelSetNextWriteMirrorToDebugChannel(BmwChannelId.BMS_STATE_COMMAND_CLEAR_ERROR_DEBUG)),

		BMS_STATE_COMMAND_CLOSE_PRECHARGE_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
		BMS_STATE_COMMAND_CLOSE_PRECHARGE(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onChannelSetNextWriteMirrorToDebugChannel(BmwChannelId.BMS_STATE_COMMAND_CLOSE_PRECHARGE_DEBUG)),

		BMS_STATE_COMMAND_ERROR_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
		BMS_STATE_COMMAND_ERROR(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onChannelSetNextWriteMirrorToDebugChannel(BmwChannelId.BMS_STATE_COMMAND_ERROR_DEBUG)),

		BMS_STATE_COMMAND_CLOSE_CONTACTOR_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
		BMS_STATE_COMMAND_CLOSE_CONTACTOR(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onChannelSetNextWriteMirrorToDebugChannel(BmwChannelId.BMS_STATE_COMMAND_CLOSE_CONTACTOR_DEBUG)),

		BMS_STATE_COMMAND_WAKE_UP_FROM_STOP_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
		BMS_STATE_COMMAND_WAKE_UP_FROM_STOP(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onChannelSetNextWriteMirrorToDebugChannel(BmwChannelId.BMS_STATE_COMMAND_WAKE_UP_FROM_STOP_DEBUG)),

		BMS_STATE_COMMAND_ENABLE_BATTERY_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
		BMS_STATE_COMMAND_ENABLE_BATTERY(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onChannelSetNextWriteMirrorToDebugChannel(BmwChannelId.BMS_STATE_COMMAND_ENABLE_BATTERY_DEBUG)),

		OPERATING_STATE_INVERTER_DEBUG(Doc.of(OpenemsType.INTEGER)), //
		OPERATING_STATE_INVERTER(new IntegerDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onChannelSetNextWriteMirrorToDebugChannel(BmwChannelId.OPERATING_STATE_INVERTER_DEBUG)),

		DC_LINK_VOLTAGE_DEBUG(Doc.of(OpenemsType.INTEGER)), //
		DC_LINK_VOLTAGE(new IntegerDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.VOLT).onChannelSetNextWriteMirrorToDebugChannel(BmwChannelId.DC_LINK_VOLTAGE_DEBUG)),

		DC_LINK_CURRENT_DEBUG(Doc.of(OpenemsType.INTEGER)), //
		DC_LINK_CURRENT(new IntegerDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onChannelSetNextWriteMirrorToDebugChannel(BmwChannelId.DC_LINK_VOLTAGE_DEBUG)),

		OPERATION_MODE_REQUEST_GRANTED_DEBUG(Doc.of(OpenemsType.INTEGER)), //
		OPERATION_MODE_REQUEST_GRANTED(new IntegerDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onChannelSetNextWriteMirrorToDebugChannel(BmwChannelId.OPERATION_MODE_REQUEST_GRANTED_DEBUG)),

		OPERATION_MODE_REQUEST_CANCELED_DEBUG(Doc.of(OpenemsType.INTEGER)), //
		OPERATION_MODE_REQUEST_CANCELED(new IntegerDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onChannelSetNextWriteMirrorToDebugChannel(BmwChannelId.OPERATION_MODE_REQUEST_CANCELED_DEBUG)),

		CONNECTION_STRATEGY_HIGH_SOC_FIRST_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
		CONNECTION_STRATEGY_HIGH_SOC_FIRST(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onChannelSetNextWriteMirrorToDebugChannel(BmwChannelId.CONNECTION_STRATEGY_HIGH_SOC_FIRST_DEBUG)),

		CONNECTION_STRATEGY_LOW_SOC_FIRST_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
		CONNECTION_STRATEGY_LOW_SOC_FIRST(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onChannelSetNextWriteMirrorToDebugChannel(BmwChannelId.CONNECTION_STRATEGY_LOW_SOC_FIRST_DEBUG)),

		SYSTEM_TIME_DEBUG(Doc.of(OpenemsType.INTEGER)), //
		SYSTEM_TIME(new IntegerDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onChannelSetNextWriteMirrorToDebugChannel(BmwChannelId.SYSTEM_TIME_DEBUG)),

		// Read only channels
		LIFE_SIGN(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //

		BMS_STATE(Doc.of(BmsState.values()) //
				.accessMode(AccessMode.READ_ONLY)), //

		ERROR_BITS_1(Doc.of(ErrorBits1.values()) //
				.accessMode(AccessMode.READ_ONLY)), //

		ERROR_BITS_2(Doc.of(ErrorBits2.values()) //
				.accessMode(AccessMode.READ_ONLY)), //

		WARNING_BITS_1(Doc.of(WarningBits1.values()) //
				.accessMode(AccessMode.READ_ONLY)), //

		WARNING_BITS_2(Doc.of(WarningBits2.values()) //
				.accessMode(AccessMode.READ_ONLY)), //

		INFO_BITS(Doc.of(InfoBits.values()) //
				.accessMode(AccessMode.READ_ONLY)), //

		MAXIMUM_OPERATING_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.AMPERE)), //

		MINIMUM_OPERATING_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.AMPERE)), //

		MAXIMUM_OPERATING_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.VOLT)), //

		MINIMUM_OPERATING_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.VOLT)), //

		MAXIMUM_LIMIT_DYNAMIC_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.VOLT)), //

		MINIMUM_LIMIT_DYNAMIC_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.VOLT)), //

		NUMBER_OF_STRINGS_CONNECTED(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //

		NUMBER_OF_STRINGS_INSTALLED(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //

		SOC_ALL_STRINGS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.PERCENT)), //

		SOC_CONNECTED_STRINGS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.PERCENT)), //

		REMAINING_CHARGE_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.AMPERE_HOURS)), //

		REMAINING_DISCHARGE_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.AMPERE_HOURS)), //

		REMAINING_CHARGE_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.KILOWATT_HOURS)), //

		REMAINING_DISCHARGE_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.KILOWATT_HOURS)), //

		NOMINAL_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.KILOWATT_HOURS)), //

		TOTAL_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.KILOWATT_HOURS)), //

		NOMINAL_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.AMPERE_HOURS)), //

		TOTAL_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.AMPERE_HOURS)), //

		DC_VOLTAGE_CONNECTED_RACKS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.VOLT)), //

		DC_VOLTAGE_AVERAGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.VOLT)), //

		DC_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.MILLIAMPERE)), //

		AVERAGE_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //

		MINIMUM_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //

		MAXIMUM_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //

		AVERAGE_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.MILLIVOLT)), //

		INTERNAL_RESISTANCE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.MILLIOHM)), //

		INSULATION_RESISTANCE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.KILOOHM)), //

		CONTAINER_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //

		AMBIENT_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //

		HUMIDITY_CONTAINER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.PERCENT)), //

		MAXIMUM_LIMIT_DYNAMIC_CURRENT_HIGH_RES(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.MILLIAMPERE)), //

		MINIMUM_LIMIT_DYNAMIC_CURRENT_HIGH_RES(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.MILLIAMPERE)), //

		FULL_CYCLE_COUNT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //

		OPERATING_TIME_COUNT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.HOUR)), //

		COM_PRO_VERSION(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //

		SERIAL_NUMBER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //

		SOFTWARE_VERSION(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //

		STATE_MACHINE(Doc.of(State.values()) //
				.accessMode(AccessMode.READ_ONLY)), //
		;

		private final Doc doc;

		private BmwChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}

	}
}