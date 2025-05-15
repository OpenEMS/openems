package io.openems.edge.kostal.pvinverter;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.kostal.pvinverter.KostalPvInverterImpl;

public class KostalPvInverterImplTest {

  @Test
  public void test() throws Exception {
    new ComponentTest(new KostalPvInverterImpl()) //
      .addReference("cm", new DummyConfigurationAdmin()) //
      .addReference("setModbus", new DummyModbusBridge("modbus0")) //
      .activate(
        MyConfig.create() //
          .setId("pvInverter0") //
          .setReadOnly(true) //
          .setModbusId("modbus0") //
          .setModbusUnitId(71) //
          .build()
      ); //
  }
}
