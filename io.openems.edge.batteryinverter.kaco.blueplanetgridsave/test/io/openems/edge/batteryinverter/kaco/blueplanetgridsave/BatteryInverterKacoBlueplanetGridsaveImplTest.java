package io.openems.edge.batteryinverter.kaco.blueplanetgridsave;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.batteryinverter.api.SymmetricBatteryInverter.ChannelId.MAX_APPARENT_POWER;
import static io.openems.edge.batteryinverter.kaco.blueplanetgridsave.BatteryInverterKacoBlueplanetGridsave.WATCHDOG_TIMEOUT_SECONDS;
import static io.openems.edge.batteryinverter.kaco.blueplanetgridsave.BatteryInverterKacoBlueplanetGridsave.WATCHDOG_TRIGGER_SECONDS;
import static io.openems.edge.batteryinverter.kaco.blueplanetgridsave.BatteryInverterKacoBlueplanetGridsave.ChannelId.STATE_MACHINE;
import static io.openems.edge.batteryinverter.kaco.blueplanetgridsave.KacoSunSpecModel.S64201.CURRENT_STATE;
import static io.openems.edge.batteryinverter.kaco.blueplanetgridsave.KacoSunSpecModel.S64201.WATCHDOG;
import static java.time.temporal.ChronoUnit.SECONDS;

import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.test.DummyBattery;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.KacoSunSpecModel.S64201.S64201CurrentState;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine.StateMachine.State;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class BatteryInverterKacoBlueplanetGridsaveImplTest {

	private static class MyComponentTest extends ComponentTest {

		private final Battery battery = new DummyBattery("battery0");

		public MyComponentTest(OpenemsComponent sut) throws OpenemsException {
			super(sut);
		}

		@Override
		protected void handleEvent(String topic) throws Exception {
			if (topic.equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE)) {
				((BatteryInverterKacoBlueplanetGridsaveImpl) this.getSut()).run(this.battery, 0, 0);
			}
			super.handleEvent(topic);
		}

	}

	private static final TimeLeapClock CLOCK = createDummyClock();
	private static ComponentTest test;

	@Before
	public void prepareTest() throws Exception {
		var sut = new BatteryInverterKacoBlueplanetGridsaveImpl();

		test = new MyComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(CLOCK)) //
				.addReference("setModbus", new DummyModbusBridge("modbus0") //
						.withRegisters(40000, 0x5375, 0x6e53) // isSunSpec
						.withRegisters(40002, 1, 66) // Block 1
						.withRegisters(40004, IntStream.range(0, 66).map(i -> 0).toArray()) //
						.withRegisters(40070, 103, 50) // Block 103
						.withRegisters(40072, IntStream.range(0, 50).map(i -> 0).toArray()) //
						.withRegisters(40122, 121, 50) // Block 121
						.withRegisters(40124, IntStream.range(0, 50).map(i -> 0).toArray()) //
						.withRegisters(40174, 64201, 50) // Block 64201
						.withRegisters(40176, IntStream.range(0, 50).map(i -> 0).toArray()) //
						.withRegisters(40226, 64202, 50) // Block 64202
						.withRegisters(40228, IntStream.range(0, 50).map(i -> 0).toArray()) //
						.withRegisters(40278, 64203, 50) // Block 64203
						.withRegisters(40280, IntStream.range(0, 50).map(i -> 0).toArray()) //
						.withRegisters(40330, 64204, 50) // Block 64204
						.withRegisters(40332, IntStream.range(0, 50).map(i -> 0).toArray()) //
						.withRegisters(40382, 0xFFFF, 0)); // END_OF_MAP

		test.activate(MyConfig.create() //
				.setId("batteryInverter0") //
				.setStartStopConfig(StartStopConfig.START) //
				.setModbusId("modbus0") //
				.setActivateWatchdog(true) //
				.build()); //

		// let SunSpec initialize
		sut.setStartStop(StartStop.UNDEFINED);
		for (int i = 0; i < 9; i++) {
			test.next(new TestCase());
		}
		sut.setStartStop(StartStop.START);
		test.next(new TestCase());
	}

	@Test
	public void testStart() throws Exception {
		test //
				.next(new TestCase() //
						.input(CURRENT_STATE.getChannelId(), S64201CurrentState.STANDBY) //
						.input(MAX_APPARENT_POWER, 50_000) //
						.output(STATE_MACHINE, State.UNDEFINED)) //
				.next(new TestCase() //
						.timeleap(CLOCK, 4, SECONDS) //
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				.next(new TestCase() //
						.timeleap(CLOCK, 1, SECONDS) //
						.input(CURRENT_STATE.getChannelId(), S64201CurrentState.GRID_CONNECTED) //
						.output(WATCHDOG.getChannelId(), WATCHDOG_TIMEOUT_SECONDS)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.RUNNING)) //
		;
	}

	@Test
	public void testWatchdog() throws Exception {
		test //
				.next(new TestCase() //
						.output(WATCHDOG.getChannelId(), WATCHDOG_TIMEOUT_SECONDS)) //
				.next(new TestCase() //
						.timeleap(CLOCK, WATCHDOG_TRIGGER_SECONDS - 1, SECONDS) //
						.output(WATCHDOG.getChannelId(), null /* waiting till next watchdog trigger */)) //
				.next(new TestCase() //
						.timeleap(CLOCK, 1, SECONDS) //
						.output(WATCHDOG.getChannelId(), WATCHDOG_TIMEOUT_SECONDS)) //
		;
	}
}
