package io.openems.edge.batteryinverter.kaco.blueplanetgridsave;

import static io.openems.edge.batteryinverter.api.SymmetricBatteryInverter.ChannelId.MAX_APPARENT_POWER;
import static io.openems.edge.batteryinverter.kaco.blueplanetgridsave.BatteryInverterKacoBlueplanetGridsave.WATCHDOG_TIMEOUT_SECONDS;
import static io.openems.edge.batteryinverter.kaco.blueplanetgridsave.BatteryInverterKacoBlueplanetGridsave.WATCHDOG_TRIGGER_SECONDS;
import static io.openems.edge.batteryinverter.kaco.blueplanetgridsave.BatteryInverterKacoBlueplanetGridsave.ChannelId.STATE_MACHINE;
import static io.openems.edge.batteryinverter.kaco.blueplanetgridsave.KacoSunSpecModel.S64201.CURRENT_STATE;
import static io.openems.edge.batteryinverter.kaco.blueplanetgridsave.KacoSunSpecModel.S64201.WATCHDOG;
import static java.time.temporal.ChronoUnit.SECONDS;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.test.DummyBattery;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.KacoSunSpecModel.S64201.S64201CurrentState;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine.StateMachine.State;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.TestUtils;

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

	private static TimeLeapClock clock;
	private static ComponentTest test;

	@Before
	public void prepareTest() throws Exception {
		clock = TestUtils.createDummyClock();
		var sut = new BatteryInverterKacoBlueplanetGridsaveImpl();

		test = new MyComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("setModbus", new DummyModbusBridge("modbus0"));

		// TODO implement proper Dummy-Modbus-Bridge with SunSpec support. Till then...
		test.addReference("isSunSpecInitializationCompleted", true); //
		var addChannel = AbstractOpenemsComponent.class.getDeclaredMethod("addChannel", ChannelId.class);
		addChannel.setAccessible(true);
		addChannel.invoke(sut, KacoSunSpecModel.S64203.BAT_SOC_0.getChannelId());
		addChannel.invoke(sut, KacoSunSpecModel.S64203.BAT_SOH_0.getChannelId());
		addChannel.invoke(sut, KacoSunSpecModel.S64203.BAT_TEMP_0.getChannelId());
		addChannel.invoke(sut, KacoSunSpecModel.S64202.DIS_MIN_V_0.getChannelId());
		addChannel.invoke(sut, KacoSunSpecModel.S64202.CHA_MAX_V_0.getChannelId());
		addChannel.invoke(sut, KacoSunSpecModel.S64202.DIS_MAX_A_0.getChannelId());
		addChannel.invoke(sut, KacoSunSpecModel.S64202.CHA_MAX_A_0.getChannelId());
		addChannel.invoke(sut, KacoSunSpecModel.S64202.EN_LIMIT_0.getChannelId());
		addChannel.invoke(sut, KacoSunSpecModel.S64201.REQUESTED_STATE.getChannelId());
		addChannel.invoke(sut, CURRENT_STATE.getChannelId());
		addChannel.invoke(sut, WATCHDOG.getChannelId());
		addChannel.invoke(sut, KacoSunSpecModel.S64201.W_SET_PCT.getChannelId());
		addChannel.invoke(sut, KacoSunSpecModel.S64201.WPARAM_RMP_TMS.getChannelId());
		addChannel.invoke(sut, KacoSunSpecModel.S64201.ST_VND.getChannelId());

		test.activate(MyConfig.create() //
				.setId("batteryInverter0") //
				.setStartStopConfig(StartStopConfig.START) //
				.setModbusId("modbus0") //
				.setActivateWatchdog(true) //
				.build()); //
	}

	@Test
	public void testStart() throws Exception {
		test //
				.next(new TestCase() //
						.input(CURRENT_STATE.getChannelId(), S64201CurrentState.STANDBY) //
						.input(MAX_APPARENT_POWER, 50_000) //
						.output(STATE_MACHINE, State.UNDEFINED)) //
				.next(new TestCase() //
						.timeleap(clock, 4, SECONDS) //
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				.next(new TestCase() //
						.timeleap(clock, 1, SECONDS) //
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
						.timeleap(clock, WATCHDOG_TRIGGER_SECONDS - 1, SECONDS) //
						.output(WATCHDOG.getChannelId(), null /* waiting till next watchdog trigger */)) //
				.next(new TestCase() //
						.timeleap(clock, 1, SECONDS) //
						.output(WATCHDOG.getChannelId(), WATCHDOG_TIMEOUT_SECONDS)) //
		;
	}
}
