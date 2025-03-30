package io.openems.edge.kostal.ess;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class KostalManagedESSImplTest {

	  @Test
	  public void test() throws Exception {
	    new ComponentTest(new KostalManagedESSImpl()) //
	      .addReference("cm", new DummyConfigurationAdmin()) //
	      .addReference("setModbus", new DummyModbusBridge("modbus0")) //
	      .activate(
	        MyConfig.create() //
	          .setId("ess0") //
	          .setReadOnlyMode(true) //
	          .setModbusId("modbus0") //
	          .setCapacity(10000) //
	          //.setInverter_id("pvInverter0")
	          .setCtrlId("ctrlEssTimeOfUseTariff1")
	          .setModbusUnitId(71) //
	          .build()
	      ); //
	  }
}
