package io.openems.edge.evse.chargepoint.keba;

import static io.openems.common.channel.AccessMode.WRITE_ONLY;
import static io.openems.common.channel.Unit.MILLIAMPERE;
import static io.openems.common.types.OpenemsType.FLOAT;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.STRING;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.Unit;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.meter.api.ElectricityMeter;

public interface EvseChargePointKeba extends EvseChargePoint, ElectricityMeter, OpenemsComponent, EventHandler {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		PLUG(Doc.of(Plug.values())), //
		ERROR_CODE(Doc.of(INTEGER)), //
		SERIAL_NUMBER(Doc.of(INTEGER)), //
		PRODUCT_TYPE(Doc.of(INTEGER)), //
		FIRMWARE(Doc.of(STRING)), //
		ENERGY_SESSION(Doc.of(INTEGER)), //
		POWER_FACTOR(Doc.of(FLOAT)), //
		MAX_CHARGING_CURRENT(Doc.of(INTEGER)), //
		RFID(Doc.of(STRING)), //
		PHASE_SWITCH_SOURCE(Doc.of(INTEGER)), //
		PHASE_SWITCH_STATE(Doc.of(INTEGER)), //
		FAILSAFE_CURRENT_SETTING(Doc.of(INTEGER)), //
		FAILSAFE_TIMEOUT_SETTING(Doc.of(INTEGER)), //

		/*
		 * Write Registers
		 */
		DEBUG_SET_CHARGING_CURRENT(Doc.of(INTEGER) //
				.unit(MILLIAMPERE)), //
		SET_CHARGING_CURRENT(Doc.of(INTEGER) //
				.unit(MILLIAMPERE) //
				.accessMode(WRITE_ONLY) //
				.onChannelSetNextWriteMirrorToDebugChannel(DEBUG_SET_CHARGING_CURRENT)),
		SET_ENERGY_LIMIT(Doc.of(INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.accessMode(WRITE_ONLY)), //
		// TODO "0" to unlock plug
		SET_UNLOCK_PLUG(Doc.of(INTEGER) //
				.accessMode(WRITE_ONLY)), //
		DEBUG_SET_ENABLE(Doc.of(INTEGER)), //
		// TODO add OptionsEnum
		// 0: Disable charging station (Suspended mode)
		// 1: Enable charging station (Charging)
		SET_ENABLE(Doc.of(INTEGER) //
				.accessMode(WRITE_ONLY) //
				.onChannelSetNextWriteMirrorToDebugChannel(DEBUG_SET_ENABLE)),
		// TODO add OptionsEnum
		// 0: No phase toggle source is available
		// 1: Toggle via OCPP
		// 2: Direct toggle command via RESTAPI
		// 3: Toggle via Modbus
		// 4: Toggle via UDP
		SET_PHASE_SWITCH_TOGGLE(Doc.of(INTEGER) //
				.accessMode(WRITE_ONLY)), //
		// TODO add OptionsEnum
		// 0: 1 phase (default state)
		// 1: 3 phases
		SET_TRIGGER_PHASE_SWITCH(Doc.of(INTEGER) //
				.accessMode(WRITE_ONLY)), //
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
