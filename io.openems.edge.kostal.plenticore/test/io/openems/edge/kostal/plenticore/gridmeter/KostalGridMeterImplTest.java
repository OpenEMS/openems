package io.openems.edge.kostal.plenticore.gridmeter;

import org.junit.Test;

import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class KostalGridMeterImplTest {

  @Test
  public void test() throws Exception {
    new ComponentTest(new KostalGridMeterImpl()) //
      .addReference("cm", new DummyConfigurationAdmin()) //
      .addReference("setModbus", new DummyModbusBridge("modbus0")) //
      .activate(
        MyConfig.create() //
          .setId("meter0") //
          .setModbusId("modbus0") //
          .setModbusUnitId(1) //
          .setType(MeterType.GRID)
          .build()
      ); //
  }
}
