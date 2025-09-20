package io.openems.edge.meter.camillebauer.aplus;

import static io.openems.common.types.MeterType.GRID;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.meter.test.InvertTest;
import io.openems.edge.timedata.test.DummyTimedata;

public class MeterCamillebauerAplusImplTest {

	private ComponentTest test;
	private ComponentManager cma;
	private TimeLeapClock clock;

	@Before
	public void setup() throws Exception {
		this.clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */,
				ZoneOffset.UTC);
		this.cma = new DummyComponentManager(this.clock);
		this.test = new ComponentTest(new MeterCamillebauerAplusImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("cma", this.cma)//
				.addReference("timedata", new DummyTimedata("timedata0"))//
				.addReference("setModbus", new DummyModbusBridge("modbus0").withRegisters(99,
						// VOLTAGE_L1
						0x0000, 0x3F80,
						// VOLTAGE_L2
						0x0000, 0x3F80,
						// VOLTAGE_L3
						0x0000, 0x3F80,
						// VOLTAGE
						0x0000, 0x3F80,

						// DUMMY 107-114
						0x0000, 0x0000, 0x0000, 0x0000, //
						0x0000, 0x0000, 0x0000, 0x0000, //

						// CURRENT
						0x0001, 0x4040,
						// CURRENT_L1
						0x0000, 0x3F80,
						// CURRENT_L2
						0x0000, 0x3F80,
						// CURRENT_L3
						0x0000, 0x3F80,

						// DUMMY 123-132
						0x0000, 0x0000, 0x0000, 0x0000, //
						0x0000, 0x0000, 0x0000, 0x0000, //
						0x0000, 0x0000,
						// ACTIVE_POWER
						0x2000, 0x464B,
						// ACTIVE_POWER_L1
						0x4000, 0x461C,
						// ACTIVE_POWER_L2
						0x4000, 0x461C,
						// ACTIVE_POWER_L3
						0x4000, 0x461C,
						// REACTIVE_POWER_L1
						0xC000, 0x45DA,
						// REACTIVE_POWER_L2
						0xC000, 0x45DA,
						// REACTIVE_POWER_L3
						0xC000, 0x45DA,
						// REACTIVE_POWER
						0xC000, 0x45DA,
						// DUMMY 149-156
						0x0000, 0x0000, 0x0000, 0x0000, //
						0x0000, 0x0000, 0x0000, 0x0000, //
						// FREQUENCY
						0x0000, 0x40A0//
				));
	}

	@Test
	public void invertTest() throws Exception {
		this.test.activate(//
				MyConfig.create() //
						.setId("meter0") //
						.setModbusId("modbus0") //
						.setMeterType(GRID)//
						.setInvert(true)//
						.build()) //
				.next(InvertTest.testInvert(true))//
				.next(new TestCase().timeleap(this.clock, 60, ChronoUnit.MINUTES))
				.next(InvertTest.testEnergyInvert(true));
	}

	@Test
	public void nonInvertTest() throws Exception {
		this.test.activate(//
				MyConfig.create() //
						.setId("meter0") //
						.setModbusId("modbus0") //
						.setMeterType(GRID)//
						.setInvert(false)//
						.build()) //
				.next(InvertTest.testInvert(false))//
				.next(new TestCase().timeleap(this.clock, 60, ChronoUnit.MINUTES))
				.next(InvertTest.testEnergyInvert(false));
	}

}
