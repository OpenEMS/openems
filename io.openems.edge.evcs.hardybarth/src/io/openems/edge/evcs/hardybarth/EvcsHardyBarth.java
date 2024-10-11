package io.openems.edge.evcs.hardybarth;

import java.util.function.Function;

import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.type.TypeUtils;

public interface EvcsHardyBarth {

	public static final double SCALE_FACTOR_MINUS_1 = 0.1;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		// TODO: Correct Type & Unit (Waiting for Manufacturer instructions)

		// EVSE
		RAW_EVSE_GRID_CURRENT_LIMIT(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE), "secc", "port0", "ci", "evse", //
				"basic", "grid_current_limit", "actual"), //
		RAW_PHASE_COUNT(Doc.of(OpenemsType.INTEGER), "secc", "port0", "ci", "evse", "basic", "phase_count"), //

		// CHARGE
		RAW_CHARGE_STATUS_PLUG(Doc.of(OpenemsType.STRING), "secc", "port0", "ci", "charge", "plug", "status"), //
		RAW_CHARGE_STATUS_CONTACTOR(Doc.of(OpenemsType.STRING), "secc", "port0", "ci", "charge", "contactor", "status"), //
		RAW_CHARGE_STATUS_PWM(Doc.of(OpenemsType.STRING), "secc", "port0", "ci", "charge", "pwm", "status"), //
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
		RAW_CHARGE_STATUS_CHARGEPOINT(Doc.of(OpenemsType.STRING), "secc", "port0", "ci", "charge", "cp", "status"), //

		// SALIA
		RAW_SALIA_CHARGE_MODE(Doc.of(OpenemsType.STRING), "secc", "port0", "salia", "chargemode"), //
		RAW_SALIA_CHANGE_METER(Doc.of(OpenemsType.STRING), "secc", "port0", "salia", "changemeter"), //
		RAW_SALIA_AUTHMODE(Doc.of(OpenemsType.STRING), "secc", "port0", "salia", "authmode"), //
		RAW_SALIA_FIRMWARESTATE(Doc.of(OpenemsType.STRING), "secc", "port0", "salia", "firmwarestate"), //
		RAW_SALIA_FIRMWAREPROGRESS(Doc.of(OpenemsType.STRING), "secc", "port0", "salia", "firmwareprogress"), //
		RAW_SALIA_PUBLISH(Doc.of(OpenemsType.STRING), "secc", "port0", "salia", "publish"), //

		// SESSION
		RAW_SESSION_STATUS_AUTHORIZATION(Doc.of(OpenemsType.STRING), "secc", "port0", "session",
				"authorization_status"), //
		RAW_SESSION_SLAC_STARTED(Doc.of(OpenemsType.STRING), "secc", "port0", "session", "slac_started"), //
		RAW_SESSION_AUTHORIZATION_METHOD(Doc.of(OpenemsType.STRING), "secc", "port0", "session",
				"authorization_method"), //

		// CONTACTOR
		RAW_CONTACTOR_HLC_TARGET(Doc.of(OpenemsType.STRING), "secc", "port0", "contactor", "state", "hlc_target"), //
		RAW_CONTACTOR_ACTUAL(Doc.of(OpenemsType.STRING), "secc", "port0", "contactor", "state", "actual"), //
		RAW_CONTACTOR_TARGET(Doc.of(OpenemsType.STRING), "secc", "port0", "contactor", "state", "target"), //
		RAW_CONTACTOR_ERROR(Doc.of(OpenemsType.STRING), "secc", "port0", "contactor", "error"), //

		// METERING - METER

		RAW_METER_SERIALNUMBER(Doc.of(OpenemsType.STRING), "secc", "port0", "metering", "meter", "serialnumber"), //
		RAW_METER_TYPE(Doc.of(OpenemsType.STRING), "secc", "port0", "metering", "meter", "type"), //
		METER_NOT_AVAILABLE(Doc.of(Level.WARNING) //
				.translationKey(EvcsHardyBarth.class, "noMeterAvailable")), //
		RAW_METER_AVAILABLE(new BooleanDoc()//
				.onChannelSetNextValue((hardyBarth, value) -> {
					var notAvailable = value.get() == null ? null : !value.get();
					hardyBarth.channel(EvcsHardyBarth.ChannelId.METER_NOT_AVAILABLE).setNextValue(notAvailable);
				}), "secc", "port0", "metering", "meter", "available"), //

		// METERING - POWER
		RAW_ACTIVE_POWER_L1(Doc.of(OpenemsType.LONG).unit(Unit.WATT), value -> {
			Double doubleValue = TypeUtils.getAsType(OpenemsType.DOUBLE, value);
			return TypeUtils.getAsType(OpenemsType.LONG, TypeUtils.multiply(doubleValue, SCALE_FACTOR_MINUS_1));
		}, "secc", "port0", "metering", "power", "active", "ac", "l1", "actual"), //

		RAW_ACTIVE_POWER_L2(Doc.of(OpenemsType.LONG).unit(Unit.WATT), value -> {
			Double doubleValue = TypeUtils.getAsType(OpenemsType.DOUBLE, value);
			return TypeUtils.getAsType(OpenemsType.LONG, TypeUtils.multiply(doubleValue, SCALE_FACTOR_MINUS_1));
		}, "secc", "port0", "metering", "power", "active", "ac", "l2", "actual"), //

		RAW_ACTIVE_POWER_L3(Doc.of(OpenemsType.LONG).unit(Unit.WATT), value -> {
			Double doubleValue = TypeUtils.getAsType(OpenemsType.DOUBLE, value);
			return TypeUtils.getAsType(OpenemsType.LONG, TypeUtils.multiply(doubleValue, SCALE_FACTOR_MINUS_1));
		}, "secc", "port0", "metering", "power", "active", "ac", "l3", "actual"), //

		// METERING - CURRENT
		RAW_ACTIVE_CURRENT_L1(Doc.of(OpenemsType.LONG).unit(Unit.MILLIAMPERE), "secc", "port0", "metering", "current",
				"ac", "l1", "actual"), //
		RAW_ACTIVE_CURRENT_L2(Doc.of(OpenemsType.LONG).unit(Unit.MILLIAMPERE), "secc", "port0", "metering", "current",
				"ac", "l2", "actual"), //
		RAW_ACTIVE_CURRENT_L3(Doc.of(OpenemsType.LONG).unit(Unit.MILLIAMPERE), "secc", "port0", "metering", "current",
				"ac", "l3", "actual"), //

		// METERING - ENERGY
		RAW_ACTIVE_ENERGY_TOTAL(Doc.of(OpenemsType.DOUBLE).unit(Unit.CUMULATED_WATT_HOURS), "secc", "port0", "metering",
				"energy", "active_total", "actual"), //
		RAW_ACTIVE_ENERGY_EXPORT(Doc.of(OpenemsType.DOUBLE).unit(Unit.CUMULATED_WATT_HOURS), "secc", "port0",
				"metering", "energy", "active_export", "actual"), //

		// EMERGENCY SHUTDOWN
		RAW_EMERGENCY_SHUTDOWN(Doc.of(OpenemsType.STRING), "secc", "port0", "emergency_shutdown"), //

		// RCD
		RAW_RCD_AVAILABLE(Doc.of(OpenemsType.BOOLEAN), "secc", "port0", "rcd", "recloser", "available"), //

		// PLUG LOCK
		RAW_PLUG_LOCK_STATE_ACTUAL(Doc.of(OpenemsType.STRING), "secc", "port0", "plug_lock", "state", "actual"), //
		RAW_PLUG_LOCK_STATE_TARGET(Doc.of(OpenemsType.STRING), "secc", "port0", "plug_lock", "state", "target"), //
		RAW_PLUG_LOCK_ERROR(Doc.of(OpenemsType.STRING), "secc", "port0", "plug_lock", "error"), //

		// CHARGE POINT
		RAW_CP_STATE(Doc.of(OpenemsType.STRING), "secc", "port0", "cp", "state"), //

		// DIODE PRESENT
		RAW_DIODE_PRESENT(Doc.of(OpenemsType.STRING), "secc", "port0", "diode_present"), //

		// CABLE CURRENT LIMIT
		RAW_CABLE_CURRENT_LIMIT(Doc.of(OpenemsType.STRING), "secc", "port0", "cable_current_limit"), //

		// VENTILATION
		RAW_VENTILATION_STATE_ACTUAL(Doc.of(OpenemsType.STRING), "secc", "port0", "ventilation", "state", "actual"), //
		RAW_VENTILATION_STATE_TARGET(Doc.of(OpenemsType.STRING), "secc", "port0", "ventilation", "state", "target"), //
		RAW_VENTILATION_AVAILABLE(Doc.of(OpenemsType.BOOLEAN), "secc", "port0", "ventilation", "available"), //

		// EV - PRESENT
		RAW_EV_PRESENT(Doc.of(OpenemsType.STRING), "secc", "port0", "ev_present"), //

		// CHARGING
		RAW_CHARGING(Doc.of(OpenemsType.STRING), "secc", "port0", "charging"), //

		// RFID
		RAW_RFID_AUTHORIZEREQ(Doc.of(OpenemsType.STRING), "secc", "port0", "rfid", "authorizereq"), //
		RAW_RFID_AVAILABLE(Doc.of(OpenemsType.BOOLEAN), "secc", "port0", "rfid", "available"), //

		// GRID CURRENT LIMIT
		RAW_GRID_CURRENT_LIMIT(Doc.of(OpenemsType.STRING), "secc", "port0", "grid_current_limit"), //

		// SLAC ERROR
		RAW_SLAC_ERROR(Doc.of(OpenemsType.STRING), "secc", "port0", "slac_error"), //

		// DEVICE
		RAW_DEVICE_PRODUCT(Doc.of(OpenemsType.STRING), "device", "product"), //
		RAW_DEVICE_MODELNAME(Doc.of(OpenemsType.STRING), "device", "modelname"), //
		RAW_DEVICE_HARDWARE_VERSION(Doc.of(OpenemsType.STRING), "device", "hardware_version"), //
		RAW_DEVICE_SOFTWARE_VERSION(Doc.of(OpenemsType.STRING), "device", "software_version"), //
		RAW_DEVICE_VCS_VERSION(Doc.of(OpenemsType.STRING), "device", "vcs_version"), //
		RAW_DEVICE_HOSTNAME(Doc.of(OpenemsType.STRING), "device", "hostname"), //
		RAW_DEVICE_MAC_ADDRESS(Doc.of(OpenemsType.STRING), "device", "mac_address"), //
		RAW_DEVICE_SERIAL(Doc.of(OpenemsType.LONG), "device", "serial"), //
		RAW_DEVICE_UUID(Doc.of(OpenemsType.STRING), "device", "uuid"), //
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