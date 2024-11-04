package io.openems.edge.ess.sma.stpxx3se.batteryinverter;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.ess.sma.stpxx3se.battery.SmaBattery;
import io.openems.edge.ess.sma.enums.SetControlMode;

public class ApplyPowerHandler {

	private final BatteryInverterSmaStpSeImpl parent;

	public ApplyPowerHandler(BatteryInverterSmaStpSeImpl parent) {
		this.parent = parent;
	}

	/**
	 * Applies the desired active power and reactive power setpoints.
	 * 
	 * @param battery           the {@link SmaBattery}
	 * @param setActivePower    the active power setpoint
	 * @param setReactivePower  the reactive power setpoint
	 * @param controlmode       the configured {@link ControlMode}
	 * @param gridActivePower   the grid active power
	 * @param essActivePower the discharge power of the inverter
	 * @throws OpenemsNamedException on error
	 */
	public synchronized void apply(SmaBattery battery, int setActivePower, int setReactivePower,
			ControlMode controlmode, Value<Integer> gridActivePower, Value<Integer> essActivePower)
			throws OpenemsNamedException {
		
		this.parent.channel(BatteryInverterSmaStpSe.ChannelId.SMART_MODE_NOT_WORKING_WITH_PID_FILTER) //
				.setNextValue(this.parent.power.isPidEnabled() && controlmode.equals(ControlMode.SMART));
		
		var result = switch (controlmode) {
		case INTERNAL -> handleInternalMode();
		case REMOTE -> handleRemoteMode(setActivePower, setReactivePower);
		case SMART -> handleSmartMode(setActivePower, setReactivePower, gridActivePower, essActivePower);
		};
		
		this.parent._setDebugControlMode(result.controlMode);

		EnumWriteChannel setControlMode = battery.channel(SmaBattery.ChannelId.SET_CONTROL_MODE);
		IntegerWriteChannel setActivePowerChannel = battery.channel(SmaBattery.ChannelId.SET_ACTIVE_POWER);
		IntegerWriteChannel setReactivePowerChannel = battery.channel(SmaBattery.ChannelId.SET_REACTIVE_POWER);

		setControlMode.setNextWriteValue(result.controlMode);
		setActivePowerChannel.setNextWriteValue(result.setActivePower);
		setReactivePowerChannel.setNextWriteValue(result.setReactivePower);
	}

	private static record Result(SetControlMode controlMode, int setActivePower, int setReactivePower) {
	}

	private static Result handleInternalMode() {
		return new Result(SetControlMode.STOP, 0, 0);
	}

	private static Result handleRemoteMode(int setActivePower, int setReactivePower) {
		return new Result(SetControlMode.START, setActivePower, setReactivePower);
	}

	private static Result handleSmartMode(int setActivePower, int setReactivePower, Value<Integer> gridActivePower,
			Value<Integer> essActivePower) {

		// Fallback to internal mode if a value is undefined
		if (!gridActivePower.isDefined() || !essActivePower.isDefined()) {
			return handleInternalMode();
		}

		// Is balancing to zero active?
		var diffBalancing = setActivePower - (gridActivePower.get() + essActivePower.get());
		// avoid rounding errors
		if (Math.abs(diffBalancing) <= 1) {
			return handleInternalMode();
		}

		return handleRemoteMode(setActivePower, setReactivePower);
	}

}
