package io.openems.edge.evcs.hardybarth;

import static io.openems.common.channel.Level.WARNING;
import static io.openems.common.channel.Unit.AMPERE;
import static io.openems.common.channel.Unit.CUMULATED_WATT_HOURS;
import static io.openems.common.types.OpenemsType.BOOLEAN;
import static io.openems.common.types.OpenemsType.DOUBLE;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.LONG;
import static io.openems.common.types.OpenemsType.STRING;

import java.util.function.Function;

import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.Doc;

public interface EvcsHardyBarth {

	public static final float SCALE_FACTOR_MINUS_1 = 0.1F;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		// TODO: Correct Type & Unit (Waiting for Manufacturer instructions)

		// EVSE
		RAW_EVSE_GRID_CURRENT_LIMIT(Doc.of(INTEGER) //
				.unit(AMPERE), //
				"secc", "port0", "ci", "evse", "basic", "grid_current_limit", "actual"), //
		RAW_PHASE_COUNT(Doc.of(INTEGER), //
				"secc", "port0", "ci", "evse", "basic", "phase_count"), //

		// CHARGE
		RAW_CHARGE_STATUS_PLUG(Doc.of(STRING), //
				"secc", "port0", "ci", "charge", "plug", "status"), //
		RAW_CHARGE_STATUS_CONTACTOR(Doc.of(STRING), //
				"secc", "port0", "ci", "charge", "contactor", "status"), //
		RAW_CHARGE_STATUS_PWM(Doc.of(STRING), //
				"secc", "port0", "ci", "charge", "pwm", "status"), //
		/**
		 * States of the Hardy Barth.
		 *
		 * <p>
		 * <ul>
		 * <li>A = Free (no EV connected)
		 * <li>B = EV connected, no charging (pause state)
		 * <li>C = Charging
		 * <li>D = Charging (With ventilation)
		 * <li>E = Deactivated Socket
		 * <li>F = Failure
		 * </ul>
		 */
		RAW_CHARGE_STATUS_CHARGEPOINT(Doc.of(STRING), //
				"secc", "port0", "ci", "charge", "cp", "status"), //

		// SALIA
		RAW_SALIA_CHARGE_MODE(Doc.of(STRING), //
				"secc", "port0", "salia", "chargemode"), //
		RAW_SALIA_CHANGE_METER(Doc.of(STRING), //
				"secc", "port0", "salia", "changemeter"), //
		RAW_SALIA_AUTHMODE(Doc.of(STRING), //
				"secc", "port0", "salia", "authmode"), //
		RAW_SALIA_FIRMWARESTATE(Doc.of(STRING), //
				"secc", "port0", "salia", "firmwarestate"), //
		RAW_SALIA_FIRMWAREPROGRESS(Doc.of(STRING), //
				"secc", "port0", "salia", "firmwareprogress"), //
		RAW_SALIA_PUBLISH(Doc.of(STRING), //
				"secc", "port0", "salia", "publish"), //

		// SESSION
		RAW_SESSION_STATUS_AUTHORIZATION(Doc.of(STRING), //
				"secc", "port0", "session", "authorization_status"), //
		RAW_SESSION_SLAC_STARTED(Doc.of(STRING), //
				"secc", "port0", "session", "slac_started"), //
		RAW_SESSION_AUTHORIZATION_METHOD(Doc.of(STRING), //
				"secc", "port0", "session", "authorization_method"), //

		// CONTACTOR
		RAW_CONTACTOR_HLC_TARGET(Doc.of(STRING), //
				"secc", "port0", "contactor", "state", "hlc_target"), //
		RAW_CONTACTOR_ACTUAL(Doc.of(STRING), //
				"secc", "port0", "contactor", "state", "actual"), //
		RAW_CONTACTOR_TARGET(Doc.of(STRING), //
				"secc", "port0", "contactor", "state", "target"), //
		RAW_CONTACTOR_ERROR(Doc.of(STRING), //
				"secc", "port0", "contactor", "error"), //

		// METERING - METER

		RAW_METER_SERIALNUMBER(Doc.of(STRING), //
				"secc", "port0", "metering", "meter", "serialnumber"), //
		RAW_METER_TYPE(Doc.of(STRING), //
				"secc", "port0", "metering", "meter", "type"), //
		METER_NOT_AVAILABLE(Doc.of(WARNING) //
				.translationKey(EvcsHardyBarth.class, "noMeterAvailable")), //
		RAW_METER_AVAILABLE(new BooleanDoc()//
				.onChannelSetNextValue((hardyBarth, value) -> {
					var notAvailable = value.get() == null ? null : !value.get();
					hardyBarth.channel(EvcsHardyBarth.ChannelId.METER_NOT_AVAILABLE).setNextValue(notAvailable);
				}), //
				"secc", "port0", "metering", "meter", "available"), //

		// METERING - ENERGY
		RAW_ACTIVE_ENERGY_TOTAL(Doc.of(DOUBLE) //
				.unit(CUMULATED_WATT_HOURS), //
				"secc", "port0", "metering", "energy", "active_total", "actual"), //
		RAW_ACTIVE_ENERGY_EXPORT(Doc.of(DOUBLE) //
				.unit(CUMULATED_WATT_HOURS), //
				"secc", "port0", "metering", "energy", "active_export", "actual"), //

		// EMERGENCY SHUTDOWN
		RAW_EMERGENCY_SHUTDOWN(Doc.of(STRING), //
				"secc", "port0", "emergency_shutdown"), //

		// RCD
		RAW_RCD_AVAILABLE(Doc.of(BOOLEAN), //
				"secc", "port0", "rcd", "recloser", "available"), //

		// PLUG LOCK
		RAW_PLUG_LOCK_STATE_ACTUAL(Doc.of(STRING), //
				"secc", "port0", "plug_lock", "state", "actual"), //
		RAW_PLUG_LOCK_STATE_TARGET(Doc.of(STRING), //
				"secc", "port0", "plug_lock", "state", "target"), //
		RAW_PLUG_LOCK_ERROR(Doc.of(STRING), //
				"secc", "port0", "plug_lock", "error"), //

		// CHARGE POINT
		RAW_CP_STATE(Doc.of(STRING), //
				"secc", "port0", "cp", "state"), //

		// DIODE PRESENT
		RAW_DIODE_PRESENT(Doc.of(STRING), //
				"secc", "port0", "diode_present"), //

		// CABLE CURRENT LIMIT
		RAW_CABLE_CURRENT_LIMIT(Doc.of(STRING), //
				"secc", "port0", "cable_current_limit"), //

		// VENTILATION
		RAW_VENTILATION_STATE_ACTUAL(Doc.of(STRING), //
				"secc", "port0", "ventilation", "state", "actual"), //
		RAW_VENTILATION_STATE_TARGET(Doc.of(STRING), //
				"secc", "port0", "ventilation", "state", "target"), //
		RAW_VENTILATION_AVAILABLE(Doc.of(BOOLEAN), //
				"secc", "port0", "ventilation", "available"), //

		// EV - PRESENT
		RAW_EV_PRESENT(Doc.of(STRING), //
				"secc", "port0", "ev_present"), //

		// CHARGING
		RAW_CHARGING(Doc.of(STRING), //
				"secc", "port0", "charging"), //

		// RFID
		RAW_RFID_AUTHORIZEREQ(Doc.of(STRING), //
				"secc", "port0", "rfid", "authorizereq"), //
		RAW_RFID_AVAILABLE(Doc.of(BOOLEAN), //
				"secc", "port0", "rfid", "available"), //

		// GRID CURRENT LIMIT
		RAW_GRID_CURRENT_LIMIT(Doc.of(STRING), //
				"secc", "port0", "grid_current_limit"), //

		// SLAC ERROR
		RAW_SLAC_ERROR(Doc.of(STRING), //
				"secc", "port0", "slac_error"), //

		// DEVICE
		RAW_DEVICE_PRODUCT(Doc.of(STRING), //
				"device", "product"), //
		RAW_DEVICE_MODELNAME(Doc.of(STRING), //
				"device", "modelname"), //
		RAW_DEVICE_HARDWARE_VERSION(Doc.of(STRING), //
				"device", "hardware_version"), //
		RAW_DEVICE_SOFTWARE_VERSION(Doc.of(STRING), //
				"device", "software_version"), //
		RAW_DEVICE_VCS_VERSION(Doc.of(STRING), //
				"device", "vcs_version"), //
		RAW_DEVICE_HOSTNAME(Doc.of(STRING), //
				"device", "hostname"), //
		RAW_DEVICE_MAC_ADDRESS(Doc.of(STRING), //
				"device", "mac_address"), //
		RAW_DEVICE_SERIAL(Doc.of(LONG), //
				"device", "serial"), //
		RAW_DEVICE_UUID(Doc.of(STRING), //
				"device", "uuid"), //
		;

		private final Doc doc;
		private final String[] jsonPaths;

		protected final Function<Object, Object> converter;

		private ChannelId(Doc doc, String... jsonPaths) {
			this(doc, value -> value, jsonPaths);
		}

		private ChannelId(Doc doc, Function<Object, Object> converter, String... jsonPaths) {
			this.doc = doc;
			this.converter = converter;
			this.jsonPaths = jsonPaths;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}

		/**
		 * Get the whole JSON path.
		 *
		 * @return Whole path.
		 */
		public String[] getJsonPaths() {
			return this.jsonPaths;
		}
	}
}