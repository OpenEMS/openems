//package io.openems.edge.ess.mr.gridcon.helper;
//
//import java.lang.annotation.Annotation;
//
//import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
//import io.openems.edge.battery.soltaro.controller.Config;
//import io.openems.edge.battery.soltaro.controller.State;
//import io.openems.edge.battery.soltaro.controller.state.BaseState;
//
//public class Creator {
//
//	public static final int CHARGE_POWER_PERCENT = 20;
//	public static final int CHARGING_TIME = 2;
//	public static final String ESS_ID = "ess0";
//	public static final String BMS_ID = "bms0";
//	public static final boolean ENABLED = true;
//	public static final int WARNING_LOW_CELL_VOLTAGE = 2900;
//	public static final int CRITICAL_LOW_CELL_VOLTAGE = 2800;
//	public static final int CRITICAL_HIGH_CELL_VOLTAGE = 3650;
//
//	public static final int FORCE_CHARGE_REACHABLE_MIN_CELL_VOLTAGE = 3100;
//
//	public static final int WARNING_SOC = 10;
//	public static final int CRITICAL_SOC = 5;
//
//	public static final int DELTA_SOC = 5;
//
//	public static final int LOW_TEMPERATURE = 0;
//	public static final int HIGH_TEMPERATURE = 60;
//
//	public static final long UNUSED_TIME = 2;
//
//	public static final String ID = "ctrl0";
//
//	public static BaseState createBaseState(DummyEss ess, DummyBattery bms) {
//		return new BaseState(ess, bms) {
//			@Override
//			public State getState() {
//				return null;
//			}
//
//			@Override
//			public State getNextState() {
//				return null;
//			}
//
//			@Override
//			public void act() throws OpenemsNamedException {
//			}
//		};
//	}
//
//	public static Config createConfig() {
//
//		return new Config() {
//
//			@Override
//			public Class<? extends Annotation> annotationType() {
//				return null;
//			}
//
//			@Override
//			public String webconsole_configurationFactory_nameHint() {
//				return null;
//			}
//
//			@Override
//			public int warningLowCellVoltage() {
//				return WARNING_LOW_CELL_VOLTAGE;
//			}
//
//			@Override
//			public String id() {
//				return ID;
//			}
//
//			@Override
//			public String ess_id() {
//				return ESS_ID;
//			}
//
//			@Override
//			public boolean enabled() {
//				return ENABLED;
//			}
//
//			@Override
//			public int chargingTime() {
//				return CHARGING_TIME;
//			}
//
//			@Override
//			public int chargePowerPercent() {
//				return CHARGE_POWER_PERCENT;
//			}
//
//			@Override
//			public String alias() {
//				return null;
//			}
//
//			@Override
//			public int criticalLowCellVoltage() {
//				return CRITICAL_LOW_CELL_VOLTAGE;
//			}
//
//			@Override
//			public int criticalHighCellVoltage() {
//				return CRITICAL_HIGH_CELL_VOLTAGE;
//			}
//
//			@Override
//			public int warningSoC() {
//				return WARNING_SOC;
//			}
//
//			@Override
//			public int criticalSoC() {
//				return CRITICAL_SOC;
//			}
//
//			@Override
//			public int deltaSoC() {
//				return DELTA_SOC;
//			}
//
//			@Override
//			public int lowTemperature() {
//				return LOW_TEMPERATURE;
//			}
//
//			@Override
//			public int highTemperature() {
//				return HIGH_TEMPERATURE;
//			}
//
//			@Override
//			public long unusedTime() {
//				return UNUSED_TIME;
//			}
//
//			@Override
//			public int forceChargeReachableMinCellVoltage() {
//				return FORCE_CHARGE_REACHABLE_MIN_CELL_VOLTAGE;
//			}
//
//			@Override
//			public String bms_id() {
//				return BMS_ID;
//			}
//		};
//	}
//}
