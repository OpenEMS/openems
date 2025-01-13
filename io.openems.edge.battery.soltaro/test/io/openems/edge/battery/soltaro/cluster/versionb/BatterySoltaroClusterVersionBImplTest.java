package io.openems.edge.battery.soltaro.cluster.versionb;

import static io.openems.edge.battery.soltaro.cluster.SoltaroCluster.ChannelId.SUB_MASTER_1_COMMUNICATION_FAILURE;
import static io.openems.edge.battery.soltaro.cluster.SoltaroCluster.ChannelId.SUB_MASTER_2_COMMUNICATION_FAILURE;
import static io.openems.edge.battery.soltaro.cluster.SoltaroCluster.ChannelId.SUB_MASTER_3_COMMUNICATION_FAILURE;
import static io.openems.edge.battery.soltaro.cluster.SoltaroCluster.ChannelId.SUB_MASTER_4_COMMUNICATION_FAILURE;
import static io.openems.edge.battery.soltaro.cluster.SoltaroCluster.ChannelId.SUB_MASTER_5_COMMUNICATION_FAILURE;

import org.junit.Test;

import io.openems.edge.battery.soltaro.common.enums.BatteryState;
import io.openems.edge.battery.soltaro.common.enums.ModuleType;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class BatterySoltaroClusterVersionBImplTest {

	@Test
	public void test() throws Exception {
		var sut = new BatterySoltaroClusterVersionBImpl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager())
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("battery0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(0) //
						.setNumberOfSlaves(0) //
						.setModuleType(ModuleType.MODULE_3_5_KWH) //
						.setWatchdog(0) //
						.setBatteryState(BatteryState.DEFAULT) //
						.setRacks(1, 2, 3, 4) //
						.setErrorLevel2Delay(0) //
						.setMaxStartAppempts(0) //
						.setMaxStartTime(0) //
						.setPendingTolerance(0) //
						.setStartUnsuccessfulDelay(0) //
						.setMinimalCellVoltage(0) //
						.build()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> sut.parseSubMasterCommunicationFailure.accept(null)) //
						.output(SUB_MASTER_1_COMMUNICATION_FAILURE, false) //
						.output(SUB_MASTER_2_COMMUNICATION_FAILURE, false) //
						.output(SUB_MASTER_3_COMMUNICATION_FAILURE, false) //
						.output(SUB_MASTER_4_COMMUNICATION_FAILURE, false) //
						.output(SUB_MASTER_5_COMMUNICATION_FAILURE, false))
				.next(new TestCase() //
						.onBeforeProcessImage(() -> sut.parseSubMasterCommunicationFailure.accept(1)) //
						.output(SUB_MASTER_1_COMMUNICATION_FAILURE, true) //
						.output(SUB_MASTER_2_COMMUNICATION_FAILURE, false) //
						.output(SUB_MASTER_3_COMMUNICATION_FAILURE, false) //
						.output(SUB_MASTER_4_COMMUNICATION_FAILURE, false) //
						.output(SUB_MASTER_5_COMMUNICATION_FAILURE, false))
				.next(new TestCase() //
						.onBeforeProcessImage(() -> sut.parseSubMasterCommunicationFailure.accept(2)) //
						.output(SUB_MASTER_1_COMMUNICATION_FAILURE, false) //
						.output(SUB_MASTER_2_COMMUNICATION_FAILURE, true) //
						.output(SUB_MASTER_3_COMMUNICATION_FAILURE, false) //
						.output(SUB_MASTER_4_COMMUNICATION_FAILURE, false) //
						.output(SUB_MASTER_5_COMMUNICATION_FAILURE, false))
				.next(new TestCase() //
						.onBeforeProcessImage(() -> sut.parseSubMasterCommunicationFailure.accept(0x12)) //
						.output(SUB_MASTER_1_COMMUNICATION_FAILURE, false) //
						.output(SUB_MASTER_2_COMMUNICATION_FAILURE, true) //
						.output(SUB_MASTER_3_COMMUNICATION_FAILURE, false) //
						.output(SUB_MASTER_4_COMMUNICATION_FAILURE, false) //
						.output(SUB_MASTER_5_COMMUNICATION_FAILURE, false) // 5 is not activated in Config
				);
	}
}
