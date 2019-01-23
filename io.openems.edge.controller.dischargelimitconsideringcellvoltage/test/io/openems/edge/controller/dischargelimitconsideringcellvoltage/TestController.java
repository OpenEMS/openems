package io.openems.edge.controller.dischargelimitconsideringcellvoltage;

import static org.junit.Assert.assertEquals;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.dischargelimitconsideringcellvoltage.DischargeLimitConsideringCellVoltage.State;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Coefficient;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.PowerException;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

public class TestController {

	protected static final int TIME_WHILE_PENDING = 3;
	DischargeLimitConsideringCellVoltage sut = null;
	private ManagedSymmetricEss dummyEss = getDummyEss();
	private Battery dummyBattery = getDummyBattery();

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		sut = new DischargeLimitConsideringCellVoltage();

		// Set ess
		Field f = sut.getClass().getDeclaredField("ess");
		f.setAccessible(true);
		f.set(sut, dummyEss);
		f.setAccessible(false);

		// Set battery
		f = sut.getClass().getDeclaredField("battery");
		f.setAccessible(true);
		f.set(sut, dummyBattery);
		f.setAccessible(false);

		// Set config
		Config config = getConfig();
		@SuppressWarnings("rawtypes")
		Class[] argTypes = new Class[] { Config.class };
		Method m = sut.getClass().getDeclaredMethod("writeDataFromConfigIntoFields", argTypes);
		m.setAccessible(true);
		m.invoke(sut, config);
		m.setAccessible(false);

		// Set status
		@SuppressWarnings("rawtypes")
		Class[] argTypes2 = new Class[] { State.class };
		Method m2 = sut.getClass().getDeclaredMethod("setStatus", argTypes2);
		m2.setAccessible(true);
		m2.invoke(sut, State.INITIALIZING);
		m2.setAccessible(false);

		assertEquals(State.INITIALIZING, sut.getStatus());
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testChangeStatus() {
		sut.run();
		dummyBattery.getVoltage().setNextValue(700);
		sut.run();
		assertEquals(State.INITIALIZING, sut.getStatus());
		dummyBattery.getSoc().setNextValue(10);
		sut.run();
		assertEquals(State.INITIALIZING, sut.getStatus());
		dummyBattery.getMinCellVoltage().setNextValue(3000);
		sut.run();
		assertEquals(State.NORMAL, sut.getStatus());
		dummyBattery.getVoltage().setNextValue(600);
		sut.run();
		assertEquals(State.CHARGING, sut.getStatus());
		dummyBattery.getVoltage().setNextValue(700);
		sut.run();
		assertEquals(State.NORMAL, sut.getStatus());
		dummyBattery.getMinCellVoltage().setNextValue(2700);
		sut.run();
		assertEquals(State.CHARGING, sut.getStatus());
		dummyBattery.getMinCellVoltage().setNextValue(3000);
		sut.run();
		assertEquals(State.NORMAL, sut.getStatus());
		dummyBattery.getSoc().setNextValue(2);
		sut.run();
		assertEquals(State.CHARGING, sut.getStatus());
		dummyBattery.getSoc().setNextValue(10);
		sut.run();
		assertEquals(State.NORMAL, sut.getStatus());
		dummyBattery.getMinCellVoltage().setNextValue(2830);
		sut.run();
		assertEquals(State.PENDING, sut.getStatus());
		dummyBattery.getMinCellVoltage().setNextValue(3000);
		sut.run();
		assertEquals(State.NORMAL, sut.getStatus());
		dummyBattery.getMinCellVoltage().setNextValue(2830);
		sut.run();
		assertEquals(State.PENDING, sut.getStatus());
		try {
			Thread.sleep(TIME_WHILE_PENDING * 1000 + 1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		sut.run();
		assertEquals(State.CHARGING, sut.getStatus());
		dummyBattery.getMinCellVoltage().setNextValue(3000);
		sut.run();
		assertEquals(State.NORMAL, sut.getStatus());
		sut.run();
		assertEquals(State.NORMAL, sut.getStatus());
	}

	@Test
	public void testAreValuesPresent() {
		sut.run();
		dummyBattery.getVoltage().setNextValue(700);
		sut.run();
		assertEquals(State.INITIALIZING, sut.getStatus());
		dummyBattery.getSoc().setNextValue(10);
		sut.run();
		assertEquals(State.INITIALIZING, sut.getStatus());
		dummyBattery.getMinCellVoltage().setNextValue(3000);
		sut.run();
		assertEquals(State.NORMAL, sut.getStatus());
	}

	@Test
	public void testActivateComponentContextConfig() {
		// not possible currently
	}

	private Config getConfig() {
		Config config = new Config() {

			@Override
			public Class<? extends Annotation> annotationType() {
				return null;
			}

			@Override
			public String webconsole_configurationFactory_nameHint() {
				return "";
			}

			@Override
			public int timeSpan() {
				return TIME_WHILE_PENDING;
			}

			@Override
			public float secondCellVoltageLimit() {
				return 2.8f;
			}

			@Override
			public int minimalTotalVoltage() {
				return 675;
			}

			@Override
			public int minSoc() {
				return 3;
			}

			@Override
			public String id() {
				return "";
			}

			@Override
			public float firstCellVoltageLimit() {
				return 2.85f;
			}

			@Override
			public String ess_id() {
				return "ess0";
			}

			@Override
			public boolean enabled() {
				return true;
			}

			@Override
			public String battery_id() {
				return "bms0";
			}

			@Override
			public int chargeSoc() {
				return 5;
			}
		};
		return config;
	}

	private ManagedSymmetricEss getDummyEss() {
		ManagedSymmetricEss ess = new EssDummy();
		return ess;
	}

	private Battery getDummyBattery() {
		return new BatteryDummy();
	}

	private static class EssDummy extends AbstractOpenemsComponent implements ManagedSymmetricEss {

		@Override
		public Power getPower() {
			return new Power() {

				@Override
				public void removeConstraint(Constraint constraint) {

				}

				@Override
				public int getMinPower(ManagedSymmetricEss ess, Phase phase, Pwr pwr) {
					return 0;
				}

				@Override
				public int getMaxPower(ManagedSymmetricEss ess, Phase phase, Pwr pwr) {
					return 0;
				}

				@Override
				public Coefficient getCoefficient(ManagedSymmetricEss ess, Phase phase, Pwr pwr) {
					return null;
				}

				@Override
				public Constraint createSimpleConstraint(String description, ManagedSymmetricEss ess, Phase phase,
						Pwr pwr, Relationship relationship, double value) {
					return null;
				}

				@Override
				public Constraint addConstraintAndValidate(Constraint constraint) throws PowerException {
					return null;
				}

				@Override
				public Constraint addConstraint(Constraint constraint) {
					return null;
				}
			};
		}

		@Override
		public void applyPower(int activePower, int reactivePower) {

		}

		@Override
		public int getPowerPrecision() {
			return 1;
		}

	}

	private static class BatteryDummy extends AbstractOpenemsComponent implements Battery {

		private static final Integer CAPACITY_KWH = 50;
		private static final Integer DISCHARGE_MIN_V = 600;
		private static final Integer DISCHARGE_MAX_A = 80;
		private static final Integer CHARGE_MAX_V = 850;
		private static final Integer CHARGE_MAX_A = 80;

		BatteryDummy() {
			initializeChannels(this).forEach(channel -> this.addChannel(channel));
		}

		public Stream<? extends AbstractReadChannel<?>> initializeChannels(BatteryDummy s) {
			// Define the channels. Using streams + switch enables Eclipse IDE to tell us if
			// we are missing an Enum value.
			return Stream.of( //
					Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
						switch (channelId) {
						case STATE:
							return new StateCollectorChannel(s, channelId);
						}
						return null;
					}), Arrays.stream(Battery.ChannelId.values()).map(channelId -> {
						switch (channelId) {
						case SOC:
						case SOH:
						case VOLTAGE:
						case MAX_CELL_TEMPERATURE:
						case MAX_CELL_VOLTAGE:
						case MAX_POWER:
						case MIN_CELL_TEMPERATURE:
						case MIN_CELL_VOLTAGE:
							return new IntegerReadChannel(s, channelId);
						case CHARGE_MAX_CURRENT:
							return new IntegerReadChannel(s, channelId, BatteryDummy.CHARGE_MAX_A);
						case CHARGE_MAX_VOLTAGE:
							return new IntegerReadChannel(s, channelId, BatteryDummy.CHARGE_MAX_V);
						case DISCHARGE_MAX_CURRENT:
							return new IntegerReadChannel(s, channelId, BatteryDummy.DISCHARGE_MAX_A);
						case DISCHARGE_MIN_VOLTAGE:
							return new IntegerReadChannel(s, channelId, BatteryDummy.DISCHARGE_MIN_V);
						case READY_FOR_WORKING:
							return new BooleanReadChannel(s, channelId);
						case CAPACITY:
							return new IntegerReadChannel(s, channelId, BatteryDummy.CAPACITY_KWH);
						case CURRENT:
							break;
						}
						return null;
					})).flatMap(channel -> channel);
		}

	}
}
