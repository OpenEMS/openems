package io.openems.edge.evcs.hardybarth;

import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.JsonUtils;
import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.evcs.hardybarth.HardyBarth.ChannelId;

public class HardyBarthWorker extends AbstractCycleWorker {

	private final HardyBarthImpl parent;
	private final HardyBarthApi api;

	public HardyBarthWorker(HardyBarthImpl parent, HardyBarthApi api) {
		this.parent = parent;
		this.api = api;
	}

	@Override
	protected void forever() throws OpenemsNamedException {

		try {
			JsonElement result = this.api.sendGetRequest("/api");

			/*
			 * JSON TREE
			 */
			JsonObject secc = JsonUtils.getAsJsonObject(result, "secc");
			JsonObject port0 = JsonUtils.getAsJsonObject(secc, "port0");

			// Ci - general evse
			JsonObject port0Ci = JsonUtils.getAsJsonObject(port0, "ci");
			JsonObject port0CiEvse = JsonUtils.getAsJsonObject(port0Ci, "evse");
			JsonObject port0CiEvseBasic = JsonUtils.getAsJsonObject(port0CiEvse, "basic");
			JsonObject port0CiEvseBasicGridLimit = JsonUtils.getAsJsonObject(port0CiEvseBasic,
					"gridCurrentLimit");			
			this.set(port0CiEvseBasicGridLimit, ChannelId.RAW_EVSE_GRID_CURRENT_LIMIT);
			this.set(port0CiEvseBasic, ChannelId.RAW_PHASE_COUNT);
			
			// Ci - charge information
			JsonObject port0CiCharge = JsonUtils.getAsJsonObject(port0Ci, "charge");
			JsonObject port0CiChargePlug = JsonUtils.getAsJsonObject(port0CiCharge, "plug");
			JsonObject port0CiChargeCp = JsonUtils.getAsJsonObject(port0CiCharge, "cp");
			JsonObject port0CiChargeContactor = JsonUtils.getAsJsonObject(port0CiCharge, "contactor");
			JsonObject port0CiChargePwm = JsonUtils.getAsJsonObject(port0CiCharge, "pwm");
			this.set(port0CiChargePlug, ChannelId.RAW_CHARGE_STATUS_PLUG);
			this.set(port0CiChargeCp, ChannelId.RAW_CHARGE_STATUS_CHARGEPOINT);
			this.set(port0CiChargeContactor, ChannelId.RAW_CHARGE_STATUS_CONTACTOR);
			this.set(port0CiChargePwm, ChannelId.RAW_CHARGE_STATUS_PWM);

			// General info's for version salia
			JsonObject port0Salia = JsonUtils.getAsJsonObject(port0, "salia");
			this.set(port0Salia, ChannelId.RAW_SALIA_CHARGE_MODE);
			this.set(port0Salia, ChannelId.RAW_SALIA_THERMAL);
			this.set(port0Salia, ChannelId.RAW_SALIA_MEM);
			this.set(port0Salia, ChannelId.RAW_SALIA_UPTIME);
			this.set(port0Salia, ChannelId.RAW_SALIA_LOAD);
			this.set(port0Salia, ChannelId.RAW_SALIA_CHARGEDATA);
			this.set(port0Salia, ChannelId.RAW_SALIA_AUTHMODE);
			this.set(port0Salia, ChannelId.RAW_SALIA_FIRMWARESTATE);
			this.set(port0Salia, ChannelId.RAW_SALIA_FIRMWAREPROGRESS);
			this.set(port0Salia, ChannelId.RAW_SALIA_PUBLISH);	

			// Session
			JsonObject port0Session = JsonUtils.getAsJsonObject(port0, "session");
			this.set(port0Session, ChannelId.RAW_SESSION_STATUS_AUTHORIZATION);
			this.set(port0Session, ChannelId.RAW_SESSION_SLAC_STARTED);
			this.set(port0Session, ChannelId.RAW_SESSION_AUTHORIZATION_METHOD);
			
			// Contactor
			JsonObject port0Contactor = JsonUtils.getAsJsonObject(port0, "contactor");
			JsonObject port0ContactorState = JsonUtils.getAsJsonObject(port0Contactor, "state");
			this.set(port0ContactorState, ChannelId.RAW_CONTACTOR_HLC_TARGET);
			this.set(port0ContactorState, ChannelId.RAW_CONTACTOR_ACTUAL);
			this.set(port0ContactorState, ChannelId.RAW_CONTACTOR_TARGET);
			this.set(port0Contactor, ChannelId.RAW_CONTACTOR_ERROR);
			
			// Metering
			JsonObject port0Metering = JsonUtils.getAsJsonObject(port0, "metering");
			JsonObject port0MeteringMeter = JsonUtils.getAsJsonObject(port0Metering, "meter");
			JsonObject port0MeteringPower = JsonUtils.getAsJsonObject(port0Metering, "power");
			JsonObject port0MeteringPowerActive = JsonUtils.getAsJsonObject(port0MeteringPower, "active");
			JsonObject port0MeteringPowerActiveAc = JsonUtils.getAsJsonObject(port0MeteringPowerActive, "ac");
			JsonObject port0MeteringPowerActiveAcL1 = JsonUtils.getAsJsonObject(port0MeteringPowerActiveAc,
					"l1");
			JsonObject port0MeteringPowerActiveAcL2 = JsonUtils.getAsJsonObject(port0MeteringPowerActiveAc,
					"l2");
			JsonObject port0MeteringPowerActiveAcL3 = JsonUtils.getAsJsonObject(port0MeteringPowerActiveAc,
					"l3");
			JsonObject port0MeteringPowerActivetotal = JsonUtils.getAsJsonObject(port0MeteringPower,
					"active_total");
			JsonObject port0MeteringCurrent = JsonUtils.getAsJsonObject(port0Metering, "current");
			JsonObject port0MeteringCurrentAc = JsonUtils.getAsJsonObject(port0MeteringCurrent, "ac");
			JsonObject port0MeteringCurrentAcL1 = JsonUtils.getAsJsonObject(port0MeteringCurrentAc, "l1");
			JsonObject port0MeteringCurrentAcL2 = JsonUtils.getAsJsonObject(port0MeteringCurrentAc, "l2");
			JsonObject port0MeteringCurrentAcL3 = JsonUtils.getAsJsonObject(port0MeteringCurrentAc, "l3");
			JsonObject port0MeteringEnergy = JsonUtils.getAsJsonObject(port0Metering, "energy");
			JsonObject port0MeteringEnergyActTotal = JsonUtils.getAsJsonObject(port0MeteringEnergy,
					"active_total");
			JsonObject port0MeteringEnergyActExport = JsonUtils.getAsJsonObject(port0MeteringEnergy,
					"activeExport");
			JsonObject port0MeteringEnergyActImport = JsonUtils.getAsJsonObject(port0MeteringEnergy,
					"active_import");
			
			this.set(port0MeteringMeter, ChannelId.RAW_METER_SERIALNUMBER);
			this.set(port0MeteringMeter, ChannelId.RAW_METER_TYPE);
			this.set(port0MeteringMeter, ChannelId.RAW_METER_AVAILABLE);
			this.set(port0MeteringPowerActiveAcL1, ChannelId.RAW_ACTIVE_POWER_L1);
			this.set(port0MeteringPowerActiveAcL2, ChannelId.RAW_ACTIVE_POWER_L2);
			this.set(port0MeteringPowerActiveAcL3, ChannelId.RAW_ACTIVE_POWER_L3);
			this.set(port0MeteringPowerActivetotal, ChannelId.RAW_ACTIVE_POWER_TOTAL);
			this.set(port0MeteringCurrentAcL1, ChannelId.RAW_ACTIVE_CURRENT_L1);
			this.set(port0MeteringCurrentAcL2, ChannelId.RAW_ACTIVE_CURRENT_L2);
			this.set(port0MeteringCurrentAcL3, ChannelId.RAW_ACTIVE_CURRENT_L3);
			this.set(port0MeteringCurrentAcL3, ChannelId.RAW_ACTIVE_CURRENT_L3);
			this.set(port0MeteringEnergyActTotal, ChannelId.RAW_ACTIVE_ENERGY_TOTAL);
			this.set(port0MeteringEnergyActExport, ChannelId.RAW_ACTIVE_ENERGY_EXPORT);
			this.set(port0MeteringEnergyActImport, ChannelId.RAW_ACTIVE_ENERGY_IMPORT);
			
			// RCD
			JsonObject port0Rcd = JsonUtils.getAsJsonObject(port0, "rcd");
			JsonObject port0RcdRecloser = JsonUtils.getAsJsonObject(port0Rcd, "recloser");
			this.set(port0RcdRecloser, ChannelId.RAW_RCD_AVAILABLE);
			
			// Plug
			JsonObject port0PlugLock = JsonUtils.getAsJsonObject(port0, "plugLock");
			JsonObject port0PlugLockState = JsonUtils.getAsJsonObject(port0PlugLock, "state");
			this.set(port0PlugLockState, ChannelId.RAW_PLUG_LOCK_STATE_ACTUAL);
			this.set(port0PlugLockState, ChannelId.RAW_PLUG_LOCK_STATE_TARGET);
			this.set(port0PlugLock, ChannelId.RAW_PLUG_LOCK_ERROR);

			// Charge Point
			JsonObject port0Cp = JsonUtils.getAsJsonObject(port0, "cp");
			JsonObject port0CpPwmState = JsonUtils.getAsJsonObject(port0Cp, "pwmState");
			this.set(port0CpPwmState, ChannelId.RAW_CP_PWM_STATE);
			this.set(port0Cp, ChannelId.RAW_CP_STATE);
			this.set(port0Cp, ChannelId.RAW_CP_DUTY_CYCLE);

			// Ventilation
			JsonObject port0Ventilation = JsonUtils.getAsJsonObject(port0, "ventilation");
			JsonObject port0VentilationState = JsonUtils.getAsJsonObject(port0Ventilation, "state");
			this.set(port0VentilationState, ChannelId.RAW_VENTILATION_STATE_ACTUAL);
			this.set(port0VentilationState, ChannelId.RAW_VENTILATION_STATE_TARGET);
			this.set(port0Ventilation, ChannelId.RAW_VENTILATION_AVAILABLE);

			// RFID
			JsonObject port0Rfid = JsonUtils.getAsJsonObject(port0, "rfid");
			this.set(port0Rfid, ChannelId.RAW_RFID_AUTHORIZEREQ);
			this.set(port0Rfid, ChannelId.RAW_RFID_AVAILABLE);

			// Port0 general
			this.set(port0, ChannelId.RAW_EMERGENCY_SHUTDOWN);
			this.set(port0, ChannelId.RAW_DIODE_PRESENT);
			this.set(port0, ChannelId.RAW_CABLE_CURRENT_LIMIT);
			this.set(port0, ChannelId.RAW_EV_PRESENT);
			this.set(port0, ChannelId.RAW_CHARGING);
			this.set(port0, ChannelId.RAW_GRID_CURRENT_LIMIT);
			this.set(port0, ChannelId.RAW_SLAC_ERROR);
			
			// Device
			JsonObject device = JsonUtils.getAsJsonObject(result, "device");
			this.set(device, ChannelId.RAW_DEVICE_PRODUCT);
			this.set(device, ChannelId.RAW_DEVICE_MODELNAME);
			this.set(device, ChannelId.RAW_DEVICE_HARDWARE_VERSION);
			this.set(device, ChannelId.RAW_DEVICE_SOFTWARE_VERSION);
			this.set(device, ChannelId.RAW_DEVICE_VCS_VERSION);
			this.set(device, ChannelId.RAW_DEVICE_HOSTNAME);
			this.set(device, ChannelId.RAW_DEVICE_MAC_ADDRESS);
			this.set(device, ChannelId.RAW_DEVICE_SERIAL);
			this.set(device, ChannelId.RAW_DEVICE_UUID);

		} catch (OpenemsException e) {
			 this.parent.logError(this.parent.log, "REST-Api failed: " + e.getMessage());
		}
	}

	private void set(JsonObject value, HardyBarth.ChannelId channelId) {
		Object setValue = null;
		String stringValue = JsonUtils.getAsOptionalString(value, channelId.getJsonName()).orElse(null);
		try {
			OpenemsType type = channelId.doc().getType();
			switch (type) {
			case BOOLEAN:
				Optional<Integer> boolOpt = JsonUtils.getAsOptionalInt(value, channelId.getJsonName());
				setValue = boolOpt.isPresent() ? boolOpt.get() == 1 : null;
				break;
			case DOUBLE:
				setValue = Double.parseDouble(stringValue);
				break;
			case FLOAT:
				setValue = Float.parseFloat(stringValue);
				break;
			case INTEGER:
				setValue = JsonUtils.getAsOptionalInt(value, channelId.getJsonName()).orElse(null);
				break;
			case LONG:
				setValue = Long.parseLong(stringValue);
				break;
			case SHORT:
				setValue = Short.parseShort(stringValue);
				break;
			case STRING:
				setValue = stringValue;
				break;
			}
		} catch (NumberFormatException e) {
			this.parent.debugLog("Parsing Error: " + e.getMessage());
		}

		this.parent.channel(channelId).setNextValue(setValue);
	}
}
