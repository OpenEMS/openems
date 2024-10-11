package io.openems.edge.meter.eastron.sdm630;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.meter.api.MeterType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class MeterEastronSdm630ImplTest {

  private static final String METER_ID = "meter0";
  private static final String MODBUS_ID = "modbus0";

  @Test
  public void test() throws Exception {
    new ComponentTest(new MeterEastronSdm630Impl()) //
        .addReference("cm", new DummyConfigurationAdmin()) //
        .addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
        .activate(MyConfig.create() //
            .setId(METER_ID) //
            .setModbusId(MODBUS_ID) //
            .setType(MeterType.GRID) //
            .build()) //
    ;
  }

  @RunWith(Parameterized.class)
  public static class EnergyAndPowerAndCurrentInversionTest {
    @Parameterized.Parameters(name = "Test {index}: isWiringDirectionReversed={0}, meterType:{1} -> shouldInvertEnergy={2}, shouldInvertPowerAndCurrent={3}")
    public static Collection<Object[]> data() {
      return Arrays.asList(new Object[][] {
          // isWiringDirectionReversed, MeterType, expect: shouldInvertEnergy, expect: shouldInvertPowerAndCurrent
          {false, MeterType.GRID, false, false},
          {false, MeterType.PRODUCTION, false, false},
          {false, MeterType.CONSUMPTION_METERED, true, true},
          {false, MeterType.CONSUMPTION_NOT_METERED, true, true},
          //
          {true, MeterType.GRID, true, true},
          {true, MeterType.PRODUCTION, true, true},
          {true, MeterType.CONSUMPTION_METERED, false, false},
          {true, MeterType.CONSUMPTION_NOT_METERED, false, false},
      });
    }

    private final boolean shouldInvertEnergy;
    private final boolean shouldInvertPowerAndCurrent;
    private final MyConfig myConfig;

    public EnergyAndPowerAndCurrentInversionTest(
        boolean isWiringDirectionReversed,
        MeterType meterType,
        boolean shouldInvertEnergy,
        boolean shouldInvertPowerAndCurrent
    ) {
      this.myConfig = MyConfig.create() //
          .setId(METER_ID) //
          .setModbusId(MODBUS_ID) //
          .setType(meterType) //
          .setIsWiringDirectionReversed(isWiringDirectionReversed)
          .build();
      this.shouldInvertEnergy = shouldInvertEnergy;
      this.shouldInvertPowerAndCurrent = shouldInvertPowerAndCurrent;
    }

    @Test
    public void shouldInvertEnergyPowerAndCurrent() throws Exception {
      ComponentTest activate = new ComponentTest(new MeterEastronSdm630Impl()) //
          .addReference("cm", new DummyConfigurationAdmin()) //
          .addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
          .activate(myConfig);//
      MeterEastronSdm630Impl meter = (MeterEastronSdm630Impl) activate.getSut();

      String testCase =
          " given isWiringDirectionReversed=" + this.myConfig.isWiringDirectionReversed() + " and meterType=" + this.myConfig.type();
      assertEquals("expect shouldInvertEnergy=" + shouldInvertEnergy + testCase, this.shouldInvertEnergy, meter.shouldInvertEnergy());
      assertEquals("expect shouldInvertPowerAndCurrent=" + this.shouldInvertPowerAndCurrent + testCase, this.shouldInvertPowerAndCurrent,
          meter.shouldInvertPowerAndCurrent());
    }

  }
}