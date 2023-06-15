package io.openems.edge.controller.highloadtimeslot;

import org.junit.Test;

import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;

public class ControllerHighLoadTimeslotImplTest {

	private static final String CTRL_ID = "ctrl0";
	private static final String ESS_ID = "ess0";

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerHighLoadTimeslotImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEss(ESS_ID).setHysteresisSoc(90) //
						.setChargePower(10000) //
						.setDischargePower(20000) //
						.setStartDate("01.01.2019") //
						.setEndDate("01.01.2020") //
						.setStartTime("08:00") //
						.setEndTime("13:00") //
						.setWeekdayFilter(WeekdayFilter.EVERDAY) //
						.build()); //
		;
	}
}
