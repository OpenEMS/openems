package io.openems.edge.controller.ess.limitdischargecellvoltage.helper;

import java.lang.annotation.Annotation;

import io.openems.edge.controller.ess.limitdischargecellvoltage.Config;

public class CreateTestConfig {

	public static final int CHARGE_POWER_PERCENT = 20;
	public static final int CHARGING_TIME = 3;
	public static final int CRITICAL_CELL_VOLTAGE = 2800;
	public static final String ESS_ID = "ess0";
	public static final boolean ENABLED = true;
	public static final int WARNING_CELL_VOLTAGE = 2900;
	public static final int WARNING_CELL_VOLTAGE_TIME = 4;
	public static final String ID = "ctrl0";

	public static Config create() {

		return new Config() {

			@Override
			public Class<? extends Annotation> annotationType() {
				return null;
			}

			@Override
			public String webconsole_configurationFactory_nameHint() {
				return null;
			}

			@Override
			public int warningCellVoltageTime() {
				return WARNING_CELL_VOLTAGE_TIME;
			}

			@Override
			public int warningCellVoltage() {
				return WARNING_CELL_VOLTAGE;
			}

			@Override
			public String id() {
				return ID;
			}

			@Override
			public String ess_id() {
				return ESS_ID;
			}

			@Override
			public boolean enabled() {
				return ENABLED;
			}

			@Override
			public int criticalCellVoltage() {
				return CRITICAL_CELL_VOLTAGE;
			}

			@Override
			public int chargingTime() {
				return CHARGING_TIME;
			}

			@Override
			public int chargePowerPercent() {
				return CHARGE_POWER_PERCENT;
			}

			@Override
			public String alias() {
				return null;
			}
		};
	}

}
