package io.openems.edge.ess.mr.gridcon.onoffgrid.helper;

import java.lang.annotation.Annotation;

import io.openems.edge.ess.mr.gridcon.enums.ParameterSet;
import io.openems.edge.ess.mr.gridcon.onoffgrid.Config;

public class Creator {

	public static final String ESS_ID = "ess0";
	public static final String GRIDCON_ID = "gridcon0";
	public static final String BMS_A_ID = "bms1";
	public static final String BMS_C_ID = "bms2";
	public static final String BMS_B_ID = "bms3";
	public static final String METER_ID = "meter0";
	public static final String IO_ID = "io0";
	public static final ParameterSet PARAMETER_SET = ParameterSet.SET_1;
	public static final boolean ENABLE_IPU_1 = true;
	public static final boolean ENABLE_IPU_2 = true;
	public static final boolean ENABLE_IPU_3 = true;
	public static final float OFFSET_CURRENT = 0;
	public static final String INPUT_NA_PROTECTION_1 = "io0/DigitalInputM1C1";
	public static final boolean INPUT_NA_PROTECTION_1_INVERTED = false;
	public static final String INPUT_NA_PROTECTION_2 = "io0/DigitalInputM1C2";
	public static final boolean INPUT_NA_PROTECTION_2_INVERTED = false;
	public static final String INPUT_SYNC_DEVICE_BRIDGE = "io0/DigitalInputM2C1";
	public static final boolean INPUT_SYNC_DEVICE_BRIDGE_INVERTED = false;
	public static final String OUTPUT_SYNC_DEVICE_BRIDGE = "io0/DigitalOutputM1C1";
	public static final boolean OUTPUT_SYNC_DEVICE_BRIDGE_INVERTED = false;
	public static final String OUTPUT_HARD_RESET = "io0/DigitalOutputM1C2";
	public static final boolean OUTPUT_HARD_RESET_INVERTED = false;
	public static final float TARGET_FREQUENCY_ONGRID = 52.7f;
	public static final float TARGET_FREQUENCY_OFFGRID = 50.6f;
	public static final float DELTA_FREQUENCY = 0.2f;
	public static final float DELTA_VOLTAGE = 5.0f;

	public static DummyComponentManager getDummyComponentManager() {
		return new DummyComponentManager();
	}
	
	public static Config createOnOffGridConfig() {

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
			public String id() {
				return ESS_ID;
			}
			
			@Override
			public boolean enabled() {
				return true;
			}

			@Override
			public String alias() {
				return null;
			}

			@Override
			public String gridcon_id() {
				return GRIDCON_ID;
			}

			@Override
			public String bms_a_id() {
				return BMS_A_ID;
			}

			@Override
			public String bms_b_id() {
				return BMS_B_ID;
			}

			@Override
			public String bms_c_id() {
				return BMS_C_ID;
			}

			@Override
			public String meter_id() {
				return METER_ID;
			}

			@Override
			public boolean enableIPU1() {				
				return ENABLE_IPU_1;
			}

			@Override
			public boolean enableIPU2() {
				return ENABLE_IPU_2;
			}

			@Override
			public boolean enableIPU3() {
				return ENABLE_IPU_3;
			}

			@Override
			public ParameterSet parameterSet() {
				return PARAMETER_SET;
			}

			@Override
			public String inputNAProtection1() {
				return INPUT_NA_PROTECTION_1;
			}

			@Override
			public boolean isNAProtection1Inverted() {
				return INPUT_NA_PROTECTION_1_INVERTED;
			}

			@Override
			public String inputNAProtection2() {
				return INPUT_NA_PROTECTION_2;
			}

			@Override
			public boolean isNAProtection2Inverted() {
				return INPUT_NA_PROTECTION_2_INVERTED;
			}

			@Override
			public String inputSyncDeviceBridge() {
				return INPUT_SYNC_DEVICE_BRIDGE;
			}

			@Override
			public boolean isInputSyncDeviceBridgeInverted() {
				return INPUT_SYNC_DEVICE_BRIDGE_INVERTED;
			}

			@Override
			public String outputSyncDeviceBridge() {
				return OUTPUT_SYNC_DEVICE_BRIDGE;
			}

			@Override
			public boolean isOutputSyncDeviceBridgeInverted() {
				return OUTPUT_SYNC_DEVICE_BRIDGE_INVERTED;
			}

			@Override
			public String outputHardReset() {
				return OUTPUT_HARD_RESET;
			}

			@Override
			public boolean isOutputHardResetInverted() {
				return OUTPUT_HARD_RESET_INVERTED;
			}

			@Override
			public float targetFrequencyOnGrid() {
				return TARGET_FREQUENCY_ONGRID;
			}

			@Override
			public float targetFrequencyOffGrid() {
				return TARGET_FREQUENCY_OFFGRID;
			}

			@Override
			public float deltaFrequency() {				
				return DELTA_FREQUENCY;
			}

			@Override
			public float deltaVoltage() {
				return DELTA_VOLTAGE;
			}

			@Override
			public float offsetCurrent() {
				return OFFSET_CURRENT;
			}
		};
	}
}
