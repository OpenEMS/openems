package io.openems.edge.evcs.hardybarth;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;

interface HardyBarth {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		// EVSE
		RAW_EVSE_GRID_CURRENT_LIMIT(Doc.of(OpenemsType.INTEGER) // TODO: Why are there some values duplicated
				.unit(Unit.AMPERE), "actual"), //
		RAW_PHASE_COUNT(Doc.of(OpenemsType.INTEGER), "phase_count"), //

		// CHARGE
		RAW_CHARGE_STATUS_PLUG(Doc.of(OpenemsType.STRING), "status"), //
		RAW_CHARGE_STATUS_CHARGEPOINT(Doc.of(OpenemsType.STRING), "status"), //
		RAW_CHARGE_STATUS_CONTACTOR(Doc.of(OpenemsType.STRING), "status"), //
		RAW_CHARGE_STATUS_PWM(Doc.of(OpenemsType.STRING), "status"), //

		// SALIA
		RAW_SALIA_CHARGE_MODE(Doc.of(OpenemsType.STRING), "chargemode"), // TODO: ENUM bzw. setzen wenn nicht manual
		RAW_SALIA_THERMAL(Doc.of(OpenemsType.STRING), "thermal"), //
		RAW_SALIA_MEM(Doc.of(OpenemsType.STRING), "mem"), //
		RAW_SALIA_UPTIME(Doc.of(OpenemsType.STRING), "uptime"), //
		RAW_SALIA_LOAD(Doc.of(OpenemsType.STRING), "load"), //
		RAW_SALIA_CHARGEDATA(Doc.of(OpenemsType.STRING), "chargedata"), //
		RAW_SALIA_AUTHMODE(Doc.of(OpenemsType.STRING), "authmode"), //
		RAW_SALIA_FIRMWARESTATE(Doc.of(OpenemsType.STRING), "firmwarestate"), //
		RAW_SALIA_FIRMWAREPROGRESS(Doc.of(OpenemsType.STRING), "firmwareprogress"), //
		RAW_SALIA_PUBLISH(Doc.of(OpenemsType.STRING), "publish"), //

		// SESSION
		RAW_SESSION_STATUS_AUTHORIZATION(Doc.of(OpenemsType.STRING), "authorization_status"), //
		RAW_SESSION_SLAC_STARTED(Doc.of(OpenemsType.STRING), "slac_started"), //
		RAW_SESSION_AUTHORIZATION_METHOD(Doc.of(OpenemsType.STRING), "slac_started"), //

		// CONTACTOR
		RAW_CONTACTOR_HLC_TARGET(Doc.of(OpenemsType.STRING), "hlc_target"), //
		RAW_CONTACTOR_ACTUAL(Doc.of(OpenemsType.STRING), "actual"), //
		RAW_CONTACTOR_TARGET(Doc.of(OpenemsType.STRING), "target"), //
		RAW_CONTACTOR_ERROR(Doc.of(OpenemsType.STRING), "error"), //

		// METERING - METER
		RAW_METER_SERIALNUMBER(Doc.of(OpenemsType.STRING), "serialnumber"), //
		RAW_METER_TYPE(Doc.of(OpenemsType.STRING), "type"), //
		RAW_METER_AVAILABLE(Doc.of(OpenemsType.BOOLEAN), "available"), //

		// METERING - POWER
		RAW_ACTIVE_POWER_L1(Doc.of(OpenemsType.STRING), "actual"), // TODO: Find out what Type & Unit it is
		RAW_ACTIVE_POWER_L2(Doc.of(OpenemsType.STRING), "actual"), //
		RAW_ACTIVE_POWER_L3(Doc.of(OpenemsType.STRING), "actual"), //
		RAW_ACTIVE_POWER_TOTAL(Doc.of(OpenemsType.STRING), "actual"), //

		// METERING - CURRENT
		RAW_ACTIVE_CURRENT_L1(Doc.of(OpenemsType.STRING), "actual"), //
		RAW_ACTIVE_CURRENT_L2(Doc.of(OpenemsType.STRING), "actual"), //
		RAW_ACTIVE_CURRENT_L3(Doc.of(OpenemsType.STRING), "actual"), //

		// METERING - ENERGY
		RAW_ACTIVE_ENERGY_TOTAL(Doc.of(OpenemsType.STRING), "actual"), //
		RAW_ACTIVE_ENERGY_EXPORT(Doc.of(OpenemsType.STRING), "actual"), //
		RAW_ACTIVE_ENERGY_IMPORT(Doc.of(OpenemsType.STRING), "actual"), //

		// EMERGENCY SHUTDOWN
		RAW_EMERGENCY_SHUTDOWN(Doc.of(OpenemsType.STRING), "emergency_shutdown"), //

		// RCD
		RAW_RCD_AVAILABLE(Doc.of(OpenemsType.BOOLEAN), "available"), //

		// PLUG LOCK
		RAW_PLUG_LOCK_STATE_ACTUAL(Doc.of(OpenemsType.STRING), "actual"), //
		RAW_PLUG_LOCK_STATE_TARGET(Doc.of(OpenemsType.STRING), "target"), //
		RAW_PLUG_LOCK_ERROR(Doc.of(OpenemsType.STRING), "error"), //

		// CHARGE POINT
		RAW_CP_PWM_STATE(Doc.of(OpenemsType.STRING), "actual"), //
		RAW_CP_STATE(Doc.of(OpenemsType.STRING), "state"), //
		RAW_CP_DUTY_CYCLE(Doc.of(OpenemsType.STRING), "duty_cycle"), //
		
		// DIODE PRESENT
		RAW_DIODE_PRESENT(Doc.of(OpenemsType.STRING), "diode_present"), //
		
		// CABLE CURRENT LIMIT
		RAW_CABLE_CURRENT_LIMIT(Doc.of(OpenemsType.STRING), "cable_current_limit"), //

		// VENTILATION
		RAW_VENTILATION_STATE_ACTUAL(Doc.of(OpenemsType.STRING), "actual"), //
		RAW_VENTILATION_STATE_TARGET(Doc.of(OpenemsType.STRING), "target"), //
		RAW_VENTILATION_AVAILABLE(Doc.of(OpenemsType.BOOLEAN), "available"), //

		// EV - PRESENT
		RAW_EV_PRESENT(Doc.of(OpenemsType.STRING), "ev_present"), //

		// CHARGING
		RAW_CHARGING(Doc.of(OpenemsType.STRING), "charging"), //

		// RFID
		RAW_RFID_AUTHORIZEREQ(Doc.of(OpenemsType.STRING), "authorizereq"), //
		RAW_RFID_AVAILABLE(Doc.of(OpenemsType.BOOLEAN), "available"), //

		// GRID CURRENT LIMIT
		RAW_GRID_CURRENT_LIMIT(Doc.of(OpenemsType.STRING), "grid_current_limit"), //

		// SLAC ERROR
		RAW_SLAC_ERROR(Doc.of(OpenemsType.STRING), "slac_error"), //

		// DEVICE
		RAW_DEVICE_PRODUCT(Doc.of(OpenemsType.STRING), "product"), //
		RAW_DEVICE_MODELNAME(Doc.of(OpenemsType.STRING), "modelname"), //
		RAW_DEVICE_HARDWARE_VERSION(Doc.of(OpenemsType.STRING), "hardware_version"), //
		RAW_DEVICE_SOFTWARE_VERSION(Doc.of(OpenemsType.STRING), "software_version"), //
		RAW_DEVICE_VCS_VERSION(Doc.of(OpenemsType.STRING), "vcs_version"), //
		RAW_DEVICE_HOSTNAME(Doc.of(OpenemsType.STRING), "hostname"), //
		RAW_DEVICE_MAC_ADDRESS(Doc.of(OpenemsType.STRING), "mac_address"), //
		RAW_DEVICE_SERIAL(Doc.of(OpenemsType.STRING), "serial"), //
		RAW_DEVICE_UUID(Doc.of(OpenemsType.STRING), "uuid"), //
		;

		private final Doc doc;
		private final String jsonName;
		
		private ChannelId(Doc doc, String jsonName) {
			this.doc = doc;
			this.jsonName = jsonName;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}

		public String getJsonName() {
			return this.jsonName;
		}
	}
}