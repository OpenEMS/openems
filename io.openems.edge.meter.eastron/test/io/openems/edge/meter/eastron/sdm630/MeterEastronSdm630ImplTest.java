package io.openems.edge.meter.eastron.sdm630;

import static io.openems.common.types.MeterType.GRID;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.meter.api.PhaseRotation;
import io.openems.edge.meter.test.InvertTest;
import io.openems.edge.timedata.test.DummyTimedata;

public class MeterEastronSdm630ImplTest {

	private ComponentTest testBasis;
	private ComponentManager cma;
	private TimeLeapClock clock;

	@Before
	public void setup() throws OpenemsException, Exception {
		final var offset = 30001;
		this.clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */,
				ZoneOffset.UTC);
		this.cma = new DummyComponentManager(this.clock);
		this.testBasis = new ComponentTest(new MeterEastronSdm630Impl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("cma", this.cma)//
				.addReference("timedata", new DummyTimedata("timedata0"))
				.addReference("setModbus", new DummyModbusBridge("modbus0")//
						.withInputRegisters(30001 - offset,
								// Voltages (L1, L2, L3)
								0x3F80, 0x0000, // VOLTAGE_L1
								0x3F80, 0x0000, // VOLTAGE_L2
								0x3F80, 0x0000, // VOLTAGE_L3

								0x3F80, 0x0000, // CURRENT_L1
								0x3F80, 0x0000, // CURRENT_L2
								0x3F80, 0x0000, // CURRENT_L3

								// Active Powers (L1, L2, L3)
								0x461C, 0x4000, // ACTIVE_POWER_L1
								0x461C, 0x4000, // ACTIVE_POWER_L2
								0x461C, 0x4000, // ACTIVE_POWER_L3

								// Dummy Registers (30019 - 30024)
								0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,

								// Reactive Powers (L1, L2, L3)
								0x45DA, 0xC000, // REACTIVE_POWER_L1
								0x45DA, 0xC000, // REACTIVE_POWER_L2
								0x45DA, 0xC000, // REACTIVE_POWER_L3

								// Dummy Registers (30031 - 30048)
								0x44A6, 0x0000, 0x44A6, 0x0000, 0x44A6, 0x0000, //
								0x0000, 0x0000, 0x44A6, 0x0000, 0x44A6, 0x0000, //
								0x0000, 0x0000, 0x44A6, 0x0000, 0x44A6, 0x0000, //

								// Current (total)
								0x4040, 0x0001, // CURRENT_L3

								// Dummy Registers (30051 - 30052)
								0x4140, 0x0000,

								// Active Power (total)
								0x464B, 0x2000, // ACTIVE_POWER

								// Dummy Registers (30055 - 30060)
								0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,

								// Reactive Power (total)
								0x45DA, 0xC000, // REACTIVE_POWER

								// Dummy Registers (30063 - 30070)
								0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,

								// Frequency
								0x459C, 0x4000, // FREQUENCY

								// Dummy Registers (30073 - 30076)
								0x0000, 0x0000, 0x0000, 0x0000,

								// Reactive Production Energy
								0x4756, 0xD800, // REACTIVE_PRODUCTION_ENERGY

								// Reactive Consumption Energy
								0x428C, 0x0000 // REACTIVE_CONSUMPTION_ENERGY
						)); //

	}

	@Test
	public void testNonInvert() throws Exception {
		var test = this.testBasis.activate(MyConfig.create() //
				.setId("meter0") //
				.setModbusId("modbus0") //
				.setInvert(false)//
				.setType(GRID) //
				.setPhaseRotation(PhaseRotation.L1_L2_L3) //
				.build());
		test.next(InvertTest.testInvert(false));
		test.next(new TestCase().timeleap(this.clock, 60, ChronoUnit.MINUTES));
		test.next(InvertTest.testEnergyInvert(false));
	}

	@Test
	public void testInvert() throws Exception {
		var test = this.testBasis.activate(MyConfig.create() //
				.setId("meter0") //
				.setModbusId("modbus0") //
				.setInvert(true)//
				.setType(GRID) //
				.setPhaseRotation(PhaseRotation.L1_L2_L3) //
				.build());
		test.next(InvertTest.testInvert(true));
		test.next(new TestCase().timeleap(this.clock, 60, ChronoUnit.MINUTES));
		test.next(InvertTest.testEnergyInvert(true));
	}

}