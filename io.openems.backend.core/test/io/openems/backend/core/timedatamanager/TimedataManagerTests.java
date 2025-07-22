package io.openems.backend.core.timedatamanager;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ //
		QueryFirstValueBeforeTest.class, //
		QueryHistoricDataTest.class, //
		QueryHistoricEnergyTest.class, //
		QueryHistoricEnergyPerPeriodTest.class //
})
public class TimedataManagerTests {

}
