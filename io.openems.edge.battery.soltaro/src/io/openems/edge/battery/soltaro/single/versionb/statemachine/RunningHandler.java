package io.openems.edge.battery.soltaro.single.versionb.statemachine;

import java.time.LocalDateTime;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.soltaro.single.versionb.statemachine.StateMachine.State;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class RunningHandler extends StateHandler<State, Context> {

	public static final int refreshIntervalSeconds = 900;
	private LocalDateTime refreshTime = null;

	@Override
	protected void onExit(Context context) throws OpenemsNamedException {
		this.refreshTime = null;
		super.onExit(context);
	}

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		super.onEntry(context);
		this.refreshTime = LocalDateTime.now();
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		var battery = context.getParent();

		if (ControlAndLogic.hasError(battery, context.numberOfModules)) {
			return State.UNDEFINED;
		}

		if (!ControlAndLogic.isSystemRunning(battery)) {
			return State.UNDEFINED;
		}

		// Mark as started
		battery._setStartStop(StartStop.START);

		this.refreshBatteryValues(context);

		return State.RUNNING;
	}

	private void refreshBatteryValues(Context context) throws OpenemsNamedException {
		if (this.refreshTime.plusSeconds(refreshIntervalSeconds).isBefore(LocalDateTime.now())) {
			this.refreshTime = LocalDateTime.now();
			this.setBatteryValues(context);
		}

	}

	private void setBatteryValues(Context context) throws OpenemsNamedException {

		// TODO first step is only to check the values and if there is a difference show
		// a warning
		// TODO this should not be done only in Running, but tested on every cycle. It
		// might be that, because of wrong values the battery does not start properly,
		// which would look us out.

		// // 0x2086 ==> 2800
		// WriteChannel<Integer> channel =
		// context.component.channel(SingleRackVersionB.ChannelId.WARN_PARAMETER_CELL_UNDER_VOLTAGE_ALARM);
		// channel.setNextWriteValue(context.cellCharacteristic.getFinalCellDischargeVoltage_mV());
		//
		// // 0x2087 ==> 2850
		// channel =
		// context.component.channel(SingleRackVersionB.ChannelId.WARN_PARAMETER_CELL_UNDER_VOLTAGE_RECOVER);
		// channel.setNextWriteValue(context.cellCharacteristic.getFinalCellDischargeVoltage_mV()
		// + 50);
		//
		// // 0x2047 ==> 2750
		// channel =
		// context.component.channel(SingleRackVersionB.ChannelId.STOP_PARAMETER_CELL_UNDER_VOLTAGE_RECOVER);
		// channel.setNextWriteValue(context.cellCharacteristic.getForceChargeCellVoltage_mV());
		//
		// // 0x2046 ==> 2700
		// channel =
		// context.component.channel(SingleRackVersionB.ChannelId.STOP_PARAMETER_CELL_UNDER_VOLTAGE_PROTECTION);
		// channel.setNextWriteValue(context.cellCharacteristic.getForceChargeCellVoltage_mV()
		// - 50);
		//
		// // 0x2080 ==> 3650
		// channel =
		// context.component.channel(SingleRackVersionB.ChannelId.WARN_PARAMETER_CELL_OVER_VOLTAGE_ALARM);
		// channel.setNextWriteValue(context.cellCharacteristic.getFinalCellChargeVoltage_mV());
		//
		// // 0x2081 ==> 3600
		// channel =
		// context.component.channel(SingleRackVersionB.ChannelId.WARN_PARAMETER_CELL_OVER_VOLTAGE_RECOVER);
		// channel.setNextWriteValue(context.cellCharacteristic.getFinalCellChargeVoltage_mV()
		// - 50);
		//
		// // 0x2041 ==> 3680
		// channel =
		// context.component.channel(SingleRackVersionB.ChannelId.STOP_PARAMETER_CELL_OVER_VOLTAGE_RECOVER);
		// channel.setNextWriteValue(context.cellCharacteristic.getForceDischargeCellVoltage_mV());
		//
		// // 0x2040 ==> 3730
		// channel =
		// context.component.channel(SingleRackVersionB.ChannelId.STOP_PARAMETER_CELL_OVER_VOLTAGE_PROTECTION);
		// channel.setNextWriteValue(context.cellCharacteristic.getForceDischargeCellVoltage_mV()
		// + 50);
	}

}
