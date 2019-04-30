package io.openems.edge.controller.dischargelimitconsideringcellvoltage;

import static org.junit.Assert.assertEquals;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.test.DummyBattery;
import io.openems.edge.controller.dischargelimitconsideringcellvoltage.DischargeLimitConsideringCellVoltage.State;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

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
	public void testChangeStatus() throws OpenemsNamedException {
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
	public void testAreValuesPresent() throws OpenemsNamedException {
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
			public String alias() {
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
		ManagedSymmetricEss ess = new DummyManagedSymmetricEss("ess0");
		return ess;
	}

	private static final Integer CAPACITY_KWH = 50;
	private static final Integer DISCHARGE_MIN_V = 600;
	private static final Integer DISCHARGE_MAX_A = 80;
	private static final Integer CHARGE_MAX_V = 850;
	private static final Integer CHARGE_MAX_A = 80;

	private Battery getDummyBattery() {
		Battery b = new DummyBattery("bms0");
		b.channel(Battery.ChannelId.CHARGE_MAX_CURRENT).setNextValue(CHARGE_MAX_A);
		b.channel(Battery.ChannelId.CHARGE_MAX_VOLTAGE).setNextValue(CHARGE_MAX_V);
		b.channel(Battery.ChannelId.DISCHARGE_MAX_CURRENT).setNextValue(DISCHARGE_MAX_A);
		b.channel(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE).setNextValue(DISCHARGE_MIN_V);
		b.channel(Battery.ChannelId.CAPACITY).setNextValue(CAPACITY_KWH);
		return b;
	}

}
