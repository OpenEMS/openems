package io.openems.edge.sungrow.ess;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.sungrow.ess.enums.ChargeDischargeCommand;
import io.openems.edge.sungrow.ess.enums.ControlMode;
import io.openems.edge.sungrow.ess.enums.EmsMode;

/**
 * Handler to apply the set active power to the Sungrow ESS depending on the configured {@link ControlMode}.
 */
public class ApplyPowerHandler {

	/**
	 * Applies the desired active power setpoint by setting the appropiate EMS_MODE
	 * and Charge/Discharge power.
	 * 
	 * @param parent          the {@link EssSungrowImpl}
	 * @param setActivePower  the active power setpoint
	 * @param controlMode     the configured {@link ContolMode}
	 * @param gridActivePower the grid active power
	 * @throws OpenemsNamedException on write error
	 */
	public synchronized void apply(EssSungrowImpl parent, int setActivePower, ControlMode controlMode,
			Value<Integer> gridActivePower) throws OpenemsNamedException {
		parent.channel(EssSungrow.ChannelId.SMART_MODE_NOT_WORKING_WITH_PID_FILTER) //
				.setNextValue(parent.power.isPidEnabled() && controlMode.equals(ControlMode.SMART));

		var result = switch (controlMode) {
		case INTERNAL -> handleInternalMode();
		case REMOTE -> handleRemoteMode(setActivePower);
		case SMART -> handleSmartMode(setActivePower, parent.getActivePower(), gridActivePower);
		default -> handleInternalMode();
		};

		parent.getEmsModeChannel().setNextWriteValue(result.emsMode);
		if (result.setActivePower > 0) {
			parent.getChargeDischargeCommandChannel().setNextWriteValue(ChargeDischargeCommand.DISCHARGE);
			parent.getChargeDischargePowerChannel().setNextWriteValue(result.setActivePower);
		} else {
			parent.getChargeDischargeCommandChannel().setNextWriteValue(ChargeDischargeCommand.CHARGE);
			parent.getChargeDischargePowerChannel().setNextWriteValue(-result.setActivePower);
		}
	}

	private static record Result(EmsMode emsMode, int setActivePower) {
	}

	private static Result handleInternalMode() {
		return new Result(EmsMode.SELF_CONSUMPTION, 0);
	}

	private static Result handleRemoteMode(int setActivePower) {
		return new Result(EmsMode.EXTERNAL_EMS_MODE, setActivePower);
	}

	private static Result handleSmartMode(int setActivePower, Value<Integer> essActivePower,
			Value<Integer> gridActivePower) {
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

		return handleRemoteMode(setActivePower);
	}
}