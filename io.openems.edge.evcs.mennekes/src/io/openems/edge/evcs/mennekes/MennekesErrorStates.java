package io.openems.edge.evcs.mennekes;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.evcs.mennekes.EvcsMennekes.ChannelId;

public enum MennekesErrorStates implements OptionsEnum {
	UNDEFINED(0x0, //
			"Undefined", null), //
	ERR_RCMB_TRIGGERED(0x1, //
			"Residual current detected via sensor", ChannelId.ERR_RCMB_TRIGGERED), //
	ERR_VEHICLE_STATE_E(0x2, //
			"Vehicle signals error", ChannelId.ERR_VEHICLE_STATE_E), //
	ERR_MODE3_DIDE_CHECK(0x4, //
			"ReseVehicle diode check failed - tamper detection", ChannelId.ERR_MODE3_DIODE_CHECK), //
	ERR_MCB_TYPE2_TRIGGERED(0x8, //
			"MCB of type 2 socket triggered", ChannelId.ERR_MCB_TYPE2_TRIGGERED), //
	ERR_MCB_SCHUKO_TRIGGERED(0x10, //
			"MCB of domestic socket triggered", ChannelId.ERR_MCB_SCHUKO_TRIGGERED), //
	ERR_RCD_TRIGGERED(0x20, //
			"RCD triggered", ChannelId.ERR_RCD_TRIGGERED), //
	ERR_CONTACTOR_WELD(0x40, //
			"Contactor welded", ChannelId.ERR_CONTACTOR_WELD), //
	ERR_BACKEND_DISCONNECTED(0x80, //
			"Backend disconnected", ChannelId.ERR_BACKEND_DISCONNECTED), //
	ERR_ACTUATOR_LOCKING_FAILED(0x100, //
			"Plug locking failed", ChannelId.ERR_ACTUATOR_LOCKING_FAILED), //
	ERR_ACTUATOR_LOCKING_WITHOUT_PLUG_FAILED(0x200, //
			"Locking without plug error", ChannelId.ERR_ACTUATOR_LOCKING_WITHOUT_PLUG_FAILED), //
	ERR_ACTUATOR_STUCK(0x400, //
			"Actuator stuck cannot unlock", ChannelId.ERR_ACTUATOR_STUCK), //
	ERR_ACTUATOR_DETECTION_FAILED(0x800, //
			"Actuator detection failed", ChannelId.ERR_ACTUATOR_DETECTION_FAILED), //
	ERR_FW_UPDATE_RUNNING(0x1000, //
			"FW Update in progress", ChannelId.ERR_FW_UPDATE_RUNNING), //
	ERR_TILT(0x2000, //
			"The charge point is tilted", ChannelId.ERR_TILT), //
	ERR_WRONG_CP_PR_WIRING(0x4000, //
			"CP/PR wiring issue", ChannelId.ERR_WRONG_CP_PR_WIRING), //
	ERR_TYPE2_OVERLOAD_THR_2(0x8000, //
			"Car current overload, charging stopped", ChannelId.ERR_TYPE2_OVERLOAD_THR_2), //
	ERR_ACTUATOR_UNLOCKED_WHILE_CHARGING(0x10000, //
			"Actuator unlocked while charging", ChannelId.ERR_ACTUATOR_UNLOCKED_WHILE_CHARGING), //
	ERR_TILT_PREVENT_CHARGING_UNTIL_REBOOT(0x20000, //
			"The charge point was tilted and it is not allowed to charge until the charge point is rebooted",
			ChannelId.ERR_TILT_PREVENT_CHARGING_UNTIL_REBOOT), //
	ERR_PIC24(0x40000, //
			"PIC24 error", ChannelId.ERR_PIC24), //
	ERR_USB_STICK_HANDLING(0x80000, //
			"USB stick handling in progress", ChannelId.ERR_USB_STICK_HANDLING), //
	ERR_INCORRECT_PHASE_INSTALLATION(0x100000, //
			"Incorrect phase rotation direction detected", ChannelId.ERR_INCORRECT_PHASE_INSTALLATION), //
	ERR_NO_POWER(0x200000, //
			"No power on mains detected", ChannelId.ERR_NO_POWER) //
	;

	private final int value;
	private final String name;
	private final ChannelId channel;

	private MennekesErrorStates(int value, String name, ChannelId channel) {
		this.value = value;
		this.name = name;
		this.channel = channel;
	}

	public ChannelId getChannel() {
		return this.channel;
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
