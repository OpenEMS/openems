package io.openems.edge.battery.soltaro.single.versionb.statemachine;

import java.time.LocalDateTime;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.soltaro.single.versionb.statemachine.StateMachine.State;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.statemachine.StateHandler;

public class RunningHandler extends StateHandler<State, Context> {

	public static int refreshIntervalSeconds = 900;
	LocalDateTime refreshTime = null;
	
	
	@Override
	protected void onExit(Context context) throws OpenemsNamedException {
		refreshTime = null;
		super.onExit(context);
	}

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		super.onEntry(context);
		refreshTime = LocalDateTime.now();
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		if (ControlAndLogic.hasError(context.component, context.config.numberOfSlaves())) {
			return State.UNDEFINED;
		}

		if (!ControlAndLogic.isSystemRunning(context.component)) {
			return State.UNDEFINED;
		}

		// Mark as started
		context.component._setStartStop(StartStop.START);
		
		refreshBatteryValues(context);

		return State.RUNNING;
	}

	private void refreshBatteryValues(Context context) throws OpenemsNamedException {
		if (refreshTime.plusSeconds(refreshIntervalSeconds).isBefore(LocalDateTime.now())) {
			refreshTime = LocalDateTime.now();	
			setBatteryValues(context);
		}
		
	}

	private void setBatteryValues(Context context) throws OpenemsNamedException {
		
		//TODO first step is only to check the values and if there is a difference show a warning
		
//			// 0x2086 ==> 2800
//			WriteChannel<Integer> channel = context.component.channel(SingleRackVersionB.ChannelId.WARN_PARAMETER_CELL_UNDER_VOLTAGE_ALARM);
//			channel.setNextWriteValue(context.cellCharacteristic.getFinalCellDischargeVoltage_mV());
//			
//			// 0x2087 ==> 2850
//			channel = context.component.channel(SingleRackVersionB.ChannelId.WARN_PARAMETER_CELL_UNDER_VOLTAGE_RECOVER);
//			channel.setNextWriteValue(context.cellCharacteristic.getFinalCellDischargeVoltage_mV() + 50);
//			
//			// 0x2047 ==> 2750
//			channel = context.component.channel(SingleRackVersionB.ChannelId.STOP_PARAMETER_CELL_UNDER_VOLTAGE_RECOVER);
//			channel.setNextWriteValue(context.cellCharacteristic.getForceChargeCellVoltage_mV());
//			
//			// 0x2046 ==> 2700
//			channel = context.component.channel(SingleRackVersionB.ChannelId.STOP_PARAMETER_CELL_UNDER_VOLTAGE_PROTECTION);
//			channel.setNextWriteValue(context.cellCharacteristic.getForceChargeCellVoltage_mV() - 50);
//			
//			// 0x2080 ==> 3650
//			channel = context.component.channel(SingleRackVersionB.ChannelId.WARN_PARAMETER_CELL_OVER_VOLTAGE_ALARM);
//			channel.setNextWriteValue(context.cellCharacteristic.getFinalCellChargeVoltage_mV());
//			
//			// 0x2081 ==> 3600
//			channel = context.component.channel(SingleRackVersionB.ChannelId.WARN_PARAMETER_CELL_OVER_VOLTAGE_RECOVER);
//			channel.setNextWriteValue(context.cellCharacteristic.getFinalCellChargeVoltage_mV() - 50);
//			
//			// 0x2041 ==> 3680
//			channel = context.component.channel(SingleRackVersionB.ChannelId.STOP_PARAMETER_CELL_OVER_VOLTAGE_RECOVER);
//			channel.setNextWriteValue(context.cellCharacteristic.getForceDischargeCellVoltage_mV());
//			
//			// 0x2040 ==> 3730
//			channel = context.component.channel(SingleRackVersionB.ChannelId.STOP_PARAMETER_CELL_OVER_VOLTAGE_PROTECTION);
//			channel.setNextWriteValue(context.cellCharacteristic.getForceDischargeCellVoltage_mV() + 50);
	}

}
