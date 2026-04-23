package io.openems.edge.meter.chint.ddsu666;

import static io.openems.common.types.MeterType.GRID;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.CURRENT_L1;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.FREQUENCY;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.VOLTAGE_L1;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.meter.api.PhaseRotation;
import io.openems.edge.timedata.test.DummyTimedata;

public class MeterChintDdsu666ImplTest {

	// IEEE 754 float word pairs (MSW, LSW) for test values:
	//   122.0 V        → 0x42F40000 → {0x42F4, 0x0000}
	//   5.0 A          → 0x40A00000 → {0x40A0, 0x0000}
	//   1.25 kW        → 0x3FA00000 → {0x3FA0, 0x0000}
	//   50.0 Hz        → 0x42480000 → {0x4248, 0x0000}
	//   1000.0 kWh     → 0x447A0000 → {0x447A, 0x0000}

	private ComponentTest testBasis;
	private ComponentManager cma;
	private TimeLeapClock clock;

	@Before
	public void setup() throws OpenemsException, Exception {
		this.clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800), ZoneOffset.UTC);
		this.cma = new DummyComponentManager(this.clock);
		this.testBasis = new ComponentTest(new MeterChintDdsu666Impl()) //
				.addReference("cma", this.cma) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("setModbus", new DummyModbusBridge("modbus0") //
						// 0x2000 block: single-phase live values
						.withRegisters(0x2000,
								// 0x2000 voltage = 122.0 V
								0x42F4, 0x0000,
								// 0x2002 current = 5.0 A
								0x40A0, 0x0000,
								// 0x2004 active power = 1.25 kW
								0x3FA0, 0x0000,
								// 0x2006..0x2009 not used
								0x0000, 0x0000, 0x0000, 0x0000)
						.withRegisters(0x200A,
								// 0x200A..0x200D not mapped
								0x0000, 0x0000, 0x0000, 0x0000,
								// 0x200E frequency = 50.0 Hz
								0x4248, 0x0000,
								// 0x2010..0x2011 not used
								0x0000, 0x0000)
						.withRegisters(0x4000,
								// 0x4000 import active energy = 1000.0 kWh
								0x447A, 0x0000,
								// 0x4002 not used
								0x0000, 0x0000));
	}

	@Test
	public void testNonInvert() throws Exception {
		this.testBasis //
				.activate(MyConfig.create() //
						.setId("meter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(2) //
						.setInvert(false) //
						.setType(GRID) //
						.setPhaseRotation(PhaseRotation.L1_L2_L3) //
						.build()) //
				.next(new TestCase() //
						// 1.25 kW -> 1250 W
						.output(ACTIVE_POWER, 1250) //
						// 50 Hz → 50000 mHz
						.output(FREQUENCY, 50000) //
						// 122 V -> 122000 mV
						.output(VOLTAGE_L1, 122000) //
						// 5 A -> 5000 mA
						.output(CURRENT_L1, 5000) //
						// 1000 kWh -> 1000000 Wh
						.output(MeterChintDdsu666.ChannelId.ACTIVE_IMPORT_ENERGY, 1000000)) //
				.next(new TestCase() //
						.timeleap(this.clock, 60, ChronoUnit.MINUTES)) //
				.deactivate();
	}

	@Test
	public void testInvert() throws Exception {
		this.testBasis //
				.activate(MyConfig.create() //
						.setId("meter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(2) //
						.setInvert(true) //
						.setType(GRID) //
						.setPhaseRotation(PhaseRotation.L1_L2_L3) //
						.build()) //
				.next(new TestCase() //
						.output(ACTIVE_POWER, -1250)) //
				.deactivate();
	}
}
