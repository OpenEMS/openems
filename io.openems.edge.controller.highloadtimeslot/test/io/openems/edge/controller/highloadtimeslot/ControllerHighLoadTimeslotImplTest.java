package io.openems.edge.controller.highloadtimeslot;

import static io.openems.edge.controller.highloadtimeslot.WeekdayFilter.EVERDAY;

import org.junit.Test;

import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;

public class ControllerHighLoadTimeslotImplTest {

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerHighLoadTimeslotImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEss("ess0") //
						.setHysteresisSoc(90) //
						.setChargePower(10000) //
						.setDischargePower(20000) //
						.setStartDate("01.01.2019") //
						.setEndDate("01.01.2020") //
						.setStartTime("08:00") //
						.setEndTime("13:00") //
						.setWeekdayFilter(EVERDAY) //
						.build()) //
				.deactivate();
	}
}
