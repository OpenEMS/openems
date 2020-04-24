package io.openems.edge.battery.soltaro.single.versionc.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.soltaro.single.versionc.statemachine.StateMachine.Context;

public class GoConfiguration extends State.Handler {

//	private void configureSlaves() {
//	if (nextConfiguringProcess == ConfiguringProcess.NONE) {
//		nextConfiguringProcess = ConfiguringProcess.CONFIGURING_STARTED;
//	}
//
//	switch (nextConfiguringProcess) {
//	case CONFIGURING_STARTED:
//		System.out.println(" ===> CONFIGURING STARTED: setNumberOfModules() <===");
//		setNumberOfModules();
//		break;
//	case SET_ID_AUTO_CONFIGURING:
//		System.out.println(" ===> SET_ID_AUTO_CONFIGURING: setIdAutoConfiguring() <===");
//		setIdAutoConfiguring();
//		break;
//	case CHECK_ID_AUTO_CONFIGURING:
//		if (timeAfterAutoId != null) {
//			if (timeAfterAutoId.plusSeconds(delayAutoIdSeconds).isAfter(LocalDateTime.now())) {
//				break;
//			} else {
//				timeAfterAutoId = null;
//			}
//		}
//		System.out.println(" ===> CHECK_ID_AUTO_CONFIGURING: checkIdAutoConfiguring() <===");
//		checkIdAutoConfiguring();
//		break;
//	case SET_TEMPERATURE_ID_AUTO_CONFIGURING:
//		System.out.println(" ===> SET_TEMPERATURE_ID_AUTO_CONFIGURING: setTemperatureIdAutoConfiguring() <===");
//		setTemperatureIdAutoConfiguring();
//		break;
//	case CHECK_TEMPERATURE_ID_AUTO_CONFIGURING:
//		if (timeAfterAutoId != null) {
//			if (timeAfterAutoId.plusSeconds(delayAutoIdSeconds).isAfter(LocalDateTime.now())) {
//				break;
//			} else {
//				timeAfterAutoId = null;
//			}
//		}
//		System.out.println(" ===> CHECK_TEMPERATURE_ID_AUTO_CONFIGURING: checkTemperatureIdAutoConfiguring() <===");
//		checkTemperatureIdAutoConfiguring();
//		break;
//	case SET_VOLTAGE_RANGES:
//		System.out.println(" ===> SET_VOLTAGE_RANGES: setVoltageRanges() <===");
//		setVoltageRanges();
//
//		break;
//	case CONFIGURING_FINISHED:
//		System.out.println("====>>> Configuring successful! <<<====");
//
//		if (configuringFinished == null) {
//			nextConfiguringProcess = ConfiguringProcess.RESTART_AFTER_SETTING;
//		} else {
//			if (configuringFinished.plusSeconds(delayAfterConfiguringFinished).isAfter(LocalDateTime.now())) {
//				System.out.println(">>> Delay time after configuring!");
//			} else {
//				System.out.println("Delay time after configuring is over, reset system");
//				this.logInfo(this.log,
//						"Soltaro Rack Version C [CONFIGURING_FINISHED SYSTEM_RESET] is not implemented!");
//				this.resetSystem();
//			}
//		}
//		break;
//	case RESTART_AFTER_SETTING:
//		// A manual restart is needed
//		System.out.println("====>>>  Please restart system manually!");
//		break;
//	case NONE:
//		break;
//	}
//}
//
//private void setVoltageRanges() {
//	try {
//		IntegerWriteChannel level1OverVoltageChannel = this
//				.channel(SingleRackVersionC.ChannelId.LEVEL1_SYSTEM_OVER_VOLTAGE_PROTECTION);
//		level1OverVoltageChannel.setNextWriteValue(
//				this.config.numberOfSlaves() * ModuleParameters.LEVEL_1_TOTAL_OVER_VOLTAGE_MILLIVOLT.getValue());
//
//		IntegerWriteChannel level1OverVoltageChannelRecover = this
//				.channel(SingleRackVersionC.ChannelId.LEVEL1_SYSTEM_OVER_VOLTAGE_RECOVER);
//		level1OverVoltageChannelRecover.setNextWriteValue(this.config.numberOfSlaves()
//				* ModuleParameters.LEVEL_1_TOTAL_OVER_VOLTAGE_RECOVER_MILLIVOLT.getValue());
//
//		IntegerWriteChannel level1LowVoltageChannel = this
//				.channel(SingleRackVersionC.ChannelId.LEVEL1_SYSTEM_UNDER_VOLTAGE_PROTECTION);
//		level1LowVoltageChannel.setNextWriteValue(
//				this.config.numberOfSlaves() * ModuleParameters.LEVEL_1_TOTAL_LOW_VOLTAGE_MILLIVOLT.getValue());
//
//		IntegerWriteChannel level1LowVoltageChannelRecover = this
//				.channel(SingleRackVersionC.ChannelId.LEVEL1_SYSTEM_UNDER_VOLTAGE_RECOVER);
//		level1LowVoltageChannelRecover.setNextWriteValue(this.config.numberOfSlaves()
//				* ModuleParameters.LEVEL_1_TOTAL_LOW_VOLTAGE_RECOVER_MILLIVOLT.getValue());
//
//		IntegerWriteChannel level2OverVoltageChannel = this
//				.channel(SingleRackVersionC.ChannelId.LEVEL2_SYSTEM_OVER_VOLTAGE_PROTECTION);
//		level2OverVoltageChannel.setNextWriteValue(
//				this.config.numberOfSlaves() * ModuleParameters.LEVEL_2_TOTAL_OVER_VOLTAGE_MILLIVOLT.getValue());
//
//		IntegerWriteChannel level2OverVoltageChannelRecover = this
//				.channel(SingleRackVersionC.ChannelId.LEVEL2_SYSTEM_OVER_VOLTAGE_RECOVER);
//		level2OverVoltageChannelRecover.setNextWriteValue(this.config.numberOfSlaves()
//				* ModuleParameters.LEVEL_2_TOTAL_OVER_VOLTAGE_RECOVER_MILLIVOLT.getValue());
//
//		IntegerWriteChannel level2LowVoltageChannel = this
//				.channel(SingleRackVersionC.ChannelId.LEVEL2_SYSTEM_UNDER_VOLTAGE_PROTECTION);
//		level2LowVoltageChannel.setNextWriteValue(
//				this.config.numberOfSlaves() * ModuleParameters.LEVEL_2_TOTAL_LOW_VOLTAGE_MILLIVOLT.getValue());
//
//		IntegerWriteChannel level2LowVoltageChannelRecover = this
//				.channel(SingleRackVersionC.ChannelId.LEVEL2_SYSTEM_UNDER_VOLTAGE_RECOVER);
//		level2LowVoltageChannelRecover.setNextWriteValue(this.config.numberOfSlaves()
//				* ModuleParameters.LEVEL_2_TOTAL_LOW_VOLTAGE_RECOVER_MILLIVOLT.getValue());
//
//		nextConfiguringProcess = ConfiguringProcess.CONFIGURING_FINISHED;
//		configuringFinished = LocalDateTime.now();
//
//	} catch (OpenemsNamedException e) {
//		log.error("Setting voltage ranges not successful!");
//		// TODO Should throw Exception/write Warning-State-Channel
//	}
//}

//private void checkTemperatureIdAutoConfiguring() {
//	EnumWriteChannel channel = this.channel(SingleRackVersionC.ChannelId.AUTO_SET_SLAVES_TEMPERATURE_ID);
//	AutoSetFunction value = channel.value().asEnum();
//	switch (value) {
//	case FAILURE:
//		this.logError(this.log, "Auto set temperature slaves id failed! Start configuring process again!");
//		// Auto set failed, try again
//		this.nextConfiguringProcess = ConfiguringProcess.CONFIGURING_STARTED;
//		return;
//	case SUCCESS:
//		this.logInfo(this.log, "Auto set temperature slaves id succeeded!");
//		nextConfiguringProcess = ConfiguringProcess.SET_VOLTAGE_RANGES;
//		return;
//	case START_AUTO_SETTING:
//	case INIT_MODE:
//	case UNDEFINED:
//		// Waiting...
//		return;
//	}
//}

//private void setTemperatureIdAutoConfiguring() {
//	EnumWriteChannel channel = this.channel(SingleRackVersionC.ChannelId.AUTO_SET_SLAVES_TEMPERATURE_ID);
//	try {
//		channel.setNextWriteValue(AutoSetFunction.START_AUTO_SETTING);
//		this.timeAfterAutoId = LocalDateTime.now();
//		this.nextConfiguringProcess = ConfiguringProcess.CHECK_TEMPERATURE_ID_AUTO_CONFIGURING;
//	} catch (OpenemsNamedException e) {
//		// Set was not successful, it will be tried until it succeeded
//		this.logError(this.log, "Setting temperature id auto set not successful");
//	}
//}

//private void checkIdAutoConfiguring() {
//	EnumWriteChannel channel = this.channel(SingleRackVersionC.ChannelId.AUTO_SET_SLAVES_ID);
//	AutoSetFunction value = channel.value().asEnum();
//	switch (value) {
//	case FAILURE:
//		this.logError(this.log, "Auto set slaves id failed! Start configuring process again!");
//		// Auto set failed, try again
//		this.nextConfiguringProcess = ConfiguringProcess.CONFIGURING_STARTED;
//		return;
//	case SUCCESS:
//		this.logInfo(this.log, "Auto set slaves id succeeded!");
//		nextConfiguringProcess = ConfiguringProcess.SET_TEMPERATURE_ID_AUTO_CONFIGURING;
//		return;
//	case START_AUTO_SETTING:
//	case INIT_MODE:
//	case UNDEFINED:
//		// Waiting...
//		return;
//	}
//}

//private void setIdAutoConfiguring() {
//	EnumWriteChannel channel = this.channel(SingleRackVersionC.ChannelId.AUTO_SET_SLAVES_ID);
//	try {
//		channel.setNextWriteValue(AutoSetFunction.START_AUTO_SETTING);
//		this.timeAfterAutoId = LocalDateTime.now();
//		this.nextConfiguringProcess = ConfiguringProcess.CHECK_ID_AUTO_CONFIGURING;
//	} catch (OpenemsNamedException e) {
//		// Set was not successful, it will be tried until it succeeded
//		this.logError(this.log, "Setting slave numbers not successful");
//	}
//}

//private void setNumberOfModules() {
//	// Set number of modules
//	IntegerWriteChannel numberOfSlavesChannel = this
//			.channel(SingleRackVersionC.ChannelId.WORK_PARAMETER_PCS_COMMUNICATION_RATE);
//	try {
//		numberOfSlavesChannel.setNextWriteValue(this.config.numberOfSlaves());
//		nextConfiguringProcess = ConfiguringProcess.SET_ID_AUTO_CONFIGURING;
//	} catch (OpenemsNamedException e) {
//		// Set was not successful, it will be tried until it succeeded
//		this.logError(this.log, "Setting slave numbers not successful. Will try again till it succeeds");
//	}
//}

//private enum ConfiguringProcess {
//	NONE, CONFIGURING_STARTED, SET_ID_AUTO_CONFIGURING, CHECK_ID_AUTO_CONFIGURING,
//	SET_TEMPERATURE_ID_AUTO_CONFIGURING, CHECK_TEMPERATURE_ID_AUTO_CONFIGURING, SET_VOLTAGE_RANGES,
//	CONFIGURING_FINISHED, RESTART_AFTER_SETTING
//}

	/**
	 * Checks whether system has an undefined state, e.g. rack 1 & 2 are configured,
	 * but only rack 1 is running. This state can only be reached at startup coming
	 * from state undefined
	 * 
	 * @return boolean
	 */
//private boolean isSystemStatePending() {
//	return !isSystemRunning() && !isSystemStopped();
//}

	@Override
	public State getNextState(Context context) throws OpenemsNamedException {
		System.out.println("Stuck in GO_CONFIGURATION");
		return State.GO_CONFIGURATION;
	}

}
