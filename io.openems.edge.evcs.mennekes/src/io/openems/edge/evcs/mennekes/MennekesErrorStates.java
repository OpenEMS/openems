package io.openems.edge.evcs.mennekes;

import io.openems.common.types.OptionsEnum;

public enum MennekesErrorStates implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	ERR_RCMB_TRIGGERED(0x1, "Residual current detected via sensor."), //
	ERR_VEHICLE_STATE_E(0x2, "Vehicle signals error."), //
	ERR_MODE3_DIDE_CHECK(0x4, "ReseVehicle diode check failed - tamper detection"), //
	ERR_MCB_TYPE2_TRIGGERED(0x8, "MCB of type 2 socket triggered."), //
	ERR_MCB_SCHUKO_TRIGGERED(0x10, "MCB of domestic socket triggered."), //
	ERR_RCD_TRIGGERED(0x20, "RCD triggered"), //
	ERR_CONTACTOR_WELD(0x40, "Contactor welded"), //
	ERR_BACKEND_DISCONNECTED(0x80, "Backend disconnected."), //
	ERR_ACTUATOR_LOCKING_FAILED(0x100, "Plug locking failed."), //
	ERR_ACTUATOR_LOCKING_WITHOUT_PLUG_FAILED(0x200, "Locking without plug error."), //
	ERR_ACTUATOR_STUCK(0x400, "Actuator stuck cannot unlock"), //
	ERR_ACTUATOR_DETECTION_FAILED(0x800, "Actuator detection failed"), //
	ERR_FW_UPDATE_RUNNING(0x1000, "FW Update in progress."), //
	ERR_TILT(0x2000, "The charge point is tilted."), //
	ERR_WRONG_CP_PR_WIRING(0x4000, "CP/PR wiring issue"), //
	ERR_TYPE2_OVERLOAD_THR_2(0x8000, "Car current overload, charging stopped."), //
	ERR_ACTUATOR_UNLOCKED_WHILE_CHARGING(0x10000, "Actuator unlocked while charging"), //
	ERR_TILT_PREVENT_CHARGING_UNTIL_REBOOT(0x20000, "The charge point was tilted and it is not allowed to charge until the\n"
			+ "charge point is rebooted"), //
	ERR_PIC24(0x40000, "PIC24 error."), //
	ERR_USB_STICK_HANDLING(0x80000, "USB stick handling in progress."), //
	ERR_INCORRECT_PHASE_INSTALLATION(0x100000, "Incorrect phase rotation direction detected."), //
	ERR_NO_POWER(0x200000, "No power on mains detected.")//
	;

	private final int value;
	private final String name;

	private MennekesErrorStates(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}

}
