package io.openems.edge.fronius.meter;

import org.junit.Test;

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.meter.api.ElectricityMeter;

public class MeterFroniusImplTest {

	private static ComponentTest prepareTest(boolean invert) throws OpenemsException, Exception {
		return new ComponentTest(new MeterFroniusImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0") //
						.withRegisters(40000, 0x5375, 0x6e53) // isSunSpec
						.withRegisters(40002, 1, 66) // Block 1
						.withRegisters(40004, //
								/* MN */ 65, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, //
								/* MD */ 66, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, //
								/* OPT */ 67, 0, 0, 0, 0, 0, 0, 0, //
								/* VR */ 68, 0, 0, 0, 0, 0, 0, 0, //
								/* SN */ 69, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, //
								/* DA */ 1, //
								/* PAD */ 0)
						.withRegisters(40070, 213, 124) // Block 213
						.withRegistersFloat32(40072, //
								/* A */ 10.123F, /* APH_A,B,C */ 3.234F, 2.345F, 4.345F, //
								/* PH_V */ 230.123F, /* PH_VPH_A,B,C */ 229, 230, 231, //
								/* PPV */ 1, /* P_P_VPH_A_B,B_C,C_A */ 2, 3, 4, //
								/* HZ */ 50, //
								/* W */ 12345, /* WPH_A,B,C */ 5432, 4321, 6789, //
								/* VA */ 5, /* V_APH_A,B,C */ 6, 7, 8, //
								/* VAR */ 9, /* V_A_RPH_A,B,C */ 10, 11, 12, //
								/* PF */ 13, /* P_FPH_A,B,C */ 14, 15, 16, //
								/* TOT_WH_EXP */ 17, /* TOT_WH_EXP_PH_A,B,C */ 18, 19, 20, //
								/* TOT_WH_IMP */ 21, /* TOT_WH_IMP_PH_A,B,C */ 22, 23, 24, //
								/* TOT_V_AH_EXP */ 25, /* TOT_V_AH_EXP_PH_A,B,C */ 26, 27, 28, //
								/* TOT_V_AH_IMP */ 29, /* TOT_V_AH_IMP_PH_A,B,C */ 30, 31, 32, //
								/* TOT_V_ARH_IMP_Q1 */ 33, /* TOT_V_ARH_IMP_Q1PH_A,B,C */ 34, 35, 36, //
								/* TOT_V_ARH_IMP_Q2 */ 37, /* TOT_V_ARH_IMP_Q2PH_A,B,C */ 38, 39, 40, //
								/* TOT_V_ARH_EXP_Q3 */ 41, /* TOT_V_ARH_EXP_Q3PH_A,B,C */ 42, 43, 44, //
								/* TOT_V_ARH_EXP_Q4 */ 45, /* TOT_V_ARH_EXP_Q4PH_A,B,C */ 46, 47, 48, //
								/* EVT */ 49) //
						.withRegisters(40196, 0xFFFF)) // END_OF_MAP
				.activate(MyConfig.create() //
						.setId("meter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(240) //
						.setType(MeterType.GRID) //
						.setInvert(invert) //
						.build()) //
				.next(new TestCase(), 100);
	}

	@Test
	public void testNotInverted() throws Exception {
		prepareTest(false /* invert */) //
				.next(new TestCase() //
						.activateStrictMode() //
						.output(OpenemsComponent.ChannelId.STATE, Level.OK) //
						.output(ModbusComponent.ChannelId.MODBUS_COMMUNICATION_FAILED, false) //

						.output("S1Mn", "A") //
						.output("S1Md", "B") //
						.output("S1Opt", "C") //
						.output("S1Vr", "D") //
						.output("S1Sn", "E") //
						.output("S1Da", null) //
						.output("S1Pad", null) //

						.output("S213A", 10.123F) //
						.output(ElectricityMeter.ChannelId.CURRENT, 10123) //
						.output("S213AphA", 3.234F) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 3234) //
						.output("S213AphB", 2.345F) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, 2345) //
						.output("S213AphC", 4.345F) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, 4345) //
						.output("S213PhV", 230.123F) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, 230123) //
						.output("S213PhVphA", 229F) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 229000) //
						.output("S213PhVphB", 230F) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 230000) //
						.output("S213PhVphC", 231F) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 231000) //
						.output("S213Ppv", 1F) //
						.output("S213PPVphAB", 2F) //
						.output("S213PPVphBC", 3F) //
						.output("S213PPVphCA", 4F) //
						.output("S213Hz", 50F) //
						.output(ElectricityMeter.ChannelId.FREQUENCY, 50000) //
						.output("S213W", 12345F) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 12345) //
						.output("S213WphA", 5432F) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 5432) //
						.output("S213WphB", 4321F) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 4321) //
						.output("S213WphC", 6789F) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 6789) //
						.output("S213Va", 5F) //
						.output("S213VAphA", 6F) //
						.output("S213VAphB", 7F) //
						.output("S213VAphC", 8F) //
						.output("S213Var", 9F) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER, 9) //
						.output("S213VARphA", 10F) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, 10) //
						.output("S213VARphB", 11F) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, 11) //
						.output("S213VARphC", 12F) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, 12) //
						.output("S213Pf", 13F) //
						.output("S213PFphA", 14F) //
						.output("S213PFphB", 15F) //
						.output("S213PFphC", 16F) //
						.output("S213TotWhExp", 17F) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, 17L) //
						.output("S213TotWhExpPhA", 18F) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, 18L) //
						.output("S213TotWhExpPhB", 19F) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, 19L) //
						.output("S213TotWhExpPhC", 20F) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, 20L) //
						.output("S213TotWhImp", 21F) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, 21L) //
						.output("S213TotWhImpPhA", 22F) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, 22L) //
						.output("S213TotWhImpPhB", 23F) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, 23L) //
						.output("S213TotWhImpPhC", 24F) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, 24L) //
						.output("S213TotVAhExp", 25F) //
						.output("S213TotVAhExpPhA", 26F) //
						.output("S213TotVAhExpPhB", 27F) //
						.output("S213TotVAhExpPhC", 28F) //
						.output("S213TotVAhImp", 29F) //
						.output("S213TotVAhImpPhA", 30F) //
						.output("S213TotVAhImpPhB", 31F) //
						.output("S213TotVAhImpPhC", 32F) //
						.output("S213TotVArhImpQ1", 33F) //
						.output("S213TotVArhImpQ1phA", 34F) //
						.output("S213TotVArhImpQ1phB", 35F) //
						.output("S213TotVArhImpQ1phC", 36F) //
						.output("S213TotVArhImpQ2", 37F) //
						.output("S213TotVArhImpQ2phA", 38F) //
						.output("S213TotVArhImpQ2phB", 39F) //
						.output("S213TotVArhImpQ2phC", 40F) //
						.output("S213TotVArhExpQ3", 41F) //
						.output("S213TotVArhExpQ3phA", 42F) //
						.output("S213TotVArhExpQ3phB", 43F) //
						.output("S213TotVArhExpQ3phC", 44F) //
						.output("S213TotVArhExpQ4", 45F) //
						.output("S213TotVArhExpQ4phA", 46F) //
						.output("S213TotVArhExpQ4phB", 47F) //
						.output("S213TotVArhExpQ4phC", 48F) //

						.output("S213EvtPowerFailure", true) //
						.output("S213EvtUnderVoltage", false) //
						.output("S213EvtLowPF", false) //
						.output("S213EvtOverCurrent", false) //
						.output("S213EvtOverVoltage", true) //
						.output("S213EvtMissingSensor", false) //
						.output("S213EvtReserved1", false) //
						.output("S213EvtReserved2", true) //
						.output("S213EvtReserved3", false) //
						.output("S213EvtReserved4", false) //
						.output("S213EvtReserved5", false) //
						.output("S213EvtReserved6", false) //
						.output("S213EvtReserved7", true) //
						.output("S213EvtReserved8", false) //
						.output("S213EvtOem01", false) //
						.output("S213EvtOem02", false) //
						.output("S213EvtOem03", false) //
						.output("S213EvtOem04", false) //
						.output("S213EvtOem05", false) //
						.output("S213EvtOem06", false) //
						.output("S213EvtOem07", false) //
						.output("S213EvtOem08", false) //
						.output("S213EvtOem09", false) //
						.output("S213EvtOem10", false) //
						.output("S213EvtOem11", false) //
						.output("S213EvtOem12", false) //
						.output("S213EvtOem13", false) //
						.output("S213EvtOem14", false) //
						.output("S213EvtOem15", false)) //
				.deactivate();
	}

	@Test
	public void testInverted() throws Exception {
		prepareTest(true /* invert */) //
				.next(new TestCase() //
						.output(ElectricityMeter.ChannelId.CURRENT, -10123) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, -3234) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, -2345) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, -4345) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, 230123) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 229000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 230000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 231000) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, -12345) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, -5432) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, -4321) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, -6789) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER, -9) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, -10) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, -11) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, -12) //

						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, 21L) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, 22L) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, 23L) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, 24L) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, 17L) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, 18L) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, 19L) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, 20L) //
				);
	}
}
