package io.openems.edge.batteryinverter.refu;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.batteryinverter.api.SymmetricBatteryInverter.ChannelId.MAX_APPARENT_POWER;
import static io.openems.edge.batteryinverter.refu.BatteryInverterRefu.ChannelId.STATE_MACHINE;
import static java.time.temporal.ChronoUnit.SECONDS;

import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.battery.test.DummyBattery;
import io.openems.edge.batteryinverter.refu.statemachine.StateMachine.State;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;

public class BatteryInverterRefuImplTest {

	private static class MyComponentTest extends ComponentTest {

		private final DummyBattery battery = new DummyBattery("battery0");

		public MyComponentTest withBatteryStartStop(StartStop startStop) {
			this.battery.withStartStop(startStop);
			return this;
		}

		public MyComponentTest(OpenemsComponent sut) throws OpenemsException {
			super(sut);
		}

		@Override
		protected void handleEvent(String topic) throws Exception {
			if (topic.equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE)) {
				((BatteryInverterRefuImpl) this.getSut()).run(this.battery, 0, 0);
			}
			super.handleEvent(topic);
		}
	}

	private static final TimeLeapClock CLOCK = createDummyClock();
	private static ComponentTest test;

	@Before
	public void prepareTest() throws Exception {
		var sut = new BatteryInverterRefuImpl();

		test = new MyComponentTest(sut) //
				.addReference("componentManager", new DummyComponentManager(CLOCK)) //
				.addReference("setModbus", new DummyModbusBridge("modbus0") //
						.withRegisters(40000, 0x5375, 0x6e53) // SunSpec identifier
						.withRegisters(40002, 1, 66) // Block 1
						.withRegisters(40004, IntStream.range(0, 66).map(i -> 0).toArray()) //
						.withRegisters(40070, 103, 50) // Block 103
						.withRegisters(40072, IntStream.range(0, 50).map(i -> 0).toArray()) //
						.withRegisters(40122, 120, 26) // Block 120
						.withRegisters(40124, IntStream.range(0, 26).map(i -> 0).toArray()) //
						.withRegisters(40150, 121, 30) // Block 121
						.withRegisters(40152, IntStream.range(0, 30).map(i -> 0).toArray()) //
						.withRegisters(40182, 123, 24) // Block 123
						.withRegisters(40184, IntStream.range(0, 24).map(i -> 0).toArray()) //
						.withRegisters(40208, 64800, 14) // Block 64800
						.withRegisters(40210, IntStream.range(0, 14).map(i -> 0).toArray()) //
						.withRegisters(40224, 0xFFFF, 0)) // End of SunSpec map
				.activate(MyConfig.create() //
						.setId("batteryInverter0") //
						.setStartStopConfig(StartStopConfig.START) //
						.setModbusId("modbus0") //
						.setActivateWatchdog(true) //
						.build());

		sut.setStartStop(StartStop.UNDEFINED);
		for (int i = 0; i < 9; i++) {
			test.next(new TestCase());
		}
		sut.setStartStop(StartStop.START);
		test.next(new TestCase());
	}

	@Test
	public void testStart() throws Exception {
		((MyComponentTest) test).withBatteryStartStop(StartStop.START);
		test//
				.next(new TestCase("one") //
						.input(DefaultSunSpecModel.S103.ST.getChannelId(), 8) //
						.input(MAX_APPARENT_POWER, 88_000) //
						.output(STATE_MACHINE, State.UNDEFINED)) //
				.next(new TestCase("two") //
						.timeleap(CLOCK, 4, SECONDS) //
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				.next(new TestCase() //
						.timeleap(CLOCK, 1, SECONDS) //
						.input(DefaultSunSpecModel.S103.ST.getChannelId(), 4)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.RUNNING));
	}
}