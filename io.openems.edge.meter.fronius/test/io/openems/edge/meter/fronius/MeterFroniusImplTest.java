package io.openems.edge.meter.fronius;

import static java.util.stream.IntStream.range;

import org.junit.Test;

import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.meter.api.ElectricityMeter;

public class MeterFroniusImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new MeterFroniusImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0") //
						.withRegisters(40000, 0x5375, 0x6e53) // isSunSpec
						.withRegisters(40002, 1, 66) // Block 1
						.withRegisters(40004, //
								range(0, 66).map(i -> 0).toArray()) //
						.withRegisters(40070, 213, 124) // Block 213
						.withRegistersFloat32(40072, //
								/* A */ 10.123F, /* APH_A,B,C */ 3.234F, 2.345F, 4.345F //
						/* PH_V */
						/* PH_VPH_A,B,C */
						/* PPV */
						/* P_P_VPH_A_B,B_C,C_A */
						/* HZ */
						/* W */
						/* WPH_A,B,C */
						/* VA */
						/* V_APH_A,B,C */
						/* VAR */
						/* V_A_RPH_A,B,C */
						/* PF */
						/* P_FPH_A,B,C */
						/* TOT_WH_EXP */
						/* TOT_WH_EXP_PH_A,B,C */
						/* TOT_WH_IMP */
						/* TOT_WH_IMP_PH_A,B,C */
						/* TOT_V_AH_EXP */
						/* TOT_V_AH_EXP_PH_A,B,C */
						/* TOT_V_AH_IMP */
						/* TOT_V_AH_IMP_PH_A,B,C */
						/* TOT_V_ARH_IMP_Q1 */
						/* TOT_V_ARH_IMP_Q1PH_A,B,C */
						/* TOT_V_ARH_IMP_Q2 */
						/* TOT_V_ARH_IMP_Q2PH_A,B,C */
						/* TOT_V_ARH_EXP_Q3 */
						/* TOT_V_ARH_EXP_Q3PH_A,B,C */
						/* TOT_V_ARH_EXP_Q4 */
						/* TOT_V_ARH_EXP_Q4PH_A,B,C */
						/* EVT */
						) //
						.withRegisters(40080, //
								range(0, 122).map(i -> 0).toArray()) //
						.withRegisters(40196, 0xFFFF)) // END_OF_MAP
				.activate(MyConfig.create() //
						.setId("meter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(240) //
						.setType(MeterType.GRID) //
						.setInvert(false) //
						.build()) //
				.next(new TestCase(), 100) //
				.next(new TestCase() //
						.output(ElectricityMeter.ChannelId.CURRENT, 10123) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 3234) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, 2345) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, 4345) //
				) //
				.deactivate();
	}
}
