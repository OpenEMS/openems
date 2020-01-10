package io.openems.edge.controller.timeslotpeakshaving;

import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.test.AbstractComponentConfig;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.meter.test.DummySymmetricMeter;

public class ControllerTimeslotPeakshavingTest {
	
	@SuppressWarnings("all")
	private static class MyConfig extends AbstractComponentConfig implements Config {

		private final String essId;
		private final String meterId;
		private final int peakShavingPower;
		private final int rechargePower;
		private final int chargePower;
		private final String startDate;
		private final String endDate;
		private final String startTime;
		private final String endTime;
		private final String slowStartTime;
		private final WeekdayFilter weekdayFilter;
		private final int hysteresisSoc;

		public MyConfig(String id, String essId, String meterId, int peakShavingPower, int rechargePower, int chargePower,
				String startDate, String endDate, String startTime, String endTime, String slowStartTime, WeekdayFilter weekdayFilter,
				int hysteresisSoc) {
			super(Config.class, id);
			this.essId = essId;
			this.meterId = meterId;
			this.peakShavingPower = peakShavingPower;
			this.rechargePower = rechargePower;
			this.chargePower = chargePower;
			this.startDate = startDate;
			this.endDate = endDate;
			this.startTime = startTime;
			this.slowStartTime = slowStartTime;
			this.endTime = endTime;
			this.weekdayFilter = weekdayFilter;
			this.hysteresisSoc = hysteresisSoc;

		}

		@Override
		public String ess() {
			return this.essId;
		}

		@Override
		public String meter_id() {
			return this.meterId;
		}

		@Override
		public String startDate() {
			return this.startDate;
		}

		@Override
		public String endDate() {
			return this.endDate;
		}

		@Override
		public String startTime() {
			return this.startTime;
		}

		@Override
		public String endTime() {
			return this.endTime;
		}

		@Override
		public WeekdayFilter weekdayFilter() {
			return this.weekdayFilter;
		}

		@Override
		public int peakShavingPower() {
			return this.peakShavingPower;
		}

		@Override
		public int rechargePower() {
			return this.rechargePower;
		}

		@Override
		public int hysteresisSoc() {
			return this.hysteresisSoc;
		}

		@Override
		public int chargePower() {
			return this.chargePower;
		}

		@Override
		public String slowStartTime() {
			return this.slowStartTime;
		}
	}

	@Test
	public void test() throws Exception {
		TimeLeapClock clock = new TimeLeapClock(ZoneOffset.UTC);
		// initialize the controller
		TimeslotPeakshaving ctrl = new TimeslotPeakshaving(clock);
		// Add referenced services
		DummyComponentManager componentManager = new DummyComponentManager();
		ctrl.componentManager = componentManager;

		DummyPower power = new DummyPower(0.5, 0.2, 0.1);
		ctrl.power = power;

		MyConfig config = new MyConfig("ctrl0", "ess0", "meter0", 100000, 50000, 50000, "18.11.2019", "18.11.2019", "10:00",
				"11:00", "04:00", WeekdayFilter.ONLY_WEEKDAYS, 50);
		
		ctrl.activate(null, config);
		ctrl.activate(null, config);
		
		
		ChannelAddress gridMode = new ChannelAddress("ess0", "GridMode");
		ChannelAddress ess = new ChannelAddress("ess0", "ActivePower");
		ChannelAddress grid = new ChannelAddress("meter0", "ActivePower");
		ChannelAddress essSetPower = new ChannelAddress("ess0", "SetActivePowerEquals");

		ManagedSymmetricEss ess0 = new DummyManagedSymmetricEss("ess0");
		SymmetricMeter meter = new DummySymmetricMeter("meter0");


		new ControllerTest(ctrl, componentManager, ess0, meter).next(new TestCase() //
				//.timeleap(clock, 15, ChronoUnit.MINUTES)//
				.input(gridMode, GridMode.ON_GRID) //
				.input(ess, 0) //
				.input(grid, 120000) //
				.output(essSetPower, 50000)) //
		.next(new TestCase() //
				.input(gridMode, GridMode.ON_GRID) //
				.input(ess, 5000) //
				.input(grid, 120000) //
				.output(essSetPower, 50000)) //
		.next(new TestCase() //
				.timeleap(clock, 75, ChronoUnit.MINUTES)//
				.input(gridMode, GridMode.ON_GRID) //
				.input(ess, 5000) //
				.input(grid, 120000) //
				.output(essSetPower, 10000)) //
		.next(new TestCase() //
				.timeleap(clock, 75, ChronoUnit.MINUTES)//
				.input(gridMode, GridMode.ON_GRID) //
				.input(ess, 5000) //
				.input(grid, 120000) //
				.output(essSetPower, 10000)) //
				.run();		
	}
}
